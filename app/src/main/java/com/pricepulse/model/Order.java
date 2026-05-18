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
}
