# Cahier des charges — Application PROXYMED
## Module 2 : Fiche 2 — Visite de suivi mensuel à domicile

---

## 0. Instructions pour Claude Code

- **Projet séparé.** Ce projet est un **nouveau dépôt Spring Boot**, distinct du projet PROXYMED existant (Fiche 1). Le projet 1 se trouve dans `~/Downloads/proxymed` : **lis-le comme référence** (pom.xml, structure des packages, conventions, changelogs Liquibase, `GlobalExceptionHandler`, tests, génération PDF) et **reprends exactement les mêmes versions, la même stack et les mêmes conventions**. Ne modifie jamais le projet 1.
- **Authentification : ne pas l'implémenter pour l'instant** (phase 2). Pas de Spring Security, JWT ni login.
- **Avancer par étapes**, selon la section 9. **S'arrêter après chaque étape** et attendre mon retour : je veux tester dans Swagger et comprendre le code avant de continuer. Ne pas enchaîner automatiquement.
- En fin d'étape : résumé court de ce qui a été fait, comment le lancer et le tester, et les **choix que tu as faits sans que je les aie demandés**.
- **Une branche git par étape** (`feature/...`), créée avant de coder. Ne commite pas toi-même sans que je le demande.
- **Ports** : l'application tourne sur le port **8082** (le 8080 est pris par un autre projet, le 8081 par PROXYMED 1) ; PostgreSQL dans Docker sur le port **5433** (le 5432 est pris), base `proxymed_suivi`, avec un `docker-compose.yml` à la racine.

---

## 1. Contexte et objectifs

Le programme **PROXYMED** encadre le suivi médical à domicile de patients. Après la consultation initiale d'éligibilité (Fiche 1, projet existant), chaque patient éligible reçoit une **visite de suivi mensuelle à domicile**, réalisée **exclusivement par un médecin JUNIOR sous supervision d'un médecin SÉNIOR**. Cette visite est documentée sur la **Fiche 2** (document source : `FICHE_2_de_suivi_medical.docx`, à lire en entier avant de commencer).

Ce projet digitalise la Fiche 2 sous forme d'**API backend Spring Boot** (pas de frontend ; test par Swagger) permettant :

- la saisie structurée d'une visite (9 sections de la fiche) ;
- le numéro de visite automatique (cumul par patient) et l'âge calculé ;
- la **détection automatique des seuils d'alerte** sur les constantes vitales (appel immédiat du sénior) ;
- le cycle de vie de la fiche avec les trois signatures (junior, sénior, patient/aidant) ;
- la génération d'un **PDF fidèle à la fiche avec QR code** (l'UUID de la visite), puis l'**import du PDF signé** rattaché à la visite.

**Doctrine du programme à respecter dans les règles métier** : en cas de situation complexe, d'aggravation ou de doute clinique, le junior doit contacter son sénior référent *avant* toute décision thérapeutique ou de référence. Tout ajustement thérapeutique doit être validé par le sénior. Les décisions de référence et d'hospitalisation relèvent du sénior.

---

## 2. Hypothèses à confirmer (décisions provisoires)

Ces points sont des choix par défaut, à signaler clairement et à rendre faciles à changer. Ils seront confirmés avec le superviseur (Prof. Fadel Touré) :

1. **Données de référence locales.** Ce projet est séparé et ne peut pas appeler le projet 1. Il garde donc ses **propres tables minimales `Patient` et `Medecin`** (voir section 4), avec un jeu de données de départ. Le lien avec le projet 1 se fait par le **`numeroDossierProxymed`** du patient. Pas de vérification « consultation initiale signée et éligible » dans ce projet.
2. **Cycle de vie de la fiche** : `BROUILLON` → `TERMINEE` (le junior termine et signe) → `VISEE` (le sénior appose son visa dans les 24 h). La signature du patient/aidant est enregistrée à part (date + qui a signé).
3. **Bornes de validité des constantes vitales** (valeurs physiologiquement impossibles à refuser) : **provisoires**, à valider médicalement. Elles sont distinctes des seuils d'alerte, qui viennent du document.
4. **Pas de pagination** dans cette V1.

---

## 3. Acteurs

| Rôle | Description |
|---|---|
| **Médecin JUNIOR** | Réalise la visite, remplit la fiche, la termine et la signe |
| **Médecin SÉNIOR référent** | Supervise, est appelé en cas d'alerte, appose son visa dans les 24 h |
| **Patient / aidant** | Signe ou appose son empreinte |

Les rôles servent à modéliser les données et à valider les affectations (un junior doit avoir le rôle JUNIOR, un sénior le rôle SENIOR), sans sécurité active en V1.

---

## 4. Stack et architecture

### 4.1 Stack

Identique au projet 1 (à relire dans son `pom.xml`) : Java, Spring Boot, Spring Data JPA, PostgreSQL, Liquibase (changelogs SQL dans `db/changelogs/` avec `changelog-master.yaml`, `ddl-auto=validate`), Swagger/springdoc, Lombok, ZXing (QR code), Thymeleaf, openhtmltopdf, Maven (`./mvnw`).

### 4.2 Architecture en couches (mêmes règles que le projet 1, à respecter strictement)

- **Controller** (Request/Response, `@Valid`) → **Service** (interface + `XxxServiceImpl`, travaille **uniquement avec des `XxxModel`**, jamais avec des entités) → **Repository** (`@Repository`, `JpaSpecificationExecutor` si recherche) → **Entity**.
- **Deux mappers** : `com.proxymed.suivi.mappers` (Request/Response ↔ Model) et `com.proxymed.suivi.service.mappers` (Model ↔ Entity).
- **Toute la logique métier est dans les services** (calculs, validations, règles, seuils d'alerte). Le contrôleur ne contient aucune logique. Les classes utilitaires (ex. `BornesConstantesVitales`, `SeuilsAlerte`, spécifications de recherche) ne sont pas des `@Service`.
- **Chaque service ne touche que son propre agrégat** : pour vérifier un patient ou un médecin, le service des visites passe par `PatientService` et `MedecinService`, jamais par leurs repositories.
- Les référentiels / listes de valeurs fixes sont des **enums** (voir section 5).
- `PUT` = **mise à jour partielle** ; les champs inconnus du JSON sont rejetés en 400 (`spring.jackson.deserialization.fail-on-unknown-properties=true`) ; les listes (constantes, traitements) ont des **endpoints dédiés**.
- Package racine : `com.proxymed.suivi`.

---

## 5. Modèle de données

### 5.1 `Patient` (minimal, local)
`id` UUID, `numeroDossierProxymed` (unique, obligatoire), `numeroDmi`, `nomComplet`, `dateNaissance`, `age` (calculé dans le service), `sexe` (M, F), `telephone`, `adresseDomicile`, `commune`, `quartier`.

### 5.2 `Medecin` (minimal, local)
`id`, `nom`, `prenom`, `numeroOrdre` (unique), `role` (SENIOR, JUNIOR), `telephone`.

### 5.3 `VisiteSuivi` (entité racine de la Fiche 2)

**Section 1 — Identification**
`id` UUID, `patient` (FK), `numeroVisite` (calculé), `dateVisite`, `heureArrivee`, `heureDepart`, `adresseVisite` (si différente du domicile), `medecinJunior` (FK), `medecinSenior` (FK), `contactSenior` (texte), `contactSeniorJoignable` (booléen), `typeVisite` (PROGRAMMEE, URGENTE, CONTROLE_POST_ALERTE), `statut` (BROUILLON, TERMINEE, VISEE), dates de création et de mise à jour.

**Section 2 — État général**
- `etatGeneral` : BON, ALTERE, URGENCE_APPEL_SENIOR ; `etatGeneralPrecision` (obligatoire si ALTERE).
- `plaintePrincipale` (texte).
- `evenementsIntercurrents` (liste de choix multiples) : HOSPITALISATION, CONSULTATION_URGENCE, CHUTE_ACCIDENT, AGGRAVATION_SYMPTOMES, CHANGEMENT_TRAITEMENT, NON_OBSERVANCE, PROBLEME_SOCIAL, AUCUN ; `evenementsPrecision` (texte).

**Section 3 — Surveillance (champs scalaires)**
- `signesAlerteCliniques` (liste de choix multiples) : SIGNE_NEUROLOGIQUE_FOCAL_AIGU, DETRESSE_RESPIRATOIRE, DOULEUR_THORACIQUE, AUTRE_SIGNE_CLINIQUE_GRAVE.
- `heureAppelSenior`, `heureReponseSenior`, `decisionAppelSenior` (texte).

**Section 4 — Examen clinique ciblé** (5 textes)
`examenCardiovasculaire` (pouls, OMI, TSVJ, bruits du cœur), `examenRespiratoire` (MV, crépitants, dyspnée), `examenNeurologique` (conscience, déficit, marche), `examenDigestifAutre`, `examenCutane` (plaie, escarre, site d'injection).

**Section 5 — Observance (scalaires)**
`observanceTraitement` : BONNE (≥ 80 % des prises), PARTIELLE (50–80 %), MAUVAISE (< 50 %), NON_EVALUABLE ; `raisonsNonObservance` (texte).

**Section 6 — Éducation thérapeutique**
- `themesAbordes` (liste de choix multiples) : ALIMENTATION_NUTRITION, ACTIVITE_PHYSIQUE_ADAPTEE, GESTION_MEDICAMENTS, AUTOSURVEILLANCE_TA_GLYCEMIE, HYGIENE_PREVENTION, SIGNES_ALERTE_A_SURVEILLER, SOUTIEN_PSYCHOLOGIQUE, AUTRES ; `themesAutresPrecision`.
- `comprehensionPatient` : BONNE, PARTIELLE, A_REEXPLIQUER.
- `statutPlanPrevention` (PPP) : MIS_A_JOUR, INCHANGE, A_REVISER_PAR_SENIOR.

**Section 7 — Bilan social et coordination DASS**
`situationSociale` : STABLE, DETERIOREE, AMELIOREE, URGENCE_SOCIALE ; `problemesSociaux` (texte) ; `signalementDass` (booléen) ; `heureContactDass`.

**Section 8 — Conclusion**
- `decision` : PATIENT_STABLE_SUIVI_PROGRAMME, SUIVI_RENFORCE, REFERENCE_MEDECIN_SENIOR, REFERENCE_EPS, HOSPITALISATION_SAMU, DECES_CONSTATE.
- `resumeClinique` (texte, transmis au sénior dans les 12 h), `dateEnvoiCompteRendu`, `moyenTransmission` (DMI, TELEPHONE, WHATSAPP_PRO), `prochaineVisiteDate`.

**Section 9 — Signatures et validation**
`dateSignatureJunior`, `dateVisaSenior`, `dateSignaturePatient`, `signatairePatient` (PATIENT, AIDANT), `dateSaisieDmi`, `saisieDmiPar`.

### 5.4 `ConstanteVisite` (grille de la section 3)
`id`, `visite` (FK), `type` (TA, FC, FR, SPO2, TEMPERATURE, GLYCEMIE, EVA, GLASGOW), `creneau` (H0, H_PLUS_1, H_PLUS_2, H_PLUS_3, H_PLUS_4), `valeur`, `valeurDiastolique` (**obligatoire pour TA seulement**, interdite pour les autres types ; `valeur` est alors la systolique), `heureMesure` (optionnelle). **Unicité** sur (visite, type, creneau).

### 5.5 `TraitementVisite` (tableau de la section 5)
`id`, `visite` (FK), `medicament`, `posologie`, `voie`, `observant` (booléen), `probleme` (booléen), `problemePrecision` (texte, optionnel).

> Les listes à choix multiples (événements, signes d'alerte cliniques, thèmes) sont stockées au choix avec `@ElementCollection` ou des tables de jointure, via Liquibase. Choisis la solution la plus simple et dis-moi laquelle.

---

## 6. Règles métier (toutes dans les services)

1. **Numéro de visite** = nombre de visites déjà enregistrées pour ce patient + 1. Jamais fourni par le client.
2. **Âge et sexe** du patient calculés ou repris depuis le patient, jamais saisis dans la visite.
3. Le patient doit exister (404). Le médecin junior doit avoir le rôle JUNIOR, le sénior le rôle SENIOR (sinon 400/409, avec le code d'erreur déjà utilisé dans le projet 1 pour les règles de gestion).
4. **Modification possible seulement en `BROUILLON`**. Une fiche `TERMINEE` ou `VISEE` est immuable (409).
5. **Seuils d'alerte automatiques** (recalculés à chaque lecture, jamais saisis). Une constante déclenche une alerte si :
   - TA : systolique > 180 ou diastolique > 110, ou systolique < 90 ou diastolique < 60
   - FC > 120 ou < 50 batt/min
   - SpO2 < 90 %
   - FR > 25 ou < 10 R/min
   - Température > 38,5 °C ou < 35 °C
   - Glycémie < 0,6 ou > 2,5 g/l
   - Glasgow < 13
   - Douleur EVA ≥ 8/10
   
   Le détail de la fiche renvoie `alerteDeclenchee` (booléen) et la **liste des alertes** (constante, créneau, valeur, seuil franchi). Les `signesAlerteCliniques` cochés déclenchent aussi une alerte.
6. **Valeurs normales** (affichées sur le PDF et renvoyées dans la grille) : TA < 140/90 ; FC 60–100 ; FR 12–20 ; SpO2 ≥ 95 % ; température 36–37,5 ; glycémie 0,7–1,1 g/l à jeun ; EVA cible < 3 ; Glasgow 15.
7. **Bornes de validité** des valeurs saisies (utilitaire `BornesConstantesVitales`, provisoires) : refuser les valeurs impossibles (400).
8. **Terminer** (`TERMINEE`) vérifie que les champs requis sont remplis : identification complète, heure de départ, état général, au moins les constantes du créneau H0, observance, situation sociale, décision, résumé clinique. De plus :
   - si une alerte est déclenchée, `heureAppelSenior` et `decisionAppelSenior` sont obligatoires ;
   - si `signalementDass` est vrai, `heureContactDass` est obligatoire ;
   - si `etatGeneral` = ALTERE, `etatGeneralPrecision` est obligatoire.
9. **Viser** (`VISEE`) : seulement depuis `TERMINEE`, enregistre `dateVisaSenior`.
10. La signature du patient/aidant s'enregistre sur une fiche `TERMINEE` ou `VISEE` (date + `signatairePatient`).
11. **Délais** (informatifs, étape 4) : résumé au sénior sous 12 h, visa du sénior sous 24 h. Un endpoint liste les fiches terminées en attente de visa.

---

## 7. API REST

| Méthode | URL | Rôle |
|---|---|---|
| POST | `/api/patients` | créer un patient (minimal) |
| GET | `/api/patients`, `/api/patients/{id}` | lister / lire |
| GET | `/api/medecins` | lister les médecins (filtre `role`) |
| POST | `/api/patients/{patientId}/visites` | créer une visite en BROUILLON |
| GET | `/api/patients/{patientId}/visites` | visites d'un patient, de la plus récente à la plus ancienne |
| GET | `/api/visites/{id}` | détail complet, avec âge, alertes et grille |
| PUT | `/api/visites/{id}` | mise à jour partielle des champs scalaires |
| POST / GET | `/api/visites/{id}/constantes` | enregistrer (remplace si même type + créneau) / lire la grille |
| DELETE | `/api/visites/{id}/constantes/{constanteId}` | supprimer une mesure |
| POST / PUT / DELETE / GET | `/api/visites/{id}/traitements[/{traitementId}]` | gérer la liste des traitements |
| POST | `/api/visites/{id}/terminer` | junior : terminer et signer |
| POST | `/api/visites/{id}/viser` | sénior : visa |
| POST | `/api/visites/{id}/signature-patient` | signature patient/aidant (corps : `signataire`) |
| GET | `/api/visites/en-attente-visa` | fiches terminées en attente de visa |
| GET | `/api/visites/{id}/pdf` | PDF de la fiche avec QR code (téléchargement direct, `attachment`) |
| POST / GET | `/api/visites/{id}/fiche-signee` | importer / télécharger le PDF signé |

Annoter les endpoints binaires avec `@ApiResponse` en `application/pdf`, `type: string, format: binary`, pour que le bouton Download de Swagger télécharge un vrai fichier.

---

## 8. PDF, QR code et import de la fiche signée

- **PDF** : Thymeleaf + openhtmltopdf, fidèle à la mise en page de la Fiche 2 (9 sections, tableau des constantes, cases à cocher cochées selon les données). Le **QR code encode uniquement l'UUID de la visite**. Réutilise l'approche du `PdfGenerationService` du projet 1.
- **Import** : mêmes règles que `fiche-signee` du projet 1 : le fichier doit être un vrai PDF (type de contenu **et** premiers octets `%PDF-`), taille maximale 10 Mo, stockage en `bytea`, métadonnées uniquement dans la réponse du POST, la visite doit exister (404). Dans cette V1 : plusieurs fichiers par visite possibles, le GET renvoie le plus récent.

---

## 9. Étapes de livraison (une seule à la fois, avec arrêt)

1. **Socle et visite (section 1)** : projet Spring Boot, `docker-compose.yml`, Liquibase, `Patient` et `Medecin` minimaux avec données de départ (au moins un sénior, un junior et trois patients), `VisiteSuivi` (section 1), numéro de visite automatique, endpoints de création, lecture, liste et `PUT` partiel. Tests unitaires et d'intégration.
2. **Constantes et alertes (section 3)** : grille `ConstanteVisite`, bornes de validité, seuils d'alerte automatiques, champs d'appel du sénior.
3. **Autres sections (2, 4, 5, 6, 7, 8)** : champs scalaires dans le `PUT`, listes à choix multiples, `TraitementVisite`.
4. **Cycle de vie et signatures (section 9)** : terminer, viser, signature patient, vérification des champs requis, liste en attente de visa.
5. **PDF avec QR code.**
6. **Import et téléchargement de la fiche signée.**

Pour chaque étape : tests unitaires des services (Mockito) et tests d'intégration des endpoints ; toute la suite doit rester verte.

---

## 10. Données de départ pour les tests (Liquibase, contexte dédié)

- Médecins : **Diop Aminata** (SENIOR), **Ousmane Ndiaye** (JUNIOR).
- Patients : au moins trois, dont **Abdoulaye Fall**, avec un `numeroDossierProxymed` au format `PXM-2026-000X`.

---

## 11. Points à vérifier avec le superviseur

1. Projet séparé : copie locale des patients et médecins, ou appel au projet 1 par son API ?
2. Cycle de vie : BROUILLON → TERMINEE → VISEE convient-il ? La signature du patient est-elle un simple oui/non daté ?
3. Qui a le droit de cocher les décisions de référence ou d'hospitalisation : le junior (après appel du sénior) ou seulement le sénior ?
4. Bornes de validité des constantes vitales, à valider médicalement.
5. Faut-il bloquer une visite si le patient n'a pas de consultation initiale éligible dans le projet 1 ?
