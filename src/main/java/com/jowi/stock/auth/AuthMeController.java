package com.jowi.stock.auth;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthMeController {

    private final AppUserRepository repository;

    public AuthMeController(AppUserRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/me")
    public AppUser me(Authentication authentication) {
        String firebaseUid = authentication.getName();

        return repository.findByFirebaseUid(firebaseUid)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }
}