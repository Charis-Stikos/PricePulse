package com.pricepulse.repository;

import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;
import com.pricepulse.model.Order;
import com.pricepulse.model.Product;
import com.pricepulse.model.User;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class FirebaseRepository {

    private static final String TAG = "FirebaseRepository";

    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();
    private final com.google.firebase.firestore.CollectionReference productsCollection = firestore.collection("products");
    private final com.google.firebase.firestore.CollectionReference usersCollection = firestore.collection("users");
    private final com.google.firebase.firestore.CollectionReference ordersCollection = firestore.collection("orders");

    public void getProducts(long limit, RepoCallback<List<Product>> callback) {
        productsCollection
                .orderBy("rating", Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .addOnSuccessListener(snapshot ->
                        callback.onComplete(snapshot.toObjects(Product.class)))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "getProducts failed", e);
                    callback.onComplete(new ArrayList<>());
                });
    }

    public void getProductsByCategory(String category, long limit, RepoCallback<List<Product>> callback) {
        if ("All".equals(category)) {
            getProducts(limit, callback);
            return;
        }
        productsCollection
                .whereEqualTo("category", category)
                .limit(limit)
                .get()
                .addOnSuccessListener(snapshot ->
                        callback.onComplete(snapshot.toObjects(Product.class)))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "getProductsByCategory failed", e);
                    callback.onComplete(new ArrayList<>());
                });
    }

    public void getProductById(String productId, RepoCallback<Product> callback) {
        productsCollection.document(productId).get()
                .addOnSuccessListener(snapshot -> callback.onComplete(snapshot.toObject(Product.class)))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "getProductById failed", e);
                    callback.onComplete(null);
                });
    }

    public void searchProducts(String query, RepoCallback<List<Product>> callback) {
        productsCollection.limit(100).get()
                .addOnSuccessListener(snapshot -> {
                    List<Product> all = snapshot.toObjects(Product.class);
                    List<Product> filtered = new ArrayList<>();
                    String q = query.toLowerCase();
                    for (Product p : all) {
                        if (p.getTitle().toLowerCase().contains(q)
                                || p.getCategory().toLowerCase().contains(q)) {
                            filtered.add(p);
                        }
                    }
                    callback.onComplete(filtered);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "searchProducts failed", e);
                    callback.onComplete(new ArrayList<>());
                });
    }

    public void saveOrder(Order order, RepoCallback<Boolean> callback) {
        ordersCollection.document(order.getId()).set(order)
                .addOnSuccessListener(v -> callback.onComplete(true))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "saveOrder failed", e);
                    callback.onComplete(false);
                });
    }

    public void getUserOrders(String uid, RepoCallback<List<Order>> callback) {
        ordersCollection.whereEqualTo("userId", uid).get()
                .addOnSuccessListener(snapshot -> {
                    List<Order> orders = snapshot.toObjects(Order.class);
                    Collections.sort(orders, new Comparator<Order>() {
                        @Override
                        public int compare(Order a, Order b) {
                            return Long.compare(b.getTimestamp(), a.getTimestamp());
                        }
                    });
                    callback.onComplete(orders);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "getUserOrders failed", e);
                    callback.onComplete(new ArrayList<>());
                });
    }

    public void getUserProfile(String uid, RepoCallback<User> callback) {
        usersCollection.document(uid).get()
                .addOnSuccessListener(snapshot -> callback.onComplete(snapshot.toObject(User.class)))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "getUserProfile failed", e);
                    callback.onComplete(null);
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

    public void getProductsByIds(List<String> ids, RepoCallback<List<Product>> callback) {
        if (ids.isEmpty()) {
            callback.onComplete(new ArrayList<>());
            return;
        }
        List<Product> results = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger remaining = new AtomicInteger(ids.size());
        for (String id : ids) {
            getProductById(id, product -> {
                if (product != null) results.add(product);
                if (remaining.decrementAndGet() == 0) {
                    callback.onComplete(new ArrayList<>(results));
                }
            });
        }
    }
}
