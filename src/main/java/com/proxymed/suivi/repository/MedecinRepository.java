package com.proxymed.suivi.repository;

import com.proxymed.suivi.entity.Medecin;
import com.proxymed.suivi.enums.RoleMedecin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MedecinRepository extends JpaRepository<Medecin, Long> {

    List<Medecin> findByRole(RoleMedecin role);
}
