package com.proxymed.suivi.service.model;

import com.proxymed.suivi.enums.ComprehensionPatient;
import com.proxymed.suivi.enums.DecisionVisite;
import com.proxymed.suivi.enums.EtatGeneral;
import com.proxymed.suivi.enums.EvenementIntercurrent;
import com.proxymed.suivi.enums.MoyenTransmission;
import com.proxymed.suivi.enums.ObservanceTraitement;
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

/**
 * Couche interne neutre : ni le contrat API (VisiteSuiviCreateRequest/UpdateRequest/Response),
 * ni l'entite JPA. Pour une intention de creation ou de mise a jour, patient et medecins ne
 * portent que leur id ; sur le chemin de lecture ils sont complets.
 * grilleConstantes, alerteDeclenchee et alertes ne sont jamais persistes : le service les
 * recalcule a chaque lecture a partir de constantes et de signesAlerteCliniques.
 */
@Builder(toBuilder = true)
public record VisiteSuiviModel(
        UUID id,
        PatientModel patient,
        Integer numeroVisite,
        LocalDate dateVisite,
        LocalTime heureArrivee,
        LocalTime heureDepart,
        String adresseVisite,
        MedecinModel medecinJunior,
        MedecinModel medecinSenior,
        String contactSenior,
        Boolean contactSeniorJoignable,
        TypeVisite typeVisite,
        StatutVisite statut,

        // Section 3 : surveillance
        List<ConstanteVisiteModel> constantes,
        List<LigneGrilleConstantesModel> grilleConstantes,
        List<SigneAlerteClinique> signesAlerteCliniques,
        Boolean alerteDeclenchee,
        List<AlerteModel> alertes,
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
        List<TraitementVisiteModel> traitements,

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

        Instant dateCreation,
        Instant dateMiseAJour
) {
}
