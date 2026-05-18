package com.pricepulse.viewmodel;

public abstract class CartUiState {

    private CartUiState() {
    }

    public static final class Idle extends CartUiState {
    }

    public static final class Processing extends CartUiState {
    }

    public static final class Success extends CartUiState {
    }

    public static final class Error extends CartUiState {
        public final String message;

        public Error(String message) {
            this.message = message;
        }
    }
}
