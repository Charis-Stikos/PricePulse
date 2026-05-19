package com.pricepulse.viewmodel;

import android.net.Uri;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.firestore.ListenerRegistration;
import com.pricepulse.admin.AdminSession;
import com.pricepulse.model.Order;
import com.pricepulse.model.Product;
import com.pricepulse.model.User;
import com.pricepulse.repository.FirebaseRepository;

import java.util.ArrayList;
import java.util.List;

public class AdminViewModel extends ViewModel {

    public enum AdminEvent {
        ORDER_STATUS_UPDATED, ORDER_STATUS_UPDATE_FAILED,
        PRODUCT_ADDED, PRODUCT_ADD_FAILED,
        IMAGE_UPLOAD_FAILED,
        ADMIN_PROMOTED, ADMIN_REVOKED,
        ADMIN_USER_NOT_FOUND, ADMIN_ALREADY_ADMIN, ADMIN_CANNOT_REVOKE_SELF,
        ADMIN_ACTION_FAILED
    }

    private final FirebaseRepository repository = new FirebaseRepository();

    private final MutableLiveData<List<Order>> orders = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> ordersLoading = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> productSaving = new MutableLiveData<>(false);
    private final MutableLiveData<List<User>> admins = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> adminActionLoading = new MutableLiveData<>(false);
    private final SingleLiveEvent<AdminEvent> events = new SingleLiveEvent<>();

    private ListenerRegistration ordersRegistration;
    private ListenerRegistration adminsRegistration;

    public LiveData<List<Order>> getOrders() { return orders; }
    public LiveData<Boolean> getOrdersLoading() { return ordersLoading; }
    public LiveData<Boolean> getProductSaving() { return productSaving; }
    public LiveData<List<User>> getAdmins() { return admins; }
    public LiveData<Boolean> getAdminActionLoading() { return adminActionLoading; }
    public LiveData<AdminEvent> getEvents() { return events; }

    public void loadOrders() {
        if (ordersRegistration != null) return;
        ordersLoading.setValue(true);
        ordersRegistration = repository.listenToAllOrders(result -> {
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

    @Override
    protected void onCleared() {
        super.onCleared();
        if (ordersRegistration != null) { ordersRegistration.remove(); ordersRegistration = null; }
        if (adminsRegistration != null) { adminsRegistration.remove(); adminsRegistration = null; }
    }
}
