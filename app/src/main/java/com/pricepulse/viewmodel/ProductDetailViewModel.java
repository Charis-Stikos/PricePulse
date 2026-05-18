package com.pricepulse.viewmodel;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseUser;
import com.pricepulse.auth.AuthManager;
import com.pricepulse.model.Product;
import com.pricepulse.model.User;
import com.pricepulse.repository.FirebaseRepository;

import java.util.ArrayList;
import java.util.List;

public class ProductDetailViewModel extends ViewModel {

    private final FirebaseRepository repository = new FirebaseRepository();
    private final AuthManager authManager = new AuthManager();

    private final MutableLiveData<ProductDetailUiState> uiState = new MutableLiveData<>(new ProductDetailUiState.Loading());

    private String currentProductId;
    private List<String> likedProductIds = new ArrayList<>();

    public LiveData<ProductDetailUiState> getUiState() {
        return uiState;
    }

    public AuthManager getAuthManager() {
        return authManager;
    }

    public void loadProduct(@Nullable String productId) {
        if (productId == null) {
            uiState.setValue(new ProductDetailUiState.Error());
            return;
        }
        currentProductId = productId;
        uiState.setValue(new ProductDetailUiState.Loading());

        repository.getProductById(productId, product -> {
            if (product == null) {
                uiState.setValue(new ProductDetailUiState.Error());
                return;
            }
            FirebaseUser user = authManager.getCurrentUserValue();
            if (user != null && !user.isAnonymous()) {
                repository.getUserProfile(user.getUid(), profile -> {
                    likedProductIds = profile != null && profile.getLikedProductIds() != null
                            ? new ArrayList<>(profile.getLikedProductIds())
                            : new ArrayList<>();
                    uiState.setValue(new ProductDetailUiState.Success(product, likedProductIds.contains(productId)));
                });
            } else {
                likedProductIds = new ArrayList<>();
                uiState.setValue(new ProductDetailUiState.Success(product, false));
            }
        });
    }

    public void toggleLike() {
        if (currentProductId == null) return;
        FirebaseUser user = authManager.getCurrentUserValue();
        if (user == null || user.isAnonymous()) return;

        final String productId = currentProductId;
        final boolean wasLiked = likedProductIds.contains(productId);
        if (wasLiked) {
            likedProductIds.remove(productId);
        } else {
            likedProductIds.add(productId);
        }

        ProductDetailUiState current = uiState.getValue();
        if (current instanceof ProductDetailUiState.Success) {
            uiState.setValue(((ProductDetailUiState.Success) current).withLiked(!wasLiked));
        }

        repository.updateWishlist(user.getUid(), new ArrayList<>(likedProductIds), success -> {
            if (!success) {
                if (wasLiked) {
                    likedProductIds.add(productId);
                } else {
                    likedProductIds.remove(productId);
                }
                ProductDetailUiState rolledBack = uiState.getValue();
                if (rolledBack instanceof ProductDetailUiState.Success) {
                    uiState.setValue(((ProductDetailUiState.Success) rolledBack).withLiked(wasLiked));
                }
            }
        });
    }
}
