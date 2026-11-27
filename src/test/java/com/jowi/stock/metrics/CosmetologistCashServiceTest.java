package com.jowi.stock.metrics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jowi.stock.auth.CurrentUserService;
import com.jowi.stock.common.BusinessZone;
import com.jowi.stock.metrics.dto.CreateCosmetologistCashRequest;
import com.jowi.stock.metrics.entities.CosmetologistCashDeposit;
import com.jowi.stock.metrics.repositories.CosmetologistCashDepositRepository;
import com.jowi.stock.metrics.services.CosmetologistCashService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CosmetologistCashServiceTest {

  private CosmetologistCashDepositRepository repository;
  private CurrentUserService currentUser;
  private CosmetologistCashService service;

  @BeforeEach
  void setUp() {
    repository = mock(CosmetologistCashDepositRepository.class);
    currentUser = mock(CurrentUserService.class);
    when(currentUser.currentUserLabel()).thenReturn("cosmetologia@example.com");
    when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    service = new CosmetologistCashService(repository, currentUser);
  }

  private static CosmetologistCashDeposit deposit(String amount, String by) {
    CosmetologistCashDeposit d = new CosmetologistCashDeposit();
    d.setAmount(new BigDecimal(amount));
    d.setDepositDate(LocalDate.of(2026, 11, 20));
    d.setCreatedBy(by);
    return d;
  }

  @Test
  void sinFechaQuedaHoyYGuardaQuienLoCargo() {
    var e = service.create(new CreateCosmetologistCashRequest(new BigDecimal("50000"), null, "  extra  "));
    assertEquals(BusinessZone.today(), e.date());
    assertEquals(new BigDecimal("50000.00"), e.amount());
    assertEquals("extra", e.note());
    assertEquals("cosmetologia@example.com", e.createdBy());
  }

  @Test
  void montoCeroOFechaFuturaSeRechazan() {
    assertThrows(IllegalArgumentException.class,
        () -> service.create(new CreateCosmetologistCashRequest(BigDecimal.ZERO, null, null)));
    assertThrows(IllegalArgumentException.class,
        () -> service.create(new CreateCosmetologistCashRequest(
            BigDecimal.TEN, BusinessZone.today().plusDays(1), null)));
  }

  @Test
  void totalDelMesSumaLasEntregas() {
    when(repository.findByDepositDateBetweenOrderByDepositDateAscCreatedAtAsc(
        LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30)))
        .thenReturn(List.of(deposit("30000", "a"), deposit("20000.50", "b")));
    var r = service.monthly(2026, 11);
    assertEquals(new BigDecimal("50000.50"), r.total());
    assertEquals(2, r.entries().size());
  }

  @Test
  void laCosmetologaSoloBorraLoSuyoYLaMedicaTodo() {
    UUID id = UUID.randomUUID();
    CosmetologistCashDeposit ajeno = deposit("1000", "drapilar@example.com");
    when(repository.findById(id)).thenReturn(Optional.of(ajeno));

    when(currentUser.isAdmin()).thenReturn(false);
    assertThrows(IllegalStateException.class, () -> service.delete(id));
    verify(repository, never()).delete(any());

    when(currentUser.isAdmin()).thenReturn(true);
    service.delete(id);
    verify(repository).delete(ajeno);
  }
}
