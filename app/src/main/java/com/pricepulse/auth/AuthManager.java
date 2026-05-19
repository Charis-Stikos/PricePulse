package com.pricepulse.auth;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

public class AuthManager {

    public interface AuthResultCallback {
        void onResult(boolean success, @Nullable String error);
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

    public void signOut() {
        auth.signOut();
        currentUser.setValue(null);
    }

    public void updateDisplayName(String name, AuthResultCallback callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onResult(false, "Not signed in");
            return;
        }
        UserProfileChangeRequest req = new UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .build();
        user.updateProfile(req).addOnCompleteListener(task -> {
            currentUser.setValue(auth.getCurrentUser());
            callback.onResult(task.isSuccessful(),
                    task.getException() != null ? task.getException().getMessage() : null);
        });
    }

    public void changeEmail(String currentPassword, String newEmail, AuthResultCallback callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null || user.getEmail() == null) {
            callback.onResult(false, "Not signed in");
            return;
        }
        AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), currentPassword);
        user.reauthenticate(credential).addOnCompleteListener(reauthTask -> {
            if (!reauthTask.isSuccessful()) {
                callback.onResult(false, reauthTask.getException() != null
                        ? reauthTask.getException().getMessage() : "Re-authentication failed");
                return;
            }
            user.verifyBeforeUpdateEmail(newEmail).addOnCompleteListener(updateTask -> {
                currentUser.setValue(auth.getCurrentUser());
                callback.onResult(updateTask.isSuccessful(),
                        updateTask.getException() != null ? updateTask.getException().getMessage() : null);
            });
        });
    }

    public void changePassword(String currentPassword, String newPassword, AuthResultCallback callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null || user.getEmail() == null) {
            callback.onResult(false, "Not signed in");
            return;
        }
        AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), currentPassword);
        user.reauthenticate(credential).addOnCompleteListener(reauthTask -> {
            if (!reauthTask.isSuccessful()) {
                callback.onResult(false, reauthTask.getException() != null
                        ? reauthTask.getException().getMessage() : "Re-authentication failed");
                return;
            }
            user.updatePassword(newPassword).addOnCompleteListener(updateTask ->
                    callback.onResult(updateTask.isSuccessful(),
                            updateTask.getException() != null ? updateTask.getException().getMessage() : null));
        });
    }
}
