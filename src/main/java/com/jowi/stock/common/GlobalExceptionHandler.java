package com.jowi.stock.common;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.dao.DataIntegrityViolationException;

import com.jowi.stock.stock.dto.InsufficientStockErrorResponse;
import com.jowi.stock.stock.exceptions.InsufficientStockException;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // =========================
    // 400 - BAD REQUEST
    // =========================

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleInvalidJson(
            HttpMessageNotReadableException ex,
            HttpServletRequest request) {

        Throwable root = ex.getMostSpecificCause();
        String detail = (root != null && root.getMessage() != null)
                ? root.getMessage()
                : ex.getMessage();

        // si querés, dejalo para consola igual
        ex.printStackTrace();

        return buildError(
                HttpStatus.BAD_REQUEST,
                "Invalid request body: " + detail,
                request.getRequestURI());
    }

    // =========================
    // 404 - NOT FOUND
    // =========================

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            EntityNotFoundException ex,
            HttpServletRequest request) {

        return buildError(
                HttpStatus.NOT_FOUND,
                ex.getMessage(),
                request.getRequestURI());
    }

    // =========================
    // 409 - CONFLICT
    // =========================

    // Stock insuficiente: 409 con los datos del producto para que el front
    // ofrezca reconciliar el stock y reintentar.
    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<InsufficientStockErrorResponse> handleInsufficientStock(
            InsufficientStockException ex,
            HttpServletRequest request) {

        HttpStatus status = HttpStatus.CONFLICT;
        return ResponseEntity.status(status).body(new InsufficientStockErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI(),
                InsufficientStockErrorResponse.CODE,
                ex.getProductId(),
                ex.getProductName(),
                ex.getContext(),
                ex.getAvailable(),
                ex.getRequested(),
                ex.getUnit(),
                ex.getUnitsPerPackage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleConflict(
            IllegalStateException ex,
            HttpServletRequest request) {

        return buildError(
                HttpStatus.CONFLICT,
                ex.getMessage(),
                request.getRequestURI());
    }

    // =========================
    // 500 - INTERNAL ERROR
    // =========================

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(
            Exception ex,
            HttpServletRequest request) {

        ex.printStackTrace(); // detalle real en consola para vos

        // Mensaje genérico y amigable: el detalle técnico NO viaja al front.
        return buildError(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocurrió un error inesperado. Intentá de nuevo o avisá al administrador.",
                request.getRequestURI());
    }

    // =========================
    // BUILDER
    // =========================

    private ResponseEntity<ErrorResponse> buildError(
            HttpStatus status,
            String message,
            String path) {

        ErrorResponse response = new ErrorResponse(
                status.value(),
                status.getReasonPhrase(),
                message,
                path);

        return ResponseEntity.status(status).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .findFirst()
                .orElse("Validation error");

        return buildError(
                HttpStatus.BAD_REQUEST,
                message,
                request.getRequestURI());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(
            IllegalArgumentException ex,
            HttpServletRequest request) {

        return buildError(
                HttpStatus.BAD_REQUEST,
                ex.getMessage(),
                request.getRequestURI());
    }

    // =========================
    // 400 - PARAMETER ERRORS
    // =========================

    @ExceptionHandler({
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class
    })
    public ResponseEntity<ErrorResponse> handleBadRequestParams(
            Exception ex,
            HttpServletRequest request) {

        return buildError(
                HttpStatus.BAD_REQUEST,
                ex.getMessage(),
                request.getRequestURI());
    }

    // =========================
    // 409 - VIOLACIÓN DE INTEGRIDAD (constraints, FK, uniques)
    // =========================

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(
            DataIntegrityViolationException ex,
            HttpServletRequest request) {

        ex.printStackTrace(); // el detalle técnico queda en consola, no viaja al front

        return buildError(
                HttpStatus.CONFLICT,
                "No se pudo guardar la operación por un conflicto con los datos. "
                        + "Verificá la información e intentá de nuevo.",
                request.getRequestURI());
    }
}
