package com.pricepulse.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.pricepulse.R;
import com.pricepulse.databinding.ItemAdminShopBinding;
import com.pricepulse.model.Shop;

public class ShopAdapter extends ListAdapter<Shop, ShopAdapter.VH> {

    public interface OnShopClick {
        void onClick(Shop shop);
    }

    private final OnShopClick onClick;

    public ShopAdapter(OnShopClick onClick) {
        super(DIFF);
        this.onClick = onClick;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemAdminShopBinding b = ItemAdminShopBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new VH(b);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        holder.bind(getItem(position));
    }

    class VH extends RecyclerView.ViewHolder {
        final ItemAdminShopBinding b;

        VH(ItemAdminShopBinding binding) {
            super(binding.getRoot());
            this.b = binding;
        }

        void bind(Shop shop) {
            String name = shop.getName() != null ? shop.getName() : "";
            b.adminShopName.setText(name);
            b.adminShopInitial.setText(name.isEmpty() ? "S" : name.substring(0, 1).toUpperCase());
            b.adminShopActiveBadge.setVisibility(shop.isActive() ? View.VISIBLE : View.GONE);

            String ownerDisplay = shop.getOwnerEmail() != null && !shop.getOwnerEmail().isEmpty()
                    ? shop.getOwnerEmail()
                    : (shop.getOwnerId() != null && !shop.getOwnerId().isEmpty()
                            ? shop.getOwnerId() : null);
            String ownerLabel = ownerDisplay != null
                    ? b.getRoot().getContext().getString(R.string.owner_label_format, ownerDisplay)
                    : b.getRoot().getContext().getString(R.string.no_owner_assigned);
            b.adminShopOwner.setText(ownerLabel);

            b.adminShopProducts.setText(
                    b.getRoot().getContext().getString(
                            R.string.products_in_shop_format, shop.getProductCount()));

            b.adminShopCardRoot.setOnClickListener(v -> onClick.onClick(shop));
        }
    }

    private static final DiffUtil.ItemCallback<Shop> DIFF = new DiffUtil.ItemCallback<Shop>() {
        @Override
        public boolean areItemsTheSame(@NonNull Shop oldItem, @NonNull Shop newItem) {
            return oldItem.getId().equals(newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Shop oldItem, @NonNull Shop newItem) {
            return oldItem.getId().equals(newItem.getId())
                    && oldItem.getName().equals(newItem.getName())
                    && oldItem.getOwnerId().equals(newItem.getOwnerId())
                    && oldItem.getOwnerEmail().equals(newItem.getOwnerEmail())
                    && oldItem.getProductCount() == newItem.getProductCount()
                    && oldItem.isActive() == newItem.isActive();
        }
    };
}
