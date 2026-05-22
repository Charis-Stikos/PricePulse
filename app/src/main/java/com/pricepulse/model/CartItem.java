package com.pricepulse.model;

public class CartItem {
    private String productId = "";
    private String productTitle = "";
    private double productPrice = 0.0;
    private String productImageUrl = "";
    private int quantity = 1;
    private String shopId = "";

    public CartItem() {
    }

    public CartItem(String productId, String productTitle, double productPrice,
                    String productImageUrl, int quantity) {
        this.productId = productId;
        this.productTitle = productTitle;
        this.productPrice = productPrice;
        this.productImageUrl = productImageUrl;
        this.quantity = quantity;
    }

    public CartItem(String productId, String productTitle, double productPrice,
                    String productImageUrl, int quantity, String shopId) {
        this(productId, productTitle, productPrice, productImageUrl, quantity);
        this.shopId = shopId;
    }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public String getProductTitle() { return productTitle; }
    public void setProductTitle(String productTitle) { this.productTitle = productTitle; }

    public double getProductPrice() { return productPrice; }
    public void setProductPrice(double productPrice) { this.productPrice = productPrice; }

    public String getProductImageUrl() { return productImageUrl; }
    public void setProductImageUrl(String productImageUrl) { this.productImageUrl = productImageUrl; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public String getShopId() { return shopId; }
    public void setShopId(String shopId) { this.shopId = shopId; }

    public CartItem withQuantity(int newQuantity) {
        return new CartItem(productId, productTitle, productPrice, productImageUrl, newQuantity, shopId);
    }
}
