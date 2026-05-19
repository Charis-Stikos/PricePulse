package com.pricepulse.ui.fragments;

import android.app.AlertDialog;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
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

import com.pricepulse.util.ImageLoader;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.pricepulse.R;
import com.pricepulse.databinding.AdminAddProductBinding;
import com.pricepulse.databinding.AdminManageAdminsBinding;
import com.pricepulse.databinding.AdminOrdersBinding;
import com.pricepulse.databinding.FragmentAdminDashboardBinding;
import com.pricepulse.model.Product;
import com.pricepulse.ui.adapters.AdminOrderAdapter;
import com.pricepulse.ui.adapters.AdminUserAdapter;
import com.pricepulse.viewmodel.AdminViewModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AdminDashboardFragment extends Fragment {

    private FragmentAdminDashboardBinding binding;
    private AdminViewModel viewModel;
    private ViewBinding currentSubBinding;
    private AdminOrderAdapter orderAdapter;
    private AdminUserAdapter adminUserAdapter;
    private Uri pickedImageUri;
    private ActivityResultLauncher<String> imagePicker;

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
            @Override public void onTabSelected(TabLayout.Tab tab) { renderTab(tab.getPosition()); }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
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
            }
        });

        viewModel.getAdmins().observe(getViewLifecycleOwner(), admins -> {
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

        viewModel.getOrders().observe(getViewLifecycleOwner(), orders -> {
            if (currentSubBinding instanceof AdminOrdersBinding && orderAdapter != null) {
                AdminOrdersBinding b = (AdminOrdersBinding) currentSubBinding;
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

        renderTab(0);
    }

    private void renderTab(int position) {
        binding.adminContent.removeAllViews();
        currentSubBinding = null;
        orderAdapter = null;
        adminUserAdapter = null;
        if (position == 0) showOrders();
        else if (position == 1) showAddProduct();
        else showManageAdmins();
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

        viewModel.loadOrders();
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

        b.productImageUrlInput.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(android.text.Editable s) {
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

        b.promoteAdminButton.setOnClickListener(v -> {
            String email = text(b.adminEmailInput);
            if (email.isEmpty()) {
                Toast.makeText(requireContext(), R.string.all_required_fields_missing, Toast.LENGTH_SHORT).show();
                return;
            }
            viewModel.promoteAdminByEmail(email);
        });

        viewModel.loadAdmins();
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

    private static String text(android.widget.EditText input) {
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
