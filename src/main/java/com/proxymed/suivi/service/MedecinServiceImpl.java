package com.proxymed.suivi.service;

import com.proxymed.suivi.entity.Medecin;
import com.proxymed.suivi.enums.RoleMedecin;
import com.proxymed.suivi.exception.ResourceNotFoundException;
import com.proxymed.suivi.repository.MedecinRepository;
import com.proxymed.suivi.service.mappers.MedecinMapper;
import com.proxymed.suivi.service.model.MedecinModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class MedecinServiceImpl implements MedecinService {

    private final MedecinRepository medecinRepository;
    private final MedecinMapper medecinMapper;

    @Override
    public List<MedecinModel> findAll(RoleMedecin role) {
        List<Medecin> medecins = role != null ? medecinRepository.findByRole(role) : medecinRepository.findAll();
        return medecins.stream().map(medecinMapper::toModel).toList();
    }

    @Override
    public MedecinModel findById(Long id) {
        return medecinMapper.toModel(getEntityById(id));
    }

    /**
     * Seul point d'acces a l'entite JPA dans ce service : prive, jamais expose aux
     * controllers ni aux autres services.
     */
    private Medecin getEntityById(Long id) {
        return medecinRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Medecin introuvable : " + id));
    }
}
