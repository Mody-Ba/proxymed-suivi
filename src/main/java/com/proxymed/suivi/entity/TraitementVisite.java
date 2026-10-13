package com.proxymed.suivi.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

/**
 * Une ligne du tableau des traitements en cours (section 5) : un medicament, sa posologie,
 * sa voie d'administration, et si le patient le prend bien ou rencontre un probleme.
 */
@Entity
@Table(name = "traitement_visite")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TraitementVisite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "visite_id", nullable = false)
    private VisiteSuivi visite;

    @Column(nullable = false)
    private String medicament;

    private String posologie;

    private String voie;

    private Boolean observant;

    private Boolean probleme;

    @Column(name = "probleme_precision", columnDefinition = "TEXT")
    private String problemePrecision;
}
