package com.pricepulse.viewmodel;

import com.pricepulse.model.Product;

public abstract class ProductDetailUiState {

    private ProductDetailUiState() {
    }

    public static final class Loading extends ProductDetailUiState {
    }

    public static final class Error extends ProductDetailUiState {
    }

    public static final class Success extends ProductDetailUiState {
        public final Product product;
        public final boolean isLiked;

        public Success(Product product, boolean isLiked) {
            this.product = product;
            this.isLiked = isLiked;
        }

        public Success withLiked(boolean liked) {
            return new Success(product, liked);
        }
    }
}
