package com.jowi.stock.auth;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final UserRoleManagementService roleService;

    public AdminUserController(UserRoleManagementService roleService) {
        this.roleService = roleService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{firebaseUid}/role")
    public ResponseEntity<Void> updateRole(
        @PathVariable String firebaseUid,
        @Valid @RequestBody UpdateUserRoleRequest request
    ) {
        roleService.updateUserRole(firebaseUid, request.role());
        return ResponseEntity.noContent().build();
    }
}
