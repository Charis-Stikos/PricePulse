package com.pricepulse.model;

public class Review {
    private String userId = "";
    private String username = "";
    private String comment = "";
    private int rating = 0;
    private long timestamp = System.currentTimeMillis();

    public Review() {
    }

    public Review(String userId, String username, String comment, int rating, long timestamp) {
        this.userId = userId;
        this.username = username;
        this.comment = comment;
        this.rating = rating;
        this.timestamp = timestamp;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
