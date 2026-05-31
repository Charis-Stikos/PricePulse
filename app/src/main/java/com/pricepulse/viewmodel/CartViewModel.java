package com.pricepulse.viewmodel;

import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.ListenerRegistration;
import com.pricepulse.cart.CartManager;
import com.pricepulse.model.CartItem;
import com.pricepulse.model.Order;
import com.pricepulse.model.Shop;
import com.pricepulse.repository.FirebaseRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CartViewModel extends ViewModel {

    private final FirebaseRepository repository = new FirebaseRepository();
    private final CartManager cartManager = CartManager.getInstance();
    private final Handler handler = new Handler(Looper.getMainLooper());

    private final MutableLiveData<CartUiState> uiState = new MutableLiveData<>(new CartUiState.Idle());
    private final MutableLiveData<Shop> currentShop = new MutableLiveData<>();
    private final MediatorLiveData<Double> totalAmount = new MediatorLiveData<>(0.0);
    private ListenerRegistration shopRegistration;
    private String listenedShopId;

    public CartViewModel() {
        totalAmount.addSource(cartManager.getItems(), items -> {
            double total = 0.0;
            if (items != null) {
                for (CartItem item : items) total += item.getProductPrice() * item.getQuantity();
            }
            totalAmount.setValue(total);
            attachShopIfNeeded(resolveShopId(items));
        });
    }

    public LiveData<List<CartItem>> getCartItems() {
        return cartManager.getItems();
    }

    public LiveData<CartUiState> getUiState() {
        return uiState;
    }

    public LiveData<Shop> getCurrentShop() {
        return currentShop;
    }

    public LiveData<Double> getTotalAmount() {
        return totalAmount;
    }

    private String resolveShopId(List<CartItem> items) {
        if (items == null || items.isEmpty()) return "";
        return items.get(0).getShopId();
    }

    private void attachShopIfNeeded(String shopId) {
        if (shopId == null || shopId.isEmpty()) {
            detachShop();
            currentShop.setValue(null);
            return;
        }

        if (shopId.equals(listenedShopId) && shopRegistration != null) return;

        detachShop();
        listenedShopId = shopId;
        shopRegistration = repository.listenToShop(shopId, currentShop::setValue);
    }

    private void detachShop() {
        if (shopRegistration != null) {
            shopRegistration.remove();
            shopRegistration = null;
        }
        listenedShopId = null;
    }

    public void checkout() {
        Double totalValue = totalAmount.getValue();
        double total = totalValue != null ? totalValue : 0.0;

        checkout(
                0.0,
                0.0,
                total,
                false
        );
    }

    public void checkout(double deliveryFee,
                         double deliveryDiscountAmount,
                         double finalTotalAmount,
                         boolean locationDiscountApplied) {
        checkout(deliveryFee, deliveryDiscountAmount, finalTotalAmount, locationDiscountApplied,
                "", "", "", "", "", "");
    }

    public void checkout(double deliveryFee,
                         double deliveryDiscountAmount,
                         double finalTotalAmount,
                         boolean locationDiscountApplied,
                         String shippingFullName,
                         String shippingPhone,
                         String shippingAddress,
                         String shippingCity,
                         String shippingPostalCode,
                         String paymentMethod) {
        List<CartItem> items = cartManager.getItems().getValue();
        if (items == null || items.isEmpty()) return;

        uiState.setValue(new CartUiState.Processing());

        handler.postDelayed(() -> {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            Double totalValue = totalAmount.getValue();
            double total = totalValue != null ? totalValue : 0.0;

            // για τωρα ολα τα cart items μοιραζονται το ιδιο shop (single-shop prototype)
            String shopId = items.get(0).getShopId();

            Order order = new Order(
                    UUID.randomUUID().toString(),
                    currentUser != null ? currentUser.getUid() : "anonymous",
                    new ArrayList<>(items),
                    total,
                    "Pending",
                    System.currentTimeMillis(),
                    deliveryFee,
                    deliveryDiscountAmount,
                    finalTotalAmount,
                    locationDiscountApplied
            );
            order.setShopId(shopId);
            order.setShippingFullName(shippingFullName);
            order.setShippingPhone(shippingPhone);
            order.setShippingAddress(shippingAddress);
            order.setShippingCity(shippingCity);
            order.setShippingPostalCode(shippingPostalCode);
            order.setPaymentMethod(paymentMethod);

            repository.saveOrder(order, success -> {
                if (success) {
                    cartManager.clearCart();
                    uiState.setValue(new CartUiState.Success());
                } else {
                    uiState.setValue(new CartUiState.Error("Checkout failed. Please try again."));
                }
            });
        }, 1500L);
    }

    public void resetState() {
        uiState.setValue(new CartUiState.Idle());
    }

    public void incrementQuantity(String productId) {
        cartManager.incrementQuantity(productId);
    }

    public void decrementQuantity(String productId) {
        cartManager.removeProduct(productId);
    }

    public void removeItem(String productId) {
        cartManager.removeItemCompletely(productId);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        detachShop();
    }
}
