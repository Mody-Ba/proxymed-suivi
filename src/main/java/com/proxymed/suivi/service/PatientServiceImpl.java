package com.proxymed.suivi.service;

import com.proxymed.suivi.entity.Patient;
import com.proxymed.suivi.exception.RegleGestionException;
import com.proxymed.suivi.exception.ResourceNotFoundException;
import com.proxymed.suivi.repository.PatientRepository;
import com.proxymed.suivi.service.mappers.PatientMapper;
import com.proxymed.suivi.service.model.PatientModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class PatientServiceImpl implements PatientService {

    private final PatientRepository patientRepository;
    private final PatientMapper patientMapper;

    @Override
    public List<PatientModel> findAll() {
        return patientRepository.findAll().stream().map(this::toModelAvecAge).toList();
    }

    @Override
    public PatientModel findById(UUID id) {
        return toModelAvecAge(getEntityById(id));
    }

    @Override
    public PatientModel create(PatientModel model) {
        if (patientRepository.existsByNumeroDossierProxymed(model.numeroDossierProxymed())) {
            throw new RegleGestionException(
                    "Un patient existe deja avec le numero de dossier PROXYMED " + model.numeroDossierProxymed());
        }
        Patient patient = patientMapper.toEntity(model);
        return toModelAvecAge(patientRepository.save(patient));
    }

    @Override
    public List<PatientModel> findByNumerosDossierProxymed(List<String> numerosDossierProxymed) {
        Map<String, Patient> trouves = patientRepository.findByNumeroDossierProxymedIn(numerosDossierProxymed).stream()
                .collect(Collectors.toMap(Patient::getNumeroDossierProxymed, Function.identity()));
        List<String> introuvables = numerosDossierProxymed.stream().filter(n -> !trouves.containsKey(n)).toList();
        if (!introuvables.isEmpty()) {
            throw new ResourceNotFoundException(introuvables.size()
                    + " numero(s) de dossier PROXYMED introuvable(s) dans la Fiche 2 : aucune fiche n'a ete creee",
                    introuvables);
        }
        return numerosDossierProxymed.stream().map(n -> toModelAvecAge(trouves.get(n))).toList();
    }

    /**
     * L'age est une regle de gestion (derivee de la date de naissance), jamais persistee :
     * calculee ici plutot que dans l'entite ou les mappers.
     */
    @Override
    public PatientModel completerAge(PatientModel model) {
        if (model == null) {
            return null;
        }
        return model.toBuilder().age(calculerAge(model.dateNaissance())).build();
    }

    /**
     * Seul point d'acces a l'entite JPA dans ce service : prive, jamais expose aux
     * controllers ni aux autres services.
     */
    private Patient getEntityById(UUID id) {
        return patientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient introuvable : " + id));
    }

    private PatientModel toModelAvecAge(Patient entity) {
        return completerAge(patientMapper.toModel(entity));
    }

    private Integer calculerAge(LocalDate dateNaissance) {
        return dateNaissance != null ? Period.between(dateNaissance, LocalDate.now()).getYears() : null;
    }
}
