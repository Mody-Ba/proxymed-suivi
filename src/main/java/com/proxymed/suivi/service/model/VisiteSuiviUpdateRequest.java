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
import com.proxymed.suivi.enums.ThemeEducation;
import com.proxymed.suivi.enums.TypeVisite;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Mise a jour partielle des champs scalaires d'une visite en BROUILLON :
 * un champ absent (null) conserve sa valeur actuelle.
 * Une liste a choix multiples (signesAlerteCliniques, evenementsIntercurrents, themesAbordes),
 * si elle est envoyee, remplace toute la liste cochee (liste vide = tout decocher).
 * Les constantes et les traitements se gerent par leurs endpoints dedies
 * (/api/visites/{id}/constantes et /api/visites/{id}/traitements).
 */
@Builder(toBuilder = true)
public record VisiteSuiviUpdateRequest(
        LocalDate dateVisite,
        LocalTime heureArrivee,
        LocalTime heureDepart,
        String adresseVisite,
        Long medecinJuniorId,
        Long medecinSeniorId,
        String contactSenior,
        Boolean contactSeniorJoignable,
        TypeVisite typeVisite,

        // Section 3 : signes d'alerte cliniques et appel du medecin senior
        List<@NotNull(message = "valeur nulle interdite dans la liste") SigneAlerteClinique> signesAlerteCliniques,
        LocalTime heureAppelSenior,
        LocalTime heureReponseSenior,
        String decisionAppelSenior,

        // Section 2 : etat general
        EtatGeneral etatGeneral,
        String etatGeneralPrecision,
        String plaintePrincipale,
        List<@NotNull(message = "valeur nulle interdite dans la liste") EvenementIntercurrent> evenementsIntercurrents,
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

        // Section 6 : education therapeutique
        List<@NotNull(message = "valeur nulle interdite dans la liste") ThemeEducation> themesAbordes,
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

        // Section 9 : saisie dans le DMI (les signatures passent par leurs endpoints dedies)
        LocalDate dateSaisieDmi,
        @Size(max = 255, message = "255 caracteres au maximum")
        String saisieDmiPar
) {
}
