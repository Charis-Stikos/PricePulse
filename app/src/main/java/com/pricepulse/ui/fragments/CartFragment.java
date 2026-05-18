package com.pricepulse.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.pricepulse.R;
import com.pricepulse.databinding.FragmentCartBinding;
import com.pricepulse.model.CartItem;
import com.pricepulse.ui.adapters.CartAdapter;
import com.pricepulse.viewmodel.CartUiState;
import com.pricepulse.viewmodel.CartViewModel;

import java.util.List;
import java.util.Locale;

public class CartFragment extends Fragment {

    private FragmentCartBinding binding;
    private CartViewModel viewModel;
    private CartAdapter cartAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentCartBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(CartViewModel.class);

        cartAdapter = new CartAdapter(
                viewModel::incrementQuantity,
                viewModel::decrementQuantity,
                viewModel::removeItem
        );
        binding.cartRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.cartRecycler.setAdapter(cartAdapter);

        binding.goShoppingButton.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.homeFragment));
        binding.backHomeButton.setOnClickListener(v -> {
            viewModel.resetState();
            NavHostFragment.findNavController(this).navigate(R.id.homeFragment);
        });
        binding.checkoutButton.setOnClickListener(v -> viewModel.checkout());

        viewModel.getCartItems().observe(getViewLifecycleOwner(), cartAdapter::submitList);
        viewModel.getTotalAmount().observe(getViewLifecycleOwner(), total ->
                binding.totalAmountText.setText(String.format(Locale.getDefault(), "%.2f €", total != null ? total : 0.0)));
        viewModel.getUiState().observe(getViewLifecycleOwner(), this::render);
    }

    private void render(CartUiState state) {
        List<CartItem> items = viewModel.getCartItems().getValue();
        if (state instanceof CartUiState.Success) {
            binding.cartContent.setVisibility(View.GONE);
            binding.emptyCart.setVisibility(View.GONE);
            binding.checkoutSuccess.setVisibility(View.VISIBLE);
            binding.checkoutProgress.setVisibility(View.GONE);
            binding.checkoutButton.setText(getString(R.string.complete_order));
            binding.checkoutButton.setEnabled(true);
            return;
        }
        binding.checkoutSuccess.setVisibility(View.GONE);
        if (items == null || items.isEmpty()) {
            binding.cartContent.setVisibility(View.GONE);
            binding.emptyCart.setVisibility(View.VISIBLE);
        } else {
            binding.cartContent.setVisibility(View.VISIBLE);
            binding.emptyCart.setVisibility(View.GONE);
        }
        boolean processing = state instanceof CartUiState.Processing;
        binding.checkoutButton.setEnabled(!processing);
        binding.checkoutProgress.setVisibility(processing ? View.VISIBLE : View.GONE);
        binding.checkoutButton.setText(processing ? "" : getString(R.string.complete_order));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
