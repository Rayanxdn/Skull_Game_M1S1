# Lot 1 – Descriptif et qualification des tests

Projet Skull – M1 MIAGE. Document rédigé par Zakraf Rania (tests et documentation).
Date de livraison prévue : **30 octobre 2026**.

## 1. Descriptif fonctionnel du lot

Le Lot 1 met en place les fondations du projet et la gestion des comptes :

- **Socle technique** : dépôt Git, quatre services Spring Boot (player, game, ladder, market), base PostgreSQL, environnement Docker Compose.
- **Gestion des utilisateurs** : inscription, connexion avec jeton JWT, consultation, modification et suppression de son compte.
- **Sécurité** : Spring Security et JWT, deux rôles (joueur et administrateur), un joueur n'agit que sur son propre compte, compte administrateur créé au démarrage.
- **Bannissement** : l'administrateur peut bannir et débannir un joueur ; un joueur banni ne peut plus se connecter.
- **Interface** : pages d'inscription, de connexion et tableau de bord (Membre 4).
- **Tests et documentation** : plan de tests, rapport de tests, document d'architecture (Membre 6).

À la fin du Lot 1, seul le `player-service` expose des endpoints métier. Les trois autres services répondent sur `/ping`.

## 2. Qualification du lot

**Tests fonctionnels** (boîte noire).

Chaque scénario envoie une requête HTTP à l'application lancée avec Docker et compare la réponse (code HTTP et message) à ce qui est attendu, sans lire le code source. Les tests ont été exécutés **manuellement avec Postman** ; ils ne sont pas automatisés.

Des tests d'intégration sur la sécurité (JWT, rôles, propriété, ban) existent aussi dans le code du projet, écrits par le Membre 3. Ils n'ont pas été exécutés dans le cadre de ce rapport.

## 3. Plan de tests

35 scénarios, répartis en 7 domaines :

| Domaine | Scénarios |
|---|---|
| Inscription | 5 |
| Connexion | 4 |
| Sécurité JWT | 5 |
| Permissions | 9 |
| Administration | 2 |
| Bannissement | 8 |
| Infrastructure | 2 |

Chaque scénario précise la requête, le résultat et le code HTTP attendus. Le détail est dans `docs/Plan_de_tests_Lot1_Resultats__.xlsx`.

## 4. Environnement de test

- Application lancée en local avec `docker compose up --build -d`, sur la branche `develop`.
- Outil : Postman. Services interrogés sur les ports 8081 (`player-service`) à 8084.
- Comptes de test créés pour l'occasion (deux joueurs) et compte administrateur du projet.
- Une capture d'écran par scénario est conservée dans `docs/preuves/` (nom du fichier : numéro du test, intitulé, code HTTP obtenu).

## 5. Résultats

- **35 scénarios exécutés : 32 OK, 3 KO.**
- Le service répond correctement aux cas nominaux et aux erreurs de saisie (400, 401, 404, 409).
- Les accès non autorisés sont refusés : aucun accès à un compte tiers, aucun ban par un simple joueur, aucune connexion d'un joueur banni.
- Les données sont **persistantes** après redémarrage des conteneurs (test T35).

## 6. Anomalies

| N° | Résumé | Gravité | Issue GitHub |
|---|---|---|---|
| A01 | Faute dans le titre des erreurs de validation (« Erruer ») | Faible | #14 |
| A02 | Liste des joueurs : 401 au lieu de 403 pour un joueur connecté | Moyenne | #15 |
| A03 | Ban par un joueur : 401 au lieu de 403 | Moyenne | #16 |
| A04 | Joueur banni avec un token valide : 401 au lieu de 403 | Moyenne | #18 |

Dans les cas A02 à A04, l'accès est bien refusé ; seul le code de réponse diffère de ce qui était prévu.

## 7. Limites et suites

- Les endpoints d'administration du Membre 5 (demande de fusion #11) n'étaient pas encore dans `develop` lors des tests ; ils seront testés après leur fusion.
- Le front-end n'a pas été testé dans ce rapport.
- Après correction des anomalies, les tests concernés (T03, T04, T05, T09, T20, T27, T30) seront rejoués et le rapport mis à jour.
- Les tests ne sont pas automatisés ; une automatisation (collection Postman exécutable ou tests JUnit) pourra être envisagée pour les lots suivants.
