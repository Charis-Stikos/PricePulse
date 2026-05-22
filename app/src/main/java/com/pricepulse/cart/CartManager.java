package com.pricepulse.cart;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.pricepulse.model.CartItem;
import com.pricepulse.model.Product;

import java.util.ArrayList;
import java.util.List;

public final class CartManager {

    private static volatile CartManager instance;

    public static CartManager getInstance() {
        if (instance == null) {
            synchronized (CartManager.class) {
                if (instance == null) instance = new CartManager();
            }
        }
        return instance;
    }

    private final MutableLiveData<List<CartItem>> items = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Integer> cartCount = new MutableLiveData<>(0);

    private CartManager() {
    }

    public LiveData<List<CartItem>> getItems() {
        return items;
    }

    public LiveData<Integer> getCartCount() {
        return cartCount;
    }

    public void addProduct(Product product) {
        List<CartItem> current = new ArrayList<>(currentItems());
        int index = indexOf(current, product.getId());
        if (index != -1) {
            CartItem existing = current.get(index);
            current.set(index, existing.withQuantity(existing.getQuantity() + 1));
        } else {
            current.add(new CartItem(
                    product.getId(),
                    product.getTitle(),
                    product.getPrice(),
                    product.getImageUrl(),
                    1,
                    product.getShopId()
            ));
        }
        items.setValue(current);
        updateCount(current);
    }

    public void removeProduct(String productId) {
        List<CartItem> current = new ArrayList<>(currentItems());
        int index = indexOf(current, productId);
        if (index != -1) {
            CartItem item = current.get(index);
            if (item.getQuantity() > 1) {
                current.set(index, item.withQuantity(item.getQuantity() - 1));
            } else {
                current.remove(index);
            }
        }
        items.setValue(current);
        updateCount(current);
    }

    public void incrementQuantity(String productId) {
        List<CartItem> current = new ArrayList<>(currentItems());
        int index = indexOf(current, productId);
        if (index != -1) {
            CartItem item = current.get(index);
            current.set(index, item.withQuantity(item.getQuantity() + 1));
        }
        items.setValue(current);
        updateCount(current);
    }

    public void removeItemCompletely(String productId) {
        List<CartItem> current = new ArrayList<>(currentItems());
        int index = indexOf(current, productId);
        if (index != -1) current.remove(index);
        items.setValue(current);
        updateCount(current);
    }

    public void clearCart() {
        items.setValue(new ArrayList<>());
        cartCount.setValue(0);
    }

    public double getTotalAmount() {
        double total = 0.0;
        for (CartItem item : currentItems()) {
            total += item.getProductPrice() * item.getQuantity();
        }
        return total;
    }

    private List<CartItem> currentItems() {
        List<CartItem> value = items.getValue();
        return value != null ? value : new ArrayList<>();
    }

    private int indexOf(List<CartItem> list, String productId) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getProductId().equals(productId)) return i;
        }
        return -1;
    }

    private void updateCount(List<CartItem> list) {
        int total = 0;
        for (CartItem item : list) total += item.getQuantity();
        cartCount.setValue(total);
    }
}
