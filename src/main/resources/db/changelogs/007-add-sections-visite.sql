--liquibase formatted sql

-- Sections 2, 4, 5, 6, 7 et 8 de la Fiche 2 : champs scalaires de la visite et listes a
-- choix multiples (evenements intercurrents, themes d'education therapeutique).

--changeset Mody:007-1-add-etat-general-visite
ALTER TABLE visite_suivi ADD COLUMN etat_general VARCHAR(40);
ALTER TABLE visite_suivi ADD COLUMN etat_general_precision TEXT;
ALTER TABLE visite_suivi ADD COLUMN plainte_principale TEXT;
ALTER TABLE visite_suivi ADD COLUMN evenements_precision TEXT;

--changeset Mody:007-2-create-visite-evenement-intercurrent
CREATE TABLE visite_evenement_intercurrent
(
    visite_id UUID        NOT NULL,
    evenement VARCHAR(40) NOT NULL,
    CONSTRAINT visite_evenement_intercurrent_pkey PRIMARY KEY (visite_id, evenement),
    CONSTRAINT fk_evenement_intercurrent_visite FOREIGN KEY (visite_id) REFERENCES visite_suivi (id)
);

--changeset Mody:007-3-add-examen-clinique-visite
ALTER TABLE visite_suivi ADD COLUMN examen_cardiovasculaire TEXT;
ALTER TABLE visite_suivi ADD COLUMN examen_respiratoire TEXT;
ALTER TABLE visite_suivi ADD COLUMN examen_neurologique TEXT;
ALTER TABLE visite_suivi ADD COLUMN examen_digestif_autre TEXT;
ALTER TABLE visite_suivi ADD COLUMN examen_cutane TEXT;

--changeset Mody:007-4-add-observance-visite
ALTER TABLE visite_suivi ADD COLUMN observance_traitement VARCHAR(40);
ALTER TABLE visite_suivi ADD COLUMN raisons_non_observance TEXT;

--changeset Mody:007-5-add-education-therapeutique-visite
ALTER TABLE visite_suivi ADD COLUMN themes_autres_precision TEXT;
ALTER TABLE visite_suivi ADD COLUMN comprehension_patient VARCHAR(40);
ALTER TABLE visite_suivi ADD COLUMN statut_plan_prevention VARCHAR(40);

--changeset Mody:007-6-create-visite-theme-aborde
CREATE TABLE visite_theme_aborde
(
    visite_id UUID        NOT NULL,
    theme     VARCHAR(40) NOT NULL,
    CONSTRAINT visite_theme_aborde_pkey PRIMARY KEY (visite_id, theme),
    CONSTRAINT fk_theme_aborde_visite FOREIGN KEY (visite_id) REFERENCES visite_suivi (id)
);

--changeset Mody:007-7-add-bilan-social-visite
ALTER TABLE visite_suivi ADD COLUMN situation_sociale VARCHAR(40);
ALTER TABLE visite_suivi ADD COLUMN problemes_sociaux TEXT;
ALTER TABLE visite_suivi ADD COLUMN signalement_dass BOOLEAN;
ALTER TABLE visite_suivi ADD COLUMN heure_contact_dass TIME;

--changeset Mody:007-8-add-conclusion-visite
ALTER TABLE visite_suivi ADD COLUMN decision VARCHAR(40);
ALTER TABLE visite_suivi ADD COLUMN resume_clinique TEXT;
ALTER TABLE visite_suivi ADD COLUMN date_envoi_compte_rendu TIMESTAMP;
ALTER TABLE visite_suivi ADD COLUMN moyen_transmission VARCHAR(40);
ALTER TABLE visite_suivi ADD COLUMN prochaine_visite_date DATE;
