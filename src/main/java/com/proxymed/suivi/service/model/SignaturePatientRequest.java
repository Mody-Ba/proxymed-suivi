package com.proxymed.suivi.service.model;

import com.proxymed.suivi.enums.SignatairePatient;
import jakarta.validation.constraints.NotNull;

/**
 * Signature (ou empreinte) du patient ou de son aidant sur une fiche terminee ou visee.
 */
public record SignaturePatientRequest(
        @NotNull(message = "le signataire est obligatoire (PATIENT ou AIDANT)")
        SignatairePatient signataire
) {
}
