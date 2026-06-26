package com.jowi.stock.cash.services;

import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.jowi.stock.cash.dto.CashDailySplitResponse;
import com.jowi.stock.cash.dto.CashSalesTotalsResponse;
import com.jowi.stock.cash.dto.CreateCashMovementRequest;
import com.jowi.stock.cash.entities.CashMovement;
import com.jowi.stock.cash.enums.CashActor;
import com.jowi.stock.cash.enums.CashContext;
import com.jowi.stock.cash.enums.CashMovementType;
import com.jowi.stock.cash.enums.CashSource;
import com.jowi.stock.cash.enums.PaymentMethod;
import com.jowi.stock.cash.repositories.CashMovementRepository;

@Service
@Transactional
public class CashMovementService {

  private static final BigDecimal DEFAULT_CARD_RETENTION = new BigDecimal("0.30");

  private final CashMovementRepository repository;

  public CashMovementService(CashMovementRepository repository) {
    this.repository = repository;
  }

  public CashMovement create(CreateCashMovementRequest req) {
    if (req == null)
      throw new IllegalArgumentException("request is required");

    BigDecimal amount = req.amount();
    if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("amount must be > 0");
    }

    if (req.type() == null)
      throw new IllegalArgumentException("type is required");

    if (req.source() == null)
      throw new IllegalArgumentException("source is required");

    if (req.paymentMethod() == null)
      throw new IllegalArgumentException("paymentMethod is required");

    if (req.context() == null)
      throw new IllegalArgumentException("context is required");

    BigDecimal percent = resolveRetentionPercent(
        req.paymentMethod(),
        req.retentionPercent());

    BigDecimal retention = amount
        .multiply(percent)
        .setScale(2, RoundingMode.HALF_UP);

    BigDecimal net = amount
        .subtract(retention)
        .setScale(2, RoundingMode.HALF_UP);

    CashMovement m = new CashMovement();
    m.setType(req.type());
    m.setSource(req.source());
    m.setPaymentMethod(req.paymentMethod());
    m.setContext(req.context());
    m.setAmount(amount.setScale(2, RoundingMode.HALF_UP));
    m.setRetention(retention);
    m.setNetAmount(net);
    m.setComment(req.comment());
    m.setReferenceId(req.referenceId());

    if (req.source() == CashSource.PROCEDURE &&
        req.context() == CashContext.CONSULTORIO) {

      BigDecimal doctorPercent = req.doctorSharePercent();
      BigDecimal cosmetologistPercent = req.cosmetologistSharePercent();

      if (doctorPercent == null || cosmetologistPercent == null) {
        throw new IllegalArgumentException(
            "doctorSharePercent and cosmetologistSharePercent are required for procedure income");
      }

      BigDecimal total = doctorPercent.add(cosmetologistPercent);

      if (total.compareTo(BigDecimal.ONE) != 0) {
        throw new IllegalArgumentException(
            "doctorSharePercent + cosmetologistSharePercent must equal 1");
      }

      BigDecimal doctorShare = net
          .multiply(doctorPercent)
          .setScale(2, RoundingMode.HALF_UP);

      BigDecimal cosmetologistShare = net
          .subtract(doctorShare)
          .setScale(2, RoundingMode.HALF_UP);

      m.setDoctorShare(doctorShare);
      m.setCosmetologistShare(cosmetologistShare);
    }

    if (req.source() == CashSource.PRODUCT_SALE &&
        req.context() == CashContext.CONSULTORIO) {

      if (req.performedBy() == null) {
        throw new IllegalArgumentException(
            "performedBy is required for product sale in consultorio");
      }

      if (req.performedBy() == CashActor.MEDICA) {
        m.setDoctorShare(net);
        m.setCosmetologistShare(
            BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
      }

      if (req.performedBy() == CashActor.COSMETOLOGA) {
        BigDecimal cosmetologistShare = net
            .multiply(new BigDecimal("0.05"))
            .setScale(2, RoundingMode.HALF_UP);

        BigDecimal doctorShare = net
            .subtract(cosmetologistShare)
            .setScale(2, RoundingMode.HALF_UP);

        m.setDoctorShare(doctorShare);
        m.setCosmetologistShare(cosmetologistShare);
      }
    }

    return repository.save(m);
  }

  /**
   * Crea un movimiento de caja con los shares (doctor/cosmetóloga) dados en
   * MONTO, no en porcentaje. Útil cuando el reparto es fijo (ej: la
   * cosmetóloga cobra $40.000 netos garantizados, sin importar la retención).
   *
   * La retención y el neto se calculan igual que en {@link #create}. Los
   * shares se aplican sobre el neto y deben sumar exactamente el neto.
   */
  public CashMovement createWithFixedShares(
      CashMovementType type,
      CashSource source,
      PaymentMethod paymentMethod,
      CashContext context,
      BigDecimal amount,
      BigDecimal retentionPercentOverride,
      String comment,
      java.util.UUID referenceId,
      BigDecimal doctorShareAmount,
      BigDecimal cosmetologistShareAmount) {

    if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("amount must be > 0");
    }
    if (type == null)
      throw new IllegalArgumentException("type is required");
    if (source == null)
      throw new IllegalArgumentException("source is required");
    if (paymentMethod == null)
      throw new IllegalArgumentException("paymentMethod is required");
    if (context == null)
      throw new IllegalArgumentException("context is required");
    if (doctorShareAmount == null || cosmetologistShareAmount == null) {
      throw new IllegalArgumentException("share amounts are required");
    }
    if (doctorShareAmount.signum() < 0 || cosmetologistShareAmount.signum() < 0) {
      throw new IllegalArgumentException("share amounts must be >= 0");
    }

    BigDecimal percent = resolveRetentionPercent(paymentMethod, retentionPercentOverride);

    BigDecimal grossAmount = amount.setScale(2, RoundingMode.HALF_UP);
    BigDecimal retention = grossAmount
        .multiply(percent)
        .setScale(2, RoundingMode.HALF_UP);
    BigDecimal net = grossAmount.subtract(retention).setScale(2, RoundingMode.HALF_UP);

    BigDecimal doctorShare = doctorShareAmount.setScale(2, RoundingMode.HALF_UP);
    BigDecimal cosmetologistShare = cosmetologistShareAmount.setScale(2, RoundingMode.HALF_UP);

    // Los shares en monto deben repartir exactamente el neto.
    if (doctorShare.add(cosmetologistShare).compareTo(net) != 0) {
      throw new IllegalArgumentException(
          "doctorShareAmount + cosmetologistShareAmount must equal the net amount");
    }

    CashMovement m = new CashMovement();
    m.setType(type);
    m.setSource(source);
    m.setPaymentMethod(paymentMethod);
    m.setContext(context);
    m.setAmount(grossAmount);
    m.setRetention(retention);
    m.setNetAmount(net);
    m.setComment(comment);
    m.setReferenceId(referenceId);
    m.setDoctorShare(doctorShare);
    m.setCosmetologistShare(cosmetologistShare);

    return repository.save(m);
  }

  public Page<CashMovement> list(Pageable pageable) {
    return repository.findAll(pageable);
  }

  public Page<CashMovement> listByContext(CashContext context, Pageable pageable) {
    if (context == null)
      throw new IllegalArgumentException("context is required");

    return repository.findByContext(context, pageable);
  }

  public CashDailySplitResponse dailySplit(
      CashContext context,
      java.time.LocalDate date) {

    if (context == null) {
      throw new IllegalArgumentException("context is required");
    }

    if (date == null) {
      date = java.time.LocalDate.now();
    }

    java.time.ZoneId zone = java.time.ZoneId.systemDefault();

    java.time.Instant from = date.atStartOfDay(zone).toInstant();
    java.time.Instant to = date.plusDays(1).atStartOfDay(zone).toInstant();

    Object[] result = repository.cashSplitByContextAndDateRange(context, from, to);

    Object[] row = (Object[]) result[0];

    return new CashDailySplitResponse(
        date,
        context,
        (BigDecimal) row[0],
        (BigDecimal) row[1],
        (BigDecimal) row[2]);
  }

  public CashSalesTotalsResponse salesTotals(CashContext context) {
    if (context == null) {
      throw new IllegalArgumentException("context is required");
    }

    Object[] result = repository.salesTotalsByContext(context);
    Object[] row = (Object[]) result[0];

    return new CashSalesTotalsResponse(
        context,
        (BigDecimal) row[0],
        (BigDecimal) row[1]);
  }

  private BigDecimal resolveRetentionPercent(
      PaymentMethod method,
      BigDecimal override) {

    if (override != null) {
      if (override.compareTo(BigDecimal.ZERO) < 0 ||
          override.compareTo(BigDecimal.ONE) > 0) {
        throw new IllegalArgumentException(
            "retentionPercent must be between 0 and 1");
      }

      return override;
    }

    return (method == PaymentMethod.CREDIT || method == PaymentMethod.DEBIT)
        ? DEFAULT_CARD_RETENTION
        : BigDecimal.ZERO;
  }
}