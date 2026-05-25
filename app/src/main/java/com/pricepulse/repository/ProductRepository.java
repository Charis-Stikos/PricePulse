package com.pricepulse.repository;

import android.net.Uri;
import android.util.Log;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.pricepulse.model.Product;
import com.pricepulse.model.Review;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class ProductRepository {

    private static final String TAG = "ProductRepository";

    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();
    private final CollectionReference productsCollection = firestore.collection("products");

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

    public ListenerRegistration listenToAllProducts(RepoCallback<List<Product>> callback) {
        return productsCollection.addSnapshotListener((snapshot, error) -> {
            if (error != null) {
                Log.e(TAG, "listenToAllProducts failed", error);
                callback.onComplete(new ArrayList<>());
                return;
            }
            callback.onComplete(snapshot != null
                    ? snapshot.toObjects(Product.class) : new ArrayList<>());
        });
    }

    public ListenerRegistration listenToProductsByShop(String shopId, RepoCallback<List<Product>> callback) {
        return productsCollection.whereEqualTo("shopId", shopId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "listenToProductsByShop failed", error);
                        callback.onComplete(new ArrayList<>());
                        return;
                    }
                    callback.onComplete(snapshot != null
                            ? snapshot.toObjects(Product.class) : new ArrayList<>());
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

    public void searchProducts(String query, RepoCallback<List<Product>> callback) {
        productsCollection.limit(100).get()
                .addOnSuccessListener(snapshot -> {
                    List<Product> all = snapshot.toObjects(Product.class);
                    List<Product> filtered = new ArrayList<>();
                    String q = query.toLowerCase();
                    for (Product p : all) {
                        if (!p.isShopActive()) continue;
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

    public void submitReview(String productId, Review review, RepoCallback<Boolean> callback) {
        DocumentReference ref = productsCollection.document(productId);
        firestore.runTransaction(transaction -> {
            DocumentSnapshot snap = transaction.get(ref);
            Product p = snap.toObject(Product.class);
            if (p == null) {
                throw new IllegalStateException("Product not found");
            }
            List<Review> reviews = p.getReviews() != null
                    ? new ArrayList<>(p.getReviews()) : new ArrayList<>();
            reviews.add(review);

            double sum = 0.0;
            for (Review r : reviews) sum += r.getRating();
            double newAverage = reviews.isEmpty() ? 0.0 : sum / reviews.size();

            Map<String, Object> updates = new HashMap<>();
            updates.put("reviews", reviews);
            updates.put("rating", newAverage);
            updates.put("reviewCount", reviews.size());
            transaction.update(ref, updates);
            return null;
        }).addOnSuccessListener(v -> callback.onComplete(true))
          .addOnFailureListener(e -> {
              Log.e(TAG, "submitReview failed", e);
              callback.onComplete(false);
          });
    }

    // ενημερωνει τα denormalized shopName/shopActive πανω στα προϊοντα του shop,
    // ωστε home/search να φιλτραρουν σωστα χωρις extra fetch ανα προϊον
    public void propagateShopFields(String shopId, String newName, Boolean newActive,
                                    RepoCallback<Boolean> callback) {
        productsCollection.whereEqualTo("shopId", shopId).get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) {
                        callback.onComplete(true);
                        return;
                    }
                    WriteBatch batch = firestore.batch();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        if (newName != null) batch.update(doc.getReference(), "shopName", newName);
                        if (newActive != null) batch.update(doc.getReference(), "shopActive", newActive);
                    }
                    batch.commit()
                            .addOnSuccessListener(v -> callback.onComplete(true))
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "propagateShopFields batch failed", e);
                                callback.onComplete(false);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "propagateShopFields query failed", e);
                    callback.onComplete(false);
                });
    }
}
