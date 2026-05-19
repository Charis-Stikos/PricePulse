package com.pricepulse.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.ListenerRegistration;
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

    private ListenerRegistration profileListener;
    private ListenerRegistration ordersListener;
    private List<String> lastLikedIds = new ArrayList<>();

    private final FirebaseAuth.AuthStateListener authStateListener = auth -> attach(auth.getCurrentUser());

    public ProfileViewModel() {
        FirebaseAuth fa = FirebaseAuth.getInstance();
        attach(fa.getCurrentUser());
        fa.addAuthStateListener(authStateListener);
    }

    private void attach(FirebaseUser user) {
        detach();
        if (user == null || user.isAnonymous()) {
            userProfile.postValue(null);
            wishlist.postValue(new ArrayList<>());
            orderHistory.postValue(new ArrayList<>());
            return;
        }
        String uid = user.getUid();

        profileListener = repository.listenToUserProfile(uid, profile -> {
            userProfile.postValue(profile);
            List<String> ids = profile != null && profile.getLikedProductIds() != null
                    ? new ArrayList<>(profile.getLikedProductIds())
                    : new ArrayList<>();
            if (!ids.equals(lastLikedIds)) {
                lastLikedIds = ids;
                reloadWishlistProducts(ids);
            }
        });

        ordersListener = repository.listenToUserOrders(uid, orderHistory::postValue);
    }

    private void detach() {
        if (profileListener != null) { profileListener.remove(); profileListener = null; }
        if (ordersListener != null) { ordersListener.remove(); ordersListener = null; }
        lastLikedIds = new ArrayList<>();
    }

    public AuthManager getAuthManager() {
        return authManager;
    }

    public LiveData<ProfileTabState> getTabState() { return tabState; }
    public LiveData<List<Product>> getWishlist() { return wishlist; }
    public LiveData<List<Order>> getOrderHistory() { return orderHistory; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<User> getUserProfile() { return userProfile; }
    public LiveData<SettingsEvent> getSettingsEvents() { return settingsEvents; }

    public void setTab(ProfileTabState tab) {
        tabState.setValue(tab);
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

        authManager.updateDisplayName(name, (authSuccess, authError) ->
                repository.updateUserProfile(updated, dbSuccess -> {
                    boolean success = authSuccess && dbSuccess;
                    isLoading.setValue(false);
                    settingsEvents.setValue(success ? SettingsEvent.UPDATE_SUCCEEDED : SettingsEvent.UPDATE_FAILED);
                }));
    }

    public void updateEmail(String currentPassword, String newEmail) {
        FirebaseUser currentUser = authManager.getCurrentUserValue();
        if (currentUser == null || currentUser.isAnonymous()) {
            settingsEvents.setValue(SettingsEvent.EMAIL_UPDATE_FAILED);
            return;
        }
        isLoading.setValue(true);
        authManager.changeEmail(currentPassword, newEmail, (success, error) -> {
            isLoading.setValue(false);
            if (success) {
                User existing = userProfile.getValue();
                if (existing != null) {
                    User updated = new User(
                            existing.getUid(),
                            newEmail,
                            existing.getDisplayName(),
                            existing.getLikedProductIds()
                    );
                    repository.updateUserProfile(updated, ok -> { /* ο listener θα κανει update μονος του */ });
                }
                settingsEvents.setValue(SettingsEvent.EMAIL_UPDATED);
            } else {
                settingsEvents.setValue(SettingsEvent.EMAIL_UPDATE_FAILED);
            }
        });
    }

    public void updatePassword(String currentPassword, String newPassword) {
        FirebaseUser currentUser = authManager.getCurrentUserValue();
        if (currentUser == null || currentUser.isAnonymous()) {
            settingsEvents.setValue(SettingsEvent.PASSWORD_UPDATE_FAILED);
            return;
        }
        isLoading.setValue(true);
        authManager.changePassword(currentPassword, newPassword, (success, error) -> {
            isLoading.setValue(false);
            settingsEvents.setValue(success ? SettingsEvent.PASSWORD_UPDATED : SettingsEvent.PASSWORD_UPDATE_FAILED);
        });
    }

    private void reloadWishlistProducts(List<String> ids) {
        if (ids.isEmpty()) {
            wishlist.postValue(new ArrayList<>());
            return;
        }
        repository.getProductsByIds(ids, wishlist::postValue);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        FirebaseAuth.getInstance().removeAuthStateListener(authStateListener);
        detach();
    }
}
