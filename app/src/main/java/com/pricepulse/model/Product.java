package com.pricepulse.model;

import java.util.ArrayList;
import java.util.List;

public class Product {
    private String id = "";
    private String title = "";
    private String description = "";
    private double price = 0.0;
    private String imageUrl = "";
    private String category = "";
    private double rating = 0.0;
    private int reviewCount = 0;
    private int shopCount = 0;
    private String shopId = "";
    private String shopName = "";
    private boolean shopActive = true;
    private List<Review> reviews = new ArrayList<>();

    public Product() {
    }

    public Product(String id, String title, String description, double price,
                   String imageUrl, String category, double rating,
                   int reviewCount, int shopCount, List<Review> reviews) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.price = price;
        this.imageUrl = imageUrl;
        this.category = category;
        this.rating = rating;
        this.reviewCount = reviewCount;
        this.shopCount = shopCount;
        this.reviews = reviews;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }

    public int getReviewCount() { return reviewCount; }
    public void setReviewCount(int reviewCount) { this.reviewCount = reviewCount; }

    public int getShopCount() { return shopCount; }
    public void setShopCount(int shopCount) { this.shopCount = shopCount; }

    public String getShopId() { return shopId; }
    public void setShopId(String shopId) { this.shopId = shopId; }

    public String getShopName() { return shopName; }
    public void setShopName(String shopName) { this.shopName = shopName; }

    public boolean isShopActive() { return shopActive; }
    public void setShopActive(boolean shopActive) { this.shopActive = shopActive; }

    public List<Review> getReviews() { return reviews; }
    public void setReviews(List<Review> reviews) { this.reviews = reviews; }
}
