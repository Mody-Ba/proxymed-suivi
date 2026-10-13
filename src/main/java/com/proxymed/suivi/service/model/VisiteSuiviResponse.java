package com.proxymed.suivi.service.model;

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
import lombok.Builder;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Builder(toBuilder = true)
public record VisiteSuiviResponse(
        UUID id,
        Integer numeroVisite,
        StatutVisite statut,
        PatientResponse patient,
        LocalDate dateVisite,
        LocalTime heureArrivee,
        LocalTime heureDepart,
        String adresseVisite,
        MedecinResponse medecinJunior,
        MedecinResponse medecinSenior,
        String contactSenior,
        Boolean contactSeniorJoignable,
        TypeVisite typeVisite,

        // Section 3 : surveillance (alertes recalculees a chaque lecture)
        boolean alerteDeclenchee,
        List<AlerteResponse> alertes,
        List<LigneGrilleConstantesResponse> grilleConstantes,
        List<SigneAlerteClinique> signesAlerteCliniques,
        LocalTime heureAppelSenior,
        LocalTime heureReponseSenior,
        String decisionAppelSenior,

        // Section 2 : etat general
        EtatGeneral etatGeneral,
        String etatGeneralPrecision,
        String plaintePrincipale,
        List<EvenementIntercurrent> evenementsIntercurrents,
        String evenementsPrecision,

        // Section 4 : examen clinique cible
        String examenCardiovasculaire,
        String examenRespiratoire,
        String examenNeurologique,
        String examenDigestifAutre,
        String examenCutane,

        // Section 5 : observance et traitements
        ObservanceTraitement observanceTraitement,
        String raisonsNonObservance,
        List<TraitementVisiteResponse> traitements,

        // Section 6 : education therapeutique
        List<ThemeEducation> themesAbordes,
        String themesAutresPrecision,
        ComprehensionPatient comprehensionPatient,
        StatutPlanPrevention statutPlanPrevention,

        // Section 7 : bilan social et coordination DASS
        SituationSociale situationSociale,
        String problemesSociaux,
        Boolean signalementDass,
        LocalTime heureContactDass,

        // Section 8 : conclusion
        DecisionVisite decision,
        String resumeClinique,
        LocalDateTime dateEnvoiCompteRendu,
        MoyenTransmission moyenTransmission,
        LocalDate prochaineVisiteDate,

        // Section 9 : signatures et validation
        Instant dateSignatureJunior,
        Instant dateVisaSenior,
        Instant dateSignaturePatient,
        SignatairePatient signatairePatient,
        LocalDate dateSaisieDmi,
        String saisieDmiPar,

        // Delais informatifs (regle 11), recalcules a chaque lecture : compte rendu au senior
        // sous 12 h apres la fin de la visite, visa du senior sous 24 h apres la signature du junior
        LocalDateTime echeanceCompteRendu,
        Boolean compteRenduEnRetard,
        Instant echeanceVisa,
        Boolean visaEnRetard,

        Instant dateCreation,
        Instant dateMiseAJour
) {
}
