--liquibase formatted sql

-- Section 3 de la Fiche 2 : signes d'alerte cliniques coches et appel du medecin senior.

--changeset Mody:006-1-add-appel-senior-visite
ALTER TABLE visite_suivi ADD COLUMN heure_appel_senior TIME;
ALTER TABLE visite_suivi ADD COLUMN heure_reponse_senior TIME;
ALTER TABLE visite_suivi ADD COLUMN decision_appel_senior TEXT;

--changeset Mody:006-2-create-visite-signe-alerte-clinique
CREATE TABLE visite_signe_alerte_clinique
(
    visite_id UUID        NOT NULL,
    signe     VARCHAR(40) NOT NULL,
    CONSTRAINT visite_signe_alerte_clinique_pkey PRIMARY KEY (visite_id, signe),
    CONSTRAINT fk_signe_alerte_visite FOREIGN KEY (visite_id) REFERENCES visite_suivi (id)
);
