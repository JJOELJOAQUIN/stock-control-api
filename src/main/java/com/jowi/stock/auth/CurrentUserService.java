package com.jowi.stock.auth;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Único punto donde los services le preguntan al contexto de seguridad quién
 * está operando. Hasta ahora ningún service conocía al usuario: el auth vivía
 * sólo en los filtros.
 *
 * Lo usan el aislamiento de datos de la caja (la cosmetóloga sólo ve sus
 * movimientos) y, próximamente, la anulación ("quién anuló").
 */
@Service
public class CurrentUserService {

  private final AppUserRepository repository;

  public CurrentUserService(AppUserRepository repository) {
    this.repository = repository;
  }

  /**
   * True si el usuario autenticado tiene rol COSMETOLOGA. Se decide acá y no
   * en el front porque el front puede pedir lo que quiera: la restricción de
   * qué datos ve cada rol tiene que vivir del lado que los entrega.
   */
  public boolean isCosmetologist() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();

    if (auth == null) {
      return false;
    }

    return auth.getAuthorities().stream()
        .anyMatch(a -> "ROLE_COSMETOLOGA".equals(a.getAuthority()));
  }

  /**
   * Email del usuario autenticado, o el principal crudo si no hay AppUser
   * (caso del filtro dev, donde el principal ya es un email). Nunca null:
   * preferimos un registro feo a una acción sin autor.
   */
  public String currentUserLabel() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();

    if (auth == null || auth.getPrincipal() == null) {
      return "desconocido";
    }

    String principal = String.valueOf(auth.getPrincipal());

    return repository.findByFirebaseUid(principal)
        .map(AppUser::getEmail)
        .orElse(principal);
  }
}