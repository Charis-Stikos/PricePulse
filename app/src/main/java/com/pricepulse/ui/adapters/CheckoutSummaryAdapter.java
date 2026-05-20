package com.pricepulse.ui.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.pricepulse.databinding.ItemCheckoutSummaryBinding;
import com.pricepulse.model.CartItem;
import com.pricepulse.util.ImageLoader;

import java.util.Locale;

public class CheckoutSummaryAdapter extends ListAdapter<CartItem, CheckoutSummaryAdapter.VH> {

    public CheckoutSummaryAdapter() {
        super(DIFF);
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCheckoutSummaryBinding binding = ItemCheckoutSummaryBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new VH(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        holder.bind(getItem(position));
    }

    static class VH extends RecyclerView.ViewHolder {
        private final ItemCheckoutSummaryBinding binding;

        VH(ItemCheckoutSummaryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(CartItem item) {
            binding.checkoutItemTitle.setText(item.getProductTitle());
            binding.checkoutItemQuantity.setText(
                    String.format(Locale.getDefault(), "Qty: %d", item.getQuantity())
            );

            double lineTotal = item.getProductPrice() * item.getQuantity();
            binding.checkoutItemPrice.setText(
                    String.format(Locale.getDefault(), "%.2f €", lineTotal)
            );

            ImageLoader.load(binding.checkoutItemImage, item.getProductImageUrl());
        }
    }

    private static final DiffUtil.ItemCallback<CartItem> DIFF =
            new DiffUtil.ItemCallback<CartItem>() {
                @Override
                public boolean areItemsTheSame(@NonNull CartItem oldItem, @NonNull CartItem newItem) {
                    return oldItem.getProductId().equals(newItem.getProductId());
                }

                @Override
                public boolean areContentsTheSame(@NonNull CartItem oldItem, @NonNull CartItem newItem) {
                    return oldItem.getProductId().equals(newItem.getProductId())
                            && oldItem.getQuantity() == newItem.getQuantity()
                            && oldItem.getProductPrice() == newItem.getProductPrice()
                            && oldItem.getProductTitle().equals(newItem.getProductTitle());
                }
            };
}