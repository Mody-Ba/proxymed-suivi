package com.proxymed.suivi.entity;

import com.proxymed.suivi.enums.StatutVisite;
import com.proxymed.suivi.enums.TypeVisite;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Entite racine de la Fiche 2 (visite de suivi mensuel a domicile).
 * Ne porte pour l'instant que la section 1 (identification de la visite).
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

    @Column(name = "date_creation", nullable = false)
    private Instant dateCreation;

    @Column(name = "date_mise_a_jour", nullable = false)
    private Instant dateMiseAJour;
}
