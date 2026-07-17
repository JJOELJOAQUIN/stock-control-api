package com.jowi.stock.business.services;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import com.jowi.stock.batch.services.ProductBatchService;
import com.jowi.stock.business.dto.InternalConsumptionRequest;
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
            null,              // retentionPercent
            comment,
            product.getName(), // detail
            productId,         // referenceId
            null,              // doctorSharePercent
            null,              // cosmetologistSharePercent
            null,              // performedBy
            null,              // procedureCode
            null,              // splitPreset
            null));            // peelingPaymentKind

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
            BigDecimal.ZERO, // retentionPercent
            comment,
            null,            // detail
            null,            // referenceId
            null,            // doctorSharePercent
            null,            // cosmetologistSharePercent
            null,            // performedBy
            null,            // procedureCode
            null,            // splitPreset
            null));          // peelingPaymentKind
  }

  /**
   * Procesa un único ítem de la compra (stock + lote + precios) y devuelve su
   * subtotal. No crea movimiento de caja: ese se hace una sola vez por la
   * orden completa.
   */
  private BigDecimal processPurchaseItem(
      PurchaseItemRequest item, StockContext stockContext) {

    UUID productId = item.productId();
    int quantity = item.quantity();

    var product = productService.getById(productId);

    if (!stockService.exists(productId, stockContext)) {
      stockService.initStock(productId, stockContext, 0);
    }

    stockService.increase(productId, stockContext, quantity);

    batchService.createBatch(
        productId,
        stockContext,
        quantity,
        item.expirationDate(),
        item.lotNumber());

    BigDecimal unitCost = item.unitCost();

    if (Boolean.TRUE.equals(item.updateCostPrice())) {
      product.setCostPrice(unitCost);
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
            null,              // retentionPercent
            comment,
            product.getName(), // detail
            product.getId(),   // referenceId
            null,              // doctorSharePercent
            null,              // cosmetologistSharePercent
            performedBy,
            null,              // procedureCode
            null,              // splitPreset
            null));            // peelingPaymentKind

    stockMovementService.linkToCashMovement(
        stockMovement.getId(), cashMovement.getId());
  }

  /**
   * Venta combinada atómica: productos (descuentan stock) y/o procedimientos
   * (no tocan stock) en una única operación. Genera un solo CashMovement con
   * N ítems. Corre en la transacción de la clase: si algo falla, se revierte
   * el stock descontado y no se crea el movimiento.
   *
   * El peeling profundo no pasa por acá: está excluido del carrito a propósito
   * y tiene su propio flujo por la card de procedimientos, que es donde vive
   * el reparto configurable.
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

        // Autoria del procedimiento: la del item si vino, si no la de la
        // cabecera. Sin esto no hay forma de distinguir un procedimiento
        // hecho por Gise con 0% para ella de uno propio de Pili.
        CashActor procedurePerformer = item.performedBy() != null
            ? item.performedBy()
            : req.performedBy();

        lines.add(new CombinedItemLine(
            CashMovementItemKind.PROCEDURE,
            null,
            item.procedureCode(),
            description,
            item.quantity(),
            item.unitAmount(),
            item.subtotal(),
            procedurePerformer,
            item.doctorSharePercent(),
            item.cosmetologistSharePercent()));

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
}