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
      CashContext context) {

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

    CashMovement movement;
    if (isFirst && t.getCosmetologistFixedShare() != null
        && context == CashContext.CONSULTORIO) {

      BigDecimal cosmoShare = t.getCosmetologistFixedShare();

      if (cosmoShare.compareTo(net) > 0)
        throw new IllegalStateException(
            "El monto fijo de la cosmetóloga (" + cosmoShare
                + ") supera el neto del pago (" + net + ")");

      BigDecimal doctorShare = net.subtract(cosmoShare).setScale(2, RoundingMode.HALF_UP);

      movement = cashService.createWithFixedShares(
          CashMovementType.IN,
          CashSource.PROCEDURE,
          paymentMethod,
          context,
          amountScaled,
          null,
          "Pago 1 - " + t.getDescription(),
          treatmentId,
          doctorShare,
          cosmoShare);

    } else {
      // Segundo pago (o sin fijo): todo a la médica.
      movement = cashService.createWithFixedShares(
          CashMovementType.IN,
          CashSource.PROCEDURE,
          paymentMethod,
          context,
          amountScaled,
          null,
          "Pago " + nextInstallment + " - " + t.getDescription(),
          treatmentId,
          net,
          BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
    }

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
}