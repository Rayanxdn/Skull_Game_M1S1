# Architecture du projet Skull – Lot 1

Document rédigé par Zakraf Rania (tests et documentation). Il décrit l'architecture telle qu'elle existe à la fin du Lot 1 et ce qui a été vérifié lors des tests. Les points marqués « à confirmer » sont à valider avec l'auteur du code concerné.

## 1. Vue d'ensemble

L'application est découpée en **quatre services Spring Boot**, un par domaine, qui s'appuient sur une **base PostgreSQL** commune. Le tout est lancé par **Docker Compose**.

| Service | Rôle | Port hôte | Port conteneur |
|---|---|---|---|
| `player-service` | Utilisateurs, inscription, connexion, ban | 8081 | 8080 |
| `game-service` | Parties, règles du jeu, IA | 8082 | 8080 |
| `ladder-service` | Historique et classement | 8083 | 8080 |
| `market-service` | Marketplace d'apparences | 8084 | 8080 |
| `db` (PostgreSQL 16) | Persistance | 5432 | 5432 |

À la fin du Lot 1, **seul le `player-service` expose des endpoints métier**. Les trois autres services démarrent et répondent sur `/ping` (contrôle de vie), en attendant les Lots 2 et 3.

## 2. Infrastructure (Docker Compose)

- **Un Dockerfile par service**, construit avec Maven puis exécuté sur une image Java 21.
- **Base de données** : image `postgres:16`, base `skull`, données conservées dans le volume `db_data`.
- **Ordre de démarrage** : les services attendent que la base soit saine (`healthcheck` avec `pg_isready`, toutes les 5 secondes, 10 essais).
- **Configuration partagée** : un bloc `x-service-env` commun fournit à chaque service l'adresse de la base, les identifiants, la clé JWT et la durée de vie du token.
- **Variables d'environnement** (fichier `.env`, copié depuis `.env.example`, **jamais commité**) : `DB_USER`, `DB_PASSWORD`, `JWT_SECRET`, `JWT_EXPIRATION_MS` (3 600 000 ms, soit 1 heure, par défaut), et pour le `player-service` `ADMIN_EMAIL`, `ADMIN_PASSWORD`, `ADMIN_PSEUDO`.

Lancement :

```
cp .env.example .env
docker compose up --build -d
curl http://localhost:8081/ping
```

**Persistance vérifiée** (test T35) : après `docker compose down` puis `docker compose up -d` (sans l'option `-v`), les comptes créés existent toujours. L'option `-v` supprime le volume et donc toutes les données.

## 3. Organisation du code

- Package de base : `fr.miage.skull.<service>`.
- Découpage MVC : `controller`, `service`, `repository`, `model`, `dto`.
- Gestion des erreurs centralisée, réponses d'erreur au format `ProblemDetail` (champs `type`, `title`, `status`, `detail`, `instance`, `timestamp`).
- Documentation des endpoints par annotations OpenAPI (Swagger) dans le code.
- Build avec le wrapper Maven `./mvnw`.

## 4. Authentification et autorisations

- **Authentification par JWT** (Spring Security). Le token est obtenu par `POST /api/v1/auth/login` et envoyé ensuite dans l'en-tête `Authorization: Bearer <token>`.
- Le token contient l'email (`sub`), l'identifiant (`id`), les rôles (`roles`), la date d'émission (`iat`) et d'expiration (`exp`). Durée de vie observée : **1 heure** (test T13 : un token ancien est refusé).
- **Deux rôles** : `USER` et `ADMIN`. Un compte administrateur est créé au démarrage du `player-service` à partir de `ADMIN_EMAIL` et `ADMIN_PASSWORD` (test T24).
- **Règle de propriété** : un joueur ne peut lire, modifier ou supprimer que **son propre compte** ; l'administrateur peut agir sur tous les comptes (tests T15 à T17, T21).
- **Bannissement** : un compte banni ne peut plus se connecter (réponse 403, « Ce compte a été banni », test T29) et son token précédent est refusé (test T30).

## 5. Endpoints du `player-service` (observés)

| Méthode | Chemin | Accès |
|---|---|---|
| `POST` | `/api/v1/players/register` | public |
| `POST` | `/api/v1/auth/login` | public |
| `GET` | `/api/v1/players` | administrateur |
| `GET` | `/api/v1/players/{id}` | administrateur ou propriétaire |
| `PUT` | `/api/v1/players/{id}` | administrateur ou propriétaire |
| `DELETE` | `/api/v1/players/{id}` | administrateur ou propriétaire |
| `PUT` | `/api/v1/players/{id}/ban?banned=true\|false` | administrateur |
| `GET` | `/ping` | public |

Les endpoints d'administration propres au Membre 5 (demande de fusion #11) n'étaient pas encore dans `develop` au moment des tests : ils seront retestés après leur fusion.

## 6. Choix d'architecture

- **Un service par domaine**, comme le demande le sujet, pour que chaque membre travaille dans son dossier sans conflit.
- **PostgreSQL** pour la persistance exigée par le sujet, avec un volume Docker pour survivre aux redémarrages.
- **JWT sans session côté serveur** : chaque requête porte son identité, ce qui convient à plusieurs services.
- **Docker Compose** pour que n'importe quel membre lance tout le projet avec les mêmes commandes.
- **Secrets hors du code** : les mots de passe et la clé JWT viennent du fichier `.env`, ignoré par Git.

## 7. Organisation du travail (Git)

- `main` : versions livrées, une par lot, avec un tag.
- `develop` : intégration.
- `feature/lotX-nom` : une branche par tâche, créée depuis `develop`.
- Aucun envoi direct sur `main` ni sur `develop` : passage par une Pull Request relue par un autre membre.
- Messages de commit du type `feat(player): ...`, `fix(game): ...`, `docs: ...`.

## 8. Qualité et tests du Lot 1

- **Qualification : tests fonctionnels** (comportement observé de l'extérieur avec Postman, sans lire le code).
- **35 scénarios exécutés : 32 OK, 3 KO.** Détail et preuves : `docs/Plan_de_tests_Lot1_Resultats__.xlsx` et `docs/preuves/`.
- **4 anomalies signalées** :
  - #14 : faute dans le titre des erreurs de validation (« Erruer »).
  - #15, #16, #18 : un joueur sans droit, ou banni, reçoit **401 au lieu de 403** sur les routes réservées (liste des joueurs, ban, token d'un joueur banni). L'accès est bien refusé, seul le code diffère.

## 9. Points à confirmer

- Cause du 401 à la place du 403 sur les routes réservées à l'administrateur (configuration de la sécurité, à confirmer avec le Membre 3).
- Endpoints du Membre 5 (ban et administration) : à retester après fusion de la demande #11.
- Version de Spring Boot : celle du `player-service` avait été modifiée par rapport aux autres services (remarque du 14/10) ; à harmoniser si besoin.
