package com.pricepulse.ui.fragments;

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

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseUser;
import com.pricepulse.R;
import com.pricepulse.databinding.FragmentProfileBinding;
import com.pricepulse.databinding.ProfileGuestBinding;
import com.pricepulse.databinding.ProfileHelpBinding;
import com.pricepulse.databinding.ProfileMainBinding;
import com.pricepulse.databinding.ProfileOrdersBinding;
import com.pricepulse.databinding.ProfileSettingsBinding;
import com.pricepulse.databinding.ProfileWishlistBinding;
import com.pricepulse.model.Product;
import com.pricepulse.model.User;
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
            if (currentSubBinding instanceof ProfileWishlistBinding) {
                ((ProfileWishlistBinding) currentSubBinding).wishlistProgress
                        .setVisibility(Boolean.TRUE.equals(loading) ? View.VISIBLE : View.GONE);
            } else if (currentSubBinding instanceof ProfileOrdersBinding) {
                ((ProfileOrdersBinding) currentSubBinding).ordersProgress
                        .setVisibility(Boolean.TRUE.equals(loading) ? View.VISIBLE : View.GONE);
            } else if (currentSubBinding instanceof ProfileSettingsBinding) {
                ((ProfileSettingsBinding) currentSubBinding).updateButton
                        .setEnabled(!Boolean.TRUE.equals(loading));
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
        }

        binding.toolbar.setNavigationIcon(
                tab != ProfileTabState.MAIN
                        ? ContextCompat.getDrawable(requireContext(), R.drawable.ic_arrow_back)
                        : null);

        binding.profileContentHost.removeAllViews();
        currentSubBinding = null;
        wishlistAdapter = null;
        orderAdapter = null;

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

        b.rowSettings.optionIcon.setImageResource(R.drawable.ic_settings);
        b.rowSettings.optionTitle.setText(R.string.account_settings);
        b.rowSettings.getRoot().setOnClickListener(v -> viewModel.setTab(ProfileTabState.ACCOUNT_SETTINGS));

        b.rowHelp.optionIcon.setImageResource(R.drawable.ic_help_outline);
        b.rowHelp.optionTitle.setText(R.string.help_center);
        b.rowHelp.getRoot().setOnClickListener(v -> viewModel.setTab(ProfileTabState.HELP_CENTER));

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
            b.guestButton.setEnabled(!isProcessing[0]);
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

        b.guestButton.setOnClickListener(v -> {
            if (isProcessing[0]) return;
            BottomNavigationView bottomNav = requireActivity().findViewById(R.id.bottomNav);
            if (bottomNav != null) bottomNav.setSelectedItemId(R.id.homeFragment);
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
        b.updateButton.setEnabled(!Boolean.TRUE.equals(viewModel.getIsLoading().getValue()));

        b.updateButton.setOnClickListener(v -> {
            String name = b.displayNameInput.getText() != null
                    ? b.displayNameInput.getText().toString().trim() : "";
            if (name.isEmpty()) {
                Toast.makeText(requireContext(), R.string.display_name_required, Toast.LENGTH_SHORT).show();
                return;
            }
            viewModel.updateDisplayName(name);
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
        binding = null;
    }
}
