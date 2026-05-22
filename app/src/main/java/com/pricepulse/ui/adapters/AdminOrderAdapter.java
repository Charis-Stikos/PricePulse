package com.pricepulse.ui.adapters;

import android.graphics.PorterDuff;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.PopupMenu;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.pricepulse.R;
import com.pricepulse.databinding.ItemAdminOrderBinding;
import com.pricepulse.model.CartItem;
import com.pricepulse.model.Order;

import java.util.Locale;

public class AdminOrderAdapter extends ListAdapter<Order, AdminOrderAdapter.VH> {

    public interface OnStatusChange {
        void onStatusChanged(Order order, String newStatus);
    }

    public static final String STATUS_PENDING = "Pending";
    public static final String STATUS_IN_TRANSIT = "In Transit";
    public static final String STATUS_COMPLETED = "Completed";
    private String expandedOrderId = null;

    private final OnStatusChange listener;

    public AdminOrderAdapter(OnStatusChange listener) {
        super(DIFF);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<Order> DIFF = new DiffUtil.ItemCallback<Order>() {
        @Override
        public boolean areItemsTheSame(@NonNull Order a, @NonNull Order b) {
            return a.getId().equals(b.getId());
        }
        @Override
        public boolean areContentsTheSame(@NonNull Order a, @NonNull Order b) {
            return a.getStatus().equals(b.getStatus())
                    && a.getTotalAmount() == b.getTotalAmount();
        }
    };

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemAdminOrderBinding b = ItemAdminOrderBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new VH(b);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        holder.bind(getItem(position));
    }

    class VH extends RecyclerView.ViewHolder {
        final ItemAdminOrderBinding b;
        VH(ItemAdminOrderBinding binding) {
            super(binding.getRoot());
            this.b = binding;
        }

        void bind(Order order) {
            b.adminOrderId.setText(b.getRoot().getContext().getString(R.string.order_id_format,
                    shortId(order.getId())));
            b.adminOrderTotal.setText(String.format(Locale.getDefault(), "%.2f €", order.getTotalAmount()));
            b.adminOrderUser.setText(order.getUserId());

            StringBuilder items = new StringBuilder();
            if (order.getItems() != null) {
                for (int i = 0; i < order.getItems().size(); i++) {
                    CartItem ci = order.getItems().get(i);
                    if (i > 0) items.append(", ");
                    items.append(ci.getQuantity()).append("× ").append(ci.getProductTitle());
                }
            }
            b.adminOrderItems.setText(items.toString());

            boolean expanded = order.getId() != null && order.getId().equals(expandedOrderId);
            b.adminOrderDetailsContainer.setVisibility(expanded ? android.view.View.VISIBLE : android.view.View.GONE);
            b.adminOrderViewBtn.setText(expanded
                    ? b.getRoot().getContext().getString(R.string.hide_order)
                    : b.getRoot().getContext().getString(R.string.view_order));

            b.adminOrderDetailsItems.setText(buildOrderDetails(order));
            b.adminOrderShipping.setText(buildShippingDetails(b, order));

            b.adminOrderViewBtn.setOnClickListener(v -> {
                if (order.getId() != null && order.getId().equals(expandedOrderId)) {
                    expandedOrderId = null;
                } else {
                    expandedOrderId = order.getId();
                }

                int pos = getBindingAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    notifyItemChanged(pos);
                }
            });

            applyStatusStyle(order.getStatus());

            b.adminOrderUpdateBtn.setOnClickListener(v -> {
                PopupMenu menu = new PopupMenu(v.getContext(), v);
                menu.getMenu().add(0, 1, 0, STATUS_PENDING);
                menu.getMenu().add(0, 2, 0, STATUS_IN_TRANSIT);
                menu.getMenu().add(0, 3, 0, STATUS_COMPLETED);
                menu.setOnMenuItemClickListener(item -> {
                    String chosen = item.getTitle().toString();
                    if (!chosen.equals(order.getStatus())) {
                        listener.onStatusChanged(order, chosen);
                    }
                    return true;
                });
                menu.show();
            });
        }

        private void applyStatusStyle(String status) {
            b.adminOrderStatusChip.setText(status);
            int colorRes;
            switch (status) {
                case STATUS_COMPLETED: colorRes = R.color.success_green; break;
                case STATUS_IN_TRANSIT: colorRes = R.color.skroutz_blue; break;
                default: colorRes = R.color.skroutz_orange;
            }
            int color = ContextCompat.getColor(b.getRoot().getContext(), colorRes);
            b.adminOrderStatusChip.getBackground().mutate()
                    .setColorFilter(color, PorterDuff.Mode.SRC_IN);
        }

        private String buildShippingDetails(ItemAdminOrderBinding b, Order order) {
            String name = nullSafe(order.getShippingFullName());
            String phone = nullSafe(order.getShippingPhone());
            String address = nullSafe(order.getShippingAddress());
            String city = nullSafe(order.getShippingCity());
            String postal = nullSafe(order.getShippingPostalCode());
            String payment = nullSafe(order.getPaymentMethod());

            boolean anything = !(name.isEmpty() && phone.isEmpty() && address.isEmpty()
                    && city.isEmpty() && postal.isEmpty() && payment.isEmpty());
            if (!anything) {
                return b.getRoot().getContext().getString(R.string.shipping_no_info);
            }

            StringBuilder out = new StringBuilder();
            if (!name.isEmpty()) out.append(name).append("\n");
            if (!phone.isEmpty()) out.append(phone).append("\n");
            if (!address.isEmpty()) out.append(address);
            if (!postal.isEmpty() || !city.isEmpty()) {
                if (!address.isEmpty()) out.append("\n");
                if (!postal.isEmpty()) out.append(postal);
                if (!postal.isEmpty() && !city.isEmpty()) out.append(" ");
                if (!city.isEmpty()) out.append(city);
            }
            if (!payment.isEmpty()) {
                out.append("\n\n").append(b.getRoot().getContext()
                        .getString(R.string.payment_method_label))
                        .append(": ").append(payment);
            }
            return out.toString();
        }

        private String nullSafe(String s) { return s == null ? "" : s.trim(); }

        private String buildOrderDetails(Order order) {
            StringBuilder details = new StringBuilder();

            if (order.getItems() == null || order.getItems().isEmpty()) {
                return "No items available";
            }

            for (CartItem ci : order.getItems()) {
                double lineTotal = ci.getProductPrice() * ci.getQuantity();

                details.append("• ")
                        .append(ci.getQuantity())
                        .append("× ")
                        .append(ci.getProductTitle())
                        .append(" — ")
                        .append(String.format(Locale.getDefault(), "%.2f €", lineTotal))
                        .append("\n");
            }

            return details.toString().trim();
        }

        private String shortId(String id) {
            if (id == null) return "";
            return id.length() > 8 ? id.substring(0, 8) : id;
        }
    }
}
