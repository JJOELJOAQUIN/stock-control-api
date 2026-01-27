package com.jowi.stock.auth;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class FirebaseUserRoleService {

    public void setRole(String firebaseUid, Role role) {
        try {
            FirebaseAuth.getInstance().setCustomUserClaims(
                firebaseUid,
                Map.of("role", role.name())
            );
        } catch (FirebaseAuthException e) {
            throw new IllegalStateException("Failed to update Firebase custom claims", e);
        }
    }
}
