package com.proxymed.suivi.repository;

import java.time.Instant;
import java.util.UUID;


public interface FicheSigneeResume {

    UUID getId();

    UUID getVisiteId();

    UUID getPatientId();

    Integer getNumeroVisite();

    String getNomFichier();

    String getTypeContenu();

    long getTailleOctets();

    Instant getDateImport();
}
