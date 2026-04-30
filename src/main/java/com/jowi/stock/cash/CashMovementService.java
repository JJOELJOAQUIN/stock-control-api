package com.jowi.stock.cash;

import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

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