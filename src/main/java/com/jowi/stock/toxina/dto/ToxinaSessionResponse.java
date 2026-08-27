package com.jowi.stock.toxina.dto;

import com.jowi.stock.toxina.entities.ToxinaSession;
import com.jowi.stock.toxina.enums.OpenVialStatus;

import java.time.Instant;
import java.util.UUID;

public record ToxinaSessionResponse(
    UUID id,
    UUID treatmentId,
    UUID openVialId,
    Integer sessionNumber,
    Instant performedAt,
    Integer unitsUsed,
    Integer vialUnitsRemaining,
    Instant vialExpiresAt,
    OpenVialStatus vialStatus
) {
  public static ToxinaSessionResponse from(ToxinaSession s) {
    return new ToxinaSessionResponse(
        s.getId(),
        s.getTreatment().getId(),
        s.getOpenVial().getId(),
        s.getSessionNumber(),
        s.getPerformedAt(),
        s.getUnitsUsed(),
        s.getOpenVial().getUnitsRemaining(),
        s.getOpenVial().getExpiresAt(),
        s.getOpenVial().getStatus());
  }
}
