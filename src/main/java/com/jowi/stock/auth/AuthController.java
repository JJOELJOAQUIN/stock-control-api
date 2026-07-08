package com.jowi.stock.auth;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

  private final AppUserRepository repository;

  public AuthController(AppUserRepository repository) {
    this.repository = repository;
  }

  @GetMapping("/me")
  public ResponseEntity<AuthMeResponse> me(Authentication authentication) {
    System.out.println(">>> /me authentication = " + authentication);
    System.out.println(">>> /me getName() = " + (authentication == null ? "NULL" : authentication.getName()));

    String firebaseUid = authentication.getName();

    AppUser user = repository.findByFirebaseUid(firebaseUid)
        .orElseThrow(() -> new IllegalArgumentException("User not found"));

    return ResponseEntity.ok(
        new AuthMeResponse(
            user.getFirebaseUid(),
            user.getEmail(),
            user.getRole().name(),
            user.getEnabled()));
  }
}