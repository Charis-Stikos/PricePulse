package com.pricepulse.viewmodel;

import android.net.Uri;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.firestore.ListenerRegistration;
import com.pricepulse.admin.AdminSession;
import com.pricepulse.model.Order;
import com.pricepulse.model.Product;
import com.pricepulse.model.Shop;
import com.pricepulse.model.User;
import com.pricepulse.repository.FirebaseRepository;

import java.util.Calendar;

import java.util.ArrayList;
import java.util.List;

public class AdminViewModel extends ViewModel {

    public enum AdminEvent {
        ORDER_STATUS_UPDATED, ORDER_STATUS_UPDATE_FAILED,
        PRODUCT_ADDED, PRODUCT_ADD_FAILED,
        IMAGE_UPLOAD_FAILED,
        ADMIN_PROMOTED, ADMIN_REVOKED,
        ADMIN_USER_NOT_FOUND, ADMIN_ALREADY_ADMIN, ADMIN_CANNOT_REVOKE_SELF,
        ADMIN_ACTION_FAILED,
        SHOP_OWNER_PROMOTED, SHOP_OWNER_REVOKED,
        SHOP_OWNER_ALREADY_OWNER,
        SHOP_SAVED, SHOP_SAVE_FAILED
    }

    // για τωρα κάθε νέος shop owner ανατιθεται στο default-shop. Στο επομενο phase θα το διαλεγει.
    private static final String DEFAULT_SHOP_ID = "default-shop";

    private final FirebaseRepository repository = new FirebaseRepository();

    private final MutableLiveData<List<Order>> orders = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> ordersLoading = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> productSaving = new MutableLiveData<>(false);
    private final MutableLiveData<List<User>> admins = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<User>> shopOwners = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> adminActionLoading = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> shopOwnerActionLoading = new MutableLiveData<>(false);
    private final SingleLiveEvent<AdminEvent> events = new SingleLiveEvent<>();

    private ListenerRegistration ordersRegistration;
    private ListenerRegistration adminsRegistration;
    private ListenerRegistration shopOwnersRegistration;

    // overview state
    public static final class ShopOverview {
        public final Shop shop;
        public final int productCount;
        public final int pendingOrderCount;
        public final double monthlyRevenue;
        public final double rating;
        public ShopOverview(Shop shop, int productCount, int pendingOrderCount,
                            double monthlyRevenue, double rating) {
            this.shop = shop;
            this.productCount = productCount;
            this.pendingOrderCount = pendingOrderCount;
            this.monthlyRevenue = monthlyRevenue;
            this.rating = rating;
        }
    }

    public static final class PlatformOverview {
        public final int shopCount;
        public final int productCount;
        public final int pendingOrderCount;
        public final double monthlyRevenue;
        public PlatformOverview(int shopCount, int productCount, int pendingOrderCount,
                                double monthlyRevenue) {
            this.shopCount = shopCount;
            this.productCount = productCount;
            this.pendingOrderCount = pendingOrderCount;
            this.monthlyRevenue = monthlyRevenue;
        }
    }

    private final MutableLiveData<ShopOverview> shopOverview = new MutableLiveData<>();
    private final MutableLiveData<PlatformOverview> platformOverview = new MutableLiveData<>();
    private final MutableLiveData<List<Shop>> allShops = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> shopSaving = new MutableLiveData<>(false);

    private ListenerRegistration ownedShopRegistration;
    private ListenerRegistration ownedShopProductsRegistration;
    private ListenerRegistration ownedShopOrdersRegistration;
    private ListenerRegistration allShopsRegistration;
    private ListenerRegistration allProductsRegistration;
    private ListenerRegistration allOrdersRegistration;

    // last seen values για να συνθεσω τα ShopOverview / PlatformOverview snapshots
    private Shop lastOwnedShop;
    private int lastOwnedProductCount;
    private int lastOwnedPendingCount;
    private double lastOwnedMonthlyRevenue;
    private int lastShopCount;
    private int lastAllProductCount;
    private int lastAllPendingCount;
    private double lastAllMonthlyRevenue;

    public LiveData<List<Order>> getOrders() { return orders; }
    public LiveData<Boolean> getOrdersLoading() { return ordersLoading; }
    public LiveData<Boolean> getProductSaving() { return productSaving; }
    public LiveData<List<User>> getAdmins() { return admins; }
    public LiveData<List<User>> getShopOwners() { return shopOwners; }
    public LiveData<Boolean> getAdminActionLoading() { return adminActionLoading; }
    public LiveData<Boolean> getShopOwnerActionLoading() { return shopOwnerActionLoading; }
    public LiveData<AdminEvent> getEvents() { return events; }

    public void loadOrders() {
        if (ordersRegistration != null) return;
        ordersLoading.setValue(true);
        ordersRegistration = repository.listenToAllOrders(result -> {
            ordersLoading.setValue(false);
            orders.setValue(result);
        });
    }

    public void loadShopOrders(String shopId) {
        if (ordersRegistration != null) return;
        if (shopId == null || shopId.isEmpty()) {
            ordersLoading.setValue(false);
            orders.setValue(new ArrayList<>());
            return;
        }
        ordersLoading.setValue(true);
        ordersRegistration = repository.listenToOrdersByShop(shopId, result -> {
            ordersLoading.setValue(false);
            orders.setValue(result);
        });
    }

    public void updateOrderStatus(Order order, String status) {
        repository.updateOrderStatus(order.getId(), status, success ->
                events.setValue(success
                        ? AdminEvent.ORDER_STATUS_UPDATED
                        : AdminEvent.ORDER_STATUS_UPDATE_FAILED));
    }

    public void addProduct(Product product, Uri imageUri) {
        productSaving.setValue(true);
        String shopId = AdminSession.getInstance().ownedShopId().getValue();
        if (shopId == null || shopId.isEmpty()) {
            productSaving.setValue(false);
            events.setValue(AdminEvent.PRODUCT_ADD_FAILED);
            return;
        }
        product.setShopId(shopId);

        // πρωτα φερνουμε το shop για να γραψουμε το shopName denormalized,
        // ωστε ο ProductAdapter να δειχνει το shop χωρις extra fetch.
        repository.getShop(shopId, shop -> {
            if (shop != null) {
                product.setShopName(shop.getName());
                product.setShopActive(shop.isActive());
            }
            if (imageUri != null) {
                repository.uploadProductImage(imageUri, url -> {
                    if (url == null) {
                        productSaving.setValue(false);
                        events.setValue(AdminEvent.IMAGE_UPLOAD_FAILED);
                        return;
                    }
                    product.setImageUrl(url);
                    saveProductInternal(product);
                });
            } else {
                saveProductInternal(product);
            }
        });
    }

    private void saveProductInternal(Product product) {
        repository.addProduct(product, success -> {
            productSaving.setValue(false);
            events.setValue(success ? AdminEvent.PRODUCT_ADDED : AdminEvent.PRODUCT_ADD_FAILED);
        });
    }

    public void loadAdmins() {
        if (adminsRegistration != null) return;
        adminsRegistration = repository.listenToAdmins(admins::setValue);
    }

    public void promoteAdminByEmail(String email) {
        String normalized = email.trim().toLowerCase();
        adminActionLoading.setValue(true);
        repository.findUserByEmail(normalized, user -> {
            if (user == null) {
                adminActionLoading.setValue(false);
                events.setValue(AdminEvent.ADMIN_USER_NOT_FOUND);
                return;
            }
            if (user.isAdmin()) {
                adminActionLoading.setValue(false);
                events.setValue(AdminEvent.ADMIN_ALREADY_ADMIN);
                return;
            }
            repository.setUserAdmin(user.getUid(), true, success -> {
                adminActionLoading.setValue(false);
                if (success) {
                    events.setValue(AdminEvent.ADMIN_PROMOTED);
                    AdminSession.getInstance().refresh();
                } else {
                    events.setValue(AdminEvent.ADMIN_ACTION_FAILED);
                }
            });
        });
    }

    public void revokeAdmin(User user, String currentUserUid) {
        if (currentUserUid != null && currentUserUid.equals(user.getUid())) {
            events.setValue(AdminEvent.ADMIN_CANNOT_REVOKE_SELF);
            return;
        }
        adminActionLoading.setValue(true);
        repository.setUserAdmin(user.getUid(), false, success -> {
            adminActionLoading.setValue(false);
            if (success) {
                events.setValue(AdminEvent.ADMIN_REVOKED);
                AdminSession.getInstance().refresh();
            } else {
                events.setValue(AdminEvent.ADMIN_ACTION_FAILED);
            }
        });
    }

    public void loadShopOwners() {
        if (shopOwnersRegistration != null) return;
        shopOwnersRegistration = repository.listenToShopOwners(shopOwners::setValue);
    }

    public void promoteShopOwnerByEmail(String email, String shopId) {
        String normalized = email.trim().toLowerCase();
        String targetShopId = (shopId == null || shopId.isEmpty()) ? DEFAULT_SHOP_ID : shopId;
        shopOwnerActionLoading.setValue(true);
        repository.findUserByEmail(normalized, user -> {
            if (user == null) {
                shopOwnerActionLoading.setValue(false);
                events.setValue(AdminEvent.ADMIN_USER_NOT_FOUND);
                return;
            }
            if (user.isShopOwner()) {
                shopOwnerActionLoading.setValue(false);
                events.setValue(AdminEvent.SHOP_OWNER_ALREADY_OWNER);
                return;
            }
            // ενημερωνουμε user (shopOwner + ownedShopId) ΚΑΙ shop (ownerId + ownerEmail) σε ενα batch
            repository.linkShopOwner(user.getUid(), normalized, targetShopId, success -> {
                shopOwnerActionLoading.setValue(false);
                events.setValue(success
                        ? AdminEvent.SHOP_OWNER_PROMOTED
                        : AdminEvent.ADMIN_ACTION_FAILED);
            });
        });
    }

    public LiveData<ShopOverview> getShopOverview() { return shopOverview; }
    public LiveData<PlatformOverview> getPlatformOverview() { return platformOverview; }

    public void loadShopOverview(String shopId) {
        if (shopId == null || shopId.isEmpty()) return;
        detachShopOverview();

        ownedShopRegistration = repository.listenToShop(shopId, shop -> {
            lastOwnedShop = shop;
            emitShopOverview();
        });
        ownedShopProductsRegistration = repository.listenToProductsByShop(shopId, products -> {
            lastOwnedProductCount = products != null ? products.size() : 0;
            emitShopOverview();
        });
        ownedShopOrdersRegistration = repository.listenToOrdersByShop(shopId, orders -> {
            lastOwnedPendingCount = countPending(orders);
            lastOwnedMonthlyRevenue = sumMonthlyRevenue(orders);
            emitShopOverview();
        });
    }

    public LiveData<List<Shop>> getAllShops() { return allShops; }
    public LiveData<Boolean> getShopSaving() { return shopSaving; }

    public void loadAllShops() {
        if (allShopsRegistration != null) return;
        allShopsRegistration = repository.listenToShops(shops -> {
            lastShopCount = shops != null ? shops.size() : 0;
            allShops.setValue(shops != null ? shops : new ArrayList<>());
            emitPlatformOverview();
        });
    }

    public void saveShop(Shop shop) {
        shopSaving.setValue(true);
        final String newName = shop.getName();
        final boolean newActive = shop.isActive();
        // αν ηδη υπαρχει shop με αυτο το id, μετα το save θα προπαγκαντερουμε shopName + shopActive
        // στα προϊοντα του ωστε home/search να φιλτραρει σωστα
        boolean preExisting = shop.getId() != null && !shop.getId().isEmpty();
        final String shopIdForPropagation = preExisting ? shop.getId() : null;

        repository.saveShop(shop, success -> {
            if (!success) {
                shopSaving.setValue(false);
                events.setValue(AdminEvent.SHOP_SAVE_FAILED);
                return;
            }
            if (shopIdForPropagation != null) {
                repository.propagateShopFields(shopIdForPropagation, newName, newActive, ok -> {
                    shopSaving.setValue(false);
                    events.setValue(AdminEvent.SHOP_SAVED);
                });
            } else {
                shopSaving.setValue(false);
                events.setValue(AdminEvent.SHOP_SAVED);
            }
        });
    }

    public void loadPlatformOverview() {
        if (allShopsRegistration != null) return;
        allShopsRegistration = repository.listenToShops(shops -> {
            lastShopCount = shops != null ? shops.size() : 0;
            allShops.setValue(shops != null ? shops : new ArrayList<>());
            emitPlatformOverview();
        });
        allProductsRegistration = repository.listenToAllProducts(products -> {
            lastAllProductCount = products != null ? products.size() : 0;
            emitPlatformOverview();
        });
        allOrdersRegistration = repository.listenToAllOrders(orders -> {
            lastAllPendingCount = countPending(orders);
            lastAllMonthlyRevenue = sumMonthlyRevenue(orders);
            emitPlatformOverview();
        });
    }

    private void emitShopOverview() {
        shopOverview.setValue(new ShopOverview(
                lastOwnedShop,
                lastOwnedProductCount,
                lastOwnedPendingCount,
                lastOwnedMonthlyRevenue,
                lastOwnedShop != null ? lastOwnedShop.getRating() : 0.0));
    }

    private void emitPlatformOverview() {
        platformOverview.setValue(new PlatformOverview(
                lastShopCount, lastAllProductCount, lastAllPendingCount, lastAllMonthlyRevenue));
    }

    private static int countPending(java.util.List<Order> orders) {
        if (orders == null) return 0;
        int n = 0;
        for (Order o : orders) {
            if ("Pending".equals(o.getStatus())) n++;
        }
        return n;
    }

    private static double sumMonthlyRevenue(java.util.List<Order> orders) {
        if (orders == null) return 0.0;
        Calendar now = Calendar.getInstance();
        int year = now.get(Calendar.YEAR);
        int month = now.get(Calendar.MONTH);
        double total = 0.0;
        Calendar c = Calendar.getInstance();
        for (Order o : orders) {
            c.setTimeInMillis(o.getTimestamp());
            if (c.get(Calendar.YEAR) == year && c.get(Calendar.MONTH) == month) {
                double amount = o.getFinalTotalAmount() > 0 ? o.getFinalTotalAmount() : o.getTotalAmount();
                total += amount;
            }
        }
        return total;
    }

    private void detachShopOverview() {
        if (ownedShopRegistration != null) { ownedShopRegistration.remove(); ownedShopRegistration = null; }
        if (ownedShopProductsRegistration != null) { ownedShopProductsRegistration.remove(); ownedShopProductsRegistration = null; }
        if (ownedShopOrdersRegistration != null) { ownedShopOrdersRegistration.remove(); ownedShopOrdersRegistration = null; }
        lastOwnedShop = null;
        lastOwnedProductCount = 0;
        lastOwnedPendingCount = 0;
        lastOwnedMonthlyRevenue = 0.0;
    }

    public void revokeShopOwner(User user) {
        shopOwnerActionLoading.setValue(true);
        // καθαριζουμε user (shopOwner false + ownedShopId "") ΚΑΙ shop (ownerId "" + ownerEmail "")
        repository.unlinkShopOwner(user.getUid(), user.getOwnedShopId(), success -> {
            shopOwnerActionLoading.setValue(false);
            events.setValue(success
                    ? AdminEvent.SHOP_OWNER_REVOKED
                    : AdminEvent.ADMIN_ACTION_FAILED);
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (ordersRegistration != null) { ordersRegistration.remove(); ordersRegistration = null; }
        if (adminsRegistration != null) { adminsRegistration.remove(); adminsRegistration = null; }
        if (shopOwnersRegistration != null) { shopOwnersRegistration.remove(); shopOwnersRegistration = null; }
        detachShopOverview();
        if (allShopsRegistration != null) { allShopsRegistration.remove(); allShopsRegistration = null; }
        if (allProductsRegistration != null) { allProductsRegistration.remove(); allProductsRegistration = null; }
        if (allOrdersRegistration != null) { allOrdersRegistration.remove(); allOrdersRegistration = null; }
    }
}
