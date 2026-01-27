package com.jowi.stock.auth;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class UserRoleService {

    public void setUserRole(String uid, Role role) {
        try {
            FirebaseAuth.getInstance()
                .setCustomUserClaims(uid, Map.of(
                    "role", role.name()
                ));
        } catch (FirebaseAuthException e) {
            throw new IllegalStateException(
                "Error setting role for user " + uid, e
            );
        }
    }
}