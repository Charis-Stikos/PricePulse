package com.pricepulse.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.Manifest;
import android.content.pm.PackageManager;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;

import com.pricepulse.R;
import com.pricepulse.databinding.FragmentCheckoutBinding;
import com.pricepulse.model.CartItem;
import com.pricepulse.ui.adapters.CheckoutSummaryAdapter;
import com.pricepulse.viewmodel.CartUiState;
import com.pricepulse.viewmodel.CartViewModel;

import java.util.List;
import java.util.Locale;

public class CheckoutFragment extends Fragment {

    private FragmentCheckoutBinding binding;
    private CartViewModel viewModel;
    private CheckoutSummaryAdapter checkoutAdapter;
    private String selectedPaymentMethod = "card";

    private ActivityResultLauncher<String> locationPermissionLauncher;

    private static final double DELIVERY_FEE = 3.50;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentCheckoutBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(CartViewModel.class);

        checkoutAdapter = new CheckoutSummaryAdapter();
        binding.checkoutItemsRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.checkoutItemsRecycler.setAdapter(checkoutAdapter);

        binding.backButton.setOnClickListener(v ->
                NavHostFragment.findNavController(this).popBackStack()
        );

        binding.cardPaymentOption.setOnClickListener(v -> {
            selectedPaymentMethod = "card";
            updatePaymentMethodUi();
        });

        binding.cashOnDeliveryOption.setOnClickListener(v -> {
            selectedPaymentMethod = "cash";
            updatePaymentMethodUi();
        });

        updatePaymentMethodUi();

        locationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (Boolean.TRUE.equals(isGranted)) {
                        binding.locationDiscountStatusText.setText(
                                getString(R.string.location_permission_granted)
                        );
                        binding.locationDiscountStatusText.setTextColor(
                                requireContext().getColor(R.color.skroutz_blue)
                        );
                    } else {
                        binding.locationDiscountStatusText.setText(
                                getString(R.string.location_permission_denied)
                        );
                        binding.locationDiscountStatusText.setTextColor(
                                requireContext().getColor(R.color.error_red)
                        );
                    }
                }
        );

        binding.verifyLocationButton.setOnClickListener(v -> requestLocationPermission());

        binding.placeOrderButton.setOnClickListener(v -> {
            if (!validateDeliveryFields()) return;
            authenticateThenCheckout();
        });

        viewModel.getCartItems().observe(getViewLifecycleOwner(), items -> {
            checkoutAdapter.submitList(items);

            if (items == null || items.isEmpty()) {
                NavHostFragment.findNavController(this).popBackStack();
            }
        });

        viewModel.getTotalAmount().observe(getViewLifecycleOwner(), total -> {
            double subtotal = total != null ? total : 0.0;
            double finalTotal = subtotal + DELIVERY_FEE;

            binding.subtotalAmountText.setText(
                    String.format(Locale.getDefault(), "%.2f €", subtotal)
            );

            binding.deliveryFeeText.setText(
                    String.format(Locale.getDefault(), "%.2f €", DELIVERY_FEE)
            );

            binding.finalTotalAmountText.setText(
                    String.format(Locale.getDefault(), "%.2f €", finalTotal)
            );
        });

        viewModel.getUiState().observe(getViewLifecycleOwner(), this::renderState);
    }

    private boolean validateDeliveryFields() {
        String fullName = text(binding.fullNameInput);
        String phone = text(binding.phoneInput);
        String address = text(binding.addressInput);
        String city = text(binding.cityInput);
        String postalCode = text(binding.postalCodeInput);

        if (fullName.isEmpty()
                || phone.isEmpty()
                || address.isEmpty()
                || city.isEmpty()
                || postalCode.isEmpty()) {
            Toast.makeText(
                    requireContext(),
                    R.string.all_required_fields_missing,
                    Toast.LENGTH_SHORT
            ).show();
            return false;
        }

        return true;
    }

    private void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED) {

            binding.locationDiscountStatusText.setText(
                    getString(R.string.location_permission_granted)
            );
            binding.locationDiscountStatusText.setTextColor(
                    requireContext().getColor(R.color.skroutz_blue)
            );
            return;
        }

        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
    }

    private void authenticateThenCheckout() {
        BiometricManager biometricManager = BiometricManager.from(requireContext());
        int authenticators = BiometricManager.Authenticators.BIOMETRIC_WEAK;
        int canAuth = biometricManager.canAuthenticate(authenticators);

        if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) {
            int messageRes;
            if (canAuth == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED) {
                messageRes = R.string.biometric_not_enrolled;
            } else {
                messageRes = R.string.biometric_not_available;
            }
            Toast.makeText(requireContext(), messageRes, Toast.LENGTH_LONG).show();
            return;
        }

        BiometricPrompt prompt = new BiometricPrompt(
                this,
                ContextCompat.getMainExecutor(requireContext()),
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(
                            @NonNull BiometricPrompt.AuthenticationResult result) {
                        if (binding == null) return;
                        viewModel.checkout();
                    }

                    @Override
                    public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                        if (binding == null) return;
                        Toast.makeText(
                                requireContext(),
                                R.string.checkout_auth_failed,
                                Toast.LENGTH_SHORT
                        ).show();
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        // user gets in-prompt feedback και μπορει να ξαναπροσπαθησει
                    }
                }
        );

        BiometricPrompt.PromptInfo info = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.biometric_prompt_title))
                .setSubtitle(getString(R.string.biometric_prompt_subtitle))
                .setNegativeButtonText(getString(android.R.string.cancel))
                .setAllowedAuthenticators(authenticators)
                .build();

        prompt.authenticate(info);
    }

    private void updatePaymentMethodUi() {
        boolean cardSelected = "card".equals(selectedPaymentMethod);
        boolean cashSelected = "cash".equals(selectedPaymentMethod);

        binding.cardPaymentRadio.setText(cardSelected ? "●" : "○");
        binding.cashPaymentRadio.setText(cashSelected ? "●" : "○");

        binding.cardPaymentRadio.setTextColor(
                requireContext().getColor(cardSelected
                        ? R.color.skroutz_orange
                        : R.color.skroutz_text_secondary)
        );

        binding.cashPaymentRadio.setTextColor(
                requireContext().getColor(cashSelected
                        ? R.color.skroutz_orange
                        : R.color.skroutz_text_secondary)
        );

        binding.cardPaymentOption.setBackgroundResource(
                cardSelected ? R.drawable.bg_payment_selected : R.drawable.bg_payment_unselected
        );

        binding.cashOnDeliveryOption.setBackgroundResource(
                cashSelected ? R.drawable.bg_payment_selected : R.drawable.bg_payment_unselected
        );
    }

    private void renderState(CartUiState state) {
        boolean processing = state instanceof CartUiState.Processing;

        binding.placeOrderButton.setEnabled(!processing);
        binding.placeOrderButton.setText(
                processing ? "" : getString(R.string.place_order)
        );

        if (state instanceof CartUiState.Success) {
            Toast.makeText(
                    requireContext(),
                    R.string.order_successful,
                    Toast.LENGTH_SHORT
            ).show();

            viewModel.resetState();

            NavHostFragment.findNavController(this)
                    .navigate(R.id.cartFragment);
        } else if (state instanceof CartUiState.Error) {
            Toast.makeText(
                    requireContext(),
                    ((CartUiState.Error) state).message,
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    private static String text(android.widget.EditText input) {
        return input.getText() != null
                ? input.getText().toString().trim()
                : "";
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}