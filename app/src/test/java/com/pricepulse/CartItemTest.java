package com.pricepulse;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

import com.pricepulse.model.CartItem;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests για το CartItem — καθαρή Java, χωρίς Android framework.
 */
public class CartItemTest {

    private CartItem item;

    @Before
    public void setUp() {
        item = new CartItem("prod-001", "Laptop XYZ", 799.99, "https://img.example.com/1.jpg", 1, "shop-A");
    }

    // ------------------------------------------------------------------ //
    // TC-U01: Constructor αρχικοποιεί σωστά όλα τα πεδία
    // ------------------------------------------------------------------ //
    @Test
    public void constructor_setsAllFieldsCorrectly() {
        assertEquals("prod-001", item.getProductId());
        assertEquals("Laptop XYZ", item.getProductTitle());
        assertEquals(799.99, item.getProductPrice(), 0.001);
        assertEquals("https://img.example.com/1.jpg", item.getProductImageUrl());
        assertEquals(1, item.getQuantity());
        assertEquals("shop-A", item.getShopId());
    }

    // ------------------------------------------------------------------ //
    // TC-U02: withQuantity() επιστρέφει νέο αντικείμενο με ενημερωμένη
    //         ποσότητα, χωρίς να τροποποιεί το αρχικό
    // ------------------------------------------------------------------ //
    @Test
    public void withQuantity_returnsNewInstanceWithUpdatedQuantity() {
        CartItem updated = item.withQuantity(5);

        // Νέο αντικείμενο, όχι ίδια αναφορά
        assertNotSame(item, updated);
        // Ποσότητα ενημερώθηκε
        assertEquals(5, updated.getQuantity());
        // Το αρχικό παραμένει αναλλοίωτο
        assertEquals(1, item.getQuantity());
    }

    // ------------------------------------------------------------------ //
    // TC-U03: withQuantity() διατηρεί όλα τα υπόλοιπα πεδία αναλλοίωτα
    // ------------------------------------------------------------------ //
    @Test
    public void withQuantity_preservesOtherFields() {
        CartItem updated = item.withQuantity(3);

        assertEquals(item.getProductId(), updated.getProductId());
        assertEquals(item.getProductTitle(), updated.getProductTitle());
        assertEquals(item.getProductPrice(), updated.getProductPrice(), 0.001);
        assertEquals(item.getProductImageUrl(), updated.getProductImageUrl());
        assertEquals(item.getShopId(), updated.getShopId());
    }

    // ------------------------------------------------------------------ //
    // TC-U04: Η επί μέρους αξία (price × quantity) υπολογίζεται σωστά
    //         (χρησιμοποιείται άμεσα από CartManager.getTotalAmount)
    // ------------------------------------------------------------------ //
    @Test
    public void lineTotal_priceTimesQuantity_isCorrect() {
        CartItem twoItems = item.withQuantity(2);
        double expectedLineTotal = 799.99 * 2;

        assertEquals(expectedLineTotal, twoItems.getProductPrice() * twoItems.getQuantity(), 0.001);
    }

    // ------------------------------------------------------------------ //
    // TC-U05: withQuantity(1) — ειδική περίπτωση: ποσότητα = 1
    // ------------------------------------------------------------------ //
    @Test
    public void withQuantity_withOne_returnsCorrectQuantity() {
        CartItem alreadyOne = item.withQuantity(1);
        assertEquals(1, alreadyOne.getQuantity());
    }

    // ------------------------------------------------------------------ //
    // TC-U06: Constructor χωρίς shopId αναθέτει κενό string (default)
    // ------------------------------------------------------------------ //
    @Test
    public void constructorWithoutShopId_defaultsToEmptyString() {
        CartItem noShop = new CartItem("prod-002", "Headphones", 49.99, "url", 1);
        assertEquals("", noShop.getShopId());
    }
}
