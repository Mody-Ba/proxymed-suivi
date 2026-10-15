--liquibase formatted sql

-- Fiche 2 remplie et signee (PDF scanne) importee et rattachee a une visite. Plusieurs
-- fichiers par visite sont possibles : le telechargement renvoie le plus recent.

--changeset Mody:010-1-create-fiche-signee
CREATE TABLE fiche_signee
(
    id            UUID                     NOT NULL,
    visite_id     UUID                     NOT NULL,
    nom_fichier   VARCHAR(255)             NOT NULL,
    type_contenu  VARCHAR(100)             NOT NULL,
    taille_octets BIGINT                   NOT NULL,
    date_import   TIMESTAMP WITH TIME ZONE NOT NULL,
    contenu       BYTEA                    NOT NULL,
    CONSTRAINT fiche_signee_pkey PRIMARY KEY (id),
    CONSTRAINT fk_fiche_signee_visite FOREIGN KEY (visite_id) REFERENCES visite_suivi (id)
);

--changeset Mody:010-2-index-fiche-signee
CREATE INDEX idx_fiche_signee_visite ON fiche_signee (visite_id);
