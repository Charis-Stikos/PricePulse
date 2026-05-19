package com.pricepulse.repository;

import android.net.Uri;
import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.pricepulse.model.Order;
import com.pricepulse.model.Product;
import com.pricepulse.model.User;

import java.util.UUID;

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

    public ListenerRegistration listenToProducts(long limit, RepoCallback<List<Product>> callback) {
        return productsCollection
                .orderBy("rating", Query.Direction.DESCENDING)
                .limit(limit)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "listenToProducts failed", error);
                        callback.onComplete(new ArrayList<>());
                        return;
                    }
                    callback.onComplete(snapshot != null ? snapshot.toObjects(Product.class) : new ArrayList<>());
                });
    }

    public ListenerRegistration listenToProductsByCategory(String category, long limit,
                                                           RepoCallback<List<Product>> callback) {
        if ("All".equals(category)) {
            return listenToProducts(limit, callback);
        }
        return productsCollection
                .whereEqualTo("category", category)
                .limit(limit)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "listenToProductsByCategory failed", error);
                        callback.onComplete(new ArrayList<>());
                        return;
                    }
                    callback.onComplete(snapshot != null ? snapshot.toObjects(Product.class) : new ArrayList<>());
                });
    }

    public ListenerRegistration listenToProduct(String productId, RepoCallback<Product> callback) {
        return productsCollection.document(productId).addSnapshotListener((snapshot, error) -> {
            if (error != null) {
                Log.e(TAG, "listenToProduct failed", error);
                callback.onComplete(null);
                return;
            }
            callback.onComplete(snapshot != null ? snapshot.toObject(Product.class) : null);
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
            Collections.sort(orders, (a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
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
            Collections.sort(orders, (a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
            callback.onComplete(orders);
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

    public void getAdmins(RepoCallback<List<User>> callback) {
        usersCollection.whereEqualTo("admin", true).get()
                .addOnSuccessListener(snapshot -> callback.onComplete(snapshot.toObjects(User.class)))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "getAdmins failed", e);
                    callback.onComplete(new ArrayList<>());
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

    public void getAllOrders(RepoCallback<List<Order>> callback) {
        ordersCollection.get()
                .addOnSuccessListener(snapshot -> {
                    List<Order> orders = snapshot.toObjects(Order.class);
                    Collections.sort(orders, (a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
                    callback.onComplete(orders);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "getAllOrders failed", e);
                    callback.onComplete(new ArrayList<>());
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

    public void addProduct(Product product, RepoCallback<Boolean> callback) {
        if (product.getId() == null || product.getId().isEmpty()) {
            product.setId(UUID.randomUUID().toString());
        }
        productsCollection.document(product.getId()).set(product)
                .addOnSuccessListener(v -> callback.onComplete(true))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "addProduct failed", e);
                    callback.onComplete(false);
                });
    }

    public void uploadProductImage(Uri localUri, RepoCallback<String> callback) {
        StorageReference ref = FirebaseStorage.getInstance()
                .getReference("products/" + UUID.randomUUID() + ".jpg");
        ref.putFile(localUri)
                .continueWithTask(task -> {
                    if (!task.isSuccessful() && task.getException() != null) {
                        throw task.getException();
                    }
                    return ref.getDownloadUrl();
                })
                .addOnSuccessListener(uri -> callback.onComplete(uri != null ? uri.toString() : null))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "uploadProductImage failed", e);
                    callback.onComplete(null);
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
