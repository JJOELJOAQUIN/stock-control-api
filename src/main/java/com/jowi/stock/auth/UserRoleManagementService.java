package com.jowi.stock.auth;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class UserRoleManagementService {

    private final AppUserService appUserService;

    public UserRoleManagementService(AppUserService appUserService) {
        this.appUserService = appUserService;
    }

    @Transactional(readOnly = true)
    public List<AuthMeResponse> findAllUsers() {
        return appUserService.findAll()
            .stream()
            .map(user -> new AuthMeResponse(
                user.getFirebaseUid(),
                user.getEmail(),
                user.getRole().name(),
                user.getEnabled()
            ))
            .toList();
    }

    public void updateUserRole(String firebaseUid, Role role) {
        appUserService.updateRole(firebaseUid, role);
    }
}
