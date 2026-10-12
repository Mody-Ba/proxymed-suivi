package com.proxymed.suivi.repository;

import com.proxymed.suivi.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PatientRepository extends JpaRepository<Patient, UUID> {

    boolean existsByNumeroDossierProxymed(String numeroDossierProxymed);
}
