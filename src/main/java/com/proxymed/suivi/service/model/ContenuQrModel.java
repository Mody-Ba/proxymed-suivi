package com.proxymed.suivi.service.model;

import lombok.Builder;

import java.util.UUID;


@Builder(toBuilder = true)
public record ContenuQrModel(
        String type,
        UUID patientId,
        UUID visiteId
) {
}
