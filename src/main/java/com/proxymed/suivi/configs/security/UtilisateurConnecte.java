package com.proxymed.suivi.configs.security;

import java.util.List;
import java.util.UUID;

/**
 * Identite portee par le badge, placee dans le SecurityContext par AuthTokenFilter.
 * Construite uniquement a partir des claims : la Fiche 2 n'a aucune table d'utilisateurs.
 */
public record UtilisateurConnecte(
        UUID id,
        String email,
        List<String> roles
) {
}
