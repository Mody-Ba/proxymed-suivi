package com.proxymed.suivi.repository;

import com.proxymed.suivi.entity.TraitementVisite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TraitementVisiteRepository extends JpaRepository<TraitementVisite, Long> {
}
