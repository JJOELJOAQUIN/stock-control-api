package com.jowi.stock.auth;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

  @GetMapping("/me")
  public ResponseEntity<AuthMeResponse> me(Authentication authentication) {

    String uid = (String) authentication.getPrincipal();
    String role = authentication.getAuthorities()
        .stream()
        .findFirst()
        .map(a -> a.getAuthority().replace("ROLE_", ""))
        .orElse("USER");

    return ResponseEntity.ok(
        new AuthMeResponse(uid, role)
    );
  }
}
