package com.proxymed.suivi.entity;

import com.proxymed.suivi.enums.CreneauMesure;
import com.proxymed.suivi.enums.TypeConstante;
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

import java.math.BigDecimal;
import java.time.LocalTime;

/**
 * Une case de la grille des constantes (section 3.1) : une mesure par type et par creneau.
 * Pour la tension arterielle, valeur porte la systolique et valeurDiastolique la diastolique.
 * L'alerte n'est pas stockee : elle est recalculee par le service a chaque lecture.
 */
@Entity
@Table(name = "constante_visite")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConstanteVisite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "visite_id", nullable = false)
    private VisiteSuivi visite;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeConstante type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CreneauMesure creneau;

    @Column(nullable = false, precision = 7, scale = 2)
    private BigDecimal valeur;

    @Column(name = "valeur_diastolique", precision = 7, scale = 2)
    private BigDecimal valeurDiastolique;

    @Column(name = "heure_mesure")
    private LocalTime heureMesure;
}
