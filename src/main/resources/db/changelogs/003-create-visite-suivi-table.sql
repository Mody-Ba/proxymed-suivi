--liquibase formatted sql

-- Section 1 de la Fiche 2 (identification de la visite). Les colonnes des sections 2 a 9
-- seront ajoutees par les changelogs des etapes suivantes.

--changeset Mody:003-1-create-visite-suivi
CREATE TABLE visite_suivi
(
    id                       UUID                     NOT NULL,
    patient_id               UUID                     NOT NULL,
    numero_visite            INTEGER                  NOT NULL,
    date_visite              DATE,
    heure_arrivee            TIME,
    heure_depart             TIME,
    adresse_visite           VARCHAR(255),
    medecin_junior_id        BIGINT                   NOT NULL,
    medecin_senior_id        BIGINT                   NOT NULL,
    contact_senior           VARCHAR(255),
    contact_senior_joignable BOOLEAN,
    type_visite              VARCHAR(30),
    statut                   VARCHAR(20)              NOT NULL DEFAULT 'BROUILLON',
    date_creation            TIMESTAMP WITH TIME ZONE NOT NULL,
    date_mise_a_jour         TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT visite_suivi_pkey PRIMARY KEY (id),
    CONSTRAINT fk_visite_patient FOREIGN KEY (patient_id) REFERENCES patient (id),
    CONSTRAINT fk_visite_medecin_junior FOREIGN KEY (medecin_junior_id) REFERENCES medecin (id),
    CONSTRAINT fk_visite_medecin_senior FOREIGN KEY (medecin_senior_id) REFERENCES medecin (id),
    -- Le numero de visite est un cumul par patient : deux visites du meme patient ne peuvent
    -- pas porter le meme numero (garde-fou en cas de creations simultanees).
    CONSTRAINT visite_suivi_patient_numero_key UNIQUE (patient_id, numero_visite)
);

--changeset Mody:003-2-index-visite-statut
CREATE INDEX idx_visite_suivi_statut ON visite_suivi (statut);
