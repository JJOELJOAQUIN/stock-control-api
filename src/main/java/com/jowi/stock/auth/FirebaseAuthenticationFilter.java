package com.jowi.stock.auth;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class FirebaseAuthenticationFilter extends OncePerRequestFilter {

  private final AppUserService appUserService;

  public FirebaseAuthenticationFilter(AppUserService appUserService) {
    this.appUserService = appUserService;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {

    String authHeader = request.getHeader("Authorization");

    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      filterChain.doFilter(request, response);
      return;
    }

    String token = authHeader.substring(7);

    try {
      FirebaseToken decoded = FirebaseAuth.getInstance().verifyIdToken(token);

      String uid = decoded.getUid();
      String email = decoded.getEmail();

      // 🔥 ACÁ ESTÁ LA CLAVE
      AppUser user = appUserService.findOrCreate(uid, email);

      String role = user.getRole().name();

      var authorities = List.of(
          new SimpleGrantedAuthority("ROLE_" + role));

      UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(uid, null,
          authorities);

      SecurityContextHolder.getContext().setAuthentication(authentication);

      filterChain.doFilter(request, response);

    } catch (FirebaseAuthException e) {
      response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      response.getWriter().write("Invalid Firebase token");
    }
  }
}
