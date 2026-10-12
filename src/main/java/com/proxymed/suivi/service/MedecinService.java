package com.proxymed.suivi.service;

import com.proxymed.suivi.enums.RoleMedecin;
import com.proxymed.suivi.service.model.MedecinModel;

import java.util.List;

public interface MedecinService {

    List<MedecinModel> findAll(RoleMedecin role);

    MedecinModel findById(Long id);
}
