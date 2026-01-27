package com.jowi.stock.dashboard;

import com.jowi.stock.cash.*;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CashKpiPaymentMethodService {

  private final CashMovementRepository repository;

  public CashKpiPaymentMethodService(CashMovementRepository repository) {
    this.repository = repository;
  }

  public List<CashKpiByPaymentMethodResponse> getAll() {

    return Arrays.stream(PaymentMethod.values())
        .map(this::buildForMethod)
        .toList();
  }

  private CashKpiByPaymentMethodResponse buildForMethod(PaymentMethod method) {

    BigDecimal income =
        repository.sumAmountByTypeAndPaymentMethod(
            CashMovementType.IN, method);

    BigDecimal expenses =
        repository.sumAmountByTypeAndPaymentMethod(
            CashMovementType.OUT, method);

    BigDecimal retained =
        repository.sumRetentionByPaymentMethod(method);

    BigDecimal net =
        income.subtract(expenses).subtract(retained);

    return new CashKpiByPaymentMethodResponse(
        method.name(),
        income,
        expenses,
        retained,
        net
    );
  }
}
