package com.pricepulse.viewmodel;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.ListenerRegistration;
import com.pricepulse.auth.AuthManager;
import com.pricepulse.model.Product;
import com.pricepulse.repository.FirebaseRepository;

import java.util.ArrayList;
import java.util.List;

public class ProductDetailViewModel extends ViewModel {

    private final FirebaseRepository repository = new FirebaseRepository();
    private final AuthManager authManager = new AuthManager();

    private final MutableLiveData<ProductDetailUiState> uiState = new MutableLiveData<>(new ProductDetailUiState.Loading());

    private String currentProductId;
    private List<String> likedProductIds = new ArrayList<>();
    private Product currentProduct;

    private ListenerRegistration productRegistration;
    private ListenerRegistration userRegistration;

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

        detach();

        productRegistration = repository.listenToProduct(productId, product -> {
            if (product == null) {
                uiState.setValue(new ProductDetailUiState.Error());
                return;
            }
            currentProduct = product;
            emitState();
        });

        FirebaseUser user = authManager.getCurrentUserValue();
        if (user != null && !user.isAnonymous()) {
            userRegistration = repository.listenToUserProfile(user.getUid(), profile -> {
                likedProductIds = profile != null && profile.getLikedProductIds() != null
                        ? new ArrayList<>(profile.getLikedProductIds())
                        : new ArrayList<>();
                emitState();
            });
        } else {
            likedProductIds = new ArrayList<>();
        }
    }

    private void emitState() {
        if (currentProduct == null || currentProductId == null) return;
        uiState.setValue(new ProductDetailUiState.Success(
                currentProduct, likedProductIds.contains(currentProductId)));
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
        emitState();

        repository.updateWishlist(user.getUid(), new ArrayList<>(likedProductIds), success -> {
            if (!success) {
                if (wasLiked) {
                    likedProductIds.add(productId);
                } else {
                    likedProductIds.remove(productId);
                }
                emitState();
            }
        });
    }

    private void detach() {
        if (productRegistration != null) {
            productRegistration.remove();
            productRegistration = null;
        }
        if (userRegistration != null) {
            userRegistration.remove();
            userRegistration = null;
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        detach();
    }
}
