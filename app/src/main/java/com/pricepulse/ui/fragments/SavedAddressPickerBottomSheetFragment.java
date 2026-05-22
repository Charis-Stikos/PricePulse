package com.pricepulse.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.pricepulse.databinding.BottomSheetSavedAddressesBinding;
import com.pricepulse.model.Address;
import com.pricepulse.ui.adapters.AddressPickerAdapter;

import java.util.List;

public class SavedAddressPickerBottomSheetFragment extends BottomSheetDialogFragment {

    public interface OnAddressPickedListener {
        void onAddressPicked(Address address);
    }

    private BottomSheetSavedAddressesBinding binding;
    private List<Address> addresses;
    private OnAddressPickedListener listener;

    public static SavedAddressPickerBottomSheetFragment create(
            List<Address> addresses, OnAddressPickedListener listener) {
        SavedAddressPickerBottomSheetFragment sheet = new SavedAddressPickerBottomSheetFragment();
        sheet.addresses = addresses;
        sheet.listener = listener;
        return sheet;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = BottomSheetSavedAddressesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        AddressPickerAdapter adapter = new AddressPickerAdapter(address -> {
            if (listener != null) listener.onAddressPicked(address);
            dismiss();
        });
        binding.pickerRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.pickerRecycler.setAdapter(adapter);
        adapter.submitList(addresses);

        boolean empty = addresses == null || addresses.isEmpty();
        binding.pickerEmptyText.setVisibility(empty ? View.VISIBLE : View.GONE);
        binding.pickerRecycler.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
