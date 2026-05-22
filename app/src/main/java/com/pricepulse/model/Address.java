package com.pricepulse.model;

public class Address {

    public static final String LABEL_HOME = "Home";
    public static final String LABEL_WORK = "Work";
    public static final String LABEL_OTHER = "Other";

    private String id = "";
    private String userId = "";
    private String label = LABEL_HOME;
    private String fullName = "";
    private String phone = "";
    private String addressLine = "";
    private String city = "";
    private String postalCode = "";
    // ονομα defaultAddress αντι για default επειδη το default ειναι java keyword,
    // και ονομα isDefault δεν δουλευει με τη firestore (το is αφαιρειται απο το getter ονομα)
    private boolean defaultAddress = false;
    private long timestamp = System.currentTimeMillis();

    public Address() {
    }

    public Address(String id, String userId, String label, String fullName, String phone,
                   String addressLine, String city, String postalCode,
                   boolean defaultAddress, long timestamp) {
        this.id = id;
        this.userId = userId;
        this.label = label;
        this.fullName = fullName;
        this.phone = phone;
        this.addressLine = addressLine;
        this.city = city;
        this.postalCode = postalCode;
        this.defaultAddress = defaultAddress;
        this.timestamp = timestamp;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAddressLine() { return addressLine; }
    public void setAddressLine(String addressLine) { this.addressLine = addressLine; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getPostalCode() { return postalCode; }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }

    public boolean isDefaultAddress() { return defaultAddress; }
    public void setDefaultAddress(boolean defaultAddress) { this.defaultAddress = defaultAddress; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
