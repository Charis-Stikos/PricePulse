package com.pricepulse.auth;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class AuthManager {

    public interface AuthResultCallback {
        void onResult(boolean success, @Nullable String error);
    }

    public interface AnonymousResultCallback {
        void onResult(boolean success);
    }

    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final MutableLiveData<FirebaseUser> currentUser = new MutableLiveData<>();

    public AuthManager() {
        currentUser.setValue(auth.getCurrentUser());
        auth.addAuthStateListener(firebaseAuth -> currentUser.setValue(firebaseAuth.getCurrentUser()));
    }

    public LiveData<FirebaseUser> getCurrentUser() {
        return currentUser;
    }

    @Nullable
    public FirebaseUser getCurrentUserValue() {
        return currentUser.getValue();
    }

    public boolean isUserLoggedIn() {
        return auth.getCurrentUser() != null;
    }

    public void signUp(String email, String pass, AuthResultCallback callback) {
        auth.createUserWithEmailAndPassword(email, pass)
                .addOnCompleteListener(task -> {
                    currentUser.setValue(auth.getCurrentUser());
                    callback.onResult(task.isSuccessful(),
                            task.getException() != null ? task.getException().getMessage() : null);
                });
    }

    public void signIn(String email, String pass, AuthResultCallback callback) {
        auth.signInWithEmailAndPassword(email, pass)
                .addOnCompleteListener(task -> {
                    currentUser.setValue(auth.getCurrentUser());
                    callback.onResult(task.isSuccessful(),
                            task.getException() != null ? task.getException().getMessage() : null);
                });
    }

    public void signInAnonymously(AnonymousResultCallback callback) {
        auth.signInAnonymously()
                .addOnCompleteListener(task -> {
                    currentUser.setValue(auth.getCurrentUser());
                    callback.onResult(task.isSuccessful());
                });
    }

    public void signOut() {
        auth.signOut();
        currentUser.setValue(null);
    }
}
