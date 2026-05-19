package com.pricepulse.ui.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.pricepulse.databinding.ItemWishlistBinding;
import com.pricepulse.model.Product;
import com.pricepulse.util.ImageLoader;

public class WishlistAdapter extends ListAdapter<Product, WishlistAdapter.VH> {

    public interface OnClick {
        void onClick(Product product);
    }

    private final OnClick onClick;

    public WishlistAdapter(OnClick onClick) {
        super(DIFF);
        this.onClick = onClick;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemWishlistBinding b = ItemWishlistBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new VH(b);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Product p = getItem(position);
        ItemWishlistBinding b = holder.b;
        b.wishTitle.setText(p.getTitle());
        b.wishPrice.setText(p.getPrice() + " €");
        ImageLoader.load(b.wishImage, p.getImageUrl());
        b.rootClick.setOnClickListener(v -> onClick.onClick(p));
    }

    static class VH extends RecyclerView.ViewHolder {
        final ItemWishlistBinding b;

        VH(ItemWishlistBinding binding) {
            super(binding.getRoot());
            this.b = binding;
        }
    }

    private static final DiffUtil.ItemCallback<Product> DIFF = new DiffUtil.ItemCallback<Product>() {
        @Override
        public boolean areItemsTheSame(@NonNull Product oldItem, @NonNull Product newItem) {
            return oldItem.getId().equals(newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Product oldItem, @NonNull Product newItem) {
            return oldItem.getId().equals(newItem.getId())
                    && oldItem.getTitle().equals(newItem.getTitle())
                    && oldItem.getPrice() == newItem.getPrice();
        }
    };
}
