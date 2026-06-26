package com.jowi.stock.treatment.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jowi.stock.cash.entities.CashMovement;
import com.jowi.stock.cash.enums.CashContext;
import com.jowi.stock.cash.enums.CashMovementType;
import com.jowi.stock.cash.enums.CashSource;
import com.jowi.stock.cash.enums.PaymentMethod;
import com.jowi.stock.cash.services.CashMovementService;
import com.jowi.stock.patient.entities.Patient;
import com.jowi.stock.patient.repositories.PatientRepository;
import com.jowi.stock.treatment.entities.Treatment;
import com.jowi.stock.treatment.entities.TreatmentPayment;
import com.jowi.stock.treatment.enums.TreatmentStatus;
import com.jowi.stock.treatment.repositories.TreatmentRepository;

@Service
public class TreatmentService {

  // Procedimientos que exigen paciente asociado. Hoy solo el peeling protocolo;
  // mañana se agregan codes acá (o se migra a un flag por procedimiento).
  private static final Set<String> PATIENT_REQUIRED_CODES =
      Set.of("PEELING_PROFUNDO_PROTOCOLO");

  // Parte fija (NETA) que cobra la cosmetóloga en tratamientos que la usan
  // (default global, editable). Cada tratamiento puede override este valor.
  public static final BigDecimal DEFAULT_COSMETOLOGIST_FIXED_SHARE =
      new BigDecimal("40000");

  // Debe coincidir con la retención de tarjeta del CashMovementService.
  private static final BigDecimal DEFAULT_CARD_RETENTION = new BigDecimal("0.30");

  private final TreatmentRepository treatmentRepository;
  private final PatientRepository patientRepository;
  private final CashMovementService cashMovementService;

  public TreatmentService(
      TreatmentRepository treatmentRepository,
      PatientRepository patientRepository,
      CashMovementService cashMovementService) {
    this.treatmentRepository = treatmentRepository;
    this.patientRepository = patientRepository;
    this.cashMovementService = cashMovementService;
  }

  /**
   * Crea un tratamiento y, opcionalmente, registra su primer pago en la
   * misma operación.
   */
  @Transactional
  public Treatment createTreatment(
      String procedureCode,
      String procedureLabel,
      UUID patientId,
      CashContext context,
      BigDecimal totalAmount,
      BigDecimal cosmetologistFixedShare,
      String comment,
      // primer pago (opcional)
      BigDecimal firstPaymentAmount,
      PaymentMethod firstPaymentMethod) {

    if (procedureCode == null || procedureCode.isBlank()) {
      throw new IllegalArgumentException("procedureCode is required");
    }
    if (context == null) {
      throw new IllegalArgumentException("context is required");
    }
    if (totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("totalAmount must be > 0");
    }

    Patient patient = null;
    if (patientId != null) {
      patient = patientRepository.findById(patientId)
          .orElseThrow(() -> new IllegalArgumentException("patient not found"));
    }

    // Validación: ciertos procedimientos exigen paciente.
    if (PATIENT_REQUIRED_CODES.contains(procedureCode) && patient == null) {
      throw new IllegalArgumentException(
          "Este tratamiento requiere un paciente asociado");
    }

    Treatment treatment = new Treatment();
    treatment.setProcedureCode(procedureCode);
    treatment.setProcedureLabel(procedureLabel);
    treatment.setPatient(patient);
    treatment.setContext(context);
    treatment.setTotalAmount(totalAmount.setScale(2, RoundingMode.HALF_UP));

    // Reparto fijo de la cosmetóloga: usa el override si vino, si no el
    // default global para los procedimientos que lo requieren (peeling).
    BigDecimal resolvedFixedShare = cosmetologistFixedShare;
    if (resolvedFixedShare == null && PATIENT_REQUIRED_CODES.contains(procedureCode)) {
      resolvedFixedShare = DEFAULT_COSMETOLOGIST_FIXED_SHARE;
    }
    treatment.setCosmetologistFixedShare(
        resolvedFixedShare != null
            ? resolvedFixedShare.setScale(2, RoundingMode.HALF_UP)
            : null);
    treatment.setComment(comment);

    treatment = treatmentRepository.save(treatment);

    // Primer pago opcional.
    if (firstPaymentAmount != null && firstPaymentAmount.compareTo(BigDecimal.ZERO) > 0) {
      registerPayment(treatment.getId(), firstPaymentAmount, firstPaymentMethod, null);
      // recargar para reflejar el pago en la colección
      treatment = treatmentRepository.findById(treatment.getId()).orElseThrow();
    }

    return treatment;
  }

  /**
   * Registra un pago de un tratamiento: valida que no sobrepague, calcula el
   * reparto (la cosmetóloga cobra su parte fija primero), crea el CashMovement
   * y lo vincula al pago.
   */
  @Transactional
  public TreatmentPayment registerPayment(
      UUID treatmentId,
      BigDecimal amount,
      PaymentMethod paymentMethod,
      String comment) {

    if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("amount must be > 0");
    }
    if (paymentMethod == null) {
      throw new IllegalArgumentException("paymentMethod is required");
    }

    Treatment treatment = treatmentRepository.findById(treatmentId)
        .orElseThrow(() -> new IllegalArgumentException("treatment not found"));

    BigDecimal pending = treatment.getPendingAmount();
    BigDecimal normalizedAmount = amount.setScale(2, RoundingMode.HALF_UP);

    // Anti-sobrepago: no se puede pagar más que el saldo pendiente.
    if (normalizedAmount.compareTo(pending) > 0) {
      throw new IllegalArgumentException(
          "El pago supera el saldo pendiente del tratamiento");
    }

    String movementComment = comment != null && !comment.isBlank()
        ? comment
        : buildPaymentComment(treatment);

    // El reparto se calcula sobre el NETO (después de retención de tarjeta).
    // Primero creamos el movimiento para conocer el neto real, pero como el
    // neto depende solo del monto y el método, lo anticipamos acá para repartir.
    BigDecimal retentionPercent =
        (paymentMethod == PaymentMethod.CREDIT || paymentMethod == PaymentMethod.DEBIT)
            ? DEFAULT_CARD_RETENTION
            : BigDecimal.ZERO;
    BigDecimal retention = normalizedAmount
        .multiply(retentionPercent)
        .setScale(2, RoundingMode.HALF_UP);
    BigDecimal net = normalizedAmount.subtract(retention).setScale(2, RoundingMode.HALF_UP);

    // Reparto: la cosmetóloga cobra su parte fija NETA hasta agotarla; el resto
    // (incluida la retención) lo absorbe la médica.
    BigDecimal cosmeShareThisPayment = BigDecimal.ZERO;
    BigDecimal fixedShare = treatment.getCosmetologistFixedShare();

    if (fixedShare != null && fixedShare.compareTo(BigDecimal.ZERO) > 0) {
      BigDecimal alreadyToCosme = treatment.getPayments().stream()
          .map(TreatmentPayment::getCosmetologistShare)
          .filter(s -> s != null)
          .reduce(BigDecimal.ZERO, BigDecimal::add);

      BigDecimal remainingForCosme = fixedShare.subtract(alreadyToCosme);
      if (remainingForCosme.signum() < 0) {
        remainingForCosme = BigDecimal.ZERO;
      }

      // No puede llevarse más que el neto disponible de este pago.
      cosmeShareThisPayment = remainingForCosme.min(net);
    }

    BigDecimal doctorShareThisPayment = net.subtract(cosmeShareThisPayment);

    // Crea el movimiento de caja con shares en monto (única fuente de verdad
    // del dinero). referenceId apunta al tratamiento.
    CashMovement movement = cashMovementService.createWithFixedShares(
        CashMovementType.IN,
        CashSource.PROCEDURE,
        paymentMethod,
        treatment.getContext(),
        normalizedAmount,
        null, // retentionPercent override (usa el default por método)
        movementComment,
        treatment.getId(),
        doctorShareThisPayment,
        cosmeShareThisPayment);

    // Registra el pago vinculado al movimiento.
    TreatmentPayment payment = new TreatmentPayment();
    payment.setTreatment(treatment);
    payment.setAmount(normalizedAmount);
    payment.setPaymentMethod(paymentMethod);
    payment.setCashMovementId(movement.getId());
    payment.setDoctorShare(movement.getDoctorShare());
    payment.setCosmetologistShare(movement.getCosmetologistShare());
    payment.setComment(movementComment);

    treatment.getPayments().add(payment);
    treatmentRepository.save(treatment);

    return payment;
  }

  @Transactional(readOnly = true)
  public TreatmentStatus resolveStatus(Treatment treatment) {
    BigDecimal paid = treatment.getPaidAmount();
    if (paid.compareTo(BigDecimal.ZERO) == 0) {
      return TreatmentStatus.PENDIENTE;
    }
    if (paid.compareTo(treatment.getTotalAmount()) >= 0) {
      return TreatmentStatus.PAGADO;
    }
    return TreatmentStatus.PARCIALMENTE_PAGADO;
  }

  @Transactional(readOnly = true)
  public Treatment getById(UUID id) {
    return treatmentRepository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("treatment not found"));
  }

  @Transactional(readOnly = true)
  public List<Treatment> listByContext(CashContext context) {
    return treatmentRepository.findByContextOrderByCreatedAtDesc(context);
  }

  @Transactional(readOnly = true)
  public List<Treatment> listPendingByContext(CashContext context) {
    return treatmentRepository.findPendingByContext(context);
  }

  @Transactional(readOnly = true)
  public List<Treatment> listByPatient(UUID patientId) {
    return treatmentRepository.findByPatientIdOrderByCreatedAtDesc(patientId);
  }

  private String buildPaymentComment(Treatment treatment) {
    String label = treatment.getProcedureLabel() != null
        ? treatment.getProcedureLabel()
        : treatment.getProcedureCode();
    int paymentNumber = treatment.getPayments().size() + 1;
    return String.format("%s - pago %d", label, paymentNumber);
  }
}