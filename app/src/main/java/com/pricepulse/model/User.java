package com.pricepulse.model;

import java.util.ArrayList;
import java.util.List;

public class User {
    private String uid = "";
    private String email = "";
    private String displayName = "";
    private List<String> likedProductIds = new ArrayList<>();
    private boolean admin = false;
    private boolean shopOwner = false;
    private String ownedShopId = "";

    public User() {
    }

    public User(String uid, String email, String displayName, List<String> likedProductIds) {
        this.uid = uid;
        this.email = email;
        this.displayName = displayName;
        this.likedProductIds = likedProductIds;
    }

    public boolean isAdmin() { return admin; }
    public void setAdmin(boolean admin) { this.admin = admin; }

    public boolean isShopOwner() { return shopOwner; }
    public void setShopOwner(boolean shopOwner) { this.shopOwner = shopOwner; }

    public String getOwnedShopId() { return ownedShopId; }
    public void setOwnedShopId(String ownedShopId) { this.ownedShopId = ownedShopId; }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public List<String> getLikedProductIds() { return likedProductIds; }
    public void setLikedProductIds(List<String> likedProductIds) { this.likedProductIds = likedProductIds; }
}
