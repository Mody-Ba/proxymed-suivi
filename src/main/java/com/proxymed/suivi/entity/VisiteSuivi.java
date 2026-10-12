package com.proxymed.suivi.entity;

import com.proxymed.suivi.enums.SigneAlerteClinique;
import com.proxymed.suivi.enums.StatutVisite;
import com.proxymed.suivi.enums.TypeVisite;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Entite racine de la Fiche 2 (visite de suivi mensuel a domicile).
 * Porte pour l'instant la section 1 (identification) et la section 3 (surveillance).
 * Les regles de gestion (numero de visite, roles des medecins, statut modifiable, etc.)
 * sont appliquees au niveau service, pas ici.
 */
@Entity
@Table(name = "visite_suivi")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VisiteSuivi {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @Column(name = "numero_visite", nullable = false)
    private Integer numeroVisite;

    @Column(name = "date_visite")
    private LocalDate dateVisite;

    @Column(name = "heure_arrivee")
    private LocalTime heureArrivee;

    @Column(name = "heure_depart")
    private LocalTime heureDepart;

    @Column(name = "adresse_visite")
    private String adresseVisite;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "medecin_junior_id", nullable = false)
    private Medecin medecinJunior;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "medecin_senior_id", nullable = false)
    private Medecin medecinSenior;

    @Column(name = "contact_senior")
    private String contactSenior;

    @Column(name = "contact_senior_joignable")
    private Boolean contactSeniorJoignable;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_visite")
    private TypeVisite typeVisite;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private StatutVisite statut = StatutVisite.BROUILLON;

    /**
     * Grille des constantes (section 3.1). Les mesures sont creees et supprimees par
     * ConstanteVisiteService ; cette collection ne sert qu'a les relire avec la visite.
     */
    @OneToMany(mappedBy = "visite")
    @Builder.Default
    private List<ConstanteVisite> constantes = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "visite_signe_alerte_clinique", joinColumns = @JoinColumn(name = "visite_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "signe", nullable = false)
    @Builder.Default
    private Set<SigneAlerteClinique> signesAlerteCliniques = new HashSet<>();

    @Column(name = "heure_appel_senior")
    private LocalTime heureAppelSenior;

    @Column(name = "heure_reponse_senior")
    private LocalTime heureReponseSenior;

    @Column(name = "decision_appel_senior", columnDefinition = "TEXT")
    private String decisionAppelSenior;

    @Column(name = "date_creation", nullable = false)
    private Instant dateCreation;

    @Column(name = "date_mise_a_jour", nullable = false)
    private Instant dateMiseAJour;
}
