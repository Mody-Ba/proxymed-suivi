package com.proxymed.suivi.entity;

import com.proxymed.suivi.enums.ComprehensionPatient;
import com.proxymed.suivi.enums.DecisionVisite;
import com.proxymed.suivi.enums.EtatGeneral;
import com.proxymed.suivi.enums.EvenementIntercurrent;
import com.proxymed.suivi.enums.MoyenTransmission;
import com.proxymed.suivi.enums.ObservanceTraitement;
import com.proxymed.suivi.enums.SignatairePatient;
import com.proxymed.suivi.enums.SigneAlerteClinique;
import com.proxymed.suivi.enums.SituationSociale;
import com.proxymed.suivi.enums.StatutPlanPrevention;
import com.proxymed.suivi.enums.StatutVisite;
import com.proxymed.suivi.enums.ThemeEducation;
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
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Entite racine de la Fiche 2 (visite de suivi mensuel a domicile).
 * Porte les 9 sections de la fiche.
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

    // Section 2 : etat general
    @Enumerated(EnumType.STRING)
    @Column(name = "etat_general")
    private EtatGeneral etatGeneral;

    @Column(name = "etat_general_precision", columnDefinition = "TEXT")
    private String etatGeneralPrecision;

    @Column(name = "plainte_principale", columnDefinition = "TEXT")
    private String plaintePrincipale;

    @ElementCollection
    @CollectionTable(name = "visite_evenement_intercurrent", joinColumns = @JoinColumn(name = "visite_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "evenement", nullable = false)
    @Builder.Default
    private Set<EvenementIntercurrent> evenementsIntercurrents = new HashSet<>();

    @Column(name = "evenements_precision", columnDefinition = "TEXT")
    private String evenementsPrecision;

    // Section 4 : examen clinique cible
    @Column(name = "examen_cardiovasculaire", columnDefinition = "TEXT")
    private String examenCardiovasculaire;

    @Column(name = "examen_respiratoire", columnDefinition = "TEXT")
    private String examenRespiratoire;

    @Column(name = "examen_neurologique", columnDefinition = "TEXT")
    private String examenNeurologique;

    @Column(name = "examen_digestif_autre", columnDefinition = "TEXT")
    private String examenDigestifAutre;

    @Column(name = "examen_cutane", columnDefinition = "TEXT")
    private String examenCutane;

    // Section 5 : observance et traitements
    @Enumerated(EnumType.STRING)
    @Column(name = "observance_traitement")
    private ObservanceTraitement observanceTraitement;

    @Column(name = "raisons_non_observance", columnDefinition = "TEXT")
    private String raisonsNonObservance;

    /**
     * Tableau des traitements (section 5). Les lignes sont creees, modifiees et supprimees par
     * TraitementVisiteService ; cette collection ne sert qu'a les relire avec la visite.
     */
    @OneToMany(mappedBy = "visite")
    @Builder.Default
    private List<TraitementVisite> traitements = new ArrayList<>();

    // Section 6 : education therapeutique
    @ElementCollection
    @CollectionTable(name = "visite_theme_aborde", joinColumns = @JoinColumn(name = "visite_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "theme", nullable = false)
    @Builder.Default
    private Set<ThemeEducation> themesAbordes = new HashSet<>();

    @Column(name = "themes_autres_precision", columnDefinition = "TEXT")
    private String themesAutresPrecision;

    @Enumerated(EnumType.STRING)
    @Column(name = "comprehension_patient")
    private ComprehensionPatient comprehensionPatient;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut_plan_prevention")
    private StatutPlanPrevention statutPlanPrevention;

    // Section 7 : bilan social et coordination DASS
    @Enumerated(EnumType.STRING)
    @Column(name = "situation_sociale")
    private SituationSociale situationSociale;

    @Column(name = "problemes_sociaux", columnDefinition = "TEXT")
    private String problemesSociaux;

    @Column(name = "signalement_dass")
    private Boolean signalementDass;

    @Column(name = "heure_contact_dass")
    private LocalTime heureContactDass;

    // Section 8 : conclusion
    @Enumerated(EnumType.STRING)
    private DecisionVisite decision;

    @Column(name = "resume_clinique", columnDefinition = "TEXT")
    private String resumeClinique;

    @Column(name = "date_envoi_compte_rendu")
    private LocalDateTime dateEnvoiCompteRendu;

    @Enumerated(EnumType.STRING)
    @Column(name = "moyen_transmission")
    private MoyenTransmission moyenTransmission;

    @Column(name = "prochaine_visite_date")
    private LocalDate prochaineVisiteDate;

    // Section 9 : signatures et validation
    @Column(name = "date_signature_junior")
    private Instant dateSignatureJunior;

    @Column(name = "date_visa_senior")
    private Instant dateVisaSenior;

    @Column(name = "date_signature_patient")
    private Instant dateSignaturePatient;

    @Enumerated(EnumType.STRING)
    @Column(name = "signataire_patient")
    private SignatairePatient signatairePatient;

    @Column(name = "date_saisie_dmi")
    private LocalDate dateSaisieDmi;

    @Column(name = "saisie_dmi_par")
    private String saisieDmiPar;

    @Column(name = "date_creation", nullable = false)
    private Instant dateCreation;

    @Column(name = "date_mise_a_jour", nullable = false)
    private Instant dateMiseAJour;
}
