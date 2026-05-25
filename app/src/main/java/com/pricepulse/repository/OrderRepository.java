package com.pricepulse.repository;

import android.util.Log;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.SetOptions;
import com.pricepulse.model.Order;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OrderRepository {

    private static final String TAG = "OrderRepository";

    private static final Comparator<Order> NEWEST_FIRST =
            (a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp());

    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();
    private final CollectionReference ordersCollection = firestore.collection("orders");

    public void saveOrder(Order order, RepoCallback<Boolean> callback) {
        ordersCollection.document(order.getId()).set(order)
                .addOnSuccessListener(v -> callback.onComplete(true))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "saveOrder failed", e);
                    callback.onComplete(false);
                });
    }

    public ListenerRegistration listenToUserOrders(String uid, RepoCallback<List<Order>> callback) {
        return ordersCollection.whereEqualTo("userId", uid).addSnapshotListener((snapshot, error) -> {
            if (error != null) {
                Log.e(TAG, "listenToUserOrders failed", error);
                callback.onComplete(new ArrayList<>());
                return;
            }
            List<Order> orders = snapshot != null ? snapshot.toObjects(Order.class) : new ArrayList<>();
            Collections.sort(orders, NEWEST_FIRST);
            callback.onComplete(orders);
        });
    }

    public ListenerRegistration listenToAllOrders(RepoCallback<List<Order>> callback) {
        return ordersCollection.addSnapshotListener((snapshot, error) -> {
            if (error != null) {
                Log.e(TAG, "listenToAllOrders failed", error);
                callback.onComplete(new ArrayList<>());
                return;
            }
            List<Order> orders = snapshot != null ? snapshot.toObjects(Order.class) : new ArrayList<>();
            Collections.sort(orders, NEWEST_FIRST);
            callback.onComplete(orders);
        });
    }

    public ListenerRegistration listenToOrdersByShop(String shopId, RepoCallback<List<Order>> callback) {
        return ordersCollection.whereEqualTo("shopId", shopId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "listenToOrdersByShop failed", error);
                        callback.onComplete(new ArrayList<>());
                        return;
                    }
                    List<Order> orders = snapshot != null
                            ? snapshot.toObjects(Order.class) : new ArrayList<>();
                    Collections.sort(orders, NEWEST_FIRST);
                    callback.onComplete(orders);
                });
    }

    public void updateOrderStatus(String orderId, String status, RepoCallback<Boolean> callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("status", status);
        ordersCollection.document(orderId).set(data, SetOptions.merge())
                .addOnSuccessListener(v -> callback.onComplete(true))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "updateOrderStatus failed", e);
                    callback.onComplete(false);
                });
    }
}
