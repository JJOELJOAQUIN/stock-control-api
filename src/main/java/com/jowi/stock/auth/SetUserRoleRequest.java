package com.jowi.stock.auth;



import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SetUserRoleRequest(
    @NotBlank String uid,
    @NotNull Role role
) {}