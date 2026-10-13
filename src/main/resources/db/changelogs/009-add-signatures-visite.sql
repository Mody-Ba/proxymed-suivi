--liquibase formatted sql

-- Section 9 de la Fiche 2 : signature du medecin junior (fiche terminee), visa du medecin
-- senior, signature du patient ou de l'aidant, et saisie dans le DMI.

--changeset Mody:009-1-add-signatures-visite
ALTER TABLE visite_suivi ADD COLUMN date_signature_junior TIMESTAMP WITH TIME ZONE;
ALTER TABLE visite_suivi ADD COLUMN date_visa_senior TIMESTAMP WITH TIME ZONE;
ALTER TABLE visite_suivi ADD COLUMN date_signature_patient TIMESTAMP WITH TIME ZONE;
ALTER TABLE visite_suivi ADD COLUMN signataire_patient VARCHAR(20);
ALTER TABLE visite_suivi ADD COLUMN date_saisie_dmi DATE;
ALTER TABLE visite_suivi ADD COLUMN saisie_dmi_par VARCHAR(255);
