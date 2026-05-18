package com.pricepulse.viewmodel;

import com.pricepulse.model.Product;

import java.util.List;

public abstract class HomeUiState {

    private HomeUiState() {
    }

    public static final class Loading extends HomeUiState {
    }

    public static final class Success extends HomeUiState {
        public final List<Product> products;

        public Success(List<Product> products) {
            this.products = products;
        }
    }

    public static final class Error extends HomeUiState {
        public final String message;

        public Error(String message) {
            this.message = message;
        }
    }
}
