package com.pricepulse;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.pricepulse.util.LocationDiscountHelper;

import org.junit.Test;

/**
 * Unit tests για τις καθαρά αριθμητικές μεθόδους της LocationDiscountHelper.
 * Δεν χρησιμοποιούν Android context, Geocoder ή GPS —
 * τρέχουν ως απλά JUnit tests στο JVM.
 */
public class LocationDiscountHelperTest {

    private static final double DELTA = 0.0001;

    // ================================================================== //
    //  Ομάδα Α — Σταθερές (constants)
    // ================================================================== //

    // TC-U10: Ο ρυθμός έκπτωσης είναι 30 %
    @Test
    public void constant_deliveryDiscountRate_is30Percent() {
        assertEquals(0.30, LocationDiscountHelper.DELIVERY_DISCOUNT_RATE, DELTA);
    }

    // TC-U11: Η μέγιστη απόσταση χρήστη↔διεύθυνσης είναι 10 km
    @Test
    public void constant_maxUserToAddressDistance_is10km() {
        assertEquals(10.0, LocationDiscountHelper.MAX_USER_TO_ADDRESS_DISTANCE_KM, DELTA);
    }

    // TC-U12: Η μέγιστη απόσταση διεύθυνσης↔καταστήματος είναι 30 km
    @Test
    public void constant_maxAddressToShopDistance_is30km() {
        assertEquals(30.0, LocationDiscountHelper.MAX_ADDRESS_TO_SHOP_DISTANCE_KM, DELTA);
    }

    // ================================================================== //
    //  Ομάδα Β — calculateDiscountAmount(double deliveryFee)
    // ================================================================== //

    // TC-U13: Τυπική χρέωση μεταφορικών — έκπτωση 30 %
    @Test
    public void calculateDiscountAmount_typicalFee_returns30Percent() {
        double fee = 5.00;
        double expected = 1.50; // 30 % του 5,00
        assertEquals(expected, LocationDiscountHelper.calculateDiscountAmount(fee), DELTA);
    }

    // TC-U14: Μηδενική χρέωση — έκπτωση 0,00 €
    @Test
    public void calculateDiscountAmount_zeroFee_returnsZero() {
        assertEquals(0.0, LocationDiscountHelper.calculateDiscountAmount(0.0), DELTA);
    }

    // TC-U15: Μεγάλη χρέωση (20 €) — επαλήθευση αναλογίας
    @Test
    public void calculateDiscountAmount_largeFee_correct() {
        double fee = 20.00;
        double expected = 6.00; // 30 % του 20
        assertEquals(expected, LocationDiscountHelper.calculateDiscountAmount(fee), DELTA);
    }

    // TC-U16: Δεκαδική χρέωση (3,30 €)
    @Test
    public void calculateDiscountAmount_decimalFee_correct() {
        double fee = 3.30;
        double expected = 0.99; // 30 % του 3,30
        assertEquals(expected, LocationDiscountHelper.calculateDiscountAmount(fee), DELTA);
    }

    // ================================================================== //
    //  Ομάδα Γ — calculateFinalDeliveryFee(double deliveryFee)
    // ================================================================== //

    // TC-U17: Τελικά μεταφορικά = 70 % της αρχικής χρέωσης
    @Test
    public void calculateFinalDeliveryFee_typicalFee_returns70Percent() {
        double fee = 5.00;
        double expected = 3.50; // 70 % του 5,00
        assertEquals(expected, LocationDiscountHelper.calculateFinalDeliveryFee(fee), DELTA);
    }

    // TC-U18: Μηδενική χρέωση → τελικά μεταφορικά = 0,00 €
    @Test
    public void calculateFinalDeliveryFee_zeroFee_returnsZero() {
        assertEquals(0.0, LocationDiscountHelper.calculateFinalDeliveryFee(0.0), DELTA);
    }

    // TC-U19: Έλεγχος συνέπειας: discount + finalFee == originalFee
    @Test
    public void discountPlusFinalFee_equalsOriginalFee() {
        double fee = 8.50;
        double discount = LocationDiscountHelper.calculateDiscountAmount(fee);
        double finalFee = LocationDiscountHelper.calculateFinalDeliveryFee(fee);
        assertEquals(fee, discount + finalFee, DELTA);
    }

    // ================================================================== //
    //  Ομάδα Δ — Αντικείμενο Result (factory methods)
    // ================================================================== //

    // TC-U20: Result.eligible() → isSuccess=true, isEligible=true
    @Test
    public void result_eligible_isSuccessAndEligible() {
        LocationDiscountHelper.Result result =
                LocationDiscountHelper.Result.eligible(37.97, 23.73, 0.5, 8.0);

        assertTrue(result.isSuccess());
        assertTrue(result.isEligible());
        assertNotNull(result.getMessage());
    }

    // TC-U21: Result.notEligible() → isSuccess=true, isEligible=false
    @Test
    public void result_notEligible_isSuccessButNotEligible() {
        LocationDiscountHelper.Result result =
                LocationDiscountHelper.Result.notEligible(37.97, 23.73, 15.0, 0.0,
                        "Your current location is too far from the shipping address.");

        assertTrue(result.isSuccess());
        assertFalse(result.isEligible());
        assertEquals("Your current location is too far from the shipping address.", result.getMessage());
    }

    // TC-U22: Result.error() → isSuccess=false, isEligible=false
    @Test
    public void result_error_isNotSuccessAndNotEligible() {
        LocationDiscountHelper.Result result =
                LocationDiscountHelper.Result.error("Shipping address is empty.");

        assertFalse(result.isSuccess());
        assertFalse(result.isEligible());
        assertEquals("Shipping address is empty.", result.getMessage());
    }

    // TC-U23: Result.error() → συντεταγμένες μηδενίζονται (0,0)
    @Test
    public void result_error_coordinatesAreZero() {
        LocationDiscountHelper.Result result =
                LocationDiscountHelper.Result.error("Could not find the shipping address.");

        assertEquals(0.0, result.getShippingLatitude(), DELTA);
        assertEquals(0.0, result.getShippingLongitude(), DELTA);
        assertEquals(0.0, result.getUserToAddressDistanceKm(), DELTA);
        assertEquals(0.0, result.getAddressToShopDistanceKm(), DELTA);
    }

    // TC-U24: Result.eligible() αποθηκεύει σωστά τις αποστάσεις
    @Test
    public void result_eligible_storesDistancesCorrectly() {
        LocationDiscountHelper.Result result =
                LocationDiscountHelper.Result.eligible(37.97, 23.73, 2.3, 14.7);

        assertEquals(2.3, result.getUserToAddressDistanceKm(), DELTA);
        assertEquals(14.7, result.getAddressToShopDistanceKm(), DELTA);
    }
}
