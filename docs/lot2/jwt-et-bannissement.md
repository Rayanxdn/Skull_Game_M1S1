# JWT du game-service et joueur banni (Lot 2)

## Fonctionnement

1. Le joueur se connecte au `player-service` (`POST /api/v1/auth/login`), qui renvoie un jeton JWT signé avec `JWT_SECRET`.
2. Le jeton contient l'email (`sub`), l'identifiant (`id`), les rôles (`roles`), la date de création (`iat`) et d'expiration (`exp`).
3. Le client envoie `Authorization: Bearer <jeton>` au `game-service`.
4. Le filtre `JwtAuthFilter` vérifie la signature (même `JWT_SECRET` que le `player-service`) et l'expiration, puis place un `AuthPlayer` (id, email, rôles) dans le contexte de sécurité.
5. Les contrôleurs lisent l'identité du joueur avec `@AuthenticationPrincipal AuthPlayer`, jamais depuis un paramètre envoyé par le client.

Réponses : 401 sans jeton valide (absent, falsifié, expiré), 403 pour un joueur connecté qui n'a pas le droit. Seuls `/ping` et Swagger sont publics.

## Limite connue : le joueur banni

Dans le `player-service`, le joueur est rechargé en base à chaque requête : un joueur banni est refusé tout de suite (403).

Le `game-service` ne consulte pas le `player-service` : il fait confiance au jeton. Un joueur banni **garde donc un jeton valide dans le game-service jusqu'à son expiration** (1 heure par défaut, `JWT_EXPIRATION_MS`), et peut encore y jouer pendant ce temps.

## Décision

Limite acceptée pour le Lot 2 : c'est le compromis d'une authentification sans état (pas d'appel entre services à chaque requête), et la durée du jeton la borne à 1 heure. Pistes si on voulait la lever : durée de jeton plus courte, ou vérification du statut auprès du `player-service` à chaque action de jeu (au prix d'une dépendance entre services).
