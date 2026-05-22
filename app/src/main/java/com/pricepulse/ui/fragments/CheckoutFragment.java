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

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;

import com.pricepulse.util.LocationDiscountHelper;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.ListenerRegistration;
import com.pricepulse.R;
import com.pricepulse.databinding.FragmentCheckoutBinding;
import com.pricepulse.model.Address;
import com.pricepulse.repository.FirebaseRepository;
import com.pricepulse.ui.adapters.CheckoutSummaryAdapter;
import com.pricepulse.viewmodel.CartUiState;
import com.pricepulse.viewmodel.CartViewModel;

import java.util.ArrayList;
import java.util.List;



import java.util.Locale;

public class CheckoutFragment extends Fragment {

    private FragmentCheckoutBinding binding;
    private CartViewModel viewModel;
    private CheckoutSummaryAdapter checkoutAdapter;
    private String selectedPaymentMethod = "card";

    private ActivityResultLauncher<String> locationPermissionLauncher;

    private final FirebaseRepository repository = new FirebaseRepository();
    private ListenerRegistration addressesRegistration;
    private List<Address> savedAddresses = new ArrayList<>();
    private boolean defaultAddressApplied = false;

    private static final double DELIVERY_FEE = 3.50;

    private double currentDeliveryFee = DELIVERY_FEE;
    private double currentDeliveryDiscountAmount = 0.0;
    private double currentFinalTotalAmount = 0.0;
    private boolean locationDiscountApplied = false;
    private double currentSubtotal = 0.0;

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

        binding.savedAddressCardRoot.setOnClickListener(v -> openAddressPicker());
        startListeningForAddresses();

        locationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (Boolean.TRUE.equals(isGranted)) {
                        verifyDeliveryAddressLocation();
                    } else {
                        resetLocationDiscount();
                        binding.locationDiscountStatusText.setText(
                                getString(R.string.location_permission_denied)
                        );
                        binding.locationDiscountStatusText.setTextColor(
                                requireContext().getColor(R.color.error_red)
                        );
                    }
                }
        );

        binding.verifyLocationButton.setOnClickListener(v -> {
            if (!validateDeliveryFields()) return;
            requestLocationPermission();
        });

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
            currentSubtotal = total != null ? total : 0.0;
            updateCheckoutTotals();
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
            verifyDeliveryAddressLocation();
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
                        viewModel.checkout(
                                currentDeliveryFee,
                                currentDeliveryDiscountAmount,
                                currentFinalTotalAmount,
                                locationDiscountApplied,
                                text(binding.fullNameInput),
                                text(binding.phoneInput),
                                text(binding.addressInput),
                                text(binding.cityInput),
                                text(binding.postalCodeInput),
                                selectedPaymentMethod
                        );
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

    @SuppressLint("MissingPermission")
    private void verifyDeliveryAddressLocation() {
        binding.verifyLocationButton.setEnabled(false);
        binding.locationDiscountStatusText.setText(getString(R.string.location_discount_checking));
        binding.locationDiscountStatusText.setTextColor(
                requireContext().getColor(R.color.skroutz_blue)
        );

        LocationManager locationManager =
                (LocationManager) requireContext().getSystemService(Context.LOCATION_SERVICE);

        if (locationManager == null) {
            showLocationDiscountError(getString(R.string.location_service_unavailable));
            return;
        }

        Location lastKnownLocation = getBestLastKnownLocation(locationManager);

        if (lastKnownLocation != null) {
            checkDiscountWithLocation(lastKnownLocation);
            return;
        }

        try {
            locationManager.requestSingleUpdate(
                    LocationManager.GPS_PROVIDER,
                    new LocationListener() {
                        @Override
                        public void onLocationChanged(@NonNull Location location) {
                            checkDiscountWithLocation(location);
                        }

                        @Override
                        public void onProviderDisabled(@NonNull String provider) {
                            showLocationDiscountError(getString(R.string.location_service_disabled));
                        }
                    },
                    null
            );
        } catch (Exception ex) {
            showLocationDiscountError(getString(R.string.location_service_unavailable));
        }
    }

    @SuppressLint("MissingPermission")
    private Location getBestLastKnownLocation(LocationManager locationManager) {
        Location gpsLocation = null;
        Location networkLocation = null;

        try {
            gpsLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        } catch (Exception ignored) {
        }

        try {
            networkLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        } catch (Exception ignored) {
        }

        if (gpsLocation == null) return networkLocation;
        if (networkLocation == null) return gpsLocation;

        return gpsLocation.getTime() >= networkLocation.getTime()
                ? gpsLocation
                : networkLocation;
    }

    private void checkDiscountWithLocation(Location userLocation) {
        String shippingAddress = buildShippingAddress();

        LocationDiscountHelper.checkDiscountEligibility(
                requireContext(),
                shippingAddress,
                userLocation.getLatitude(),
                userLocation.getLongitude(),
                result -> {
                    if (binding == null) return;

                    binding.verifyLocationButton.setEnabled(true);

                    if (!result.isSuccess()) {
                        showLocationDiscountError(result.getMessage());
                        return;
                    }

                    if (result.isEligible()) {
                        applyLocationDiscount(result);
                    } else {
                        resetLocationDiscount();
                        binding.locationDiscountStatusText.setText(result.getMessage());
                        binding.locationDiscountStatusText.setTextColor(
                                requireContext().getColor(R.color.error_red)
                        );
                    }
                }
        );
    }

    private String buildShippingAddress() {
        String address = text(binding.addressInput);
        String city = text(binding.cityInput);
        String postalCode = text(binding.postalCodeInput);

        return address + ", " + postalCode + ", " + city + ", Greece";
    }

    private void applyLocationDiscount(LocationDiscountHelper.Result result) {
        double discountAmount = LocationDiscountHelper.calculateDiscountAmount(DELIVERY_FEE);
        double discountedDeliveryFee = LocationDiscountHelper.calculateFinalDeliveryFee(DELIVERY_FEE);

        currentDeliveryFee = discountedDeliveryFee;
        currentDeliveryDiscountAmount = discountAmount;
        locationDiscountApplied = true;

        binding.deliveryDiscountRow.setVisibility(View.VISIBLE);
        binding.deliveryDiscountText.setText(
                String.format(Locale.getDefault(), "-%.2f €", discountAmount)
        );

        binding.locationDiscountStatusText.setText(
                getString(
                        R.string.location_discount_approved,
                        result.getUserToAddressDistanceKm(),
                        result.getAddressToShopDistanceKm()
                )
        );
        binding.locationDiscountStatusText.setTextColor(
                requireContext().getColor(R.color.success_green)
        );

        updateCheckoutTotals();
    }

    private void resetLocationDiscount() {
        currentDeliveryFee = DELIVERY_FEE;
        currentDeliveryDiscountAmount = 0.0;
        locationDiscountApplied = false;

        if (binding != null) {
            binding.deliveryDiscountRow.setVisibility(View.GONE);
            binding.deliveryDiscountText.setText("-0.00 €");
            updateCheckoutTotals();
        }
    }

    private void showLocationDiscountError(String message) {
        if (binding == null) return;

        binding.verifyLocationButton.setEnabled(true);
        resetLocationDiscount();

        binding.locationDiscountStatusText.setText(message);
        binding.locationDiscountStatusText.setTextColor(
                requireContext().getColor(R.color.error_red)
        );
    }

    private void updateCheckoutTotals() {
        currentFinalTotalAmount = currentSubtotal + currentDeliveryFee;

        binding.subtotalAmountText.setText(
                String.format(Locale.getDefault(), "%.2f €", currentSubtotal)
        );

        binding.deliveryFeeText.setText(
                String.format(Locale.getDefault(), "%.2f €", currentDeliveryFee)
        );

        binding.finalTotalAmountText.setText(
                String.format(Locale.getDefault(), "%.2f €", currentFinalTotalAmount)
        );
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

    private void startListeningForAddresses() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || user.isAnonymous()) {
            savedAddresses = new ArrayList<>();
            updateSavedAddressCard();
            return;
        }
        addressesRegistration = repository.listenToUserAddresses(user.getUid(), result -> {
            if (binding == null) return;
            savedAddresses = result != null ? result : new ArrayList<>();
            updateSavedAddressCard();
            // αυτοματο autofill μονο μια φορα, στην πρωτη εμφανιση default address
            if (!defaultAddressApplied) {
                for (Address a : savedAddresses) {
                    if (a.isDefaultAddress()) {
                        applyAddressToForm(a, false);
                        defaultAddressApplied = true;
                        break;
                    }
                }
            }
        });
    }

    private void updateSavedAddressCard() {
        if (binding == null) return;
        boolean hasAddresses = !savedAddresses.isEmpty();
        binding.savedAddressCard.setVisibility(hasAddresses ? View.VISIBLE : View.GONE);
    }

    private void openAddressPicker() {
        if (savedAddresses.isEmpty()) return;
        SavedAddressPickerBottomSheetFragment sheet = SavedAddressPickerBottomSheetFragment.create(
                new ArrayList<>(savedAddresses),
                picked -> applyAddressToForm(picked, true)
        );
        sheet.show(getChildFragmentManager(), "saved_address_picker");
    }

    private void applyAddressToForm(Address address, boolean showAsSelected) {
        if (binding == null || address == null) return;
        binding.fullNameInput.setText(address.getFullName());
        binding.phoneInput.setText(address.getPhone());
        binding.addressInput.setText(address.getAddressLine());
        binding.cityInput.setText(address.getCity());
        binding.postalCodeInput.setText(address.getPostalCode());

        binding.savedAddressTitleText.setText(address.getLabel());
        binding.savedAddressSubtitleText.setText(
                address.getAddressLine() + ", " + address.getPostalCode() + " " + address.getCity()
        );
        binding.savedAddressChangeText.setVisibility(View.VISIBLE);

        // η διευθυνση αλλαξε, οπότε καθε προηγουμενη επαληθευση τοποθεσιας ακυρωνεται
        resetLocationDiscount();
        binding.locationDiscountStatusText.setText(R.string.location_discount_not_checked);
        binding.locationDiscountStatusText.setTextColor(
                requireContext().getColor(R.color.skroutz_text_secondary));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (addressesRegistration != null) {
            addressesRegistration.remove();
            addressesRegistration = null;
        }
        binding = null;
    }
}