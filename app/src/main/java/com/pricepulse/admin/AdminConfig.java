package com.pricepulse.admin;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

// Αρχική λίστα admins - αυτά τα emails γίνονται admin την πρώτη φορά.
// Μετά ολα γίνονται μέσω firestore (admin πεδίο στο user doc), η λίστα δεν ξανατρέχει.
public final class AdminConfig {

    public static final Set<String> SEED_ADMIN_EMAILS = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList(
                    "charis@test.gr",
                    "kostas@test.gr"
            )));

    private AdminConfig() {}

    public static boolean isSeedAdmin(String email) {
        return email != null && SEED_ADMIN_EMAILS.contains(email.toLowerCase());
    }
}
