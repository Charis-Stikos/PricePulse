package com.pricepulse.util;

import android.util.Log;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
import com.pricepulse.model.Product;
import com.pricepulse.model.Review;
import com.pricepulse.model.Shop;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class FirestoreSeeder {

    public interface SeedCallback {
        void onComplete(boolean success);
    }

    private static final String TAG = "FirestoreSeeder";

    private static final String DEFAULT_SHOP_ID = "default-shop";

    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();
    private final CollectionReference productsCollection = firestore.collection("products");
    private final CollectionReference shopsCollection = firestore.collection("shops");
    private final Random random = new Random();

    public void seedData() {
        seedData(null);
    }

    public void seedData(SeedCallback callback) {
        productsCollection.document("e1").get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        Log.i(TAG, "Seed data already present, skipping.");
                        if (callback != null) callback.onComplete(true);
                        return;
                    }
                    writeProducts(callback);
                })
                .addOnFailureListener(error -> {
                    Log.e(TAG, "Failed to check existing seed data", error);
                    if (callback != null) callback.onComplete(false);
                });
    }

    private void writeProducts(SeedCallback callback) {
        List<Product> products = buildProducts();
        Shop defaultShop = buildDefaultShop(products.size());

        WriteBatch batch = firestore.batch();
        batch.set(shopsCollection.document(defaultShop.getId()), defaultShop);
        for (Product product : products) {
            product.setShopId(defaultShop.getId());
            product.setShopName(defaultShop.getName());
            product.setShopActive(defaultShop.isActive());
            batch.set(productsCollection.document(product.getId()), product);
        }
        batch.commit()
                .addOnSuccessListener(v -> {
                    Log.i(TAG, "Seeded " + products.size() + " products + 1 shop to Firestore.");
                    if (callback != null) callback.onComplete(true);
                })
                .addOnFailureListener(error -> {
                    Log.e(TAG, "Failed to seed products/shop", error);
                    if (callback != null) callback.onComplete(false);
                });
    }

    private Shop buildDefaultShop(int productCount) {
        // demo shop στη Θεσσαλονικη — οι ιδιες συντεταγμενες με το LocationDiscountHelper
        Shop shop = new Shop();
        shop.setId(DEFAULT_SHOP_ID);
        shop.setOwnerId("");
        shop.setName("PricePulse Store");
        shop.setDescription("Demo storefront. Electronics, fashion, home and more.");
        shop.setMainCategory("Electronics");
        shop.setBusinessEmail("contact@pricepulse.app");
        shop.setBusinessPhone("+30 210 000 0000");
        shop.setAddress("12 Market Street, Thessaloniki");
        shop.setOpeningHours("Monday - Friday, 09:00 - 18:00");
        shop.setDeliveryOptions("Courier delivery and in-store pickup available");
        shop.setRating(4.8);
        shop.setProductCount(productCount);
        shop.setActive(true);
        shop.setLatitude(40.6401);
        shop.setLongitude(22.9444);
        return shop;
    }

    private List<Product> buildProducts() {
        List<Product> products = new ArrayList<>();
        products.add(new Product(
                "e1",
                "Apple iPhone 15 Pro Max 256GB Natural Titanium",
                "The iPhone 15 Pro Max features a strong and light aerospace-grade titanium design. It has a Pro camera system with a 48MP main camera and 5x Telephoto lens for high-resolution photos and incredible detail.",
                1449.00,
                "https://images.unsplash.com/photo-1696446701796-da61225697cc?q=80&w=800&auto=format&fit=crop",
                "Electronics",
                4.9, 124, 42,
                createReviews("Best phone ever!", "Incredible camera quality.", "Titanium finish feels premium.")
        ));
        products.add(new Product(
                "e2",
                "Sony WH-1000XM5 Wireless Noise Cancelling Headphones",
                "Industry-leading noise cancellation with two processors controlling eight microphones. Exceptional sound quality with the new Integrated Processor V1 and specially designed 30mm driver unit.",
                329.00,
                "https://images.unsplash.com/photo-1618366712010-f4ae9c647dcb?q=80&w=800&auto=format&fit=crop",
                "Electronics",
                4.8, 85, 15,
                createReviews("Silent bliss.", "Battery life is amazing.", "Very comfortable for long flights.")
        ));
        products.add(new Product(
                "e3",
                "Samsung 49\" Odyssey G9 OLED Curved Gaming Monitor",
                "The world's first 49\" OLED gaming monitor with 240Hz refresh rate and 0.03ms response time. Experience ultra-wide immersion with a 1800R curvature and stunning Dual QHD resolution.",
                1199.99,
                "https://images.unsplash.com/photo-1527443224154-c4a3942d3acf?q=80&w=800&auto=format&fit=crop",
                "Electronics",
                4.7, 45, 8,
                createReviews("Immersive experience.", "Colors are popping!", "Great for multitasking too.")
        ));
        products.add(new Product(
                "f1",
                "Nike Air Jordan 1 Retro High OG 'Chicago'",
                "The shoe that started it all. The Air Jordan 1 Retro High OG features premium leather and the classic Chicago Bulls colorway. A must-have for any sneaker collector.",
                450.00,
                "https://images.unsplash.com/photo-1552346154-21d32810aba3?q=80&w=800&auto=format&fit=crop",
                "Fashion",
                5.0, 210, 3,
                createReviews("Iconic.", "Worth every penny.", "Classic style never dies.")
        ));
        products.add(new Product(
                "f2",
                "Levi's 501 Original Fit Men's Jeans",
                "The original blue jean since 1873. The 501 features a classic straight fit and signature button fly. Crafted from premium non-stretch denim for a timeless look.",
                89.00,
                "https://images.unsplash.com/photo-1542272604-787c3835535d?q=80&w=800&auto=format&fit=crop",
                "Fashion",
                4.6, 1500, 55,
                createReviews("Great fit.", "Very durable.", "The only jeans I buy.")
        ));
        products.add(new Product(
                "h1",
                "Nespresso Vertuo Next Coffee and Espresso Maker",
                "Centrifusion technology to gently and fully extract every drop of flavor. Brews a wide range of coffee styles from Espresso to Alto. Compact design with 54% recycled plastics.",
                169.00,
                "https://images.unsplash.com/photo-1517668808822-9ebb02f2a0e6?q=80&w=800&auto=format&fit=crop",
                "Home",
                4.5, 320, 20,
                createReviews("Morning saver.", "Elegant design.", "Pods are a bit pricey though.")
        ));
        products.add(new Product(
                "h2",
                "Dyson V15 Detect Cordless Vacuum Cleaner",
                "The most powerful, intelligent cordless vacuum. Features a laser that reveals microscopic dust and a piezo sensor that automatically adjusts suction power based on dust volume.",
                749.00,
                "https://images.unsplash.com/photo-1558317374-067df5f15430?q=80&w=800&auto=format&fit=crop",
                "Home",
                4.9, 890, 12,
                createReviews("Cleaning is fun now.", "Incredible suction.", "The laser light is a game changer.")
        ));
        products.add(new Product(
                "s1",
                "Wilson Evolution Indoor Game Basketball",
                "The #1 indoor game basketball in America. Features a microfiber composite cover for a soft feel and consistent grip. Designed for maximum durability and performance.",
                79.99,
                "https://images.unsplash.com/photo-1519861531473-9200362f88bf?q=80&w=800&auto=format&fit=crop",
                "Sports",
                4.8, 450, 25,
                createReviews("Best ball on the market.", "Grip is perfect.", "Highly recommend for indoor play.")
        ));
        products.add(new Product(
                "b1",
                "Chanel No. 5 Eau de Parfum",
                "The essence of femininity. A powdery floral bouquet housed in an iconic bottle with a minimalist design. A timeless, legendary fragrance.",
                135.00,
                "https://images.unsplash.com/photo-1541643600914-78b084683601?q=80&w=800&auto=format&fit=crop",
                "Beauty",
                4.7, 670, 10,
                createReviews("Classic and elegant.", "Long lasting scent.", "The perfect gift.")
        ));
        products.add(new Product(
                "bk1",
                "The Great Gatsby by F. Scott Fitzgerald",
                "A classic of 20th-century literature. Set in the Roaring Twenties, this novel explores themes of wealth, love, and the American Dream through the mysterious Jay Gatsby.",
                12.99,
                "https://images.unsplash.com/photo-1544947950-fa07a98d237f?q=80&w=800&auto=format&fit=crop",
                "Books",
                4.6, 1200, 100,
                createReviews("A masterpiece.", "Beautifully written.", "Short but powerful story.")
        ));
        return products;
    }

    private List<Review> createReviews(String... comments) {
        List<String> usernames = Arrays.asList("George_K", "Maria_P", "TechLover", "ShoppingKing", "BookNerd", "SneakerHead");
        List<Review> reviews = new ArrayList<>();
        for (String comment : comments) {
            String username = usernames.get(random.nextInt(usernames.size()));
            int rating = 4 + random.nextInt(2);
            long delta = (long) (random.nextDouble() * 1_000_000_000L);
            reviews.add(new Review(
                    UUID.randomUUID().toString(),
                    username,
                    comment,
                    rating,
                    System.currentTimeMillis() - delta
            ));
        }
        return reviews;
    }
}
