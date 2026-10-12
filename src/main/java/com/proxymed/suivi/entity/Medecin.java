package com.proxymed.suivi.entity;

import com.proxymed.suivi.enums.RoleMedecin;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Copie locale minimale du medecin (hypothese provisoire, cahier des charges section 2).
 * Le lien vers un compte utilisateur (authentification) sera ajoute en phase 2.
 */
@Entity
@Table(name = "medecin")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Medecin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false)
    private String prenom;

    @Column(name = "numero_ordre", nullable = false, unique = true)
    private String numeroOrdre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoleMedecin role;

    private String telephone;
}
