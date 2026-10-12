package com.proxymed.suivi.repository;

import com.proxymed.suivi.entity.ConstanteVisite;
import com.proxymed.suivi.enums.CreneauMesure;
import com.proxymed.suivi.enums.TypeConstante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConstanteVisiteRepository extends JpaRepository<ConstanteVisite, Long> {

    Optional<ConstanteVisite> findByVisiteIdAndTypeAndCreneau(UUID visiteId, TypeConstante type, CreneauMesure creneau);
}
