package com.pricepulse.model;

import java.util.ArrayList;
import java.util.List;

public class Order {
    private String id = "";
    private String userId = "";
    private List<CartItem> items = new ArrayList<>();
    private double totalAmount = 0.0;
    private String status = "Pending";
    private long timestamp = System.currentTimeMillis();

    private double deliveryFee = 0.0;
    private double deliveryDiscountAmount = 0.0;
    private double finalTotalAmount = 0.0;
    private boolean locationDiscountApplied = false;

    public Order() {
    }

    public Order(String id, String userId, List<CartItem> items,
                 double totalAmount, String status, long timestamp) {
        this.id = id;
        this.userId = userId;
        this.items = items;
        this.totalAmount = totalAmount;
        this.status = status;
        this.timestamp = timestamp;

        this.deliveryFee = 0.0;
        this.deliveryDiscountAmount = 0.0;
        this.finalTotalAmount = totalAmount;
        this.locationDiscountApplied = false;
    }

    public Order(String id, String userId, List<CartItem> items,
                 double totalAmount, String status, long timestamp,
                 double deliveryFee, double deliveryDiscountAmount,
                 double finalTotalAmount, boolean locationDiscountApplied) {
        this.id = id;
        this.userId = userId;
        this.items = items;
        this.totalAmount = totalAmount;
        this.status = status;
        this.timestamp = timestamp;
        this.deliveryFee = deliveryFee;
        this.deliveryDiscountAmount = deliveryDiscountAmount;
        this.finalTotalAmount = finalTotalAmount;
        this.locationDiscountApplied = locationDiscountApplied;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public List<CartItem> getItems() { return items; }
    public void setItems(List<CartItem> items) { this.items = items; }

    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public double getDeliveryFee() { return deliveryFee; }
    public void setDeliveryFee(double deliveryFee) { this.deliveryFee = deliveryFee; }

    public double getDeliveryDiscountAmount() { return deliveryDiscountAmount; }
    public void setDeliveryDiscountAmount(double deliveryDiscountAmount) {
        this.deliveryDiscountAmount = deliveryDiscountAmount;
    }

    public double getFinalTotalAmount() { return finalTotalAmount; }
    public void setFinalTotalAmount(double finalTotalAmount) {
        this.finalTotalAmount = finalTotalAmount;
    }

    public boolean isLocationDiscountApplied() { return locationDiscountApplied; }
    public void setLocationDiscountApplied(boolean locationDiscountApplied) {
        this.locationDiscountApplied = locationDiscountApplied;
    }
}
