package com.pricepulse.ui.fragments;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.GridLayoutManager;

import com.pricepulse.R;
import com.pricepulse.databinding.FragmentSearchBinding;
import com.pricepulse.databinding.ItemSuggestionBinding;
import com.pricepulse.ui.adapters.ProductAdapter;
import com.pricepulse.viewmodel.SearchUiState;
import com.pricepulse.viewmodel.SearchViewModel;

import java.util.Arrays;
import java.util.List;

public class SearchFragment extends Fragment {

    private FragmentSearchBinding binding;
    private SearchViewModel viewModel;
    private ProductAdapter productAdapter;
    private TextWatcher textWatcher;

    private final List<String> suggestions = Arrays.asList(
            "Smartphone", "Wireless Headphones", "Gaming Laptop", "Sneakers"
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentSearchBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(SearchViewModel.class);

        productAdapter = new ProductAdapter(product -> {
            Bundle args = new Bundle();
            args.putString("productId", product.getId());
            NavHostFragment.findNavController(this).navigate(R.id.action_search_to_detail, args);
        });
        binding.resultsRecycler.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        binding.resultsRecycler.setAdapter(productAdapter);

        binding.backButton.setOnClickListener(v -> NavHostFragment.findNavController(this).popBackStack());

        binding.clearButton.setOnClickListener(v -> {
            binding.searchInput.setText("");
            viewModel.onQueryChanged("");
        });

        binding.searchInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                InputMethodManager imm = (InputMethodManager)
                        requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                return true;
            }
            return false;
        });

        textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String text = s != null ? s.toString() : "";
                viewModel.onQueryChanged(text);
                binding.clearButton.setVisibility(text.isEmpty() ? View.GONE : View.VISIBLE);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        };
        binding.searchInput.addTextChangedListener(textWatcher);

        renderSuggestions();

        viewModel.getQuery().observe(getViewLifecycleOwner(), q -> {
            String currentText = binding.searchInput.getText() != null
                    ? binding.searchInput.getText().toString() : "";
            if (!currentText.equals(q)) {
                binding.searchInput.setText(q);
                binding.searchInput.setSelection(q.length());
            }
        });

        viewModel.getUiState().observe(getViewLifecycleOwner(), this::render);
    }

    private void renderSuggestions() {
        binding.suggestionsContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (String suggestion : suggestions) {
            ItemSuggestionBinding row = ItemSuggestionBinding.inflate(inflater, binding.suggestionsContainer, false);
            row.suggestionText.setText(suggestion);
            row.getRoot().setOnClickListener(v -> {
                binding.searchInput.setText(suggestion);
                binding.searchInput.setSelection(suggestion.length());
                viewModel.onQueryChanged(suggestion);
            });
            binding.suggestionsContainer.addView(row.getRoot());
        }
    }

    private void render(SearchUiState state) {
        if (state instanceof SearchUiState.Idle) {
            binding.idleLayout.setVisibility(View.VISIBLE);
            binding.resultsRecycler.setVisibility(View.GONE);
            binding.emptyLayout.setVisibility(View.GONE);
        } else if (state instanceof SearchUiState.Loading) {
            binding.idleLayout.setVisibility(View.GONE);
            binding.emptyLayout.setVisibility(View.GONE);
            binding.resultsRecycler.setVisibility(View.VISIBLE);
            productAdapter.submitShimmer(4);
        } else if (state instanceof SearchUiState.Success) {
            binding.idleLayout.setVisibility(View.GONE);
            binding.emptyLayout.setVisibility(View.GONE);
            binding.resultsRecycler.setVisibility(View.VISIBLE);
            productAdapter.submitProducts(((SearchUiState.Success) state).products);
        } else if (state instanceof SearchUiState.Empty) {
            binding.idleLayout.setVisibility(View.GONE);
            binding.resultsRecycler.setVisibility(View.GONE);
            binding.emptyLayout.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (textWatcher != null && binding != null) {
            binding.searchInput.removeTextChangedListener(textWatcher);
        }
        binding = null;
    }
}
