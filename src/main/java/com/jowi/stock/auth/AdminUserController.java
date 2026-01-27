package com.jowi.stock.auth;

import com.jowi.stock.auth.SetUserRoleRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final UserRoleService roleService;

    public AdminUserController(UserRoleService roleService) {
        this.roleService = roleService;
    }

    @PostMapping("/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> setRole(
            @Valid @RequestBody SetUserRoleRequest request) {

        roleService.setUserRole(
            request.uid(),
            request.role()
        );

        return ResponseEntity.noContent().build();
    }
}