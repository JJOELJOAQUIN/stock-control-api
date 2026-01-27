package com.jowi.stock.auth;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AppUserService {

    private final AppUserRepository repository;

    public AppUserService(AppUserRepository repository) {
        this.repository = repository;
    }

    public AppUser findOrCreate(String firebaseUid, String email) {

        return repository.findByFirebaseUid(firebaseUid)
            .orElseGet(() -> {
                // ⚠️ Regla de negocio inicial
                Role defaultRole = Role.USER;

                AppUser user = new AppUser(firebaseUid, email, defaultRole);
                return repository.save(user);
            });
    }

    public void updateRole(String firebaseUid, Role newRole) {
        AppUser user = repository.findByFirebaseUid(firebaseUid)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setRole(newRole);
    }
}
