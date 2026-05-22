package com.pricepulse.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.pricepulse.databinding.ItemAddressPickerBinding;
import com.pricepulse.model.Address;

public class AddressPickerAdapter
        extends ListAdapter<Address, AddressPickerAdapter.VH> {

    public interface OnPick {
        void onPick(Address address);
    }

    private final OnPick onPick;

    public AddressPickerAdapter(OnPick onPick) {
        super(DIFF);
        this.onPick = onPick;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemAddressPickerBinding b = ItemAddressPickerBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new VH(b);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        holder.bind(getItem(position));
    }

    class VH extends RecyclerView.ViewHolder {
        final ItemAddressPickerBinding b;

        VH(ItemAddressPickerBinding binding) {
            super(binding.getRoot());
            this.b = binding;
        }

        void bind(Address address) {
            b.pickerAddressLabel.setText(address.getLabel());
            b.pickerDefaultBadge.setVisibility(address.isDefaultAddress() ? View.VISIBLE : View.GONE);
            b.pickerAddressLine.setText(address.getAddressLine()
                    + ", " + address.getPostalCode()
                    + " " + address.getCity());
            b.getRoot().setOnClickListener(v -> onPick.onPick(address));
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
                    && oldItem.isDefaultAddress() == newItem.isDefaultAddress()
                    && oldItem.getAddressLine().equals(newItem.getAddressLine())
                    && oldItem.getPostalCode().equals(newItem.getPostalCode())
                    && oldItem.getCity().equals(newItem.getCity());
        }
    };
}
