package com.pricepulse.ui.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseUser;
import com.pricepulse.R;
import com.pricepulse.cart.CartManager;
import com.pricepulse.databinding.FragmentProductDetailBinding;
import com.pricepulse.databinding.ItemReviewBinding;
import com.pricepulse.model.Product;
import com.pricepulse.model.Review;
import com.pricepulse.viewmodel.ProductDetailUiState;
import com.pricepulse.viewmodel.ProductDetailViewModel;

import java.util.Locale;

public class ProductDetailFragment extends Fragment {

    private FragmentProductDetailBinding binding;
    private ProductDetailViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentProductDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(ProductDetailViewModel.class);

        String productId = getArguments() != null ? getArguments().getString("productId") : null;

        binding.detailBack.setOnClickListener(v -> NavHostFragment.findNavController(this).popBackStack());

        viewModel.loadProduct(productId);

        binding.detailLikeButton.setOnClickListener(v -> {
            FirebaseUser user = viewModel.getAuthManager().getCurrentUserValue();
            if (user == null || user.isAnonymous()) {
                NavHostFragment.findNavController(this).navigate(R.id.action_detail_to_profile);
            } else {
                viewModel.toggleLike();
            }
        });

        viewModel.getUiState().observe(getViewLifecycleOwner(), this::render);
    }

    private void render(ProductDetailUiState state) {
        if (state instanceof ProductDetailUiState.Loading) {
            binding.detailProgress.setVisibility(View.VISIBLE);
            binding.scrollView.setVisibility(View.GONE);
            binding.bottomBar.setVisibility(View.GONE);
            binding.detailLikeButton.setVisibility(View.GONE);
            binding.detailError.setVisibility(View.GONE);
        } else if (state instanceof ProductDetailUiState.Success) {
            ProductDetailUiState.Success s = (ProductDetailUiState.Success) state;
            binding.detailProgress.setVisibility(View.GONE);
            binding.scrollView.setVisibility(View.VISIBLE);
            binding.bottomBar.setVisibility(View.VISIBLE);
            binding.detailLikeButton.setVisibility(View.VISIBLE);
            binding.detailError.setVisibility(View.GONE);
            bindProduct(s.product, s.isLiked);
        } else if (state instanceof ProductDetailUiState.Error) {
            binding.detailProgress.setVisibility(View.GONE);
            binding.scrollView.setVisibility(View.GONE);
            binding.bottomBar.setVisibility(View.GONE);
            binding.detailLikeButton.setVisibility(View.GONE);
            binding.detailError.setVisibility(View.VISIBLE);
        }
    }

    private void bindProduct(Product p, boolean isLiked) {
        Context ctx = requireContext();
        binding.detailCategory.setText(p.getCategory().toUpperCase(Locale.getDefault()));
        binding.detailTitle.setText(p.getTitle());
        binding.detailDescription.setText(p.getDescription());
        binding.detailRating.setText(String.format(Locale.getDefault(), "%.1f", p.getRating()));
        binding.detailReviewCount.setText(" (" + p.getReviewCount() + " Reviews)");
        binding.detailPrice.setText(String.format(Locale.getDefault(), "%.2f €", p.getPrice()));
        Glide.with(binding.detailImage).load(p.getImageUrl()).into(binding.detailImage);

        ImageView[] stars = {binding.dStar1, binding.dStar2, binding.dStar3, binding.dStar4, binding.dStar5};
        int gold = ContextCompat.getColor(ctx, R.color.star_gold);
        int gray = ContextCompat.getColor(ctx, R.color.light_gray);
        int filled = (int) p.getRating();
        for (int i = 0; i < stars.length; i++) {
            stars[i].setColorFilter(i < filled ? gold : gray);
        }

        binding.detailLikeButton.setImageResource(
                isLiked ? R.drawable.ic_favorite : R.drawable.ic_favorite_border);
        binding.detailLikeButton.setColorFilter(
                ContextCompat.getColor(ctx, isLiked ? R.color.error_red : R.color.skroutz_text_primary));

        binding.reviewsContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(ctx);
        if (p.getReviews() == null || p.getReviews().isEmpty()) {
            CardView card = new CardView(ctx);
            float density = ctx.getResources().getDisplayMetrics().density;
            card.setRadius(density * 12f);
            card.setCardBackgroundColor(ContextCompat.getColor(ctx, R.color.white));
            card.setCardElevation(0f);
            TextView tv = new TextView(ctx);
            tv.setText(getString(R.string.no_reviews));
            tv.setTextColor(ContextCompat.getColor(ctx, R.color.skroutz_text_secondary));
            tv.setPadding(48, 48, 48, 48);
            card.addView(tv);
            binding.reviewsContainer.addView(card);
        } else {
            for (Review review : p.getReviews()) {
                ItemReviewBinding rb = ItemReviewBinding.inflate(inflater, binding.reviewsContainer, false);
                bindReview(rb, review, gold, gray);
                binding.reviewsContainer.addView(rb.getRoot());
            }
        }

        binding.addToCartButton.setOnClickListener(v -> CartManager.getInstance().addProduct(p));
    }

    private void bindReview(ItemReviewBinding b, Review review, int gold, int gray) {
        b.reviewUser.setText(review.getUsername());
        b.reviewComment.setText(review.getComment());
        ImageView[] stars = {b.rStar1, b.rStar2, b.rStar3, b.rStar4, b.rStar5};
        for (int i = 0; i < stars.length; i++) {
            stars[i].setColorFilter(i < review.getRating() ? gold : gray);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
