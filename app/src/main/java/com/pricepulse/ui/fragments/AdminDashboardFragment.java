package com.pricepulse.ui.fragments;

import android.app.AlertDialog;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.viewbinding.ViewBinding;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.pricepulse.R;
import com.pricepulse.admin.AdminSession;
import com.pricepulse.databinding.AdminAddProductBinding;
import com.pricepulse.databinding.AdminManageAdminsBinding;
import com.pricepulse.databinding.AdminOrdersBinding;
import com.pricepulse.databinding.AdminOverviewBinding;
import com.pricepulse.databinding.AdminPlatformOverviewBinding;
import com.pricepulse.databinding.AdminShopDetailsBinding;
import com.pricepulse.databinding.AdminShopsBinding;
import com.pricepulse.databinding.FragmentAdminDashboardBinding;
import com.pricepulse.model.Order;
import com.pricepulse.model.Product;
import com.pricepulse.model.Shop;
import com.pricepulse.model.User;
import com.pricepulse.ui.adapters.AdminOrderAdapter;
import com.pricepulse.ui.adapters.AdminUserAdapter;
import com.pricepulse.ui.adapters.ShopAdapter;
import com.pricepulse.util.ImageLoader;
import com.pricepulse.viewmodel.AdminViewModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class AdminDashboardFragment extends Fragment {

    private enum TabKey { OVERVIEW, SHOP_DETAILS, ORDERS, ADD_PRODUCT, MANAGE_ADMINS, SHOPS }

    private FragmentAdminDashboardBinding binding;
    private AdminViewModel viewModel;
    private ViewBinding currentSubBinding;
    private AdminOrderAdapter orderAdapter;
    private AdminUserAdapter adminUserAdapter;
    private AdminUserAdapter shopOwnerAdapter;
    private ShopAdapter shopAdapter;
    private Shop shopBeingEdited;
    private Uri pickedImageUri;
    private ActivityResultLauncher<String> imagePicker;
    private List<Order> cachedOrders = new ArrayList<>();
    private List<User> cachedAdmins = new ArrayList<>();
    private List<User> cachedShopOwners = new ArrayList<>();
    private String selectedOrderFilter = "All";

    private static final List<String> CATEGORIES = Arrays.asList(
            "Electronics", "Fashion", "Home", "Sports", "Beauty", "Books");

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAdminDashboardBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(AdminViewModel.class);

        Drawable backIcon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_arrow_back);
        if (backIcon != null) {
            backIcon = backIcon.mutate();
            backIcon.setColorFilter(
                    ContextCompat.getColor(requireContext(), R.color.skroutz_text_primary),
                    PorterDuff.Mode.SRC_IN);
        }
        binding.toolbar.setNavigationIcon(backIcon);
        binding.toolbar.setNavigationContentDescription(R.string.back);
        binding.toolbar.setNavigationOnClickListener(v ->
                NavHostFragment.findNavController(this).popBackStack());

        imagePicker = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null && currentSubBinding instanceof AdminAddProductBinding) {
                pickedImageUri = uri;
                AdminAddProductBinding b = (AdminAddProductBinding) currentSubBinding;
                b.productImageUrlInput.setText("");
                ImageLoader.load(b.productImagePreview, uri);
                b.productImageEmpty.setVisibility(View.GONE);
            }
        });

        binding.adminTabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getTag() instanceof TabKey) renderTab((TabKey) tab.getTag());
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        AdminSession session = AdminSession.getInstance();
        boolean isAdmin = Boolean.TRUE.equals(session.isAdmin().getValue());
        boolean isShopOwner = Boolean.TRUE.equals(session.isShopOwner().getValue());
        buildTabsForRole(isAdmin, isShopOwner);

        // αν χάσει το δικαιωμα ενω βρισκεται μεσα, popback
        session.canAccessDashboard().observe(getViewLifecycleOwner(), canAccess -> {
            if (!Boolean.TRUE.equals(canAccess)) {
                NavHostFragment.findNavController(this).popBackStack();
            }
        });

        viewModel.getEvents().observe(getViewLifecycleOwner(), event -> {
            if (event == null) return;
            switch (event) {
                case ORDER_STATUS_UPDATED:
                    Toast.makeText(requireContext(), R.string.profile_updated, Toast.LENGTH_SHORT).show();
                    break;
                case ORDER_STATUS_UPDATE_FAILED:
                    Toast.makeText(requireContext(), R.string.profile_update_failed, Toast.LENGTH_SHORT).show();
                    break;
                case PRODUCT_ADDED:
                    Toast.makeText(requireContext(), R.string.product_added, Toast.LENGTH_SHORT).show();
                    resetAddProductForm();
                    break;
                case PRODUCT_ADD_FAILED:
                    Toast.makeText(requireContext(), R.string.product_add_failed, Toast.LENGTH_SHORT).show();
                    break;
                case IMAGE_UPLOAD_FAILED:
                    Toast.makeText(requireContext(), R.string.image_upload_failed, Toast.LENGTH_SHORT).show();
                    break;
                case ADMIN_PROMOTED:
                    Toast.makeText(requireContext(), R.string.admin_promoted, Toast.LENGTH_SHORT).show();
                    if (currentSubBinding instanceof AdminManageAdminsBinding) {
                        ((AdminManageAdminsBinding) currentSubBinding).adminEmailInput.setText("");
                    }
                    break;
                case ADMIN_REVOKED:
                    Toast.makeText(requireContext(), R.string.admin_revoked, Toast.LENGTH_SHORT).show();
                    break;
                case ADMIN_USER_NOT_FOUND:
                    Toast.makeText(requireContext(), R.string.admin_user_not_found, Toast.LENGTH_SHORT).show();
                    break;
                case ADMIN_ALREADY_ADMIN:
                    Toast.makeText(requireContext(), R.string.admin_already_admin, Toast.LENGTH_SHORT).show();
                    break;
                case ADMIN_CANNOT_REVOKE_SELF:
                    Toast.makeText(requireContext(), R.string.admin_cannot_revoke_self, Toast.LENGTH_SHORT).show();
                    break;
                case ADMIN_ACTION_FAILED:
                    Toast.makeText(requireContext(), R.string.profile_update_failed, Toast.LENGTH_SHORT).show();
                    break;
                case SHOP_OWNER_PROMOTED:
                    Toast.makeText(requireContext(), R.string.shop_owner_promoted, Toast.LENGTH_SHORT).show();
                    if (currentSubBinding instanceof AdminManageAdminsBinding) {
                        ((AdminManageAdminsBinding) currentSubBinding).shopOwnerEmailInput.setText("");
                    }
                    break;
                case SHOP_OWNER_REVOKED:
                    Toast.makeText(requireContext(), R.string.shop_owner_revoked, Toast.LENGTH_SHORT).show();
                    break;
                case SHOP_OWNER_ALREADY_OWNER:
                    Toast.makeText(requireContext(), R.string.admin_already_admin, Toast.LENGTH_SHORT).show();
                    break;
                case SHOP_SAVED:
                    Toast.makeText(requireContext(), R.string.shop_saved, Toast.LENGTH_SHORT).show();
                    if (currentSubBinding instanceof AdminShopsBinding) {
                        showShopsList((AdminShopsBinding) currentSubBinding);
                    }
                    break;
                case SHOP_SAVE_FAILED:
                    Toast.makeText(requireContext(), R.string.shop_save_failed, Toast.LENGTH_SHORT).show();
                    break;
            }
        });

        viewModel.getAllShops().observe(getViewLifecycleOwner(), shops -> {
            if (currentSubBinding instanceof AdminShopsBinding && shopAdapter != null) {
                AdminShopsBinding b = (AdminShopsBinding) currentSubBinding;
                shopAdapter.submitList(shops);
                boolean empty = shops == null || shops.isEmpty();
                b.shopsEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
            }
        });

        viewModel.getShopSaving().observe(getViewLifecycleOwner(), saving -> {
            if (currentSubBinding instanceof AdminShopsBinding) {
                AdminShopsBinding b = (AdminShopsBinding) currentSubBinding;
                boolean isSaving = Boolean.TRUE.equals(saving);
                b.saveShopProgress.setVisibility(isSaving ? View.VISIBLE : View.GONE);
                b.saveShopButton.setEnabled(!isSaving);
                b.saveShopButton.setText(isSaving ? "" : getString(R.string.save_shop_details));
            }
        });

        viewModel.getAdmins().observe(getViewLifecycleOwner(), admins -> {
            cachedAdmins = admins == null ? new ArrayList<>() : new ArrayList<>(admins);

            if (currentSubBinding instanceof AdminManageAdminsBinding && adminUserAdapter != null) {
                AdminManageAdminsBinding b = (AdminManageAdminsBinding) currentSubBinding;

                adminUserAdapter.submitList(admins == null ? new ArrayList<>() : new ArrayList<>(admins));
                b.adminsEmpty.setVisibility(admins == null || admins.isEmpty() ? View.VISIBLE : View.GONE);
            }
        });

        viewModel.getAdminActionLoading().observe(getViewLifecycleOwner(), loading -> {
            if (currentSubBinding instanceof AdminManageAdminsBinding) {
                AdminManageAdminsBinding b = (AdminManageAdminsBinding) currentSubBinding;
                boolean isLoading = Boolean.TRUE.equals(loading);
                b.promoteAdminProgress.setVisibility(isLoading ? View.VISIBLE : View.GONE);
                b.promoteAdminButton.setEnabled(!isLoading);
                b.promoteAdminButton.setText(isLoading ? "" : getString(R.string.add_admin));
            }
        });

        viewModel.getShopOwners().observe(getViewLifecycleOwner(), owners -> {
            cachedShopOwners = owners == null ? new ArrayList<>() : new ArrayList<>(owners);

            if (currentSubBinding instanceof AdminManageAdminsBinding && shopOwnerAdapter != null) {
                AdminManageAdminsBinding b = (AdminManageAdminsBinding) currentSubBinding;
                shopOwnerAdapter.submitList(new ArrayList<>(cachedShopOwners));
                b.shopOwnersEmpty.setVisibility(cachedShopOwners.isEmpty() ? View.VISIBLE : View.GONE);
            }
        });

        viewModel.getShopOwnerActionLoading().observe(getViewLifecycleOwner(), loading -> {
            if (currentSubBinding instanceof AdminManageAdminsBinding) {
                AdminManageAdminsBinding b = (AdminManageAdminsBinding) currentSubBinding;
                boolean isLoading = Boolean.TRUE.equals(loading);
                b.promoteShopOwnerProgress.setVisibility(isLoading ? View.VISIBLE : View.GONE);
                b.promoteShopOwnerButton.setEnabled(!isLoading);
                b.promoteShopOwnerButton.setText(isLoading ? "" : getString(R.string.add_shop_owner));
            }
        });

        viewModel.getOrders().observe(getViewLifecycleOwner(), orders -> {
            cachedOrders = orders == null ? new ArrayList<>() : new ArrayList<>(orders);

            if (currentSubBinding instanceof AdminOrdersBinding && orderAdapter != null) {
                AdminOrdersBinding b = (AdminOrdersBinding) currentSubBinding;

                applyOrderFilter();
                updateOrderStats(b);

                orderAdapter.submitList(orders == null ? new ArrayList<>() : new ArrayList<>(orders));
                boolean loading = Boolean.TRUE.equals(viewModel.getOrdersLoading().getValue());
                b.adminOrdersEmpty.setVisibility(
                        (orders == null || orders.isEmpty()) && !loading ? View.VISIBLE : View.GONE);
            }
        });

        viewModel.getOrdersLoading().observe(getViewLifecycleOwner(), loading -> {
            if (currentSubBinding instanceof AdminOrdersBinding) {
                AdminOrdersBinding b = (AdminOrdersBinding) currentSubBinding;
                b.adminOrdersProgress.setVisibility(Boolean.TRUE.equals(loading) ? View.VISIBLE : View.GONE);
            }
        });

        viewModel.getProductSaving().observe(getViewLifecycleOwner(), saving -> {
            if (currentSubBinding instanceof AdminAddProductBinding) {
                AdminAddProductBinding b = (AdminAddProductBinding) currentSubBinding;
                boolean isSaving = Boolean.TRUE.equals(saving);
                b.saveProductProgress.setVisibility(isSaving ? View.VISIBLE : View.GONE);
                b.saveProductButton.setEnabled(!isSaving);
                b.saveProductButton.setText(isSaving ? "" : getString(R.string.save_product));
            }
        });

    }

    private void buildTabsForRole(boolean isAdmin, boolean isShopOwner) {
        binding.adminTabs.removeAllTabs();
        if (isShopOwner || isAdmin) {
            // shop owner -> δικα του στατιστικα. admin (χωρις shop) -> platform-wide στατιστικα.
            addTab(TabKey.OVERVIEW, R.string.admin_overview);
        }
        if (isShopOwner) {
            addTab(TabKey.SHOP_DETAILS, R.string.admin_shop_details);
            addTab(TabKey.ORDERS, R.string.admin_orders);
            addTab(TabKey.ADD_PRODUCT, R.string.admin_add_product);
        }
        if (isAdmin) {
            addTab(TabKey.SHOPS, R.string.admin_shops);
            addTab(TabKey.MANAGE_ADMINS, R.string.admin_privilages);
        }
        // αν δεν εχει κανενα ρολο, popback
        if (binding.adminTabs.getTabCount() == 0) {
            NavHostFragment.findNavController(this).popBackStack();
        }
    }

    private void addTab(TabKey key, int titleRes) {
        TabLayout.Tab tab = binding.adminTabs.newTab()
                .setText(titleRes)
                .setTag(key);
        binding.adminTabs.addTab(tab);
    }

    private void renderTab(TabKey key) {
        binding.adminContent.removeAllViews();
        currentSubBinding = null;
        orderAdapter = null;
        adminUserAdapter = null;
        shopOwnerAdapter = null;
        shopAdapter = null;
        switch (key) {
            case OVERVIEW: showOverview(); break;
            case SHOP_DETAILS: showShopDetails(); break;
            case ORDERS: showOrders(); break;
            case ADD_PRODUCT: showAddProduct(); break;
            case MANAGE_ADMINS: showManageAdmins(); break;
            case SHOPS: showShops(); break;
        }
    }

    private void showOverview() {
        AdminSession session = AdminSession.getInstance();
        String shopId = session.ownedShopId().getValue();
        boolean isShopOwner = Boolean.TRUE.equals(session.isShopOwner().getValue());

        if (isShopOwner && shopId != null && !shopId.isEmpty()) {
            showShopOverview(shopId);
        } else {
            showPlatformOverview();
        }
    }

    private void showShopOverview(String shopId) {
        AdminOverviewBinding b = AdminOverviewBinding.inflate(
                LayoutInflater.from(requireContext()),
                binding.adminContent,
                false
        );
        binding.adminContent.addView(b.getRoot());
        currentSubBinding = b;

        viewModel.getShopOverview().observe(getViewLifecycleOwner(), overview -> {
            if (currentSubBinding != b || overview == null) return;
            String shopName = overview.shop != null ? overview.shop.getName() : "";
            b.shopNameText.setText(shopName);
            b.shopInitialText.setText(shopName.isEmpty() ? "S" : shopName.substring(0, 1).toUpperCase());
            b.shopStatusText.setVisibility(
                    overview.shop != null && overview.shop.isActive() ? View.VISIBLE : View.GONE);

            b.productsCountText.setText(String.valueOf(overview.productCount));
            b.ordersCountText.setText(String.valueOf(overview.pendingOrderCount));
            b.revenueText.setText(String.format(Locale.getDefault(),
                    "€%.2f", overview.monthlyRevenue));
            b.shopRatingText.setText(String.format(Locale.getDefault(),
                    "%.1f", overview.rating));
        });

        viewModel.loadShopOverview(shopId);
    }

    private void showPlatformOverview() {
        AdminPlatformOverviewBinding b = AdminPlatformOverviewBinding.inflate(
                LayoutInflater.from(requireContext()),
                binding.adminContent,
                false
        );
        binding.adminContent.addView(b.getRoot());
        currentSubBinding = b;

        viewModel.getPlatformOverview().observe(getViewLifecycleOwner(), overview -> {
            if (currentSubBinding != b || overview == null) return;
            b.platformShopsCountText.setText(String.valueOf(overview.shopCount));
            b.platformProductsCountText.setText(String.valueOf(overview.productCount));
            b.platformPendingOrdersCountText.setText(String.valueOf(overview.pendingOrderCount));
            b.platformRevenueText.setText(String.format(Locale.getDefault(),
                    "€%.2f", overview.monthlyRevenue));
        });

        viewModel.loadPlatformOverview();
    }

    private void showShopDetails() {
        AdminShopDetailsBinding b = AdminShopDetailsBinding.inflate(
                LayoutInflater.from(requireContext()),
                binding.adminContent,
                false
        );

        binding.adminContent.addView(b.getRoot());
        currentSubBinding = b;

        String shopId = AdminSession.getInstance().ownedShopId().getValue();
        if (shopId == null || shopId.isEmpty()) {
            // δεν εχει shop ακομα — απενεργοποιουμε save
            b.saveShopDetailsButton.setEnabled(false);
            return;
        }

        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_list_item_1, CATEGORIES);
        b.shopCategoryDropdown.setAdapter(categoryAdapter);

        // φορτωνουμε ζωντανα το shop ωστε το form να ανανεωνεται οταν αλλαζει
        viewModel.getAllShops().observe(getViewLifecycleOwner(), shops -> {
            if (currentSubBinding != b || shops == null) return;
            Shop mine = null;
            for (Shop s : shops) {
                if (shopId.equals(s.getId())) { mine = s; break; }
            }
            if (mine == null) return;
            shopBeingEdited = mine;
            bindShopDetailsForm(b, mine);
        });
        viewModel.loadAllShops();

        b.saveShopDetailsButton.setOnClickListener(v -> submitShopDetails(b));

        viewModel.getShopSaving().observe(getViewLifecycleOwner(), saving -> {
            if (currentSubBinding != b) return;
            boolean isSaving = Boolean.TRUE.equals(saving);
            b.saveShopDetailsButton.setEnabled(!isSaving);
            b.saveShopDetailsButton.setText(isSaving
                    ? getString(R.string.image_uploading)
                    : getString(R.string.save_shop_details));
        });
    }

    private void bindShopDetailsForm(AdminShopDetailsBinding b, Shop shop) {
        String name = shop.getName() != null ? shop.getName() : "";
        b.shopPreviewName.setText(name);
        b.shopInitialPreview.setText(name.isEmpty() ? "S" : name.substring(0, 1).toUpperCase());
        b.shopPreviewStatus.setVisibility(shop.isActive() ? View.VISIBLE : View.GONE);

        // δεν θελουμε να τσακιζουμε καθε φορα ο,τι εχει πληκτρολογησει ο χρηστης.
        // Setarroume μονο αν το πεδιο ειναι ακομα στην default τιμη του ή κενο.
        setIfBlank(b.shopNameInput, name);
        setIfBlank(b.shopDescriptionInput, shop.getDescription());
        setIfBlank(b.shopCategoryDropdown, shop.getMainCategory());
        setIfBlank(b.shopEmailInput, shop.getBusinessEmail());
        setIfBlank(b.shopPhoneInput, shop.getBusinessPhone());
        setIfBlank(b.shopAddressInput, shop.getAddress());
        setIfBlank(b.openingHoursInput, shop.getOpeningHours());
        setIfBlank(b.deliveryOptionsInput, shop.getDeliveryOptions());
    }

    private static void setIfBlank(EditText input, String value) {
        if (value == null) return;
        String current = input.getText() != null ? input.getText().toString() : "";
        if (current.isEmpty()) {
            input.setText(value);
        }
    }

    private static void setIfBlank(MaterialAutoCompleteTextView input, String value) {
        if (value == null) return;
        String current = input.getText() != null ? input.getText().toString() : "";
        if (current.isEmpty()) {
            input.setText(value, false);
        }
    }

    private void submitShopDetails(AdminShopDetailsBinding b) {
        if (shopBeingEdited == null) return;
        Shop shop = copyOf(shopBeingEdited);
        shop.setName(text(b.shopNameInput));
        shop.setDescription(text(b.shopDescriptionInput));
        shop.setMainCategory(text(b.shopCategoryDropdown));
        shop.setBusinessEmail(text(b.shopEmailInput));
        shop.setBusinessPhone(text(b.shopPhoneInput));
        shop.setAddress(text(b.shopAddressInput));
        shop.setOpeningHours(text(b.openingHoursInput));
        shop.setDeliveryOptions(text(b.deliveryOptionsInput));

        if (shop.getName().isEmpty()) {
            Toast.makeText(requireContext(), R.string.all_required_fields_missing,
                    Toast.LENGTH_SHORT).show();
            return;
        }
        viewModel.saveShop(shop);
    }

    private void showOrders() {
        AdminOrdersBinding b = AdminOrdersBinding.inflate(LayoutInflater.from(requireContext()),
                binding.adminContent, false);
        binding.adminContent.addView(b.getRoot());
        currentSubBinding = b;

        orderAdapter = new AdminOrderAdapter((order, newStatus) -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle(R.string.update_status)
                    .setMessage(newStatus)
                    .setPositiveButton(android.R.string.ok, (d, w) -> viewModel.updateOrderStatus(order, newStatus))
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        });
        b.adminOrdersRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        b.adminOrdersRecycler.setAdapter(orderAdapter);

        setupOrderFilters(b);
        applyOrderFilter();
        updateOrderStats(b);

        boolean loading = Boolean.TRUE.equals(viewModel.getOrdersLoading().getValue());
        b.adminOrdersProgress.setVisibility(loading ? View.VISIBLE : View.GONE);
        b.adminOrdersEmpty.setVisibility(cachedOrders.isEmpty() && !loading ? View.VISIBLE : View.GONE);

        String shopId = AdminSession.getInstance().ownedShopId().getValue();
        viewModel.loadShopOrders(shopId);
    }

    private void updateOrderStats(AdminOrdersBinding b) {
        int total = cachedOrders.size();
        int pending = 0, completed = 0;
        for (Order o : cachedOrders) {
            String s = o.getStatus();
            if (AdminOrderAdapter.STATUS_PENDING.equals(s)) pending++;
            else if (AdminOrderAdapter.STATUS_COMPLETED.equals(s)) completed++;
        }
        b.totalOrdersCountText.setText(String.valueOf(total));
        b.pendingOrdersCountText.setText(String.valueOf(pending));
        b.completedOrdersCountText.setText(String.valueOf(completed));
    }

    private void showAddProduct() {
        AdminAddProductBinding b = AdminAddProductBinding.inflate(LayoutInflater.from(requireContext()),
                binding.adminContent, false);
        binding.adminContent.addView(b.getRoot());
        currentSubBinding = b;

        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_list_item_1, CATEGORIES);
        b.productCategoryInput.setAdapter(categoryAdapter);

        if (pickedImageUri != null) {
            ImageLoader.load(b.productImagePreview, pickedImageUri);
            b.productImageEmpty.setVisibility(View.GONE);
        } else {
            b.productImageEmpty.setVisibility(View.VISIBLE);
        }

        b.productPickImageButton.setOnClickListener(v -> imagePicker.launch("image/*"));

        b.productImageUrlInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                String url = s.toString().trim();
                if (!url.isEmpty()) {
                    pickedImageUri = null;
                    ImageLoader.load(b.productImagePreview, url);
                    b.productImageEmpty.setVisibility(View.GONE);
                } else if (pickedImageUri == null) {
                    b.productImagePreview.setImageDrawable(null);
                    b.productImageEmpty.setVisibility(View.VISIBLE);
                }
            }
        });

        b.saveProductButton.setOnClickListener(v -> {
            String title = text(b.productTitleInput);
            String description = text(b.productDescriptionInput);
            String priceStr = text(b.productPriceInput);
            String category = text(b.productCategoryInput);
            String imageUrl = text(b.productImageUrlInput);

            if (title.isEmpty() || description.isEmpty() || priceStr.isEmpty()
                    || category.isEmpty() || (imageUrl.isEmpty() && pickedImageUri == null)) {
                Toast.makeText(requireContext(), R.string.all_required_fields_missing, Toast.LENGTH_SHORT).show();
                return;
            }

            double price;
            try {
                price = Double.parseDouble(priceStr);
            } catch (NumberFormatException ex) {
                Toast.makeText(requireContext(), R.string.product_price, Toast.LENGTH_SHORT).show();
                return;
            }

            Product product = new Product("", title, description, price, imageUrl, category,
                    0.0, 0, 1, new ArrayList<>());
            viewModel.addProduct(product, pickedImageUri);
        });
    }

    private void showManageAdmins() {
        AdminManageAdminsBinding b = AdminManageAdminsBinding.inflate(LayoutInflater.from(requireContext()),
                binding.adminContent, false);
        binding.adminContent.addView(b.getRoot());
        currentSubBinding = b;

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String currentUid = currentUser != null ? currentUser.getUid() : null;

        adminUserAdapter = new AdminUserAdapter(currentUid, user ->
                new AlertDialog.Builder(requireContext())
                        .setTitle(R.string.revoke_admin)
                        .setMessage(getString(R.string.confirm_revoke_admin,
                                user.getEmail() != null ? user.getEmail() : user.getDisplayName()))
                        .setPositiveButton(android.R.string.ok,
                                (d, w) -> viewModel.revokeAdmin(user, currentUid))
                        .setNegativeButton(android.R.string.cancel, null)
                        .show());

        b.adminsRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        b.adminsRecycler.setAdapter(adminUserAdapter);

        adminUserAdapter.submitList(new ArrayList<>(cachedAdmins));
        b.adminsEmpty.setVisibility(cachedAdmins.isEmpty() ? View.VISIBLE : View.GONE);

        b.promoteAdminButton.setOnClickListener(v -> {
            String email = text(b.adminEmailInput);
            if (email.isEmpty()) {
                Toast.makeText(requireContext(), R.string.all_required_fields_missing, Toast.LENGTH_SHORT).show();
                return;
            }
            viewModel.promoteAdminByEmail(email);
        });

        shopOwnerAdapter = new AdminUserAdapter(currentUid, user ->
                new AlertDialog.Builder(requireContext())
                        .setTitle(R.string.revoke_admin)
                        .setMessage(getString(R.string.confirm_revoke_shop_owner,
                                user.getEmail() != null ? user.getEmail() : user.getDisplayName()))
                        .setPositiveButton(android.R.string.ok,
                                (d, w) -> viewModel.revokeShopOwner(user))
                        .setNegativeButton(android.R.string.cancel, null)
                        .show());

        b.shopOwnersRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        b.shopOwnersRecycler.setAdapter(shopOwnerAdapter);

        shopOwnerAdapter.submitList(new ArrayList<>(cachedShopOwners));
        b.shopOwnersEmpty.setVisibility(cachedShopOwners.isEmpty() ? View.VISIBLE : View.GONE);

        // dropdown με ολα τα shops για το assignment
        List<Shop> shopsForDropdown = viewModel.getAllShops().getValue();
        if (shopsForDropdown == null) shopsForDropdown = new ArrayList<>();
        wireShopDropdown(b, shopsForDropdown);

        viewModel.getAllShops().observe(getViewLifecycleOwner(), shops -> {
            if (currentSubBinding == b) {
                wireShopDropdown(b, shops != null ? shops : new ArrayList<>());
            }
        });

        b.promoteShopOwnerButton.setOnClickListener(v -> {
            String email = text(b.shopOwnerEmailInput);
            if (email.isEmpty()) {
                Toast.makeText(requireContext(), R.string.all_required_fields_missing, Toast.LENGTH_SHORT).show();
                return;
            }
            Object selected = b.shopOwnerShopInput.getTag();
            if (!(selected instanceof String) || ((String) selected).isEmpty()) {
                Toast.makeText(requireContext(), R.string.shop_required, Toast.LENGTH_SHORT).show();
                return;
            }
            viewModel.promoteShopOwnerByEmail(email, (String) selected);
        });

        viewModel.loadAdmins();
        viewModel.loadShopOwners();
        viewModel.loadAllShops();
    }

    private void wireShopDropdown(AdminManageAdminsBinding b, List<Shop> shops) {
        String[] names = new String[shops.size()];
        for (int i = 0; i < shops.size(); i++) names[i] = shops.get(i).getName();

        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_list_item_1, names);
        b.shopOwnerShopInput.setAdapter(adapter);

        b.shopOwnerShopInput.setOnItemClickListener((parent, view, position, id) -> {
            if (position >= 0 && position < shops.size()) {
                b.shopOwnerShopInput.setTag(shops.get(position).getId());
            }
        });

        // αν εχουμε μονο ενα shop, προ-επιλεξτο
        if (shops.size() == 1) {
            b.shopOwnerShopInput.setText(shops.get(0).getName(), false);
            b.shopOwnerShopInput.setTag(shops.get(0).getId());
        }
    }

    private void showShops() {
        AdminShopsBinding b = AdminShopsBinding.inflate(
                LayoutInflater.from(requireContext()), binding.adminContent, false);
        binding.adminContent.addView(b.getRoot());
        currentSubBinding = b;

        shopAdapter = new ShopAdapter(shop -> showShopEditor(b, shop));
        b.shopsRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        b.shopsRecycler.setAdapter(shopAdapter);

        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_list_item_1, CATEGORIES);
        b.editorShopCategoryInput.setAdapter(categoryAdapter);

        b.addShopButton.setOnClickListener(v -> showShopEditor(b, null));
        b.cancelShopEditorButton.setOnClickListener(v -> showShopsList(b));
        b.saveShopButton.setOnClickListener(v -> submitShopEditor(b));

        // αρχικη κατασταση — list
        showShopsList(b);

        List<Shop> current = viewModel.getAllShops().getValue();
        if (current != null) {
            shopAdapter.submitList(current);
            b.shopsEmpty.setVisibility(current.isEmpty() ? View.VISIBLE : View.GONE);
        }

        viewModel.loadAllShops();
    }

    private void showShopsList(AdminShopsBinding b) {
        shopBeingEdited = null;
        b.shopsListContainer.setVisibility(View.VISIBLE);
        b.shopEditorContainer.setVisibility(View.GONE);
    }

    private void showShopEditor(AdminShopsBinding b, @Nullable Shop shop) {
        shopBeingEdited = shop;
        b.shopsListContainer.setVisibility(View.GONE);
        b.shopEditorContainer.setVisibility(View.VISIBLE);

        b.shopEditorTitle.setText(shop == null ? R.string.add_shop : R.string.edit_shop);
        b.editorShopNameInput.setText(shop != null ? shop.getName() : "");
        b.editorShopDescriptionInput.setText(shop != null ? shop.getDescription() : "");
        b.editorShopCategoryInput.setText(
                shop != null ? shop.getMainCategory() : "", false);
        b.editorShopEmailInput.setText(shop != null ? shop.getBusinessEmail() : "");
        b.editorShopPhoneInput.setText(shop != null ? shop.getBusinessPhone() : "");
        b.editorShopAddressInput.setText(shop != null ? shop.getAddress() : "");
        b.editorShopActiveSwitch.setChecked(shop == null || shop.isActive());
    }

    private void submitShopEditor(AdminShopsBinding b) {
        String name = text(b.editorShopNameInput);
        if (name.isEmpty()) {
            Toast.makeText(requireContext(), R.string.all_required_fields_missing, Toast.LENGTH_SHORT).show();
            return;
        }
        Shop shop = shopBeingEdited != null ? copyOf(shopBeingEdited) : new Shop();
        shop.setName(name);
        shop.setDescription(text(b.editorShopDescriptionInput));
        shop.setMainCategory(text(b.editorShopCategoryInput));
        shop.setBusinessEmail(text(b.editorShopEmailInput));
        shop.setBusinessPhone(text(b.editorShopPhoneInput));
        shop.setAddress(text(b.editorShopAddressInput));
        shop.setActive(b.editorShopActiveSwitch.isChecked());
        if (shopBeingEdited == null) {
            shop.setCreatedAt(System.currentTimeMillis());
        }
        viewModel.saveShop(shop);
    }

    private Shop copyOf(Shop other) {
        Shop s = new Shop();
        s.setId(other.getId());
        s.setOwnerId(other.getOwnerId());
        s.setOwnerEmail(other.getOwnerEmail());
        s.setName(other.getName());
        s.setDescription(other.getDescription());
        s.setMainCategory(other.getMainCategory());
        s.setBusinessEmail(other.getBusinessEmail());
        s.setBusinessPhone(other.getBusinessPhone());
        s.setAddress(other.getAddress());
        s.setOpeningHours(other.getOpeningHours());
        s.setDeliveryOptions(other.getDeliveryOptions());
        s.setRating(other.getRating());
        s.setProductCount(other.getProductCount());
        s.setActive(other.isActive());
        s.setLatitude(other.getLatitude());
        s.setLongitude(other.getLongitude());
        s.setCreatedAt(other.getCreatedAt());
        return s;
    }

    private void resetAddProductForm() {
        if (!(currentSubBinding instanceof AdminAddProductBinding)) return;
        AdminAddProductBinding b = (AdminAddProductBinding) currentSubBinding;
        b.productTitleInput.setText("");
        b.productDescriptionInput.setText("");
        b.productPriceInput.setText("");
        b.productCategoryInput.setText("", false);
        b.productImageUrlInput.setText("");
        pickedImageUri = null;
        b.productImagePreview.setImageDrawable(null);
        b.productImageEmpty.setVisibility(View.VISIBLE);
    }

    private void setupOrderFilters(AdminOrdersBinding b) {
        b.filterAll.setOnClickListener(v -> {
            selectedOrderFilter = "All";
            applyOrderFilter();
        });

        b.filterPending.setOnClickListener(v -> {
            selectedOrderFilter = AdminOrderAdapter.STATUS_PENDING;
            applyOrderFilter();
        });

        b.filterInTransit.setOnClickListener(v -> {
            selectedOrderFilter = AdminOrderAdapter.STATUS_IN_TRANSIT;
            applyOrderFilter();
        });

        b.filterCompleted.setOnClickListener(v -> {
            selectedOrderFilter = AdminOrderAdapter.STATUS_COMPLETED;
            applyOrderFilter();
        });
    }

    private void applyOrderFilter() {
        if (!(currentSubBinding instanceof AdminOrdersBinding) || orderAdapter == null) return;

        AdminOrdersBinding b = (AdminOrdersBinding) currentSubBinding;

        List<Order> filtered = new ArrayList<>();

        for (Order order : cachedOrders) {
            if ("All".equals(selectedOrderFilter)) {
                filtered.add(order);
            } else if (order.getStatus() != null && order.getStatus().equals(selectedOrderFilter)) {
                filtered.add(order);
            }
        }

        orderAdapter.submitList(filtered);

        b.adminOrdersEmpty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);

        updateFilterChipStyles(b);
    }

    private void updateFilterChipStyles(AdminOrdersBinding b) {
        setChipSelected(b.filterAll, "All".equals(selectedOrderFilter));
        setChipSelected(b.filterPending, AdminOrderAdapter.STATUS_PENDING.equals(selectedOrderFilter));
        setChipSelected(b.filterInTransit, AdminOrderAdapter.STATUS_IN_TRANSIT.equals(selectedOrderFilter));
        setChipSelected(b.filterCompleted, AdminOrderAdapter.STATUS_COMPLETED.equals(selectedOrderFilter));
    }

    private void setChipSelected(TextView chip, boolean selected) {
        chip.setTextColor(ContextCompat.getColor(
                requireContext(),
                selected ? R.color.white : R.color.skroutz_text_primary
        ));

        chip.setBackgroundResource(
                selected ? R.drawable.bg_chip_selected : R.drawable.bg_chip_unselected
        );
    }

    private static String text(EditText input) {
        return input.getText() != null ? input.getText().toString().trim() : "";
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        currentSubBinding = null;
        orderAdapter = null;
    }
}
