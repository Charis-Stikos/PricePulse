package com.pricepulse.repository;

import android.util.Log;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;
import com.pricepulse.model.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserRepository {

    private static final String TAG = "UserRepository";

    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();
    private final CollectionReference usersCollection = firestore.collection("users");
    private final CollectionReference shopsCollection = firestore.collection("shops");

    public ListenerRegistration listenToUserProfile(String uid, RepoCallback<User> callback) {
        return usersCollection.document(uid).addSnapshotListener((snapshot, error) -> {
            if (error != null) {
                Log.e(TAG, "listenToUserProfile failed", error);
                callback.onComplete(null);
                return;
            }
            callback.onComplete(snapshot != null ? snapshot.toObject(User.class) : null);
        });
    }

    public void updateUserProfile(User user, RepoCallback<Boolean> callback) {
        usersCollection.document(user.getUid()).set(user, SetOptions.merge())
                .addOnSuccessListener(v -> callback.onComplete(true))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "updateUserProfile failed", e);
                    callback.onComplete(false);
                });
    }

    public void findUserByEmail(String email, RepoCallback<User> callback) {
        usersCollection.whereEqualTo("email", email).limit(1).get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) {
                        callback.onComplete(null);
                    } else {
                        callback.onComplete(snapshot.getDocuments().get(0).toObject(User.class));
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "findUserByEmail failed", e);
                    callback.onComplete(null);
                });
    }

    public void setUserAdmin(String uid, boolean isAdmin, RepoCallback<Boolean> callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("admin", isAdmin);
        usersCollection.document(uid).set(data, SetOptions.merge())
                .addOnSuccessListener(v -> callback.onComplete(true))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "setUserAdmin failed", e);
                    callback.onComplete(false);
                });
    }

    public ListenerRegistration listenToAdmins(RepoCallback<List<User>> callback) {
        return usersCollection.whereEqualTo("admin", true).addSnapshotListener((snapshot, error) -> {
            if (error != null) {
                Log.e(TAG, "listenToAdmins failed", error);
                callback.onComplete(new ArrayList<>());
                return;
            }
            callback.onComplete(snapshot != null ? snapshot.toObjects(User.class) : new ArrayList<>());
        });
    }

    public ListenerRegistration listenToShopOwners(RepoCallback<List<User>> callback) {
        return usersCollection.whereEqualTo("shopOwner", true).addSnapshotListener((snapshot, error) -> {
            if (error != null) {
                Log.e(TAG, "listenToShopOwners failed", error);
                callback.onComplete(new ArrayList<>());
                return;
            }
            callback.onComplete(snapshot != null ? snapshot.toObjects(User.class) : new ArrayList<>());
        });
    }

    public void updateWishlist(String uid, List<String> productIds, RepoCallback<Boolean> callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("likedProductIds", productIds);
        usersCollection.document(uid).set(data, SetOptions.merge())
                .addOnSuccessListener(v -> callback.onComplete(true))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "updateWishlist failed", e);
                    callback.onComplete(false);
                });
    }

    // ενημερωνει σε ενα batch τον χρηστη (ρολο + ownedShopId) ΚΑΙ το shop (ownerId + ownerEmail)
    public void linkShopOwner(String uid, String email, String shopId,
                              RepoCallback<Boolean> callback) {
        WriteBatch batch = firestore.batch();

        Map<String, Object> userUpdate = new HashMap<>();
        userUpdate.put("shopOwner", true);
        userUpdate.put("ownedShopId", shopId);
        batch.set(usersCollection.document(uid), userUpdate, SetOptions.merge());

        Map<String, Object> shopUpdate = new HashMap<>();
        shopUpdate.put("ownerId", uid);
        shopUpdate.put("ownerEmail", email != null ? email : "");
        batch.set(shopsCollection.document(shopId), shopUpdate, SetOptions.merge());

        batch.commit()
                .addOnSuccessListener(v -> callback.onComplete(true))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "linkShopOwner failed", e);
                    callback.onComplete(false);
                });
    }

    // καθαριζει σε ενα batch τον χρηστη (ρολο + ownedShopId) ΚΑΙ το shop (ownerId + ownerEmail)
    public void unlinkShopOwner(String uid, String shopId, RepoCallback<Boolean> callback) {
        WriteBatch batch = firestore.batch();

        Map<String, Object> userUpdate = new HashMap<>();
        userUpdate.put("shopOwner", false);
        userUpdate.put("ownedShopId", "");
        batch.set(usersCollection.document(uid), userUpdate, SetOptions.merge());

        if (shopId != null && !shopId.isEmpty()) {
            Map<String, Object> shopUpdate = new HashMap<>();
            shopUpdate.put("ownerId", "");
            shopUpdate.put("ownerEmail", "");
            batch.set(shopsCollection.document(shopId), shopUpdate, SetOptions.merge());
        }

        batch.commit()
                .addOnSuccessListener(v -> callback.onComplete(true))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "unlinkShopOwner failed", e);
                    callback.onComplete(false);
                });
    }
}
