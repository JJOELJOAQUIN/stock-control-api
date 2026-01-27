package com.jowi.stock.dashboard;

import com.jowi.stock.cash.*;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CashKpiService {

    private final CashMovementRepository repository;

    public CashKpiService(CashMovementRepository repository) {
        this.repository = repository;
    }

    // =========================
    // KPI GLOBAL DE CAJA
    // =========================
    public CashKpiSummaryResponse getSummary() {

        BigDecimal totalIn        = nz(repository.totalIn());
        BigDecimal totalOut       = nz(repository.totalOut());
        BigDecimal totalRetention = nz(repository.totalRetention());

        BigDecimal netTotal =
            totalIn.subtract(totalOut).subtract(totalRetention);

        return new CashKpiSummaryResponse(
                totalIn,
                totalOut,
                totalRetention,
                netTotal,

                nz(repository.netByContext(CashContext.LOCAL)),
                nz(repository.netByContext(CashContext.CONSULTORIO)),

                nz(repository.netByPayment(PaymentMethod.CASH)),
                nz(repository.netByPayment(PaymentMethod.TRANSFER)),
                nz(repository.netByPayment(PaymentMethod.CREDIT)),
                nz(repository.netByPayment(PaymentMethod.DEBIT))
        );
    }

    // =========================
    // KPI POR CONTEXTO
    // =========================
    public CashKpiByContextResponse getByContext(CashContext context) {

        if (context == null) {
            throw new IllegalArgumentException("context is required");
        }

        BigDecimal income   = nz(repository.sumAmountByTypeAndContext(CashMovementType.IN, context));
        BigDecimal expenses = nz(repository.sumAmountByTypeAndContext(CashMovementType.OUT, context));
        BigDecimal retained = nz(repository.sumRetentionByContext(context));

        BigDecimal net = income.subtract(expenses).subtract(retained);

        return new CashKpiByContextResponse(
                income,
                expenses,
                retained,
                net
        );
    }

    // =========================
    // HELPER
    // =========================
    private BigDecimal nz(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
