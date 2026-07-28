package com.jowi.stock.business.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumSet;
import java.util.List;

import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import com.jowi.stock.batch.services.ProductBatchService;
import com.jowi.stock.business.dto.InternalConsumptionRequest;
import com.jowi.stock.business.dto.DermatoProcedureRequest;
import com.jowi.stock.business.dto.PurchaseItemRequest;
import com.jowi.stock.cash.dto.CreateCashMovementRequest;
import com.jowi.stock.cash.enums.CashActor;
import com.jowi.stock.cash.enums.CashContext;
import com.jowi.stock.cash.enums.CashMovementType;
import com.jowi.stock.cash.enums.CashSource;
import com.jowi.stock.cash.enums.PaymentMethod;
import com.jowi.stock.cash.entities.CashMovement;
import com.jowi.stock.cash.services.CashMovementService;
import com.jowi.stock.movement.entities.StockMovement;
import com.jowi.stock.movement.services.StockMovementService;
import com.jowi.stock.movement.enums.StockMovementReason;
import com.jowi.stock.product.services.interfaces.ProductService;
import com.jowi.stock.stock.enums.StockContext;
import com.jowi.stock.stock.services.StockService;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import com.jowi.stock.business.dto.CombinedSaleItemRequest;
import com.jowi.stock.business.dto.CombinedSaleRequest;
import com.jowi.stock.cash.dto.CombinedItemLine;
import com.jowi.stock.cash.enums.CashMovementItemKind;

@Service
@Transactional
public class BusinessOperationService {

  /**
   * Motivos válidos para un consumo interno. VENTA y COMPRA_PROVEEDOR quedan
   * excluidos: esos flujos tienen sus propios endpoints e impactan caja.
   */
  private static final Set<StockMovementReason> INTERNAL_CONSUMPTION_REASONS =
      EnumSet.of(
          StockMovementReason.USO_PERSONAL,
          StockMovementReason.USO_CAMILLA,
          StockMovementReason.TRASLADO,
          StockMovementReason.MUESTRA,
          StockMovementReason.REGALO,
          StockMovementReason.PEDIDO_ESPECIAL,
          StockMovementReason.VENCIMIENTO,
          StockMovementReason.OTRO);

  /**
   * Reparto autoritativo de un procedimiento: quién lo realiza y qué % le toca
   * a cada una. El backend NO confía en los porcentajes que manda el cliente
   * para procedimientos; los resuelve acá por código. Un bug de front (como el
   * default 60/40 que le metía 40% de cada CONSULTA a la cosmetóloga) ya no
   * puede escribir plata mal: el server pisa lo que venga.
   *
   * @param performer     quién hace el trabajo (define la card de la cosmetóloga)
   * @param doctorPercent % neto para la médica
   * @param cosmoPercent  % neto para la cosmetóloga (doctor + cosmo == 1)
   */
  private record ProcedureSplit(
      CashActor performer, BigDecimal doctorPercent, BigDecimal cosmoPercent) {}

  private static final BigDecimal COSMO_PROCEDURE_PERCENT = new BigDecimal("0.70");
  private static final BigDecimal COSMO_PROCEDURE_DOCTOR_PERCENT = new BigDecimal("0.30");

  private static final ProcedureSplit MEDICA_SPLIT =
      new ProcedureSplit(CashActor.MEDICA, BigDecimal.ONE, BigDecimal.ZERO);
  private static final ProcedureSplit COSMO_SPLIT =
      new ProcedureSplit(
          CashActor.COSMETOLOGA, COSMO_PROCEDURE_DOCTOR_PERCENT, COSMO_PROCEDURE_PERCENT);

  /**
   * Procedimientos de cosmetología (70% cosmetóloga / 30% médica). Espejo del
   * COSMETOLOGIA_PROCEDURES del front (cash.types.ts). Todo código que NO esté
   * acá se trata como procedimiento médico: 100% médica, performer MEDICA.
   *
   * El default hacia médica es deliberado: si algún día se agrega una
   * cosmetología nueva y se olvida sumarla acá, el error es "a la cosmetóloga
   * le falta plata" (visible, se reclama) y no "la cosmetóloga cobra de más"
   * (invisible, es justo el bug que estamos arreglando).
   */
  private static final Set<String> COSMETOLOGIA_PROCEDURE_CODES = Set.of(
      "DERMAPEN_DERMAPLANING",
      "DERMAPEN_DERMAPLANING_LIMPIEZA_PREMIUM",
      "LIMPIEZA_SIMPLE",
      "LIMPIEZA_PREMIUM",
      "LIMPIEZA_PREMIUM_HIDRATACION",
      "LIMPIEZA_PREMIUM_DERMAPLANING",
      "DERMAPLANING",
      "DERMAPEN",
      "EXOSOMAS",
      "HYDRA",
      "ESPALDA",
      "PEELING_COSMETOLOGICO",
      "FRAX_FACE_COSMETOLOGICO",
      "FRAX_FACE_COSMETOLOGICO_EXOSOMAS");

  private static ProcedureSplit resolveProcedureSplit(String procedureCode) {
    if (procedureCode != null
        && COSMETOLOGIA_PROCEDURE_CODES.contains(procedureCode.trim().toUpperCase())) {
      return COSMO_SPLIT;
    }
    return MEDICA_SPLIT;
  }

  private final StockService stockService;
  private final CashMovementService cashService;
  private final ProductService productService;
  private final ProductBatchService batchService;
  private final StockMovementService stockMovementService;

  public BusinessOperationService(
      StockService stockService,
      CashMovementService cashService,
      ProductService productService,
      ProductBatchService batchService,
      StockMovementService stockMovementService) {
    this.stockService = stockService;
    this.cashService = cashService;
    this.productService = productService;
    this.batchService = batchService;
    this.stockMovementService = stockMovementService;
  }

  public void sellProduct(
      UUID productId,
      int quantity,
      BigDecimal amount,
      PaymentMethod paymentMethod,
      CashContext context,
      String comment) {

    var product = productService.getById(productId);

    // El stock se descuenta ANTES de tocar la caja: si no alcanza, no
    // queremos haber registrado plata. Por eso el link al movimiento de
    // caja se completa despues, ya dentro de la misma transaccion.
    StockMovement stockMovement =
        stockService.decrease(productId, context.toStockContext(), quantity);

    CashMovement cashMovement = cashService.create(
        new CreateCashMovementRequest(
            CashMovementType.IN,
            CashSource.PRODUCT_SALE,
            paymentMethod,
            context,
            amount,
            null,
            comment,
            product.getName(),
            productId,
            null,
            null,
            null));

    stockMovementService.linkToCashMovement(
        stockMovement.getId(), cashMovement.getId());
  }

  /**
   * Consumo interno: descuenta stock (y lotes, vía StockService) SIN generar
   * movimiento de caja ni venta. Deja trazabilidad en stock_movements con
   * motivo y comentario (quién lo retiró / destino).
   *
   * Casos reales: 1 Labial Vitamina E para uso personal, traslado de
   * Urban Lait Prodermic SPF 35 al carrito/camilla, pedido Luca.
   */
  public void internalConsumption(InternalConsumptionRequest req) {

    if (!INTERNAL_CONSUMPTION_REASONS.contains(req.reason())) {
      throw new IllegalArgumentException(
          "Motivo inválido para consumo interno: " + req.reason());
    }

    // Valida existencia del producto (falla temprano con mensaje claro).
    var product = productService.getById(req.productId());

    String comment = req.comment() == null || req.comment().isBlank()
        ? "Consumo interno"
        : req.comment().trim();

    stockService.decrease(
        product.getId(),
        req.context().toStockContext(),
        req.quantity(),
        req.reason(),
        comment);
  }

  /**
   * Procesa una orden de compra multi-ítem de forma atómica.
   *
   * Por cada ítem: asegura el stock, lo incrementa, crea el lote y actualiza
   * los precios del producto si corresponde. Acumula el subtotal de cada ítem
   * y, al final, valida que la suma coincida con el total esperado por el
   * cliente y crea un ÚNICO movimiento de caja (OUT) por el total.
   *
   * Toda la operación corre en la transacción de la clase: si un ítem falla,
   * se revierte la compra completa.
   *
   * @param context       contexto de caja/stock
   * @param comment       comentario general (proveedor / observación)
   * @param paymentMethod método de pago de la compra
   * @param expectedTotal total calculado por el cliente, validado contra el
   *                      cálculo del backend
   * @param items         detalle de ítems (mínimo uno)
   */
  public void purchaseOrder(
      CashContext context,
      String comment,
      PaymentMethod paymentMethod,
      BigDecimal expectedTotal,
      List<PurchaseItemRequest> items) {

    if (items == null || items.isEmpty()) {
      throw new IllegalArgumentException("La compra debe tener al menos un ítem");
    }

    StockContext stockContext = context.toStockContext();

    BigDecimal computedTotal = BigDecimal.ZERO;

    for (PurchaseItemRequest item : items) {
      computedTotal = computedTotal.add(processPurchaseItem(item, stockContext));
    }

    // Integridad: el total real (calculado desde los ítems) es la fuente de
    // verdad. Se valida contra el total que envió el cliente, con tolerancia
    // de un centavo para diferencias de redondeo.
    BigDecimal difference = computedTotal.subtract(expectedTotal).abs();
    if (difference.compareTo(new BigDecimal("0.01")) > 0) {
      throw new IllegalArgumentException(
          "El total de la compra no coincide con la suma de los ítems");
    }

    cashService.create(
        new CreateCashMovementRequest(
            CashMovementType.OUT,
            CashSource.PROVIDER_PAYMENT,
            paymentMethod,
            context,
            computedTotal,
            BigDecimal.ZERO,
            comment,
            null,
            null,
            null,
            null,
            null));
  }

  /**
   * Procesa un único ítem de la compra (stock + lote + precios) y devuelve su
   * subtotal. No crea movimiento de caja: ese se hace una sola vez por la
   * orden completa.
   */
  private BigDecimal processPurchaseItem(
      PurchaseItemRequest item, StockContext stockContext) {

    UUID productId = item.productId();
    int packages = item.quantity();

    var product = productService.getById(productId);

    // La compra se carga en ENVASES; el stock y el lote entran en la unidad
    // consumible del producto. Caja de NCTF (unitsPerPackage = 15): comprar 1
    // ingresa 15 ml, y una sesión descuenta 1 o 2. Para el retail el factor
    // es 1 y nada cambia.
    int perPackage = product.getUnitsPerPackage() == null || product.getUnitsPerPackage() < 1
        ? 1
        : product.getUnitsPerPackage();
    int stockUnits = packages * perPackage;

    if (!stockService.exists(productId, stockContext)) {
      stockService.initStock(productId, stockContext, 0);
    }

    stockService.increase(productId, stockContext, stockUnits);

    // Vencimiento por defecto: si la compra no trae fecha y el producto tiene
    // vida util configurada (shelfLifeMonths), el lote nace con
    // hoy + esos meses. Es el caso de los magistrales y de productos como el
    // Labial de Vitamina E, que no traen fecha impresa pero vencen igual.
    // Si la compra SI trae fecha, la fecha manda: el default nunca la pisa.
    java.time.LocalDate expiration = item.expirationDate();
    if (expiration == null && product.getShelfLifeMonths() != null) {
      expiration = java.time.LocalDate.now().plusMonths(product.getShelfLifeMonths());
    }

    batchService.createBatch(
        productId,
        stockContext,
        stockUnits,
        expiration,
        item.lotNumber());

    BigDecimal unitCost = item.unitCost();

    if (Boolean.TRUE.equals(item.updateCostPrice())) {
      // unitCost es el costo del ENVASE; costPrice se guarda por unidad
      // consumible para que valor de stock (cantidad × costo) y el costo por
      // sesión cierren. Caja NCTF $562.400 / 15 -> $37.493,33 el ml.
      product.setCostPrice(perPackage > 1
          ? unitCost.divide(BigDecimal.valueOf(perPackage), 2, RoundingMode.HALF_UP)
          : unitCost);
    }

    if (Boolean.TRUE.equals(item.updateSalePrice()) && item.newSalePrice() != null) {
      product.setSalePrice(item.newSalePrice());
    }

    if (Boolean.TRUE.equals(item.updateMarkupPercentage())) {
      if (item.newDefaultMarkupPercentage() == null) {
        throw new IllegalArgumentException("newDefaultMarkupPercentage is required");
      }

      product.setDefaultMarkupPercentage(item.newDefaultMarkupPercentage());
    }

    return item.subtotal();
  }

  public void sellByBarcode(
      String barcode,
      int quantity,
      BigDecimal amount,
      PaymentMethod paymentMethod,
      CashContext context,
      String comment,
      CashActor performedBy) {

    var product = productService.getByBarcode(barcode);

    if (product.getCostPrice() != null) {
      BigDecimal expected = product.getCostPrice()
          .multiply(BigDecimal.valueOf(quantity));

      if (amount.compareTo(expected) < 0) {
        throw new IllegalStateException("Amount lower than cost price");
      }
    }

    StockContext stockContext = context.toStockContext();

    if (!stockService.exists(product.getId(), stockContext)) {
      stockService.initStock(product.getId(), stockContext, 0);
    }

    StockMovement stockMovement =
        stockService.decrease(product.getId(), stockContext, quantity);

    CashMovement cashMovement = cashService.create(
        new CreateCashMovementRequest(
            CashMovementType.IN,
            CashSource.PRODUCT_SALE,
            paymentMethod,
            context,
            amount,
            null,
            comment,
            product.getName(),
            product.getId(),
            null,
            null,
            performedBy));

    stockMovementService.linkToCashMovement(
        stockMovement.getId(), cashMovement.getId());
  }

  /**
   * Venta combinada atómica: productos (descuentan stock) y/o procedimientos
   * (no tocan stock) en una única operación. Genera un solo CashMovement con
   * N ítems. Corre en la transacción de la clase: si algo falla, se revierte
   * el stock descontado y no se crea el movimiento.
   */
  public void combinedSale(CombinedSaleRequest req) {
    if (req.items() == null || req.items().isEmpty()) {
      throw new IllegalArgumentException("La venta debe tener al menos un ítem");
    }

    StockContext stockContext = req.context().toStockContext();
    List<CombinedItemLine> lines = new ArrayList<>();
    List<UUID> stockMovementIds = new ArrayList<>();

    for (CombinedSaleItemRequest item : req.items()) {
      if (item.quantity() <= 0) {
        throw new IllegalArgumentException("La cantidad debe ser mayor a cero");
      }
      if (item.subtotal() == null || item.subtotal().compareTo(BigDecimal.ZERO) <= 0) {
        throw new IllegalArgumentException("El subtotal debe ser mayor a cero");
      }

      if (item.kind() == CashMovementItemKind.PRODUCT) {
        if (item.productId() == null) {
          throw new IllegalArgumentException("productId es obligatorio en un ítem de producto");
        }

        var product = productService.getById(item.productId());

        // Misma protección que sellByBarcode: no vender por debajo del costo.
        if (product.getCostPrice() != null) {
          BigDecimal minExpected = product.getCostPrice()
              .multiply(BigDecimal.valueOf(item.quantity()));
          if (item.subtotal().compareTo(minExpected) < 0) {
            throw new IllegalStateException(
                "Subtotal menor al costo del producto: " + product.getName());
          }
        }

        if (!stockService.exists(item.productId(), stockContext)) {
          stockService.initStock(item.productId(), stockContext, 0);
        }
        StockMovement stockMovement =
            stockService.decrease(item.productId(), stockContext, item.quantity());
        stockMovementIds.add(stockMovement.getId());

        lines.add(new CombinedItemLine(
            CashMovementItemKind.PRODUCT,
            item.productId(),
            null,
            product.getName(), // nombre autoritativo desde backend
            item.quantity(),
            item.unitAmount(),
            item.subtotal(),
            item.performedBy(),
            null,
            null));

      } else if (item.kind() == CashMovementItemKind.PROCEDURE) {
        if (item.procedureCode() == null || item.procedureCode().isBlank()) {
          throw new IllegalArgumentException("procedureCode es obligatorio en un ítem de procedimiento");
        }
        String description = (item.description() == null || item.description().isBlank())
            ? item.procedureCode()
            : item.description();

        // BLINDAJE: el reparto y la autoría de un procedimiento los decide el
        // backend por código, NO el cliente. Los performedBy /
        // doctorSharePercent / cosmetologistSharePercent que vengan en el
        // request se ignoran a propósito. Una CONSULTA siempre sale 100%
        // médica y firmada MEDICA, aunque el front mande 40% para la
        // cosmetóloga (que es exactamente el bug que rompía el ranking).
        ProcedureSplit split = resolveProcedureSplit(item.procedureCode());

        lines.add(new CombinedItemLine(
            CashMovementItemKind.PROCEDURE,
            null,
            item.procedureCode(),
            description,
            item.quantity(),
            item.unitAmount(),
            item.subtotal(),
            split.performer(),
            split.doctorPercent(),
            split.cosmoPercent()));

      } else {
        throw new IllegalArgumentException("kind de ítem desconocido");
      }
    }

    CashMovement cashMovement = cashService.createCombined(
        req.context(),
        req.paymentMethod(),
        req.comment(),
        req.expectedTotal(),
        lines);

    stockMovementService.linkToCashMovement(stockMovementIds, cashMovement.getId());
  }

  /**
   * Sesión de tratamiento dermatológico: descuenta los insumos del recetario
   * (motivo PROCEDIMIENTO, en la unidad consumible de cada producto) y
   * registra UN ingreso de caja 100% médica, con los movimientos de stock
   * linkeados al de caja — mismo patrón que una venta. Si un insumo no
   * alcanza, la transacción entera se revierte y no se registra plata.
   */
  /**
   * Sesión de tratamiento con recetario: descuenta los insumos (motivo
   * PROCEDIMIENTO, en la unidad consumible de cada producto) y registra UN
   * ingreso de caja con el reparto del request, con los movimientos de stock
   * linkeados al de caja — mismo patrón que una venta. Si un insumo no
   * alcanza, la transacción entera se revierte y no se registra plata.
   */
  public void dermatoProcedure(DermatoProcedureRequest req) {
    if (req.amount() == null || req.amount().compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("El monto debe ser mayor a cero");
    }
    if (req.paymentMethod() == null) {
      throw new IllegalArgumentException("paymentMethod is required");
    }
    if (req.context() == null) {
      throw new IllegalArgumentException("context is required");
    }
    if (req.description() == null || req.description().isBlank()) {
      throw new IllegalArgumentException("La descripción del procedimiento es obligatoria");
    }

    // Reparto: null = 100% médica (compatibilidad con el cliente original).
    // La suma debe dar 1: mueve plata entre dos personas, se valida acá.
    BigDecimal doctorPercent = req.doctorSharePercent() == null
        ? BigDecimal.ONE
        : req.doctorSharePercent();
    BigDecimal cosmetologistPercent = req.cosmetologistSharePercent() == null
        ? BigDecimal.ZERO
        : req.cosmetologistSharePercent();

    if (doctorPercent.add(cosmetologistPercent).compareTo(BigDecimal.ONE) != 0) {
      throw new IllegalArgumentException(
          "doctorSharePercent + cosmetologistSharePercent debe ser 1");
    }

    StockContext stockContext = req.context().toStockContext();
    List<UUID> stockMovementIds = new ArrayList<>();

    if (req.consumptions() != null) {
      for (DermatoProcedureRequest.ConsumptionLine line : req.consumptions()) {
        if (line.quantity() <= 0) {
          throw new IllegalArgumentException("La cantidad consumida debe ser mayor a cero");
        }

        var product = productService.getById(line.productId());

        StockMovement stockMovement = stockService.decrease(
            product.getId(),
            stockContext,
            line.quantity(),
            StockMovementReason.PROCEDIMIENTO,
            "Consumo por procedimiento: " + req.description());

        stockMovementIds.add(stockMovement.getId());
      }
    }

    String comment = req.comment() == null || req.comment().isBlank()
        ? req.description()
        : req.comment().trim();

    CashMovement cashMovement = cashService.create(
        new CreateCashMovementRequest(
            CashMovementType.IN,
            CashSource.PROCEDURE,
            req.paymentMethod(),
            req.context(),
            req.amount(),
            null,                 // retentionPercent
            comment,
            req.description(),    // detail
            null,                 // referenceId
            doctorPercent,
            cosmetologistPercent,
            req.performedBy(),
            req.procedureCode())); // sin esto el movimiento queda con
                                   // procedure_code NULL y la métrica, que
                                   // agrupa por código, lo tira del conteo.

    stockMovementService.linkToCashMovement(stockMovementIds, cashMovement.getId());
  }
}