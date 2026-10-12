package com.proxymed.suivi.repository;

import com.proxymed.suivi.entity.VisiteSuivi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface VisiteSuiviRepository extends JpaRepository<VisiteSuivi, UUID> {

    long countByPatientId(UUID patientId);

    List<VisiteSuivi> findByPatientIdOrderByNumeroVisiteDesc(UUID patientId);
}
