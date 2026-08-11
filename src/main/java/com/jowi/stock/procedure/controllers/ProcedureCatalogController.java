package com.jowi.stock.procedure.controllers;

import com.jowi.stock.procedure.dto.ProcedureCatalogRequest;
import com.jowi.stock.procedure.dto.ProcedureCatalogResponse;
import com.jowi.stock.procedure.services.ProcedureCatalogService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Catálogo de tratamientos.
 * - Lectura: ADMIN + COSMETOLOGA (Gise necesita la lista para los selectores
 *   de caja y los modales de referencia).
 * - Alta / edición / baja: sólo ADMIN.
 *
 * Se asegura por método (@EnableMethodSecurity ya está prendido); no hace
 * falta tocar la cadena de seguridad.
 */
@RestController
@RequestMapping("/api/procedure-catalog")
public class ProcedureCatalogController {

  private final ProcedureCatalogService service;

  public ProcedureCatalogController(ProcedureCatalogService service) {
    this.service = service;
  }

  @GetMapping
  @PreAuthorize("hasAnyRole('ADMIN', 'COSMETOLOGA')")
  public ResponseEntity<List<ProcedureCatalogResponse>> list(
      @RequestParam(name = "includeInactive", defaultValue = "false") boolean includeInactive) {
    return ResponseEntity.ok(service.list(includeInactive));
  }

  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ProcedureCatalogResponse> create(
      @Valid @RequestBody ProcedureCatalogRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ProcedureCatalogResponse> update(
      @PathVariable UUID id,
      @Valid @RequestBody ProcedureCatalogRequest request) {
    return ResponseEntity.ok(service.update(id, request));
  }

  /** Baja lógica (active=false). Reactivar: PATCH con active=true. */
  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ProcedureCatalogResponse> deactivate(@PathVariable UUID id) {
    return ResponseEntity.ok(service.setActive(id, false));
  }

  @PatchMapping("/{id}/active")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ProcedureCatalogResponse> setActive(
      @PathVariable UUID id,
      @RequestParam boolean active) {
    return ResponseEntity.ok(service.setActive(id, active));
  }
}