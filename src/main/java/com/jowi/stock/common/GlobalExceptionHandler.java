package com.jowi.stock.common;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;

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
      request.getRequestURI()
  );
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

    ex.printStackTrace(); // ← CLAVE para ver el error real

    return buildError(
        HttpStatus.INTERNAL_SERVER_ERROR,
        ex.getClass().getSimpleName() + ": " + ex.getMessage(),
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

}