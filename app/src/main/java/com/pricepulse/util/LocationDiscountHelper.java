package com.pricepulse.util;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Handler;
import android.os.Looper;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class LocationDiscountHelper {

    private LocationDiscountHelper() {
    }

    public interface DiscountCallback {
        void onResult(Result result);
    }

    public static final double MAX_USER_TO_ADDRESS_DISTANCE_KM = 10.0;
    public static final double MAX_ADDRESS_TO_SHOP_DISTANCE_KM = 30.0;
    public static final double DELIVERY_DISCOUNT_RATE = 0.30;

    // demo συντεταγμενες (κεντρο Θεσσαλονικης) — κανονικα πρεπει να ερχονται απο το επιλεγμενο shop στο Firestore
    private static final double SHOP_LATITUDE = 40.6401;
    private static final double SHOP_LONGITUDE = 22.9444;

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    public static void checkDiscountEligibility(
            Context context,
            String shippingAddressText,
            double userLatitude,
            double userLongitude,
            DiscountCallback callback
    ) {
        Context appContext = context.getApplicationContext();

        EXECUTOR.execute(() -> {
            Result result = calculateResult(
                    appContext,
                    shippingAddressText,
                    userLatitude,
                    userLongitude
            );

            MAIN_HANDLER.post(() -> callback.onResult(result));
        });
    }

    private static Result calculateResult(
            Context context,
            String shippingAddressText,
            double userLatitude,
            double userLongitude
    ) {
        if (shippingAddressText == null || shippingAddressText.trim().isEmpty()) {
            return Result.error("Shipping address is empty.");
        }

        Address shippingAddress = geocodeAddress(context, shippingAddressText);

        if (shippingAddress == null) {
            return Result.error("Could not find the shipping address.");
        }

        double shippingLatitude = shippingAddress.getLatitude();
        double shippingLongitude = shippingAddress.getLongitude();

        double userToAddressDistanceKm = distanceKm(
                userLatitude,
                userLongitude,
                shippingLatitude,
                shippingLongitude
        );

        boolean addressVerified = userToAddressDistanceKm <= MAX_USER_TO_ADDRESS_DISTANCE_KM;

        if (!addressVerified) {
            return Result.notEligible(
                    shippingLatitude,
                    shippingLongitude,
                    userToAddressDistanceKm,
                    0.0,
                    "Your current location is too far from the shipping address."
            );
        }

        double addressToShopDistanceKm = distanceKm(
                shippingLatitude,
                shippingLongitude,
                SHOP_LATITUDE,
                SHOP_LONGITUDE
        );

        boolean eligibleForDiscount = addressToShopDistanceKm <= MAX_ADDRESS_TO_SHOP_DISTANCE_KM;

        if (!eligibleForDiscount) {
            return Result.notEligible(
                    shippingLatitude,
                    shippingLongitude,
                    userToAddressDistanceKm,
                    addressToShopDistanceKm,
                    "The shipping address is outside the 30 km shop delivery discount area."
            );
        }

        return Result.eligible(
                shippingLatitude,
                shippingLongitude,
                userToAddressDistanceKm,
                addressToShopDistanceKm
        );
    }

    @SuppressWarnings("deprecation")
    private static Address geocodeAddress(Context context, String addressText) {
        Geocoder geocoder = new Geocoder(context, Locale.getDefault());

        try {
            List<Address> results = geocoder.getFromLocationName(addressText, 1);

            if (results == null || results.isEmpty()) {
                return null;
            }

            return results.get(0);
        } catch (IOException ex) {
            return null;
        }
    }

    private static double distanceKm(
            double startLatitude,
            double startLongitude,
            double endLatitude,
            double endLongitude
    ) {
        float[] results = new float[1];

        Location.distanceBetween(
                startLatitude,
                startLongitude,
                endLatitude,
                endLongitude,
                results
        );

        return results[0] / 1000.0;
    }

    public static double calculateDiscountAmount(double deliveryFee) {
        return deliveryFee * DELIVERY_DISCOUNT_RATE;
    }

    public static double calculateFinalDeliveryFee(double deliveryFee) {
        return deliveryFee - calculateDiscountAmount(deliveryFee);
    }

    public static final class Result {
        private final boolean success;
        private final boolean eligible;
        private final String message;
        private final double shippingLatitude;
        private final double shippingLongitude;
        private final double userToAddressDistanceKm;
        private final double addressToShopDistanceKm;

        private Result(
                boolean success,
                boolean eligible,
                String message,
                double shippingLatitude,
                double shippingLongitude,
                double userToAddressDistanceKm,
                double addressToShopDistanceKm
        ) {
            this.success = success;
            this.eligible = eligible;
            this.message = message;
            this.shippingLatitude = shippingLatitude;
            this.shippingLongitude = shippingLongitude;
            this.userToAddressDistanceKm = userToAddressDistanceKm;
            this.addressToShopDistanceKm = addressToShopDistanceKm;
        }

        public static Result eligible(
                double shippingLatitude,
                double shippingLongitude,
                double userToAddressDistanceKm,
                double addressToShopDistanceKm
        ) {
            return new Result(
                    true,
                    true,
                    "Location discount approved.",
                    shippingLatitude,
                    shippingLongitude,
                    userToAddressDistanceKm,
                    addressToShopDistanceKm
            );
        }

        public static Result notEligible(
                double shippingLatitude,
                double shippingLongitude,
                double userToAddressDistanceKm,
                double addressToShopDistanceKm,
                String message
        ) {
            return new Result(
                    true,
                    false,
                    message,
                    shippingLatitude,
                    shippingLongitude,
                    userToAddressDistanceKm,
                    addressToShopDistanceKm
            );
        }

        public static Result error(String message) {
            return new Result(
                    false,
                    false,
                    message,
                    0.0,
                    0.0,
                    0.0,
                    0.0
            );
        }

        public boolean isSuccess() {
            return success;
        }

        public boolean isEligible() {
            return eligible;
        }

        public String getMessage() {
            return message;
        }

        public double getShippingLatitude() {
            return shippingLatitude;
        }

        public double getShippingLongitude() {
            return shippingLongitude;
        }

        public double getUserToAddressDistanceKm() {
            return userToAddressDistanceKm;
        }

        public double getAddressToShopDistanceKm() {
            return addressToShopDistanceKm;
        }
    }
}