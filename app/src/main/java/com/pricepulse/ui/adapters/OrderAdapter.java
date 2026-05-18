package com.pricepulse.ui.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.pricepulse.R;
import com.pricepulse.databinding.ItemOrderBinding;
import com.pricepulse.model.CartItem;
import com.pricepulse.model.Order;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class OrderAdapter extends ListAdapter<Order, OrderAdapter.VH> {

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());

    public OrderAdapter() {
        super(DIFF);
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemOrderBinding b = ItemOrderBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new VH(b);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Order order = getItem(position);
        ItemOrderBinding b = holder.b;
        Context ctx = b.getRoot().getContext();

        String idLabel = order.getId().length() >= 8 ? order.getId().substring(0, 8) : order.getId();
        b.orderIdText.setText("Order ID: " + idLabel.toUpperCase(Locale.getDefault()));
        b.orderDateText.setText(dateFormat.format(new Date(order.getTimestamp())));
        b.orderStatus.setText(order.getStatus());
        b.orderTotal.setText(String.format(Locale.getDefault(), "Total: %.2f €", order.getTotalAmount()));

        b.itemsContainer.removeAllViews();
        for (CartItem item : order.getItems()) {
            TextView tv = new TextView(ctx);
            tv.setText(item.getQuantity() + "x " + item.getProductTitle());
            tv.setTextSize(14f);
            tv.setTextColor(ContextCompat.getColor(ctx, R.color.skroutz_text_primary));
            tv.setPadding(0, 4, 0, 4);
            b.itemsContainer.addView(tv);
        }
    }

    static class VH extends RecyclerView.ViewHolder {
        final ItemOrderBinding b;

        VH(ItemOrderBinding binding) {
            super(binding.getRoot());
            this.b = binding;
        }
    }

    private static final DiffUtil.ItemCallback<Order> DIFF = new DiffUtil.ItemCallback<Order>() {
        @Override
        public boolean areItemsTheSame(@NonNull Order oldItem, @NonNull Order newItem) {
            return oldItem.getId().equals(newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Order oldItem, @NonNull Order newItem) {
            return oldItem.getId().equals(newItem.getId())
                    && oldItem.getTimestamp() == newItem.getTimestamp()
                    && oldItem.getTotalAmount() == newItem.getTotalAmount();
        }
    };
}
