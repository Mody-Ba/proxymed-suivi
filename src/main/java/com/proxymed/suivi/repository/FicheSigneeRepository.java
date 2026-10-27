package com.proxymed.suivi.repository;

import com.proxymed.suivi.entity.FicheSignee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FicheSigneeRepository extends JpaRepository<FicheSignee, UUID> {

    /** Fiche signee de la visite (au plus une, contrainte uk_fiche_signee_visite). */
    Optional<FicheSignee> findByVisite_Id(UUID visiteId);

    boolean existsByVisite_Id(UUID visiteId);

    /** Fiches signees du patient, de la visite la plus recente a la plus ancienne, sans leur contenu. */
    @Query("""
            select f.id as id, v.id as visiteId, v.patient.id as patientId, v.numeroVisite as numeroVisite,
                   f.nomFichier as nomFichier, f.typeContenu as typeContenu, f.tailleOctets as tailleOctets,
                   f.dateImport as dateImport
            from FicheSignee f join f.visite v
            where v.patient.id = :patientId
            order by v.numeroVisite desc
            """)
    List<FicheSigneeResume> findResumesByPatientId(UUID patientId);
}
