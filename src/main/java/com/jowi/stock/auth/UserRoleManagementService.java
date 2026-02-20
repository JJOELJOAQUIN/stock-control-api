package com.jowi.stock.auth;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserRoleManagementService {

    private final AppUserService appUserService;
    private final FirebaseUserRoleService firebaseRoleService;

    public UserRoleManagementService(
        AppUserService appUserService,
        FirebaseUserRoleService firebaseRoleService
    ) {
        this.appUserService = appUserService;
        this.firebaseRoleService = firebaseRoleService;
    }

    public void updateUserRole(String firebaseUid, Role role) {

        // 1️⃣ Persistir en BD (fuente de verdad)
        appUserService.updateRole(firebaseUid, role);

        // 2️⃣ Sincronizar Firebase Custom Claim
        firebaseRoleService.setRole(firebaseUid, role);
    }
}
