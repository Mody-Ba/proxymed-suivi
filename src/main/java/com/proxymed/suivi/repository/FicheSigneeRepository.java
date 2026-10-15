package com.proxymed.suivi.repository;

import com.proxymed.suivi.entity.FicheSignee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface FicheSigneeRepository extends JpaRepository<FicheSignee, UUID> {

    /** Fiche la plus recente (par date d'import) de la visite. */
    Optional<FicheSignee> findFirstByVisite_IdOrderByDateImportDesc(UUID visiteId);
}
