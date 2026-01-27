package com.jowi.stock.auth;

import jakarta.validation.constraints.NotNull;

public record UpdateUserRoleRequest(
    @NotNull Role role
) {}
