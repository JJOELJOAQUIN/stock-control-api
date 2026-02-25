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

        BigDecimal percent = resolveRetentionPercent(req.paymentMethod(), req.retentionPercent());

        BigDecimal retention = amount.multiply(percent).setScale(2, RoundingMode.HALF_UP);
        BigDecimal net = amount.subtract(retention).setScale(2, RoundingMode.HALF_UP);

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

    private BigDecimal resolveRetentionPercent(PaymentMethod method, BigDecimal override) {
        if (override != null) {
            if (override.compareTo(BigDecimal.ZERO) < 0 || override.compareTo(BigDecimal.ONE) > 0) {
                throw new IllegalArgumentException("retentionPercent must be between 0 and 1");
            }
            return override;
        }
        return (method == PaymentMethod.CREDIT || method == PaymentMethod.DEBIT)
                ? DEFAULT_CARD_RETENTION
                : BigDecimal.ZERO;
    }

}



