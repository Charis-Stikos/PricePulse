package com.pricepulse.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.pricepulse.R;
import com.pricepulse.util.ImageLoader;
import com.pricepulse.databinding.ItemProductBinding;
import com.pricepulse.databinding.ItemProductShimmerBinding;
import com.pricepulse.model.Product;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ProductAdapter extends ListAdapter<ProductListItem, RecyclerView.ViewHolder> {

    public interface OnProductClick {
        void onClick(Product product);
    }

    private static final int TYPE_PRODUCT = 0;
    private static final int TYPE_SHIMMER = 1;

    private final OnProductClick onClick;

    public ProductAdapter(OnProductClick onClick) {
        super(DIFF);
        this.onClick = onClick;
    }

    public void submitShimmer(int count) {
        List<ProductListItem> items = new ArrayList<>();
        for (int i = 0; i < count; i++) items.add(new ProductListItem.Shimmer(i));
        submitList(items);
    }

    public void submitProducts(List<Product> products) {
        List<ProductListItem> items = new ArrayList<>();
        for (Product p : products) items.add(new ProductListItem.Item(p));
        submitList(items);
    }

    @Override
    public int getItemViewType(int position) {
        ProductListItem item = getItem(position);
        return item instanceof ProductListItem.Shimmer ? TYPE_SHIMMER : TYPE_PRODUCT;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_SHIMMER) {
            return new ShimmerVH(ItemProductShimmerBinding.inflate(inflater, parent, false));
        }
        return new ProductVH(ItemProductBinding.inflate(inflater, parent, false), onClick);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ProductListItem item = getItem(position);
        if (holder instanceof ProductVH && item instanceof ProductListItem.Item) {
            ((ProductVH) holder).bind(((ProductListItem.Item) item).product);
        }
    }

    static class ProductVH extends RecyclerView.ViewHolder {
        private final ItemProductBinding b;
        private final OnProductClick onClick;
        private final ImageView[] stars;

        ProductVH(ItemProductBinding binding, OnProductClick onClick) {
            super(binding.getRoot());
            this.b = binding;
            this.onClick = onClick;
            this.stars = new ImageView[]{b.star1, b.star2, b.star3, b.star4, b.star5};
        }

        void bind(Product p) {
            b.productTitle.setText(p.getTitle());
            b.reviewCount.setText("(" + p.getReviewCount() + ")");
            b.productPrice.setText(String.format(Locale.getDefault(), "%.2f €", p.getPrice()));
            String shopName = p.getShopName();
            if (shopName != null && !shopName.isEmpty()) {
                b.shopCount.setText(b.getRoot().getContext().getString(R.string.sold_by_format, shopName));
                b.shopCount.setVisibility(View.VISIBLE);
            } else {
                b.shopCount.setVisibility(View.GONE);
            }
            int gold = ContextCompat.getColor(b.getRoot().getContext(), R.color.star_gold);
            int gray = ContextCompat.getColor(b.getRoot().getContext(), R.color.skroutz_gray);
            int filled = (int) p.getRating();
            for (int i = 0; i < stars.length; i++) {
                stars[i].setColorFilter(i < filled ? gold : gray);
            }
            ImageLoader.load(b.productImage, p.getImageUrl());
            b.rootClick.setOnClickListener(v -> onClick.onClick(p));
        }
    }

    static class ShimmerVH extends RecyclerView.ViewHolder {
        ShimmerVH(ItemProductShimmerBinding binding) {
            super(binding.getRoot());
        }
    }

    private static final DiffUtil.ItemCallback<ProductListItem> DIFF = new DiffUtil.ItemCallback<ProductListItem>() {
        @Override
        public boolean areItemsTheSame(@NonNull ProductListItem oldItem, @NonNull ProductListItem newItem) {
            if (oldItem instanceof ProductListItem.Item && newItem instanceof ProductListItem.Item) {
                return ((ProductListItem.Item) oldItem).product.getId()
                        .equals(((ProductListItem.Item) newItem).product.getId());
            }
            if (oldItem instanceof ProductListItem.Shimmer && newItem instanceof ProductListItem.Shimmer) {
                return ((ProductListItem.Shimmer) oldItem).id == ((ProductListItem.Shimmer) newItem).id;
            }
            return false;
        }

        @Override
        public boolean areContentsTheSame(@NonNull ProductListItem oldItem, @NonNull ProductListItem newItem) {
            return areItemsTheSame(oldItem, newItem);
        }
    };
}
