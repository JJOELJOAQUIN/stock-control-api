package com.jowi.stock.auth;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AppUserService {

    private final AppUserRepository repository;
    private final FirebaseUserRoleService firebaseRoleService;

    public AppUserService(
            AppUserRepository repository,
            FirebaseUserRoleService firebaseRoleService) {
        this.repository = repository;
        this.firebaseRoleService = firebaseRoleService;
    }

    public AppUser findOrCreate(String firebaseUid, String email) {

        return repository.findByFirebaseUid(firebaseUid)
                .orElseGet(() -> {
                    Role defaultRole = Role.PENDING;

                    AppUser user = new AppUser(firebaseUid, email, defaultRole);
                    AppUser saved = repository.save(user);

                    // 🔥 Custom Claim inicial (para que el front vea PENDING)
                    firebaseRoleService.setRole(firebaseUid, defaultRole);

                    return saved;
                });
    }

    public void updateRole(String firebaseUid, Role newRole) {
        AppUser user = repository.findByFirebaseUid(firebaseUid)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setRole(newRole);
        // Nota: por @Transactional, se persiste al commit
    }
}