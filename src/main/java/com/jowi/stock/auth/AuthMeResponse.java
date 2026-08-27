package com.jowi.stock.auth;

public record AuthMeResponse(
    String firebaseUid,
    String email,
    String role,
    Boolean enabled
) {}
