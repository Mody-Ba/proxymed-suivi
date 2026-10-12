--liquibase formatted sql

-- Les ids des medecins ne sont pas fixes : on laisse l'identity les generer
-- (sinon la sequence reste a 1 et les insertions suivantes echouent en doublon de cle).
-- Les UUID des patients sont fixes pour pouvoir les utiliser directement dans Swagger ;
-- les numeros de dossier sont ceux du projet PROXYMED 1 (lien entre les deux projets).

--changeset Mody:004-1-insert-medecins-test context:test-data
INSERT INTO medecin (nom, prenom, numero_ordre, role, telephone) VALUES
    ('Diop', 'Aminata', 'ONMS-2011-0457', 'SENIOR', '+221 77 640 12 30'),
    ('Ndiaye', 'Ousmane', 'ONMS-2022-1893', 'JUNIOR', '+221 76 310 45 78');

--changeset Mody:004-2-insert-patients-test context:test-data
INSERT INTO patient (id, numero_dossier_proxymed, numero_dmi, nom_complet, date_naissance, sexe, telephone,
                     adresse_domicile, commune, quartier) VALUES
    ('11111111-1111-1111-1111-111111111111', 'PXM-2026-0001', 'DMI-458712', 'Fatou Sow', '1954-03-12', 'F',
     '+221 77 512 34 56', 'Villa 124, Cité Soprim', 'Parcelles Assainies', 'Unité 15'),
    ('22222222-2222-2222-2222-222222222222', 'PXM-2026-0002', 'DMI-458713', 'Abdoulaye Fall', '1968-11-27', 'M',
     '+221 76 221 45 67', 'Rue 10 x 13, Médina', 'Dakar Plateau', 'Médina'),
    ('33333333-3333-3333-3333-333333333333', 'PXM-2026-0003', NULL, 'Mariama Ba', '1991-07-04', 'F',
     '+221 70 345 67 89', 'Quartier Thiaroye Azur', 'Pikine', 'Thiaroye');
