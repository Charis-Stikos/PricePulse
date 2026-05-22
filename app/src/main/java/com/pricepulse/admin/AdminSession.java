package com.pricepulse.admin;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.ListenerRegistration;
import com.pricepulse.model.User;
import com.pricepulse.repository.FirebaseRepository;

import java.util.ArrayList;

// Live αν ο τρέχων χρήστης Είναι admin. Ακούει με snapshot listener
// η seed λίστα τρέχει Μονό αν δεν υπάρχει doc ακόμα (πρώτη φορά login).
public final class AdminSession {

    private static volatile AdminSession instance;

    public static AdminSession getInstance() {
        if (instance == null) {
            synchronized (AdminSession.class) {
                if (instance == null) instance = new AdminSession();
            }
        }
        return instance;
    }

    private final FirebaseRepository repository = new FirebaseRepository();
    private final MutableLiveData<Boolean> isAdmin = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isShopOwner = new MutableLiveData<>(false);
    private final MutableLiveData<String> ownedShopId = new MutableLiveData<>("");
    private final MediatorLiveData<Boolean> canAccessDashboard = new MediatorLiveData<>();

    private ListenerRegistration profileListener;
    private String listenedUid;
    private boolean seededForUid;

    private AdminSession() {
        canAccessDashboard.setValue(false);
        canAccessDashboard.addSource(isAdmin, v -> recomputeAccess());
        canAccessDashboard.addSource(isShopOwner, v -> recomputeAccess());

        FirebaseAuth auth = FirebaseAuth.getInstance();
        attach(auth.getCurrentUser());
        auth.addAuthStateListener(a -> attach(a.getCurrentUser()));
    }

    private void recomputeAccess() {
        canAccessDashboard.setValue(
                Boolean.TRUE.equals(isAdmin.getValue())
                        || Boolean.TRUE.equals(isShopOwner.getValue()));
    }

    public LiveData<Boolean> isAdmin() {
        return isAdmin;
    }

    public LiveData<Boolean> isShopOwner() {
        return isShopOwner;
    }

    public LiveData<String> ownedShopId() {
        return ownedShopId;
    }

    public LiveData<Boolean> canAccessDashboard() {
        return canAccessDashboard;
    }

    public void refresh() {
        attach(FirebaseAuth.getInstance().getCurrentUser());
    }

    private synchronized void attach(FirebaseUser user) {
        String newUid = user != null ? user.getUid() : null;
        if (newUid != null && newUid.equals(listenedUid) && profileListener != null) {
            return;
        }
        detach();

        if (user == null || user.isAnonymous() || user.getEmail() == null) {
            isAdmin.postValue(false);
            isShopOwner.postValue(false);
            ownedShopId.postValue("");
            return;
        }

        listenedUid = newUid;
        seededForUid = false;
        final String email = user.getEmail();
        final String uid = user.getUid();

        profileListener = repository.listenToUserProfile(uid, profile -> {
            if (profile == null) {
                if (!seededForUid) {
                    seededForUid = true;
                    boolean seed = AdminConfig.isSeedAdmin(email);
                    User created = new User(uid, email, "", new ArrayList<>());
                    created.setAdmin(seed);
                    repository.updateUserProfile(created, ok -> { /* το snapshot θα ξανατρεξει μονο του */ });
                }
                isAdmin.postValue(false);
                isShopOwner.postValue(false);
                ownedShopId.postValue("");
                return;
            }
            seededForUid = true;
            isAdmin.postValue(profile.isAdmin());
            isShopOwner.postValue(profile.isShopOwner());
            ownedShopId.postValue(profile.getOwnedShopId() != null ? profile.getOwnedShopId() : "");
        });
    }

    private synchronized void detach() {
        if (profileListener != null) {
            profileListener.remove();
            profileListener = null;
        }
        listenedUid = null;
        seededForUid = false;
    }
}
