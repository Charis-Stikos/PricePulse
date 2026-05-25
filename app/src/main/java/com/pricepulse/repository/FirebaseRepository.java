package com.pricepulse.repository;

import android.net.Uri;

import com.google.firebase.firestore.ListenerRegistration;
import com.pricepulse.model.Address;
import com.pricepulse.model.Order;
import com.pricepulse.model.Product;
import com.pricepulse.model.Review;
import com.pricepulse.model.Shop;
import com.pricepulse.model.User;

import java.util.List;

// Facade πανω απο τα per-domain repositories (Product/Order/User/Address/Shop).
// Κραταει ενιαιο API για τα ViewModels· η πραγματικη λογικη ειναι στα επιμερους repositories.
public class FirebaseRepository {

    private final ProductRepository products = new ProductRepository();
    private final OrderRepository orders = new OrderRepository();
    private final UserRepository users = new UserRepository();
    private final AddressRepository addresses = new AddressRepository();
    private final ShopRepository shops = new ShopRepository();

    // ----- Προϊόντα -----

    public ListenerRegistration listenToProductsByCategory(String category, long limit,
                                                           RepoCallback<List<Product>> callback) {
        return products.listenToProductsByCategory(category, limit, callback);
    }

    public ListenerRegistration listenToProduct(String productId, RepoCallback<Product> callback) {
        return products.listenToProduct(productId, callback);
    }

    public ListenerRegistration listenToAllProducts(RepoCallback<List<Product>> callback) {
        return products.listenToAllProducts(callback);
    }

    public ListenerRegistration listenToProductsByShop(String shopId, RepoCallback<List<Product>> callback) {
        return products.listenToProductsByShop(shopId, callback);
    }

    public void getProductsByIds(List<String> ids, RepoCallback<List<Product>> callback) {
        products.getProductsByIds(ids, callback);
    }

    public void searchProducts(String query, RepoCallback<List<Product>> callback) {
        products.searchProducts(query, callback);
    }

    public void addProduct(Product product, RepoCallback<Boolean> callback) {
        products.addProduct(product, callback);
    }

    public void uploadProductImage(Uri localUri, RepoCallback<String> callback) {
        products.uploadProductImage(localUri, callback);
    }

    public void submitReview(String productId, Review review, RepoCallback<Boolean> callback) {
        products.submitReview(productId, review, callback);
    }

    public void propagateShopFields(String shopId, String newName, Boolean newActive,
                                    RepoCallback<Boolean> callback) {
        products.propagateShopFields(shopId, newName, newActive, callback);
    }

    // ----- Παραγγελίες -----

    public void saveOrder(Order order, RepoCallback<Boolean> callback) {
        orders.saveOrder(order, callback);
    }

    public ListenerRegistration listenToUserOrders(String uid, RepoCallback<List<Order>> callback) {
        return orders.listenToUserOrders(uid, callback);
    }

    public ListenerRegistration listenToAllOrders(RepoCallback<List<Order>> callback) {
        return orders.listenToAllOrders(callback);
    }

    public ListenerRegistration listenToOrdersByShop(String shopId, RepoCallback<List<Order>> callback) {
        return orders.listenToOrdersByShop(shopId, callback);
    }

    public void updateOrderStatus(String orderId, String status, RepoCallback<Boolean> callback) {
        orders.updateOrderStatus(orderId, status, callback);
    }

    // ----- Χρήστες -----

    public ListenerRegistration listenToUserProfile(String uid, RepoCallback<User> callback) {
        return users.listenToUserProfile(uid, callback);
    }

    public void updateUserProfile(User user, RepoCallback<Boolean> callback) {
        users.updateUserProfile(user, callback);
    }

    public void findUserByEmail(String email, RepoCallback<User> callback) {
        users.findUserByEmail(email, callback);
    }

    public void setUserAdmin(String uid, boolean isAdmin, RepoCallback<Boolean> callback) {
        users.setUserAdmin(uid, isAdmin, callback);
    }

    public ListenerRegistration listenToAdmins(RepoCallback<List<User>> callback) {
        return users.listenToAdmins(callback);
    }

    public ListenerRegistration listenToShopOwners(RepoCallback<List<User>> callback) {
        return users.listenToShopOwners(callback);
    }

    public void updateWishlist(String uid, List<String> productIds, RepoCallback<Boolean> callback) {
        users.updateWishlist(uid, productIds, callback);
    }

    public void linkShopOwner(String uid, String email, String shopId, RepoCallback<Boolean> callback) {
        users.linkShopOwner(uid, email, shopId, callback);
    }

    public void unlinkShopOwner(String uid, String shopId, RepoCallback<Boolean> callback) {
        users.unlinkShopOwner(uid, shopId, callback);
    }

    // ----- Διευθύνσεις -----

    public ListenerRegistration listenToUserAddresses(String uid, RepoCallback<List<Address>> callback) {
        return addresses.listenToUserAddresses(uid, callback);
    }

    public void saveAddress(Address address, RepoCallback<Boolean> callback) {
        addresses.saveAddress(address, callback);
    }

    public void deleteAddress(String addressId, RepoCallback<Boolean> callback) {
        addresses.deleteAddress(addressId, callback);
    }

    public void setDefaultAddress(String uid, String addressId, RepoCallback<Boolean> callback) {
        addresses.setDefaultAddress(uid, addressId, callback);
    }

    // ----- Καταστήματα -----

    public ListenerRegistration listenToShops(RepoCallback<List<Shop>> callback) {
        return shops.listenToShops(callback);
    }

    public ListenerRegistration listenToShop(String shopId, RepoCallback<Shop> callback) {
        return shops.listenToShop(shopId, callback);
    }

    public void getShop(String shopId, RepoCallback<Shop> callback) {
        shops.getShop(shopId, callback);
    }

    public void saveShop(Shop shop, RepoCallback<Boolean> callback) {
        shops.saveShop(shop, callback);
    }
}
