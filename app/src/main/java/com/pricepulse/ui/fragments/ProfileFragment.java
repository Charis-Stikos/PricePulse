package com.pricepulse.ui.fragments;

import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.viewbinding.ViewBinding;

import com.google.firebase.auth.FirebaseUser;
import com.pricepulse.R;
import com.pricepulse.databinding.FragmentProfileBinding;
import com.pricepulse.databinding.ProfileAddressEditorBinding;
import com.pricepulse.databinding.ProfileAddressesBinding;
import com.pricepulse.databinding.ProfileGuestBinding;
import com.pricepulse.databinding.ProfileHelpBinding;
import com.pricepulse.databinding.ProfileMainBinding;
import com.pricepulse.databinding.ProfileOrdersBinding;
import com.pricepulse.databinding.ProfileSettingsBinding;
import com.pricepulse.databinding.ProfileWishlistBinding;
import com.pricepulse.model.Address;
import com.pricepulse.model.Order;
import com.pricepulse.model.Product;
import com.pricepulse.model.User;
import com.pricepulse.ui.adapters.AddressAdapter;
import com.pricepulse.ui.adapters.FaqAdapter;
import com.pricepulse.ui.adapters.OrderAdapter;
import com.pricepulse.ui.adapters.WishlistAdapter;
import com.pricepulse.viewmodel.ProfileTabState;
import com.pricepulse.viewmodel.ProfileViewModel;

import java.util.Arrays;
import java.util.List;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private ProfileViewModel viewModel;

    private ViewBinding currentSubBinding;
    private WishlistAdapter wishlistAdapter;
    private OrderAdapter orderAdapter;
    private AddressAdapter addressAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);

        binding.toolbar.setNavigationOnClickListener(v -> viewModel.setTab(ProfileTabState.MAIN));

        viewModel.getTabState().observe(getViewLifecycleOwner(), this::renderTab);

        viewModel.getAuthManager().getCurrentUser().observe(getViewLifecycleOwner(), user -> {
            if (viewModel.getTabState().getValue() == ProfileTabState.MAIN) {
                renderTab(ProfileTabState.MAIN);
            }
        });

        viewModel.getUserProfile().observe(getViewLifecycleOwner(), this::onUserProfileChanged);

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> {
            boolean isLoading = Boolean.TRUE.equals(loading);
            if (currentSubBinding instanceof ProfileWishlistBinding) {
                ProfileWishlistBinding b = (ProfileWishlistBinding) currentSubBinding;
                b.wishlistProgress.setVisibility(isLoading ? View.VISIBLE : View.GONE);
                List<Product> items = viewModel.getWishlist().getValue();
                boolean empty = (items == null || items.isEmpty()) && !isLoading;
                b.emptyView.getRoot().setVisibility(empty ? View.VISIBLE : View.GONE);
                b.wishlistRecycler.setVisibility(empty || isLoading ? View.GONE : View.VISIBLE);
            } else if (currentSubBinding instanceof ProfileOrdersBinding) {
                ProfileOrdersBinding b = (ProfileOrdersBinding) currentSubBinding;
                b.ordersProgress.setVisibility(isLoading ? View.VISIBLE : View.GONE);
                List<Order> items = viewModel.getOrderHistory().getValue();
                boolean empty = (items == null || items.isEmpty()) && !isLoading;
                b.emptyView.getRoot().setVisibility(empty ? View.VISIBLE : View.GONE);
                b.ordersRecycler.setVisibility(empty || isLoading ? View.GONE : View.VISIBLE);
            } else if (currentSubBinding instanceof ProfileSettingsBinding) {
                ProfileSettingsBinding b = (ProfileSettingsBinding) currentSubBinding;
                b.updateButton.setEnabled(!isLoading);
                b.updateEmailButton.setEnabled(!isLoading);
                b.updatePasswordButton.setEnabled(!isLoading);
            } else if (currentSubBinding instanceof ProfileAddressesBinding) {
                ProfileAddressesBinding b = (ProfileAddressesBinding) currentSubBinding;
                b.addressesProgress.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            } else if (currentSubBinding instanceof ProfileAddressEditorBinding) {
                ProfileAddressEditorBinding b = (ProfileAddressEditorBinding) currentSubBinding;
                b.saveAddressButton.setEnabled(!isLoading);
                b.saveAddressProgress.setVisibility(isLoading ? View.VISIBLE : View.GONE);
                b.saveAddressButton.setText(isLoading ? "" : getString(R.string.address_save));
            }
        });

        viewModel.getWishlist().observe(getViewLifecycleOwner(), products -> {
            if (currentSubBinding instanceof ProfileWishlistBinding && wishlistAdapter != null) {
                ProfileWishlistBinding b = (ProfileWishlistBinding) currentSubBinding;
                wishlistAdapter.submitList(products);
                boolean empty = (products == null || products.isEmpty())
                        && !Boolean.TRUE.equals(viewModel.getIsLoading().getValue());
                b.emptyView.getRoot().setVisibility(empty ? View.VISIBLE : View.GONE);
                b.wishlistRecycler.setVisibility(empty ? View.GONE : View.VISIBLE);
            }
        });

        viewModel.getOrderHistory().observe(getViewLifecycleOwner(), orders -> {
            if (currentSubBinding instanceof ProfileOrdersBinding && orderAdapter != null) {
                ProfileOrdersBinding b = (ProfileOrdersBinding) currentSubBinding;
                orderAdapter.submitList(orders);
                boolean empty = (orders == null || orders.isEmpty())
                        && !Boolean.TRUE.equals(viewModel.getIsLoading().getValue());
                b.emptyView.getRoot().setVisibility(empty ? View.VISIBLE : View.GONE);
                b.ordersRecycler.setVisibility(empty ? View.GONE : View.VISIBLE);
            }
        });

        viewModel.getAddresses().observe(getViewLifecycleOwner(), addresses -> {
            if (currentSubBinding instanceof ProfileAddressesBinding && addressAdapter != null) {
                ProfileAddressesBinding b = (ProfileAddressesBinding) currentSubBinding;
                addressAdapter.submitList(addresses);
                boolean loading = Boolean.TRUE.equals(viewModel.getIsLoading().getValue());
                boolean empty = (addresses == null || addresses.isEmpty()) && !loading;
                b.emptyView.getRoot().setVisibility(empty ? View.VISIBLE : View.GONE);
                b.addressesRecycler.setVisibility(empty ? View.GONE : View.VISIBLE);
            }
        });

        viewModel.getSettingsEvents().observe(getViewLifecycleOwner(), event -> {
            if (event == null) return;
            switch (event) {
                case UPDATE_SUCCEEDED:
                    Toast.makeText(requireContext(), R.string.profile_updated, Toast.LENGTH_SHORT).show();
                    viewModel.setTab(ProfileTabState.MAIN);
                    break;
                case UPDATE_FAILED:
                    Toast.makeText(requireContext(), R.string.profile_update_failed, Toast.LENGTH_SHORT).show();
                    break;
                case EMAIL_UPDATED:
                    Toast.makeText(requireContext(), R.string.email_updated, Toast.LENGTH_LONG).show();
                    if (currentSubBinding instanceof ProfileSettingsBinding) {
                        ProfileSettingsBinding b = (ProfileSettingsBinding) currentSubBinding;
                        b.newEmailInput.setText("");
                        b.emailCurrentPasswordInput.setText("");
                    }
                    break;
                case EMAIL_UPDATE_FAILED:
                    Toast.makeText(requireContext(), R.string.profile_update_failed, Toast.LENGTH_SHORT).show();
                    break;
                case PASSWORD_UPDATED:
                    Toast.makeText(requireContext(), R.string.password_updated, Toast.LENGTH_SHORT).show();
                    if (currentSubBinding instanceof ProfileSettingsBinding) {
                        ProfileSettingsBinding b = (ProfileSettingsBinding) currentSubBinding;
                        b.passwordCurrentInput.setText("");
                        b.newPasswordInput.setText("");
                        b.confirmPasswordInput.setText("");
                    }
                    break;
                case PASSWORD_UPDATE_FAILED:
                    Toast.makeText(requireContext(), R.string.profile_update_failed, Toast.LENGTH_SHORT).show();
                    break;
                case REAUTH_REQUIRED:
                    Toast.makeText(requireContext(), R.string.reauth_required, Toast.LENGTH_SHORT).show();
                    break;
                case ADDRESS_SAVED:
                    Toast.makeText(requireContext(), R.string.address_saved_toast, Toast.LENGTH_SHORT).show();
                    viewModel.setTab(ProfileTabState.SAVED_ADDRESSES);
                    break;
                case ADDRESS_SAVE_FAILED:
                    Toast.makeText(requireContext(), R.string.address_save_failed_toast, Toast.LENGTH_SHORT).show();
                    break;
                case ADDRESS_DELETED:
                    Toast.makeText(requireContext(), R.string.address_deleted_toast, Toast.LENGTH_SHORT).show();
                    break;
                case ADDRESS_DELETE_FAILED:
                    Toast.makeText(requireContext(), R.string.address_delete_failed_toast, Toast.LENGTH_SHORT).show();
                    break;
                case DEFAULT_ADDRESS_UPDATED:
                    Toast.makeText(requireContext(), R.string.default_address_updated_toast, Toast.LENGTH_SHORT).show();
                    break;
                case DEFAULT_ADDRESS_UPDATE_FAILED:
                    Toast.makeText(requireContext(), R.string.address_save_failed_toast, Toast.LENGTH_SHORT).show();
                    break;
            }
        });
    }

    private void onUserProfileChanged(@Nullable User profile) {
        if (!(currentSubBinding instanceof ProfileMainBinding) || profile == null) return;
        ProfileMainBinding b = (ProfileMainBinding) currentSubBinding;
        String name = profile.getDisplayName();
        if (name != null && !name.isEmpty()) {
            b.userName.setText(name);
            b.avatarLetter.setText(name.substring(0, 1).toUpperCase());
        }
    }

    private void renderTab(ProfileTabState tab) {
        switch (tab) {
            case MAIN:
                binding.toolbarTitle.setText(R.string.my_account);
                break;
            case WISHLIST:
                binding.toolbarTitle.setText(R.string.wishlist);
                break;
            case ORDERS:
                binding.toolbarTitle.setText(R.string.order_history);
                break;
            case ACCOUNT_SETTINGS:
                binding.toolbarTitle.setText(R.string.account_settings);
                break;
            case HELP_CENTER:
                binding.toolbarTitle.setText(R.string.help_center);
                break;
            case SAVED_ADDRESSES:
                binding.toolbarTitle.setText(R.string.saved_addresses);
                break;
            case ADDRESS_EDITOR:
                binding.toolbarTitle.setText(
                        viewModel.getEditingAddress().getValue() == null
                                ? R.string.add_new_address
                                : R.string.edit_address);
                break;
        }

        if (tab != ProfileTabState.MAIN) {
            Drawable backIcon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_arrow_back);
            if (backIcon != null) {
                backIcon = backIcon.mutate();
                backIcon.setColorFilter(
                        ContextCompat.getColor(requireContext(), R.color.skroutz_text_primary),
                        PorterDuff.Mode.SRC_IN);
            }
            binding.toolbar.setNavigationIcon(backIcon);
            binding.toolbar.setNavigationContentDescription(R.string.back);
            // απο το editor πρεπει να γυρνας στη λιστα, οχι στο main
            binding.toolbar.setNavigationOnClickListener(v -> {
                if (tab == ProfileTabState.ADDRESS_EDITOR) {
                    viewModel.setTab(ProfileTabState.SAVED_ADDRESSES);
                } else {
                    viewModel.setTab(ProfileTabState.MAIN);
                }
            });
        } else {
            binding.toolbar.setNavigationIcon(null);
            binding.toolbar.setNavigationOnClickListener(null);
        }

        binding.profileContentHost.removeAllViews();
        currentSubBinding = null;
        wishlistAdapter = null;
        orderAdapter = null;
        addressAdapter = null;

        switch (tab) {
            case MAIN:
                showMain();
                break;
            case WISHLIST:
                showWishlist();
                break;
            case ORDERS:
                showOrders();
                break;
            case ACCOUNT_SETTINGS:
                showSettings();
                break;
            case HELP_CENTER:
                showHelp();
                break;
            case SAVED_ADDRESSES:
                showAddresses();
                break;
            case ADDRESS_EDITOR:
                showAddressEditor();
                break;
        }
    }

    private void showMain() {
        FirebaseUser user = viewModel.getAuthManager().getCurrentUserValue();
        if (user == null || user.isAnonymous()) {
            showGuest();
        } else {
            showAccount(user);
        }
    }

    private void showAccount(FirebaseUser user) {
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        ProfileMainBinding b = ProfileMainBinding.inflate(inflater, binding.profileContentHost, false);
        binding.profileContentHost.addView(b.getRoot());
        currentSubBinding = b;

        User profile = viewModel.getUserProfile().getValue();
        String name = profile != null ? profile.getDisplayName() : null;
        String email = user.getEmail();
        if (name != null && !name.isEmpty()) {
            b.userName.setText(name);
            b.avatarLetter.setText(name.substring(0, 1).toUpperCase());
        } else if (email != null && !email.isEmpty()) {
            b.userName.setText(email);
            b.avatarLetter.setText(email.substring(0, 1).toUpperCase());
        } else {
            b.userName.setText("User");
            b.avatarLetter.setText("U");
        }

        b.rowWishlist.optionIcon.setImageResource(R.drawable.ic_favorite_border);
        b.rowWishlist.optionTitle.setText(R.string.my_wishlist);
        b.rowWishlist.getRoot().setOnClickListener(v -> viewModel.setTab(ProfileTabState.WISHLIST));

        b.rowOrders.optionIcon.setImageResource(R.drawable.ic_list);
        b.rowOrders.optionTitle.setText(R.string.order_history);
        b.rowOrders.getRoot().setOnClickListener(v -> viewModel.setTab(ProfileTabState.ORDERS));

        b.rowAddresses.optionIcon.setImageResource(R.drawable.ic_location);
        b.rowAddresses.optionTitle.setText(R.string.saved_addresses);
        b.rowAddresses.getRoot().setOnClickListener(v -> viewModel.setTab(ProfileTabState.SAVED_ADDRESSES));

        b.rowSettings.optionIcon.setImageResource(R.drawable.ic_settings);
        b.rowSettings.optionTitle.setText(R.string.account_settings);
        b.rowSettings.getRoot().setOnClickListener(v -> viewModel.setTab(ProfileTabState.ACCOUNT_SETTINGS));

        b.rowHelp.optionIcon.setImageResource(R.drawable.ic_help_outline);
        b.rowHelp.optionTitle.setText(R.string.help_center);
        b.rowHelp.getRoot().setOnClickListener(v -> viewModel.setTab(ProfileTabState.HELP_CENTER));

        b.rowAdmin.optionIcon.setImageResource(R.drawable.ic_admin_panel);
        b.rowAdmin.optionTitle.setText(R.string.admin_dashboard);
        b.rowAdmin.getRoot().setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.action_profile_to_admin));

        com.pricepulse.admin.AdminSession.getInstance().canAccessDashboard()
                .observe(getViewLifecycleOwner(), canAccess -> {
                    boolean show = Boolean.TRUE.equals(canAccess);
                    b.rowAdminDivider.setVisibility(show ? View.VISIBLE : View.GONE);
                    b.rowAdmin.getRoot().setVisibility(show ? View.VISIBLE : View.GONE);
                });

        b.signOutButton.setOnClickListener(v -> viewModel.getAuthManager().signOut());
    }

    private void showGuest() {
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        ProfileGuestBinding b = ProfileGuestBinding.inflate(inflater, binding.profileContentHost, false);
        binding.profileContentHost.addView(b.getRoot());
        currentSubBinding = b;

        final boolean[] isLogin = {true};
        final boolean[] isProcessing = {false};

        Runnable applyMode = () -> {
            b.guestTitle.setText(isLogin[0] ? R.string.welcome_back : R.string.create_account);
            b.guestSubtitle.setText(isLogin[0] ? R.string.sign_in_subtitle : R.string.sign_up_subtitle);
            b.actionButton.setText(isLogin[0] ? R.string.sign_in : R.string.register_now);
            b.toggleModeButton.setText(isLogin[0] ? R.string.toggle_to_register : R.string.toggle_to_login);
        };

        Runnable[] setProcessingRef = new Runnable[1];
        setProcessingRef[0] = () -> {
            b.actionButton.setEnabled(!isProcessing[0]);
            b.toggleModeButton.setEnabled(!isProcessing[0]);
            b.actionProgress.setVisibility(isProcessing[0] ? View.VISIBLE : View.GONE);
            b.actionButton.setText(isProcessing[0]
                    ? ""
                    : getString(isLogin[0] ? R.string.sign_in : R.string.register_now));
        };

        applyMode.run();

        b.toggleModeButton.setOnClickListener(v -> {
            if (isProcessing[0]) return;
            isLogin[0] = !isLogin[0];
            b.errorText.setVisibility(View.GONE);
            applyMode.run();
        });

        b.actionButton.setOnClickListener(v -> {
            if (isProcessing[0]) return;
            String email = b.emailInput.getText() != null ? b.emailInput.getText().toString().trim() : "";
            String password = b.passwordInput.getText() != null ? b.passwordInput.getText().toString() : "";
            if (email.isEmpty() || password.length() < 6) {
                b.errorText.setVisibility(View.VISIBLE);
                b.errorText.setText(R.string.invalid_credentials);
                return;
            }
            isProcessing[0] = true;
            setProcessingRef[0].run();
            b.errorText.setVisibility(View.GONE);
            com.pricepulse.auth.AuthManager.AuthResultCallback callback = (success, error) -> {
                if (binding == null) return;
                isProcessing[0] = false;
                setProcessingRef[0].run();
                if (!success) {
                    b.errorText.setVisibility(View.VISIBLE);
                    b.errorText.setText(error != null ? error : getString(R.string.invalid_credentials));
                }
            };
            if (isLogin[0]) {
                viewModel.getAuthManager().signIn(email, password, callback);
            } else {
                viewModel.getAuthManager().signUp(email, password, callback);
            }
        });
    }

    private void showWishlist() {
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        ProfileWishlistBinding b = ProfileWishlistBinding.inflate(inflater, binding.profileContentHost, false);
        binding.profileContentHost.addView(b.getRoot());
        currentSubBinding = b;

        wishlistAdapter = new WishlistAdapter(product -> {
            Bundle args = new Bundle();
            args.putString("productId", product.getId());
            NavHostFragment.findNavController(this).navigate(R.id.action_profile_to_detail, args);
        });
        b.wishlistRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        b.wishlistRecycler.setAdapter(wishlistAdapter);

        b.emptyView.emptyIcon.setImageResource(R.drawable.ic_favorite_border);
        b.emptyView.emptyTitle.setText(R.string.empty_wishlist_title);
        b.emptyView.emptySubtitle.setText(R.string.empty_wishlist_subtitle);

        List<Product> current = viewModel.getWishlist().getValue();
        wishlistAdapter.submitList(current);
        boolean loading = Boolean.TRUE.equals(viewModel.getIsLoading().getValue());
        b.wishlistProgress.setVisibility(loading ? View.VISIBLE : View.GONE);
        boolean empty = (current == null || current.isEmpty()) && !loading;
        b.emptyView.getRoot().setVisibility(empty ? View.VISIBLE : View.GONE);
        b.wishlistRecycler.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private void showOrders() {
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        ProfileOrdersBinding b = ProfileOrdersBinding.inflate(inflater, binding.profileContentHost, false);
        binding.profileContentHost.addView(b.getRoot());
        currentSubBinding = b;

        orderAdapter = new OrderAdapter();
        b.ordersRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        b.ordersRecycler.setAdapter(orderAdapter);

        b.emptyView.emptyIcon.setImageResource(R.drawable.ic_list);
        b.emptyView.emptyTitle.setText(R.string.empty_orders_title);
        b.emptyView.emptySubtitle.setText(R.string.empty_orders_subtitle);

        orderAdapter.submitList(viewModel.getOrderHistory().getValue());
        boolean loading = Boolean.TRUE.equals(viewModel.getIsLoading().getValue());
        b.ordersProgress.setVisibility(loading ? View.VISIBLE : View.GONE);
        boolean empty = (viewModel.getOrderHistory().getValue() == null
                || viewModel.getOrderHistory().getValue().isEmpty()) && !loading;
        b.emptyView.getRoot().setVisibility(empty ? View.VISIBLE : View.GONE);
        b.ordersRecycler.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private void showSettings() {
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        ProfileSettingsBinding b = ProfileSettingsBinding.inflate(inflater, binding.profileContentHost, false);
        binding.profileContentHost.addView(b.getRoot());
        currentSubBinding = b;

        User profile = viewModel.getUserProfile().getValue();
        b.displayNameInput.setText(profile != null && profile.getDisplayName() != null ? profile.getDisplayName() : "");

        FirebaseUser fbUser = viewModel.getAuthManager().getCurrentUserValue();
        b.currentEmailText.setText(fbUser != null && fbUser.getEmail() != null ? fbUser.getEmail() : "");

        boolean enabled = !Boolean.TRUE.equals(viewModel.getIsLoading().getValue());
        b.updateButton.setEnabled(enabled);
        b.updateEmailButton.setEnabled(enabled);
        b.updatePasswordButton.setEnabled(enabled);

        b.emailSectionHeader.setOnClickListener(v -> toggleSection(b.emailSectionBody, b.emailExpandIcon));
        b.passwordSectionHeader.setOnClickListener(v -> toggleSection(b.passwordSectionBody, b.passwordExpandIcon));

        b.updateButton.setOnClickListener(v -> {
            String name = textOf(b.displayNameInput);
            if (name.isEmpty()) {
                Toast.makeText(requireContext(), R.string.display_name_required, Toast.LENGTH_SHORT).show();
                return;
            }
            viewModel.updateDisplayName(name);
        });

        b.updateEmailButton.setOnClickListener(v -> {
            String newEmail = textOf(b.newEmailInput);
            String currentPassword = textOf(b.emailCurrentPasswordInput);
            if (newEmail.isEmpty() || currentPassword.isEmpty()) {
                Toast.makeText(requireContext(), R.string.all_fields_required, Toast.LENGTH_SHORT).show();
                return;
            }
            viewModel.updateEmail(currentPassword, newEmail);
        });

        b.updatePasswordButton.setOnClickListener(v -> {
            String currentPassword = textOf(b.passwordCurrentInput);
            String newPassword = textOf(b.newPasswordInput);
            String confirmPassword = textOf(b.confirmPasswordInput);
            if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(requireContext(), R.string.all_fields_required, Toast.LENGTH_SHORT).show();
                return;
            }
            if (!newPassword.equals(confirmPassword)) {
                Toast.makeText(requireContext(), R.string.passwords_dont_match, Toast.LENGTH_SHORT).show();
                return;
            }
            if (newPassword.length() < 6) {
                Toast.makeText(requireContext(), R.string.invalid_credentials, Toast.LENGTH_SHORT).show();
                return;
            }
            viewModel.updatePassword(currentPassword, newPassword);
        });
    }

    private static String textOf(com.google.android.material.textfield.TextInputEditText input) {
        return input.getText() != null ? input.getText().toString().trim() : "";
    }

    private void toggleSection(View body, android.widget.ImageView chevron) {
        boolean expanding = body.getVisibility() != View.VISIBLE;
        body.setVisibility(expanding ? View.VISIBLE : View.GONE);
        chevron.animate().rotation(expanding ? 90f : 0f).setDuration(200).start();
    }

    private void showAddresses() {
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        ProfileAddressesBinding b = ProfileAddressesBinding.inflate(inflater, binding.profileContentHost, false);
        binding.profileContentHost.addView(b.getRoot());
        currentSubBinding = b;

        addressAdapter = new AddressAdapter(new AddressAdapter.OnAddressAction() {
            @Override
            public void onEdit(Address address) {
                viewModel.openAddressEditor(address);
            }

            @Override
            public void onDelete(Address address) {
                new android.app.AlertDialog.Builder(requireContext())
                        .setTitle(R.string.address_delete_confirm_title)
                        .setMessage(R.string.address_delete_confirm_message)
                        .setPositiveButton(android.R.string.ok, (d, w) -> viewModel.deleteAddress(address))
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
            }

            @Override
            public void onSetDefault(Address address) {
                viewModel.setDefaultAddress(address);
            }
        });
        b.addressesRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        b.addressesRecycler.setAdapter(addressAdapter);

        b.emptyView.emptyIcon.setImageResource(R.drawable.ic_location);
        b.emptyView.emptyTitle.setText(R.string.empty_addresses_title);
        b.emptyView.emptySubtitle.setText(R.string.empty_addresses_subtitle);

        b.addAddressButton.setOnClickListener(v -> viewModel.openAddressEditor(null));

        List<Address> current = viewModel.getAddresses().getValue();
        addressAdapter.submitList(current);
        boolean loading = Boolean.TRUE.equals(viewModel.getIsLoading().getValue());
        b.addressesProgress.setVisibility(loading ? View.VISIBLE : View.GONE);
        boolean empty = (current == null || current.isEmpty()) && !loading;
        b.emptyView.getRoot().setVisibility(empty ? View.VISIBLE : View.GONE);
        b.addressesRecycler.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private void showAddressEditor() {
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        ProfileAddressEditorBinding b = ProfileAddressEditorBinding.inflate(inflater, binding.profileContentHost, false);
        binding.profileContentHost.addView(b.getRoot());
        currentSubBinding = b;

        Address editing = viewModel.getEditingAddress().getValue();

        String[] labels = new String[] {
                getString(R.string.address_label_home),
                getString(R.string.address_label_work),
                getString(R.string.address_label_other)
        };
        android.widget.ArrayAdapter<String> labelAdapter = new android.widget.ArrayAdapter<>(
                requireContext(), android.R.layout.simple_list_item_1, labels);
        b.addressLabelInput.setAdapter(labelAdapter);

        if (editing != null) {
            b.addressLabelInput.setText(editing.getLabel(), false);
            b.addressFullNameInput.setText(editing.getFullName());
            b.addressPhoneInput.setText(editing.getPhone());
            b.addressLineInput.setText(editing.getAddressLine());
            b.addressCityInput.setText(editing.getCity());
            b.addressPostalCodeInput.setText(editing.getPostalCode());
            b.addressDefaultSwitch.setChecked(editing.isDefaultAddress());
        } else {
            b.addressLabelInput.setText(labels[0], false);
        }

        boolean initialLoading = Boolean.TRUE.equals(viewModel.getIsLoading().getValue());
        b.saveAddressButton.setEnabled(!initialLoading);
        b.saveAddressProgress.setVisibility(initialLoading ? View.VISIBLE : View.GONE);
        b.saveAddressButton.setText(initialLoading ? "" : getString(R.string.address_save));

        b.saveAddressButton.setOnClickListener(v -> {
            String label = b.addressLabelInput.getText() != null
                    ? b.addressLabelInput.getText().toString().trim() : "";
            String fullName = textOf(b.addressFullNameInput);
            String phone = textOf(b.addressPhoneInput);
            String addressLine = textOf(b.addressLineInput);
            String city = textOf(b.addressCityInput);
            String postalCode = textOf(b.addressPostalCodeInput);

            if (fullName.isEmpty() || phone.isEmpty() || addressLine.isEmpty()
                    || city.isEmpty() || postalCode.isEmpty()) {
                Toast.makeText(requireContext(), R.string.all_required_fields_missing,
                        Toast.LENGTH_SHORT).show();
                return;
            }
            if (label.isEmpty()) label = Address.LABEL_HOME;

            Address toSave = editing != null
                    ? new Address(editing.getId(), editing.getUserId(), label, fullName, phone,
                            addressLine, city, postalCode, b.addressDefaultSwitch.isChecked(),
                            editing.getTimestamp())
                    : new Address("", "", label, fullName, phone,
                            addressLine, city, postalCode, b.addressDefaultSwitch.isChecked(),
                            System.currentTimeMillis());

            viewModel.saveAddress(toSave);
        });
    }

    private void showHelp() {
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        ProfileHelpBinding b = ProfileHelpBinding.inflate(inflater, binding.profileContentHost, false);
        binding.profileContentHost.addView(b.getRoot());
        currentSubBinding = b;

        List<FaqAdapter.Faq> faqs = Arrays.asList(
                new FaqAdapter.Faq(
                        "How do I track my order?",
                        "You can track your order in the 'Order History' section of your profile."),
                new FaqAdapter.Faq(
                        "What is your return policy?",
                        "We offer a 14-day free return policy for all unused items."),
                new FaqAdapter.Faq(
                        "How can I contact support?",
                        "You can email us at support@pricepulse.app or call our 24/7 hotline.")
        );
        b.faqRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        b.faqRecycler.setAdapter(new FaqAdapter(faqs));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        currentSubBinding = null;
        wishlistAdapter = null;
        orderAdapter = null;
        addressAdapter = null;
        binding = null;
    }
}
