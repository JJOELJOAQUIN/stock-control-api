package com.jowi.stock.dashboard;

import com.jowi.stock.stock.entities.StockEntity;
import com.jowi.stock.stock.enums.StockContext;
import com.jowi.stock.stock.repositories.JpaStockRepository;
import com.jowi.stock.cash.enums.CashContext;
import com.jowi.stock.cash.repositories.CashMovementRepository;
import com.jowi.stock.movement.enums.StockMovementType;
import com.jowi.stock.movement.repositories.StockMovementRepository;
import com.jowi.stock.product.repositories.ProductRepository;
import com.jowi.stock.common.BusinessZone;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant; 
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

    public DashboardService(
            ProductRepository productRepository,
            JpaStockRepository stockRepository,
            StockMovementRepository movementRepository,
            CashKpiService cashKpiService,
            CashKpiPaymentMethodService cashPaymentKpiService,
            CashMovementRepository cashMovementRepository) {
        this.productRepository = productRepository;
        this.stockRepository = stockRepository;
        this.movementRepository = movementRepository;
        this.cashKpiService = cashKpiService;
        this.cashPaymentKpiService = cashPaymentKpiService;
        this.cashMovementRepository = cashMovementRepository;

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

        // Un agregado por mes, con el mes delimitado en hora Argentina
        // (BusinessZone) en vez de agrupar por MONTH() en la base. Se incluye
        // sólo el mes que tuvo movimiento, igual que hacía el GROUP BY MONTH.
        List<CashMonthlyKpiResponse> result = new java.util.ArrayList<>();

        for (int month = 1; month <= 12; month++) {
            BusinessZone.Range range = BusinessZone.ofMonth(year, month);
            Object[] row = cashMovementRepository.aggregateRange(
                    context, range.from(), range.to());

            BigDecimal in = (BigDecimal) row[0];
            BigDecimal out = (BigDecimal) row[1];
            BigDecimal retention = (BigDecimal) row[2];
            BigDecimal net = (BigDecimal) row[3];

            boolean hadActivity =
                    in.compareTo(BigDecimal.ZERO) != 0
                    || out.compareTo(BigDecimal.ZERO) != 0;
            if (!hadActivity) {
                continue;
            }

            result.add(new CashMonthlyKpiResponse(month, in, out, retention, net));
        }

        return result;
    }

    // =========================
    // 🔧 FIX ACA
    // =========================
    public DashboardSummaryResponse getSummary(StockContext context) {

        long totalProducts = productRepository.count();

        List<StockEntity> stocks = stockRepository.findByContext(context);

        long productsWithStock = stocks.stream()
                .filter(s -> s.getCurrent() > 0)
                .count();

        long productsWithoutStock = totalProducts - productsWithStock;
        long lowStock = stocks.stream()
                .filter(stock -> stock.getCurrent() < stock.getProduct().getMinimumStock())
                .count();

        long totalMovements = movementRepository.countByContext(context);

        // Inicio del día en hora Argentina (no en el offset del server).
        Instant startOfDay = BusinessZone.startOfDay(BusinessZone.today());

        long movementsToday = movementRepository.countByContextAndCreatedAtAfter(context, startOfDay);

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

        // Mes actual y anterior, cada uno delimitado en hora Argentina.
        BusinessZone.Range currRange = BusinessZone.ofMonth(year, month);
        Object[] curr = cashMovementRepository.aggregateRange(
                context, currRange.from(), currRange.to());

        int prevYear = (month == 1) ? year - 1 : year;
        int prevMonth = (month == 1) ? 12 : month - 1;

        BusinessZone.Range prevRange = BusinessZone.ofMonth(prevYear, prevMonth);
        Object[] prev = cashMovementRepository.aggregateRange(
                context, prevRange.from(), prevRange.to());

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