package com.pricepulse.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseUser;
import com.pricepulse.auth.AuthManager;
import com.pricepulse.model.Order;
import com.pricepulse.model.Product;
import com.pricepulse.model.User;
import com.pricepulse.repository.FirebaseRepository;

import java.util.ArrayList;
import java.util.List;

public class ProfileViewModel extends ViewModel {

    private final FirebaseRepository repository = new FirebaseRepository();
    private final AuthManager authManager = new AuthManager();

    private final MutableLiveData<ProfileTabState> tabState = new MutableLiveData<>(ProfileTabState.MAIN);
    private final MutableLiveData<List<Product>> wishlist = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Order>> orderHistory = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<User> userProfile = new MutableLiveData<>();
    private final SingleLiveEvent<SettingsEvent> settingsEvents = new SingleLiveEvent<>();

    private final MediatorLiveData<FirebaseUser> userBridge = new MediatorLiveData<>();

    public ProfileViewModel() {
        userBridge.addSource(authManager.getCurrentUser(), new Observer<FirebaseUser>() {
            @Override
            public void onChanged(FirebaseUser user) {
                userBridge.setValue(user);
                if (user != null && !user.isAnonymous()) {
                    fetchUserProfile(user.getUid());
                } else {
                    userProfile.setValue(null);
                }
            }
        });
        userBridge.observeForever(u -> { /* keep MediatorLiveData active */ });
    }

    public AuthManager getAuthManager() {
        return authManager;
    }

    public LiveData<ProfileTabState> getTabState() {
        return tabState;
    }

    public LiveData<List<Product>> getWishlist() {
        return wishlist;
    }

    public LiveData<List<Order>> getOrderHistory() {
        return orderHistory;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<User> getUserProfile() {
        return userProfile;
    }

    public LiveData<SettingsEvent> getSettingsEvents() {
        return settingsEvents;
    }

    public void setTab(ProfileTabState tab) {
        tabState.setValue(tab);
        if (tab == ProfileTabState.WISHLIST) {
            fetchWishlist();
        } else if (tab == ProfileTabState.ORDERS) {
            fetchOrders();
        }
    }

    private void fetchUserProfile(String uid) {
        repository.getUserProfile(uid, userProfile::setValue);
    }

    public void updateDisplayName(String name) {
        FirebaseUser currentUser = authManager.getCurrentUserValue();
        if (currentUser == null || currentUser.isAnonymous()) {
            settingsEvents.setValue(SettingsEvent.UPDATE_FAILED);
            return;
        }
        isLoading.setValue(true);

        User existing = userProfile.getValue();
        User base = existing != null ? existing : new User(
                currentUser.getUid(),
                currentUser.getEmail() != null ? currentUser.getEmail() : "",
                "",
                new ArrayList<>()
        );
        User updated = new User(
                base.getUid().isEmpty() ? currentUser.getUid() : base.getUid(),
                base.getEmail(),
                name,
                base.getLikedProductIds()
        );

        repository.updateUserProfile(updated, success -> {
            if (success) {
                userProfile.setValue(updated);
            }
            isLoading.setValue(false);
            settingsEvents.setValue(success ? SettingsEvent.UPDATE_SUCCEEDED : SettingsEvent.UPDATE_FAILED);
        });
    }

    private void fetchWishlist() {
        FirebaseUser user = authManager.getCurrentUserValue();
        if (user == null || user.isAnonymous()) {
            wishlist.setValue(new ArrayList<>());
            return;
        }
        isLoading.setValue(true);
        repository.getUserProfile(user.getUid(), profile -> {
            List<String> ids = profile != null && profile.getLikedProductIds() != null
                    ? profile.getLikedProductIds()
                    : new ArrayList<>();
            if (ids.isEmpty()) {
                wishlist.setValue(new ArrayList<>());
                isLoading.setValue(false);
                return;
            }
            repository.getProductsByIds(ids, products -> {
                wishlist.setValue(products);
                isLoading.setValue(false);
            });
        });
    }

    private void fetchOrders() {
        FirebaseUser user = authManager.getCurrentUserValue();
        if (user == null || user.isAnonymous()) {
            orderHistory.setValue(new ArrayList<>());
            return;
        }
        isLoading.setValue(true);
        repository.getUserOrders(user.getUid(), orders -> {
            orderHistory.setValue(orders);
            isLoading.setValue(false);
        });
    }
}
