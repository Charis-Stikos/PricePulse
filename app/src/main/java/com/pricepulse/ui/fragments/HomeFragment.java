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
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.pricepulse.R;
import com.pricepulse.databinding.FragmentHomeBinding;
import com.pricepulse.ui.adapters.CategoryAdapter;
import com.pricepulse.ui.adapters.ProductAdapter;
import com.pricepulse.viewmodel.HomeUiState;
import com.pricepulse.viewmodel.HomeViewModel;

import java.util.Arrays;
import java.util.List;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private HomeViewModel viewModel;

    private final List<String> categories = Arrays.asList(
            "All", "Electronics", "Fashion", "Home", "Sports", "Beauty", "Books"
    );

    private ProductAdapter productAdapter;
    private CategoryAdapter categoryAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        categoryAdapter = new CategoryAdapter(categories, viewModel::onCategorySelected);
        binding.categoryRecycler.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.categoryRecycler.setAdapter(categoryAdapter);

        productAdapter = new ProductAdapter(product -> {
            Bundle args = new Bundle();
            args.putString("productId", product.getId());
            NavHostFragment.findNavController(this).navigate(R.id.action_home_to_detail, args);
        });
        binding.productsRecycler.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        binding.productsRecycler.setAdapter(productAdapter);

        binding.searchBar.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.action_home_to_search));

        viewModel.getSelectedCategory().observe(getViewLifecycleOwner(), categoryAdapter::setSelected);
        viewModel.getUiState().observe(getViewLifecycleOwner(), this::render);
    }

    private void render(HomeUiState state) {
        if (state instanceof HomeUiState.Loading) {
            binding.errorText.setVisibility(View.GONE);
            binding.productsRecycler.setVisibility(View.VISIBLE);
            productAdapter.submitShimmer(6);
        } else if (state instanceof HomeUiState.Success) {
            binding.errorText.setVisibility(View.GONE);
            binding.productsRecycler.setVisibility(View.VISIBLE);
            productAdapter.submitProducts(((HomeUiState.Success) state).products);
        } else if (state instanceof HomeUiState.Error) {
            binding.errorText.setText(((HomeUiState.Error) state).message);
            binding.errorText.setVisibility(View.VISIBLE);
            binding.productsRecycler.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
