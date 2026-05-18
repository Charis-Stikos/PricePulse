package com.pricepulse;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.badge.BadgeDrawable;
import com.pricepulse.cart.CartManager;
import com.pricepulse.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Uncomment the line below to seed data to Firestore, then comment it back out.
        // new com.pricepulse.util.FirestoreSeeder().seedData();

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        NavHostFragment navHost = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.navHostFragment);
        if (navHost == null) {
            throw new IllegalStateException("NavHostFragment not found");
        }
        NavController navController = navHost.getNavController();
        NavigationUI.setupWithNavController(binding.bottomNav, navController);

        navController.addOnDestinationChangedListener((controller, destination, arguments) ->
                binding.bottomNav.setVisibility(
                        destination.getId() == R.id.productDetailFragment ? View.GONE : View.VISIBLE));

        CartManager.getInstance().getCartCount().observe(this, count -> {
            if (count != null && count > 0) {
                BadgeDrawable badge = binding.bottomNav.getOrCreateBadge(R.id.cartFragment);
                badge.setVisible(true);
                badge.setNumber(count);
                badge.setBackgroundColor(getColor(R.color.skroutz_orange));
                badge.setBadgeTextColor(getColor(R.color.white));
            } else {
                binding.bottomNav.removeBadge(R.id.cartFragment);
            }
        });
    }
}
