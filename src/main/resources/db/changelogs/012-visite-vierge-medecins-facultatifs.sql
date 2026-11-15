--liquibase formatted sql

-- Impression en lot de fiches vierges par l'admin : la visite est creee avant d'etre attribuee,
-- le medecin junior et le medecin senior sont donc renseignes plus tard (mise a jour de la visite).

--changeset Mody:012-1-medecin-junior-facultatif
ALTER TABLE visite_suivi ALTER COLUMN medecin_junior_id DROP NOT NULL;

--changeset Mody:012-2-medecin-senior-facultatif
ALTER TABLE visite_suivi ALTER COLUMN medecin_senior_id DROP NOT NULL;
