package com.pricepulse.repository;

import android.util.Log;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.WriteBatch;
import com.pricepulse.model.Address;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class AddressRepository {

    private static final String TAG = "AddressRepository";

    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();
    private final CollectionReference addressesCollection = firestore.collection("addresses");

    public ListenerRegistration listenToUserAddresses(String uid, RepoCallback<List<Address>> callback) {
        return addressesCollection.whereEqualTo("userId", uid).addSnapshotListener((snapshot, error) -> {
            if (error != null) {
                Log.e(TAG, "listenToUserAddresses failed", error);
                callback.onComplete(new ArrayList<>());
                return;
            }
            List<Address> addresses = snapshot != null
                    ? snapshot.toObjects(Address.class) : new ArrayList<>();
            Collections.sort(addresses, (a, b) -> {
                if (a.isDefaultAddress() != b.isDefaultAddress()) {
                    return a.isDefaultAddress() ? -1 : 1;
                }
                return Long.compare(b.getTimestamp(), a.getTimestamp());
            });
            callback.onComplete(addresses);
        });
    }

    public void saveAddress(Address address, RepoCallback<Boolean> callback) {
        if (address.getId() == null || address.getId().isEmpty()) {
            address.setId(UUID.randomUUID().toString());
        }
        addressesCollection.document(address.getId()).set(address)
                .addOnSuccessListener(v -> callback.onComplete(true))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "saveAddress failed", e);
                    callback.onComplete(false);
                });
    }

    public void deleteAddress(String addressId, RepoCallback<Boolean> callback) {
        addressesCollection.document(addressId).delete()
                .addOnSuccessListener(v -> callback.onComplete(true))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "deleteAddress failed", e);
                    callback.onComplete(false);
                });
    }

    public void setDefaultAddress(String uid, String addressId, RepoCallback<Boolean> callback) {
        addressesCollection.whereEqualTo("userId", uid).get()
                .addOnSuccessListener(snapshot -> {
                    WriteBatch batch = firestore.batch();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        boolean shouldBeDefault = doc.getId().equals(addressId);
                        batch.update(doc.getReference(), "defaultAddress", shouldBeDefault);
                    }
                    batch.commit()
                            .addOnSuccessListener(v -> callback.onComplete(true))
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "setDefaultAddress batch failed", e);
                                callback.onComplete(false);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "setDefaultAddress query failed", e);
                    callback.onComplete(false);
                });
    }
}
