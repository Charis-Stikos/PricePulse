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

    public interface CoordinatesCallback {
        void onResult(Coordinates coordinates);
    }

    public static final double MAX_USER_TO_ADDRESS_DISTANCE_KM = 10.0;
    public static final double MAX_ADDRESS_TO_SHOP_DISTANCE_KM = 30.0;
    public static final double DELIVERY_DISCOUNT_RATE = 0.30;

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    // Lazy αρχικοποίηση ώστε η κλάση να φορτώνεται χωρίς Looper στα JVM unit tests
    private static Handler mainHandler() {
        return new Handler(Looper.getMainLooper());
    }

    public static void checkDiscountEligibility(
            Context context,
            String shippingAddressText,
            double userLatitude,
            double userLongitude,
            double shopLatitude,
            double shopLongitude,
            DiscountCallback callback
    ) {
        Context appContext = context.getApplicationContext();

        EXECUTOR.execute(() -> {
            Result result = calculateResult(
                    appContext,
                    shippingAddressText,
                    userLatitude,
                    userLongitude,
                    shopLatitude,
                    shopLongitude
            );

            mainHandler().post(() -> callback.onResult(result));
        });
    }

    public static void geocodeAddress(
            Context context,
            String addressText,
            CoordinatesCallback callback
    ) {
        Context appContext = context.getApplicationContext();

        EXECUTOR.execute(() -> {
            Address address = geocodeAddressInternal(appContext, addressText);
            Coordinates coordinates = address != null
                    ? new Coordinates(address.getLatitude(), address.getLongitude())
                    : null;

            mainHandler().post(() -> callback.onResult(coordinates));
        });
    }

    private static Result calculateResult(
            Context context,
            String shippingAddressText,
            double userLatitude,
            double userLongitude,
            double shopLatitude,
            double shopLongitude
    ) {
        if (shippingAddressText == null || shippingAddressText.trim().isEmpty()) {
            return Result.error("Shipping address is empty.");
        }

        Address shippingAddress = geocodeAddressInternal(context, shippingAddressText);

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
                shopLatitude,
                shopLongitude
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
    private static Address geocodeAddressInternal(Context context, String addressText) {
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

    public static final class Coordinates {
        private final double latitude;
        private final double longitude;

        private Coordinates(double latitude, double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
        }

        public double getLatitude() {
            return latitude;
        }

        public double getLongitude() {
            return longitude;
        }
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
