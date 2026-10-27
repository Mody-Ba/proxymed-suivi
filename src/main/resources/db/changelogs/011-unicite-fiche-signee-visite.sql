--liquibase formatted sql

-- Une visite n'a plus qu'une seule fiche signee. La contrainte d'unicite est le garde-fou
-- contre deux imports simultanes (le service verifie deja avant d'enregistrer).

--changeset Mody:011-1-suppression-doublons-fiche-signee
-- On garde la fiche la plus ancienne de chaque visite (a date egale, le plus petit id).
DELETE FROM fiche_signee f
WHERE EXISTS (SELECT 1
              FROM fiche_signee autre
              WHERE autre.visite_id = f.visite_id
                AND (autre.date_import < f.date_import
                    OR (autre.date_import = f.date_import AND autre.id < f.id)));

--changeset Mody:011-2-unicite-fiche-signee-visite
ALTER TABLE fiche_signee ADD CONSTRAINT uk_fiche_signee_visite UNIQUE (visite_id);

--changeset Mody:011-3-suppression-index-fiche-signee-visite
-- Redondant avec l'index cree par la contrainte d'unicite.
DROP INDEX idx_fiche_signee_visite;
