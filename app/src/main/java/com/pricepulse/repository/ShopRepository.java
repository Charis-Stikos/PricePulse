package com.pricepulse.repository;

import android.util.Log;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.pricepulse.model.Shop;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ShopRepository {

    private static final String TAG = "ShopRepository";

    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();
    private final CollectionReference shopsCollection = firestore.collection("shops");

    public ListenerRegistration listenToShops(RepoCallback<List<Shop>> callback) {
        return shopsCollection.addSnapshotListener((snapshot, error) -> {
            if (error != null) {
                Log.e(TAG, "listenToShops failed", error);
                callback.onComplete(new ArrayList<>());
                return;
            }
            callback.onComplete(snapshot != null
                    ? snapshot.toObjects(Shop.class) : new ArrayList<>());
        });
    }

    public ListenerRegistration listenToShop(String shopId, RepoCallback<Shop> callback) {
        return shopsCollection.document(shopId).addSnapshotListener((snapshot, error) -> {
            if (error != null) {
                Log.e(TAG, "listenToShop failed", error);
                callback.onComplete(null);
                return;
            }
            callback.onComplete(snapshot != null ? snapshot.toObject(Shop.class) : null);
        });
    }

    public void getShop(String shopId, RepoCallback<Shop> callback) {
        shopsCollection.document(shopId).get()
                .addOnSuccessListener(snapshot -> callback.onComplete(snapshot.toObject(Shop.class)))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "getShop failed", e);
                    callback.onComplete(null);
                });
    }

    public void saveShop(Shop shop, RepoCallback<Boolean> callback) {
        if (shop.getId() == null || shop.getId().isEmpty()) {
            shop.setId(UUID.randomUUID().toString());
        }
        shopsCollection.document(shop.getId()).set(shop)
                .addOnSuccessListener(v -> callback.onComplete(true))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "saveShop failed", e);
                    callback.onComplete(false);
                });
    }
}
