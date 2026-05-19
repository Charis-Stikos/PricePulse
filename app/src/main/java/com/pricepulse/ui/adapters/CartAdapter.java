package com.pricepulse.ui.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.pricepulse.databinding.ItemCartBinding;
import com.pricepulse.model.CartItem;
import com.pricepulse.util.ImageLoader;

public class CartAdapter extends ListAdapter<CartItem, CartAdapter.VH> {

    public interface OnAction {
        void onAction(String productId);
    }

    private final OnAction onIncrement;
    private final OnAction onDecrement;
    private final OnAction onRemove;

    public CartAdapter(OnAction onIncrement, OnAction onDecrement, OnAction onRemove) {
        super(DIFF);
        this.onIncrement = onIncrement;
        this.onDecrement = onDecrement;
        this.onRemove = onRemove;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCartBinding b = ItemCartBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new VH(b);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        CartItem item = getItem(position);
        ItemCartBinding b = holder.b;
        b.itemTitle.setText(item.getProductTitle());
        b.itemPrice.setText(item.getProductPrice() + " €");
        b.quantityText.setText(String.valueOf(item.getQuantity()));
        ImageLoader.load(b.itemImage, item.getProductImageUrl());
        b.incrementButton.setOnClickListener(v -> onIncrement.onAction(item.getProductId()));
        b.decrementButton.setOnClickListener(v -> onDecrement.onAction(item.getProductId()));
        b.removeButton.setOnClickListener(v -> onRemove.onAction(item.getProductId()));
    }

    static class VH extends RecyclerView.ViewHolder {
        final ItemCartBinding b;

        VH(ItemCartBinding binding) {
            super(binding.getRoot());
            this.b = binding;
        }
    }

    private static final DiffUtil.ItemCallback<CartItem> DIFF = new DiffUtil.ItemCallback<CartItem>() {
        @Override
        public boolean areItemsTheSame(@NonNull CartItem oldItem, @NonNull CartItem newItem) {
            return oldItem.getProductId().equals(newItem.getProductId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull CartItem oldItem, @NonNull CartItem newItem) {
            return oldItem.getProductId().equals(newItem.getProductId())
                    && oldItem.getQuantity() == newItem.getQuantity()
                    && oldItem.getProductPrice() == newItem.getProductPrice();
        }
    };
}
