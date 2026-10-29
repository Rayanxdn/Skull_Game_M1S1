# Lot 1 – Descriptif et qualification des tests

Projet Skull – M1 MIAGE. Document rédigé par Zakraf Rania (tests et documentation).
Date de livraison prévue : **30 octobre 2026**.

## 1. Descriptif fonctionnel du lot

Le Lot 1 met en place les fondations du projet et la gestion des comptes :

- **Socle technique** : dépôt Git, quatre services Spring Boot (player, game, ladder, market), base PostgreSQL, environnement Docker Compose.
- **Gestion des utilisateurs** : inscription, connexion avec jeton JWT, consultation, modification et suppression de son compte.
- **Sécurité** : Spring Security et JWT, deux rôles (joueur et administrateur), un joueur n'agit que sur son propre compte, compte administrateur créé au démarrage.
- **Bannissement** : l'administrateur peut bannir et débannir un joueur ; un joueur banni ne peut plus se connecter.
- **Interface web** (Membre 4) : pages d'inscription, de connexion, tableau de bord (statistiques, parties et historique simulés en attendant le `ladder-service`) et page d'administration (bannir et débannir un joueur), avec déconnexion.
- **Documentation de l'API** : configuration OpenAPI (Swagger) dans le `player-service`.
- **Tests et documentation** : plan de tests, rapport de tests, document d'architecture (Membre 6).

À la fin du Lot 1, seul le `player-service` expose des endpoints métier. Les trois autres services répondent sur `/ping`.

## 2. Qualification du lot

### Qualification fonctionnelle (boîte noire)

Chaque scénario envoie une requête à l'application lancée avec Docker, ou utilise l'interface dans le navigateur, et compare le résultat à ce qui est attendu, sans lire le code source. Les tests ont été exécutés **manuellement** : avec Postman pour l'API (35 scénarios) et avec Safari pour le front-end (16 scénarios).

### Qualification structurelle (tests automatiques)

Le projet contient **44 tests automatiques** : 22 pour le `player-service` (dont des tests d'intégration sur la sécurité : JWT, rôles, propriété, ban) et 22 pour le front-end. Ils ont été exécutés avec `./mvnw test` le **29/10/2026 par Rania Zakraf** : **44 tests, tous verts**. Les captures sont conservées dans `docs/preuves/` (`TA01_tests_auto_player_service_22_verts.png` et `TA02_tests_auto_frontend_22_verts.png`).

## 3. Plan de tests

**51 scénarios**, répartis en 9 domaines :

| Domaine | Scénarios |
|---|---|
| Inscription | 11 |
| Connexion | 7 |
| Sécurité JWT | 5 |
| Permissions | 9 |
| Admin (API) | 2 |
| Bannissement | 8 |
| Infrastructure | 2 |
| Tableau de bord (front) | 4 |
| Administration (front) | 3 |

Les scénarios T01 à T35 portent sur l'API, les scénarios T36 à T51 sur le front-end. Chaque scénario précise l'action, le résultat attendu, le résultat obtenu et le nom de sa capture. Le détail est dans `docs/Plan_de_tests_Lot1_Complet.xlsx`.

## 4. Environnement de test

- Application lancée en local avec `docker compose up --build -d`, sur le code de `develop` à jour au 29/10/2026.
- Outils : Postman pour l'API (services sur les ports 8081 à 8084), Safari pour le front-end (`http://localhost:8080/`).
- Comptes de test créés pour l'occasion (joueurs `rania_test`, `rania_test2`, `rania_front`) et compte administrateur du projet.
- Une capture d'écran par scénario est conservée dans `docs/preuves/` (nom du fichier : numéro du test, intitulé, code obtenu). Les captures des retests portent le suffixe `_retest`.

## 5. Résultats

- **51 scénarios exécutés : 51 OK, 0 KO.**
- Le service répond correctement aux cas nominaux et aux erreurs de saisie (400, 401, 403, 404, 409).
- Les accès non autorisés sont refusés : aucun accès à un compte tiers, aucun ban par un simple joueur, aucune connexion d'un joueur banni.
- Les données sont **persistantes** après redémarrage des conteneurs (test T35).
- Le front-end affiche des messages clairs pour chaque erreur (email invalide, mot de passe court, champs vides, email déjà utilisé, confirmation différente, mauvais identifiants, compte banni) et protège les pages réservées.

## 6. Anomalies

| N° | Résumé | Gravité | Issue | État |
|---|---|---|---|---|
| A01 | Faute dans le titre des erreurs de validation (« Erruer ») | Faible | #14 | Corrigée le 28/10, retestée le 29/10 |
| A02 | Liste des joueurs : 401 au lieu de 403 pour un joueur connecté | Moyenne | #15 | Corrigée le 28/10, retestée le 29/10 |
| A03 | Ban par un joueur : 401 au lieu de 403 | Moyenne | #16 | Corrigée le 28/10, retestée le 29/10 |
| A04 | Joueur banni avec un token valide : 401 au lieu de 403 | Moyenne | #18 | Corrigée le 28/10, retestée le 29/10 |
| A05 | Inscription, formulaire vide : deux messages cumulés sous le mot de passe | Faible | #20 | Ouverte |
| A06 | Le bouton « Précédent » réaffiche temporairement le tableau de bord après la déconnexion | Faible | #21 | Ouverte |

Pour A06, le serveur redirige vers la connexion dès la première interaction avec la page : il s'agit d'un affichage conservé en mémoire par le navigateur, sans accès réel aux données.

## 7. Limites et suites

- Les statistiques, parties ouvertes et historique du tableau de bord sont **simulés** ; ils ne sont pas testés fonctionnellement.
- Les services `game`, `ladder` et `market` n'ont pas encore d'endpoints métier : seul `/ping` est testé.
- Les tests fonctionnels sont manuels ; une automatisation (collection Postman exécutable) pourra être envisagée pour les lots suivants.
- Les anomalies A05 et A06 restent à corriger ; leurs tests (T39 et T48) seront rejoués après correction.
