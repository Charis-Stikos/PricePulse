package com.pricepulse.ui.adapters;

import com.pricepulse.model.Product;

public abstract class ProductListItem {

    private ProductListItem() {
    }

    public static final class Item extends ProductListItem {
        public final Product product;

        public Item(Product product) {
            this.product = product;
        }
    }

    public static final class Shimmer extends ProductListItem {
        public final int id;

        public Shimmer(int id) {
            this.id = id;
        }
    }
}
