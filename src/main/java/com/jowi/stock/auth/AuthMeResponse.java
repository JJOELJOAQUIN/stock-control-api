package com.jowi.stock.auth;

public record AuthMeResponse(
    String uid,
    String role
) {}