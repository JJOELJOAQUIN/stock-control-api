package com.jowi.stock.dashboard;

import com.jowi.stock.product.ProductRepository;
import com.jowi.stock.stock.JpaStockRepository;
import com.jowi.stock.cash.CashContext;
import com.jowi.stock.cash.CashMovementRepository;
import com.jowi.stock.movement.StockMovementRepository;
import com.jowi.stock.movement.StockMovementType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.Instant; // 👈 IMPORT NUEVO
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DashboardService {

    private final ProductRepository productRepository;
    private final JpaStockRepository stockRepository;
    private final StockMovementRepository movementRepository;
    private final CashKpiService cashKpiService;
    private final CashKpiPaymentMethodService cashPaymentKpiService;
    private final CashMovementRepository cashMovementRepository;
    private final MovementKpiService movementKpiService;

    public DashboardService(
            ProductRepository productRepository,
            JpaStockRepository stockRepository,
            StockMovementRepository movementRepository,
            CashKpiService cashKpiService,
            CashKpiPaymentMethodService cashPaymentKpiService,
            CashMovementRepository cashMovementRepository,
            MovementKpiService movementKpiService) {
        this.productRepository = productRepository;
        this.stockRepository = stockRepository;
        this.movementRepository = movementRepository;
        this.cashKpiService = cashKpiService;
        this.cashPaymentKpiService = cashPaymentKpiService;
        this.cashMovementRepository = cashMovementRepository;
        this.movementKpiService = movementKpiService;
    }

    public CashKpiSummaryResponse getCashKpis() {
        return cashKpiService.getSummary();
    }

    public CashKpiByContextResponse getCashByContext(CashContext context) {
        return cashKpiService.getByContext(context);
    }

    public List<CashKpiByPaymentMethodResponse> getCashByPaymentMethod() {
        return cashPaymentKpiService.getAll();
    }

    public List<CashMonthlyKpiResponse> getCashMonthly(int year, CashContext context) {

        return cashMovementRepository.aggregateMonthly(year, context)
                .stream()
                .map(row -> new CashMonthlyKpiResponse(
                        ((Number) row[0]).intValue(),
                        (BigDecimal) row[1],
                        (BigDecimal) row[2],
                        (BigDecimal) row[3],
                        (BigDecimal) row[4]))
                .toList();
    }

    // =========================
    // 🔧 FIX ACA
    // =========================
    public DashboardSummaryResponse getSummary() {

        long totalProducts = productRepository.count();
        long productsWithStock = stockRepository.countByCurrentGreaterThan(0);
        long productsWithoutStock = totalProducts - productsWithStock;
        long lowStock = stockRepository.countBelowMinimum();
        long totalMovements = movementRepository.count();

        // ⬇️ INICIO DEL DIA COMO Instant
        Instant startOfDay = OffsetDateTime.now()
                .withHour(0)
                .withMinute(0)
                .withSecond(0)
                .withNano(0)
                .toInstant();

        long movementsToday =
                movementRepository.countByCreatedAtAfter(startOfDay);

        return new DashboardSummaryResponse(
                totalProducts,
                productsWithStock,
                productsWithoutStock,
                lowStock,
                totalMovements,
                movementsToday);
    }

    public CashKpiCompareResponse getCashCompare(
            int year,
            int month,
            CashContext context) {

        Object[] curr = cashMovementRepository.aggregateMonth(year, month, context);

        int prevYear = (month == 1) ? year - 1 : year;
        int prevMonth = (month == 1) ? 12 : month - 1;

        Object[] prev = cashMovementRepository.aggregateMonth(prevYear, prevMonth, context);

        BigDecimal income = (BigDecimal) curr[0];
        BigDecimal expense = (BigDecimal) curr[1];
        BigDecimal retention = (BigDecimal) curr[2];
        BigDecimal net = (BigDecimal) curr[3];

        BigDecimal prevIncome = (BigDecimal) prev[0];
        BigDecimal prevExpense = (BigDecimal) prev[1];
        BigDecimal prevRetention = (BigDecimal) prev[2];
        BigDecimal prevNet = (BigDecimal) prev[3];

        return new CashKpiCompareResponse(
                income,
                expense,
                retention,
                net,

                income.subtract(prevIncome),
                expense.subtract(prevExpense),
                retention.subtract(prevRetention),
                net.subtract(prevNet),

                pct(income, prevIncome),
                pct(expense, prevExpense),
                pct(retention, prevRetention),
                pct(net, prevNet));
    }

    private BigDecimal pct(BigDecimal curr, BigDecimal prev) {
        if (prev.compareTo(BigDecimal.ZERO) == 0)
            return null;
        return curr
                .subtract(prev)
                .multiply(BigDecimal.valueOf(100))
                .divide(prev, 2, RoundingMode.HALF_UP);
    }

    public StockMovementKpiResponse getMovementKpis() {

        long totalIn = movementRepository.countByType(StockMovementType.IN);
        long totalOut = movementRepository.countByType(StockMovementType.OUT);

        long qtyIn = movementRepository.sumQuantityByType(StockMovementType.IN);
        long qtyOut = movementRepository.sumQuantityByType(StockMovementType.OUT);

        return new StockMovementKpiResponse(
                totalIn,
                totalOut,
                qtyIn,
                qtyOut);
    }
}
