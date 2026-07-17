package com.jowi.stock.cash.services;

import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import com.jowi.stock.cash.dto.CashDailySplitResponse;
import com.jowi.stock.cash.dto.CashCosmetologistSplitResponse;
import com.jowi.stock.cash.dto.CashSalesTotalsResponse;
import com.jowi.stock.cash.dto.CreateCashMovementRequest;
import com.jowi.stock.cash.entities.CashMovement;
import com.jowi.stock.cash.enums.CashActor;
import com.jowi.stock.cash.enums.CashContext;
import com.jowi.stock.cash.enums.CashMovementType;
import com.jowi.stock.cash.enums.CashSource;
import com.jowi.stock.cash.enums.PaymentMethod;
import com.jowi.stock.cash.enums.SplitPreset;
import com.jowi.stock.cash.repositories.CashMovementRepository;
import com.jowi.stock.cash.specifications.CashMovementSpecifications;
import com.jowi.stock.cash.dto.CashItemSpec;
import com.jowi.stock.cash.dto.CombinedItemLine;
import com.jowi.stock.cash.entities.CashMovementItem;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class CashMovementService {

  private static final BigDecimal DEFAULT_CARD_RETENTION = new BigDecimal("0.30");
  private static final BigDecimal COSMETOLOGIST_PRODUCT_PERCENT = new BigDecimal("0.05");
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
    m.setDetail(req.detail());
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

    // ── Fase 2 (espejo): venta de producto y procedimiento generan un ítem
    // de detalle con el MISMO split que ya calculó la cabecera. Así las
    // queries *FromItems cuentan estos movimientos y la rama legacy sólo
    // queda para datos históricos previos a esta fase. No hay recálculo:
    // los shares se copian de la cabecera.
    if (req.source() == CashSource.PRODUCT_SALE || req.source() == CashSource.PROCEDURE) {
      CashMovementItem mirror = new CashMovementItem();
      mirror.setKind(
          req.source() == CashSource.PRODUCT_SALE
              ? com.jowi.stock.cash.enums.CashMovementItemKind.PRODUCT
              : com.jowi.stock.cash.enums.CashMovementItemKind.PROCEDURE);

      // En PRODUCT_SALE el referenceId es el productId; en PROCEDURE no aplica.
      if (req.source() == CashSource.PRODUCT_SALE) {
        mirror.setProductId(req.referenceId());
      } else {
        // Código de procedimiento: usamos el detail como identificador legible.
        // (No hay un code estructurado en este request legacy.)
        mirror.setProcedureCode(null);
      }

      String description = (req.detail() != null && !req.detail().isBlank())
          ? req.detail()
          : (req.comment() != null && !req.comment().isBlank()
              ? req.comment()
              : (req.source() == CashSource.PRODUCT_SALE ? "Producto" : "Procedimiento"));

      mirror.setDescription(description.length() > 200
          ? description.substring(0, 197) + "..."
          : description);

      // quantity=1 / unitAmount=amount: el conteo de unidades no viaja en este
      // request legacy. Split y totales quedan exactos igual (se basan en montos).
      mirror.setQuantity(1);
      mirror.setUnitAmount(m.getAmount());
      mirror.setSubtotal(m.getAmount());

      // Copiamos el split ya resuelto en la cabecera (null en LOCAL).
      mirror.setDoctorShare(m.getDoctorShare());
      mirror.setCosmetologistShare(m.getCosmetologistShare());

      m.addItem(mirror);
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
   *
   * @param itemSpec datos del ítem espejo; null = no generar ítem.
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
      BigDecimal cosmetologistShareAmount,
      CashItemSpec itemSpec) {

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

    // Item espejo. Antes este camino no generaba ninguno, asi que los pagos de
    // tratamiento quedaban como un CashMovement sin filas en
    // cash_movement_items, y solo los levantaba la rama legacy de las queries
    // -- la que filtra por cosmetologistShare > 0. Por eso un pago donde la
    // cosmetologa cobra 0 desaparecia de su card aunque el trabajo fuera de
    // ella. El item con performed_by es lo que lo arregla.
    if (itemSpec != null) {
      CashMovementItem mirror = new CashMovementItem();
      mirror.setKind(itemSpec.kind());
      mirror.setProductId(itemSpec.productId());
      mirror.setProcedureCode(itemSpec.procedureCode());
      mirror.setPerformedBy(itemSpec.performedBy());
      mirror.setSplitPreset(itemSpec.splitPreset());

      String description = (itemSpec.description() != null && !itemSpec.description().isBlank())
          ? itemSpec.description()
          : (comment != null && !comment.isBlank() ? comment : "Pago");
      mirror.setDescription(description.length() > 200
          ? description.substring(0, 197) + "..."
          : description);

      mirror.setQuantity(1);
      mirror.setUnitAmount(grossAmount);
      mirror.setSubtotal(grossAmount);

      // Los shares se copian de la cabecera: no hay recalculo posible, el
      // movimiento es de un solo item.
      mirror.setDoctorShare(doctorShare);
      mirror.setCosmetologistShare(cosmetologistShare);

      m.addItem(mirror);
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

  /**
   * Búsqueda paginada con filtros opcionales (todos pueden ser null).
   * Los filtros nulos se ignoran (ver {@link CashMovementSpecifications}).
   * El rango de fechas es inclusivo en {@code dateFrom} y en {@code dateTo}
   * (se toma hasta el final de ese día).
   */
  public Page<CashMovement> search(
      CashContext context,
      CashMovementType type,
      CashSource source,
      java.time.LocalDate dateFrom,
      java.time.LocalDate dateTo,
      String q,
      Pageable pageable) {

    java.time.ZoneId zone = java.time.ZoneId.systemDefault();

    java.time.Instant from = dateFrom == null
        ? null
        : dateFrom.atStartOfDay(zone).toInstant();

    java.time.Instant to = dateTo == null
        ? null
        : dateTo.plusDays(1).atStartOfDay(zone).toInstant();

    Specification<CashMovement> spec = Specification
        .where(CashMovementSpecifications.hasContext(context))
        .and(CashMovementSpecifications.hasType(type))
        .and(CashMovementSpecifications.hasSource(source))
        .and(CashMovementSpecifications.createdFrom(from))
        .and(CashMovementSpecifications.createdBefore(to))
        .and(CashMovementSpecifications.textContains(q));

    return repository.findAll(spec, pageable);
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

  /**
   * Calcula el neto (amount - retención) según el método de pago.
   * Única fuente de verdad de la retención: la usan create, createWithFixedShares
   * y servicios externos (ej. TreatmentService) para no duplicar el %.
   */
  public BigDecimal computeNet(BigDecimal amount, PaymentMethod method) {
    if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0)
      throw new IllegalArgumentException("amount must be > 0");

    BigDecimal percent = resolveRetentionPercent(method, null);
    BigDecimal gross = amount.setScale(2, RoundingMode.HALF_UP);
    BigDecimal retention = gross.multiply(percent).setScale(2, RoundingMode.HALF_UP);
    return gross.subtract(retention).setScale(2, RoundingMode.HALF_UP);
  }

  public CashCosmetologistSplitResponse cosmetologistDailySplit(
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

    Object[] items = (Object[]) repository
        .cosmetologistProductionSplitFromItems(context, from, to)[0];
    Object[] legacy = (Object[]) repository
        .cosmetologistProductionSplitLegacy(context, from, to)[0];

    return new CashCosmetologistSplitResponse(
        date,
        context,
        add(items[0], legacy[0]), // procedimiento - cosmetóloga
        add(items[1], legacy[1]), // procedimiento - médica
        add(items[2], legacy[2]), // producto - cosmetóloga
        add(items[3], legacy[3])); // producto - médica
  }

  public CashSalesTotalsResponse salesTotals(CashContext context) {
    if (context == null) {
      throw new IllegalArgumentException("context is required");
    }

    Object[] items = (Object[]) repository.salesTotalsFromItems(context)[0];
    Object[] legacy = (Object[]) repository.salesTotalsLegacy(context)[0];

    return new CashSalesTotalsResponse(
        context,
        add(items[0], legacy[0]), // total productos
        add(items[1], legacy[1])); // total procedimientos
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

  /**
   * Persiste una venta combinada: una cabecera CashMovement (IN, COMBINED_SALE)
   * con N ítems. Calcula retención sobre el total, reparte el neto entre los
   * ítems (proporcional al subtotal, el último absorbe el resto para evitar
   * drift de redondeo) y resuelve el split por ítem sobre su neto.
   *
   * El split (doctor/cosmetóloga) sólo se calcula en CONSULTORIO; en LOCAL los
   * ítems se guardan sin shares.
   */
  public CashMovement createCombined(
      CashContext context,
      PaymentMethod paymentMethod,
      String comment,
      BigDecimal expectedTotal,
      List<CombinedItemLine> lines) {

    if (lines == null || lines.isEmpty()) {
      throw new IllegalArgumentException("La venta combinada requiere al menos un ítem");
    }
    if (paymentMethod == null)
      throw new IllegalArgumentException("paymentMethod is required");
    if (context == null)
      throw new IllegalArgumentException("context is required");

    BigDecimal computedTotal = BigDecimal.ZERO;
    for (CombinedItemLine line : lines) {
      if (line.subtotal() == null || line.subtotal().compareTo(BigDecimal.ZERO) <= 0) {
        throw new IllegalArgumentException("Cada ítem debe tener subtotal > 0");
      }
      computedTotal = computedTotal.add(line.subtotal());
    }
    computedTotal = computedTotal.setScale(2, RoundingMode.HALF_UP);

    if (expectedTotal != null) {
      BigDecimal diff = computedTotal.subtract(expectedTotal).abs();
      if (diff.compareTo(new BigDecimal("0.01")) > 0) {
        throw new IllegalArgumentException(
            "El total no coincide con la suma de los ítems");
      }
    }

    BigDecimal percent = resolveRetentionPercent(paymentMethod, null);
    BigDecimal retention = computedTotal.multiply(percent).setScale(2, RoundingMode.HALF_UP);
    BigDecimal net = computedTotal.subtract(retention).setScale(2, RoundingMode.HALF_UP);

    boolean consultorio = context == CashContext.CONSULTORIO;

    CashMovement m = new CashMovement();
    m.setType(CashMovementType.IN);
    m.setSource(CashSource.COMBINED_SALE);
    m.setPaymentMethod(paymentMethod);
    m.setContext(context);
    m.setAmount(computedTotal);
    m.setRetention(retention);
    m.setNetAmount(net);
    m.setComment(comment);
    m.setDetail(buildCombinedDetail(lines));

    BigDecimal headerDoctor = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    BigDecimal headerCosmo = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    BigDecimal allocatedNet = BigDecimal.ZERO;

    for (int i = 0; i < lines.size(); i++) {
      CombinedItemLine line = lines.get(i);

      BigDecimal itemNet;
      if (i < lines.size() - 1) {
        itemNet = line.subtotal()
            .divide(computedTotal, 10, RoundingMode.HALF_UP)
            .multiply(net)
            .setScale(2, RoundingMode.HALF_UP);
        allocatedNet = allocatedNet.add(itemNet);
      } else {
        // El último ítem absorbe el remanente para que la suma cierre exacta.
        itemNet = net.subtract(allocatedNet).setScale(2, RoundingMode.HALF_UP);
      }

      CashMovementItem item = new CashMovementItem();
      item.setKind(line.kind());
      item.setProductId(line.productId());
      item.setProcedureCode(line.procedureCode());
      item.setDescription(line.description());
      item.setQuantity(line.quantity());
      item.setUnitAmount(line.unitAmount().setScale(2, RoundingMode.HALF_UP));
      item.setSubtotal(line.subtotal().setScale(2, RoundingMode.HALF_UP));
      // Autoria del trabajo. Se guarda siempre, no solo en consultorio: es
      // un hecho del negocio, no un detalle del calculo de shares. Antes
      // este dato se usaba para calcular los montos y se perdia, y habia
      // que inferir quien hizo el trabajo a partir de cuanto cobro.
      item.setPerformedBy(line.performedBy());

      if (consultorio) {
        BigDecimal[] shares = resolveItemShares(line, itemNet);
        item.setDoctorShare(shares[0]);
        item.setCosmetologistShare(shares[1]);
        headerDoctor = headerDoctor.add(shares[0]);
        headerCosmo = headerCosmo.add(shares[1]);
      }

      m.addItem(item);
    }

    if (consultorio) {
      m.setDoctorShare(headerDoctor);
      m.setCosmetologistShare(headerCosmo);
    }

    return repository.save(m);
  }

  /**
   * Resuelve [doctorShare, cosmetologistShare] en monto sobre el neto del ítem.
   */
  private BigDecimal[] resolveItemShares(CombinedItemLine line, BigDecimal itemNet) {
    switch (line.kind()) {
      case PRODUCT -> {
        if (line.performedBy() == null) {
          throw new IllegalArgumentException(
              "performedBy es obligatorio para la venta de producto en consultorio");
        }
        if (line.performedBy() == CashActor.MEDICA) {
          return new BigDecimal[] { itemNet, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) };
        }
        BigDecimal cosmo = itemNet
            .multiply(COSMETOLOGIST_PRODUCT_PERCENT)
            .setScale(2, RoundingMode.HALF_UP);
        BigDecimal doctor = itemNet.subtract(cosmo).setScale(2, RoundingMode.HALF_UP);
        return new BigDecimal[] { doctor, cosmo };
      }
      case PROCEDURE -> {
        BigDecimal dp = line.doctorSharePercent();
        BigDecimal cp = line.cosmetologistSharePercent();
        if (dp == null || cp == null) {
          throw new IllegalArgumentException(
              "Los porcentajes del procedimiento son obligatorios");
        }
        if (dp.add(cp).compareTo(BigDecimal.ONE) != 0) {
          throw new IllegalArgumentException(
              "doctorSharePercent + cosmetologistSharePercent debe ser 1");
        }
        BigDecimal doctor = itemNet.multiply(dp).setScale(2, RoundingMode.HALF_UP);
        BigDecimal cosmo = itemNet.subtract(doctor).setScale(2, RoundingMode.HALF_UP);
        return new BigDecimal[] { doctor, cosmo };
      }
      default -> throw new IllegalArgumentException("kind desconocido");
    }
  }

  /** Resumen legible para la columna detail (join de descripciones, cap 200). */
  private String buildCombinedDetail(List<CombinedItemLine> lines) {
    String joined = lines.stream()
        .map(l -> l.quantity() > 1 ? l.description() + " ×" + l.quantity() : l.description())
        .collect(Collectors.joining(", "));
    return joined.length() > 200 ? joined.substring(0, 197) + "..." : joined;
  }

  /** Suma dos agregados BigDecimal que vienen como Object desde las queries. */
  private BigDecimal add(Object a, Object b) {
    BigDecimal x = a == null ? BigDecimal.ZERO : (BigDecimal) a;
    BigDecimal y = b == null ? BigDecimal.ZERO : (BigDecimal) b;
    return x.add(y);
  }
}