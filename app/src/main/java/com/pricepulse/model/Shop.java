package com.pricepulse.model;

public class Shop {

    private String id = "";
    private String ownerId = "";
    private String ownerEmail = "";
    private String name = "";
    private String description = "";
    private String mainCategory = "";
    private String businessEmail = "";
    private String businessPhone = "";
    private String address = "";
    private String openingHours = "";
    private String deliveryOptions = "";
    private double rating = 0.0;
    private int productCount = 0;
    private boolean active = true;
    // συντεταγμενες — αντικαθιστουν τις hardcoded τιμες απο τον LocationDiscountHelper
    private double latitude = 0.0;
    private double longitude = 0.0;
    private long createdAt = System.currentTimeMillis();

    public Shop() {
    }

    public Shop(String id, String ownerId, String name, String description,
                String mainCategory, String businessEmail, String businessPhone,
                String address, String openingHours, String deliveryOptions,
                double rating, int productCount, boolean active,
                double latitude, double longitude, long createdAt) {
        this.id = id;
        this.ownerId = ownerId;
        this.name = name;
        this.description = description;
        this.mainCategory = mainCategory;
        this.businessEmail = businessEmail;
        this.businessPhone = businessPhone;
        this.address = address;
        this.openingHours = openingHours;
        this.deliveryOptions = deliveryOptions;
        this.rating = rating;
        this.productCount = productCount;
        this.active = active;
        this.latitude = latitude;
        this.longitude = longitude;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }

    public String getOwnerEmail() { return ownerEmail; }
    public void setOwnerEmail(String ownerEmail) { this.ownerEmail = ownerEmail; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getMainCategory() { return mainCategory; }
    public void setMainCategory(String mainCategory) { this.mainCategory = mainCategory; }

    public String getBusinessEmail() { return businessEmail; }
    public void setBusinessEmail(String businessEmail) { this.businessEmail = businessEmail; }

    public String getBusinessPhone() { return businessPhone; }
    public void setBusinessPhone(String businessPhone) { this.businessPhone = businessPhone; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getOpeningHours() { return openingHours; }
    public void setOpeningHours(String openingHours) { this.openingHours = openingHours; }

    public String getDeliveryOptions() { return deliveryOptions; }
    public void setDeliveryOptions(String deliveryOptions) { this.deliveryOptions = deliveryOptions; }

    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }

    public int getProductCount() { return productCount; }
    public void setProductCount(int productCount) { this.productCount = productCount; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
