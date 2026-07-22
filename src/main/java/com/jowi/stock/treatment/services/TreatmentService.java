package com.jowi.stock.treatment.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.jowi.stock.cash.entities.CashMovement;
import com.jowi.stock.cash.enums.CashContext;
import com.jowi.stock.cash.enums.CashMovementType;
import com.jowi.stock.cash.enums.CashSource;
import com.jowi.stock.cash.enums.PaymentMethod;
import com.jowi.stock.cash.dto.CashItemSpec;
import com.jowi.stock.cash.enums.CashActor;
import com.jowi.stock.cash.enums.CashMovementItemKind;
import com.jowi.stock.cash.enums.SplitPreset;
import com.jowi.stock.cash.services.CashMovementService;
import com.jowi.stock.patient.entities.Patient;
import com.jowi.stock.treatment.entities.Payment;
import com.jowi.stock.treatment.entities.Treatment;
import com.jowi.stock.treatment.enums.TreatmentStatus;
import com.jowi.stock.patient.repositories.PatientRepository;
import com.jowi.stock.treatment.repositories.PaymentRepository;
import com.jowi.stock.treatment.repositories.TreatmentRepository;

import jakarta.transaction.Transactional;

@Service
@Transactional
public class TreatmentService {

  /**
   * Único tratamiento que admite un reparto distinto al suyo propio. Es una
   * constante y no una columna porque hoy es el único código que existe: el
   * día que sean dos, esto pide una bandera en Treatment, no un if más.
   */
  private static final String PEELING_PROTOCOLO_CODE = "PEELING_PROFUNDO_PROTOCOLO";

  private final PatientRepository patientRepository;
  private final TreatmentRepository treatmentRepository;
  private final PaymentRepository paymentRepository;
  private final CashMovementService cashService;

  public TreatmentService(
      PatientRepository patientRepository,
      TreatmentRepository treatmentRepository,
      PaymentRepository paymentRepository,
      CashMovementService cashService) {
    this.patientRepository = patientRepository;
    this.treatmentRepository = treatmentRepository;
    this.paymentRepository = paymentRepository;
    this.cashService = cashService;
  }

  // ======================= PACIENTES =======================

  public Patient createPatient(String firstName, String lastName, String dni, String phone) {
    if (firstName == null || firstName.isBlank())
      throw new IllegalArgumentException("El nombre es obligatorio");
    if (lastName == null || lastName.isBlank())
      throw new IllegalArgumentException("El apellido es obligatorio");

    String normalizedDni = (dni == null || dni.isBlank()) ? null : dni.trim();

    if (normalizedDni != null) {
      patientRepository.findByDni(normalizedDni).ifPresent(p -> {
        throw new IllegalStateException("Ya existe un paciente con ese DNI");
      });
    }

    Patient patient = new Patient();
    patient.setFirstName(firstName.trim());
    patient.setLastName(lastName.trim());
    patient.setDni(normalizedDni);
    patient.setPhone((phone == null || phone.isBlank()) ? null : phone.trim());

    return patientRepository.save(patient);
  }

  public List<Patient> searchPatients(String term) {
    if (term == null || term.isBlank())
      return patientRepository.findAll();
    return patientRepository
        .findByLastNameContainingIgnoreCaseOrFirstNameContainingIgnoreCase(term, term);
  }

  // ======================= TRATAMIENTOS =======================

  /**
   * Crea un tratamiento para un paciente. Genérico: sirve para cualquier
   * protocolo con pagos. Para peeling, code = código del protocolo,
   * maxInstallments = 2, cosmetologistFixedShare = el fijo de la cosmetóloga.
   */
  public Treatment createTreatment(
      UUID patientId,
      String code,
      String description,
      BigDecimal totalAmount,
      BigDecimal cosmetologistFixedShare,
      int maxInstallments) {

    Patient patient = patientRepository.findById(patientId)
        .orElseThrow(() -> new IllegalArgumentException("Paciente no encontrado"));

    if (code == null || code.isBlank())
      throw new IllegalArgumentException("El código del tratamiento es obligatorio");
    if (totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) <= 0)
      throw new IllegalArgumentException("El monto total debe ser mayor a cero");
    if (maxInstallments < 1)
      throw new IllegalArgumentException("El máximo de cuotas debe ser al menos 1");

    Treatment t = new Treatment();
    t.setPatient(patient);
    t.setCode(code);
    t.setDescription(description);
    t.setTotalAmount(totalAmount.setScale(2, RoundingMode.HALF_UP));
    t.setPaidAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
    t.setCosmetologistFixedShare(
        cosmetologistFixedShare == null
            ? null
            : cosmetologistFixedShare.setScale(2, RoundingMode.HALF_UP));
    t.setMaxInstallments(maxInstallments);
    t.setStatus(TreatmentStatus.PENDIENTE);

    return treatmentRepository.save(t);
  }

  public Treatment getTreatment(UUID treatmentId) {
    return treatmentRepository.findById(treatmentId)
        .orElseThrow(() -> new IllegalArgumentException("Tratamiento no encontrado"));
  }

  public List<Treatment> getPatientTreatments(UUID patientId) {
    return treatmentRepository.findByPatientId(patientId);
  }

  public List<Payment> getTreatmentPayments(UUID treatmentId) {
    return paymentRepository.findByTreatmentIdOrderByInstallmentNumberAsc(treatmentId);
  }

  // ======================= PAGOS =======================

  /**
   * Registra un pago de un tratamiento. El primer pago (installment 1) aplica
   * el monto fijo de la cosmetóloga; los siguientes van 100% a la médica.
   * Genera el CashMovement correspondiente y lo vincula al Payment.
   *
   * @param context contexto de caja (normalmente CONSULTORIO para peeling)
   */
  public Payment registerPayment(
      UUID treatmentId,
      BigDecimal amount,
      PaymentMethod paymentMethod,
      CashContext context,
      SplitPreset splitPreset) {

    Treatment t = getTreatment(treatmentId);

    if (t.getStatus() == TreatmentStatus.COMPLETO)
      throw new IllegalStateException("El tratamiento ya está saldado");

    if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0)
      throw new IllegalArgumentException("El monto del pago debe ser mayor a cero");

    List<Payment> existing = getTreatmentPayments(treatmentId);
    int nextInstallment = existing.size() + 1;

    if (nextInstallment > t.getMaxInstallments())
      throw new IllegalStateException(
          "El tratamiento ya alcanzó el máximo de cuotas (" + t.getMaxInstallments() + ")");

    BigDecimal amountScaled = amount.setScale(2, RoundingMode.HALF_UP);

    // No permitir pagar de más.
    BigDecimal remaining = t.getTotalAmount().subtract(t.getPaidAmount());
    if (amountScaled.compareTo(remaining) > 0)
      throw new IllegalStateException(
          "El pago supera el saldo pendiente (" + remaining + ")");

    boolean isFirst = nextInstallment == 1;
    BigDecimal net = cashService.computeNet(amountScaled, paymentMethod);
    BigDecimal zero = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    // Null = NORMAL, para que cualquier cliente que no mande el campo cobre
    // exactamente como cobraba antes de este feature.
    SplitPreset preset = splitPreset == null ? SplitPreset.NORMAL : splitPreset;
    validateSplitPreset(preset, t, context, isFirst);

    BigDecimal cosmoShare;
    BigDecimal doctorShare;

    if (preset == SplitPreset.TODO_COSMETOLOGA) {
      cosmoShare = net;
      doctorShare = zero;

    } else if (preset == SplitPreset.TODO_MEDICA) {
      cosmoShare = zero;
      doctorShare = net;

    } else if (isFirst && t.getCosmetologistFixedShare() != null
        && context == CashContext.CONSULTORIO) {

      cosmoShare = t.getCosmetologistFixedShare();

      if (cosmoShare.compareTo(net) > 0)
        throw new IllegalStateException(
            "El monto fijo de la cosmetóloga (" + cosmoShare
                + ") supera el neto del pago (" + net + ")");

      doctorShare = net.subtract(cosmoShare).setScale(2, RoundingMode.HALF_UP);

    } else {
      // Segundo pago (o sin fijo): todo a la médica.
      cosmoShare = zero;
      doctorShare = net;
    }

    String comment = buildPaymentComment(nextInstallment, t.getDescription(), preset);

    // La autoría no depende del reparto: el peeling lo hace Gise aunque en
    // este pago cobre 0. Es justamente el caso que este feature arregla, y sin
    // el performedBy del ítem el movimiento se le cae de la card.
    CashActor performedBy = PEELING_PROTOCOLO_CODE.equals(t.getCode())
        ? CashActor.COSMETOLOGA
        : CashActor.MEDICA;

    CashMovement movement = cashService.createWithFixedShares(
        CashMovementType.IN,
        CashSource.PROCEDURE,
        paymentMethod,
        context,
        amountScaled,
        null,
        comment,
        treatmentId,
        doctorShare,
        cosmoShare,
        new CashItemSpec(
            CashMovementItemKind.PROCEDURE,
            null,
            t.getCode(),
            t.getDescription(),
            performedBy,
            preset));

    Payment payment = new Payment();
    payment.setAmount(amountScaled);
    payment.setPaymentMethod(paymentMethod);
    payment.setInstallmentNumber(nextInstallment);
    payment.setCashMovementId(movement.getId());
    t.addPayment(payment);

    BigDecimal newPaid = t.getPaidAmount().add(amountScaled).setScale(2, RoundingMode.HALF_UP);
    t.setPaidAmount(newPaid);

    if (newPaid.compareTo(t.getTotalAmount()) >= 0) {
      t.setStatus(TreatmentStatus.COMPLETO);
    } else {
      t.setStatus(TreatmentStatus.PARCIAL);
    }

    treatmentRepository.save(t);
    return payment;
  }

  public List<Treatment> listTreatments(TreatmentStatus status) {
    if (status == null) {
      return treatmentRepository.findAllByOrderByStatusAscCreatedAtDesc();
    }
    return treatmentRepository.findByStatusIn(List.of(status));
  }

  /**
   * El desvío es una excepción acotada al peeling. Se valida en el backend
   * porque mueve plata de una persona a la otra, y el front no es fuente de
   * verdad de eso.
   */
  private void validateSplitPreset(
      SplitPreset preset, Treatment t, CashContext context, boolean isFirst) {

    if (preset == SplitPreset.NORMAL)
      return;

    if (!PEELING_PROTOCOLO_CODE.equals(t.getCode()))
      throw new IllegalArgumentException(
          "El reparto configurable sólo aplica a peeling profundo");

    if (context != CashContext.CONSULTORIO)
      throw new IllegalArgumentException(
          "El reparto configurable sólo aplica en consultorio");

    // "Todo a Gise" existe para un caso concreto: la primera cuota que ella se
    // cobra entera para saldar arreglos previos con Pili. En un pago completo
    // el mismo preset significaría una deuda del doble, y después no habría
    // forma de distinguir en el registro cuál de las dos era.
    if (preset == SplitPreset.TODO_COSMETOLOGA && !isFirst)
      throw new IllegalStateException(
          "\"Todo a Gise\" sólo aplica a la primera cuota del peeling");
  }

  /**
   * El dato duro es split_preset; el comment es para que la que abre la caja
   * en el celular entienda por qué ese pago se repartió distinto.
   */
  private String buildPaymentComment(int installment, String description, SplitPreset preset) {
    String base = "Pago " + installment + " - " + description;

    if (preset == SplitPreset.TODO_COSMETOLOGA)
      return "Reparto: Todo a Gise · " + base;

    if (preset == SplitPreset.TODO_MEDICA)
      return "Reparto: Todo a Pili · " + base;

    return base;
  }

  /**
   * Revierte un pago a partir de su movimiento de caja anulado. Si el
   * movimiento no corresponde a ningún pago de tratamiento (procedimiento
   * suelto), no hace nada: decide la búsqueda por cash_movement_id, no el
   * source, porque es el único vínculo confiable.
   *
   * Baja paid_amount, recalcula el estado y elimina la fila de
   * treatment_payments — así la próxima cuota se numera desde los pagos que
   * quedaron, en lugar de contar uno anulado.
   */
  public void revertPaymentByCashMovement(java.util.UUID cashMovementId) {
    Payment payment = paymentRepository.findByCashMovementId(cashMovementId).orElse(null);

    if (payment == null) {
      return;
    }

    Treatment t = payment.getTreatment();

    BigDecimal newPaid = t.getPaidAmount()
        .subtract(payment.getAmount())
        .setScale(2, RoundingMode.HALF_UP);

    if (newPaid.compareTo(BigDecimal.ZERO) < 0) {
      newPaid = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    t.setPaidAmount(newPaid);

    if (newPaid.compareTo(BigDecimal.ZERO) == 0) {
      t.setStatus(TreatmentStatus.PENDIENTE);
    } else if (newPaid.compareTo(t.getTotalAmount()) < 0) {
      t.setStatus(TreatmentStatus.PARCIAL);
    } else {
      t.setStatus(TreatmentStatus.COMPLETO);
    }

    // orphanRemoval en Treatment.payments borra la fila al sacarla de la
    // colección; borrar sólo por el repository dejaría la entidad viva en la
    // sesión y la fila reaparecería en el flush.
    t.getPayments().remove(payment);

    treatmentRepository.save(t);
  }
}