package com.pricepulse.viewmodel;

import com.pricepulse.model.Product;

import java.util.List;

public abstract class SearchUiState {

    private SearchUiState() {
    }

    public static final class Idle extends SearchUiState {
    }

    public static final class Loading extends SearchUiState {
    }

    public static final class Empty extends SearchUiState {
    }

    public static final class Success extends SearchUiState {
        public final List<Product> products;

        public Success(List<Product> products) {
            this.products = products;
        }
    }
}
