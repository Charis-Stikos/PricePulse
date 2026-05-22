package com.pricepulse.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.pricepulse.databinding.ItemAddressBinding;
import com.pricepulse.model.Address;

public class AddressAdapter extends ListAdapter<Address, AddressAdapter.VH> {

    public interface OnAddressAction {
        void onEdit(Address address);
        void onDelete(Address address);
        void onSetDefault(Address address);
    }

    private final OnAddressAction listener;

    public AddressAdapter(OnAddressAction listener) {
        super(DIFF);
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemAddressBinding b = ItemAddressBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new VH(b);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        holder.bind(getItem(position));
    }

    class VH extends RecyclerView.ViewHolder {
        final ItemAddressBinding b;

        VH(ItemAddressBinding binding) {
            super(binding.getRoot());
            this.b = binding;
        }

        void bind(Address address) {
            b.addressLabelText.setText(address.getLabel());
            b.addressFullNameText.setText(address.getFullName());
            b.addressLineText.setText(address.getAddressLine()
                    + ", " + address.getPostalCode()
                    + " " + address.getCity());
            b.addressPhoneText.setText(address.getPhone());

            b.defaultBadge.setVisibility(address.isDefaultAddress() ? View.VISIBLE : View.GONE);
            b.addressSetDefaultButton.setVisibility(address.isDefaultAddress() ? View.GONE : View.VISIBLE);

            b.addressEditButton.setOnClickListener(v -> listener.onEdit(address));
            b.addressDeleteButton.setOnClickListener(v -> listener.onDelete(address));
            b.addressSetDefaultButton.setOnClickListener(v -> listener.onSetDefault(address));
            b.addressCardRoot.setOnClickListener(v -> listener.onEdit(address));
        }
    }

    private static final DiffUtil.ItemCallback<Address> DIFF = new DiffUtil.ItemCallback<Address>() {
        @Override
        public boolean areItemsTheSame(@NonNull Address oldItem, @NonNull Address newItem) {
            return oldItem.getId().equals(newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Address oldItem, @NonNull Address newItem) {
            return oldItem.getId().equals(newItem.getId())
                    && oldItem.getLabel().equals(newItem.getLabel())
                    && oldItem.getFullName().equals(newItem.getFullName())
                    && oldItem.getPhone().equals(newItem.getPhone())
                    && oldItem.getAddressLine().equals(newItem.getAddressLine())
                    && oldItem.getCity().equals(newItem.getCity())
                    && oldItem.getPostalCode().equals(newItem.getPostalCode())
                    && oldItem.isDefaultAddress() == newItem.isDefaultAddress();
        }
    };
}
