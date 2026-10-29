# Architecture du projet Skull – Lot 1

Document rédigé par Zakraf Rania (tests et documentation). Il décrit l'architecture telle qu'elle existe à la fin du Lot 1 et ce qui a été vérifié lors des tests. Les points marqués « à confirmer » sont à valider avec l'auteur du code concerné.

## 1. Vue d'ensemble

L'application est découpée en **quatre services Spring Boot**, un par domaine, qui s'appuient sur une **base PostgreSQL** commune, et d'une **interface web** (front-end). Le tout est lancé par **Docker Compose**.

| Service | Rôle | Port hôte | Port conteneur |
|---|---|---|---|
| `frontend` | Interface web : inscription, connexion, tableau de bord, administration | 8080 | 8080 |
| `player-service` | Utilisateurs, inscription, connexion, ban | 8081 | 8080 |
| `game-service` | Parties, règles du jeu, IA | 8082 | 8080 |
| `ladder-service` | Historique et classement | 8083 | 8080 |
| `market-service` | Marketplace d'apparences | 8084 | 8080 |
| `db` (PostgreSQL 16) | Persistance | 5432 | 5432 |

À la fin du Lot 1, **seul le `player-service` expose des endpoints métier**. Les trois autres services démarrent et répondent sur `/ping` (contrôle de vie), en attendant les Lots 2 et 3. Le front-end s'appuie uniquement sur le `player-service`.

## 2. Infrastructure (Docker Compose)

- **Un Dockerfile par service**, y compris le front-end, construit avec Maven puis exécuté sur une image Java 21.
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

Le front-end est ensuite accessible sur `http://localhost:8080/`.

**Persistance vérifiée** (test T35) : après `docker compose down` puis `docker compose up -d` (sans l'option `-v`), les comptes créés existent toujours. L'option `-v` supprime le volume et donc toutes les données.

## 3. Organisation du code

- Package de base : `fr.miage.skull.<service>` (pour le front-end : `fr.miage.skull.frontend`).
- Découpage MVC : `controller`, `service`, `repository`, `model`, `dto`.
- Gestion des erreurs centralisée, réponses d'erreur au format `ProblemDetail` (champs `type`, `title`, `status`, `detail`, `instance`, `timestamp`).
- **Documentation OpenAPI (Swagger)** : le `player-service` contient une classe de configuration `OpenApiConfig` et des annotations sur ses endpoints ; l'adresse exacte de l'interface Swagger est à confirmer avec l'auteur.
- Build avec le wrapper Maven `./mvnw`.

## 4. Le front-end

Le front-end est une application **Spring Boot** distincte, qui génère des pages web côté serveur et appelle l'API du `player-service` (classe `PlayerApiClient`).

| Page | Rôle |
|---|---|
| Inscription | Pseudo, email, mot de passe et confirmation ; messages d'erreur sous chaque champ |
| Connexion | Email et mot de passe ; message « Email ou mot de passe incorrect » ou message de compte banni |
| Tableau de bord | Page réservée aux joueurs connectés, avec le pseudo et le bouton « Se déconnecter » |
| Administration | Page réservée aux administrateurs : bannir et débannir un joueur |

- Les pages protégées passent par un intercepteur d'authentification : un visiteur non connecté est redirigé vers la connexion (test T46) et un joueur ne peut pas ouvrir la page d'administration (test T49).
- **Données simulées** : les statistiques, les parties ouvertes et l'historique du tableau de bord sont fictifs (classe `MockData`), en attendant le `ladder-service`. La page l'indique elle-même.
- Le front-end a été testé dans le navigateur (tests T36 à T51).

## 5. Authentification et autorisations

- **Authentification par JWT** (Spring Security). Le token est obtenu par `POST /api/v1/auth/login` et envoyé ensuite dans l'en-tête `Authorization: Bearer <token>`.
- Le token contient l'email (`sub`), l'identifiant (`id`), les rôles (`roles`), la date d'émission (`iat`) et d'expiration (`exp`). Durée de vie observée : **1 heure** (test T13 : un token ancien est refusé).
- **Deux rôles** : `USER` et `ADMIN`. Un compte administrateur est créé au démarrage du `player-service` à partir de `ADMIN_EMAIL` et `ADMIN_PASSWORD` (test T24).
- **Règle de propriété** : un joueur ne peut lire, modifier ou supprimer que **son propre compte** ; l'administrateur peut agir sur tous les comptes (tests T15 à T17, T21).
- **Accès interdit** : un joueur connecté qui appelle une route réservée à l'administrateur reçoit **403** (tests T20 et T27, après correction des anomalies A02 et A03).
- **Bannissement** : un compte banni ne peut plus se connecter (réponse 403, « Ce compte a été banni », test T29) et son token précédent est refusé avec un **403** (test T30, après correction de A04). Le bannissement et le débannissement sont aussi utilisables depuis la page d'administration du front-end (tests T50 et T51).

## 6. Endpoints du `player-service` (observés)

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

Les endpoints de bannissement et d'administration du Membre 5 sont désormais intégrés dans `develop` et ont été testés (tests T26 à T33, retests T27 et T30, et tests du front T44, T50 et T51).

## 7. Choix d'architecture

- **Un service par domaine**, comme le demande le sujet, pour que chaque membre travaille dans son dossier sans conflit.
- **PostgreSQL** pour la persistance exigée par le sujet, avec un volume Docker pour survivre aux redémarrages.
- **JWT sans session côté serveur de l'API** : chaque requête porte son identité, ce qui convient à plusieurs services.
- **Un front-end séparé** qui ne parle qu'à l'API : l'interface peut évoluer sans toucher aux services.
- **Docker Compose** pour que n'importe quel membre lance tout le projet avec les mêmes commandes.
- **Secrets hors du code** : les mots de passe et la clé JWT viennent du fichier `.env`, ignoré par Git.

## 8. Organisation du travail (Git)

- `main` : versions livrées, une par lot, avec un tag.
- `develop` : intégration.
- `feature/lotX-nom` : une branche par tâche, créée depuis `develop`.
- Aucun envoi direct sur `main` ni sur `develop` : passage par une Pull Request relue par un autre membre.
- Messages de commit du type `feat(player): ...`, `fix(game): ...`, `docs: ...`.

## 9. Qualité et tests du Lot 1

- **Qualification fonctionnelle** : comportement observé de l'extérieur, avec Postman pour l'API et le navigateur pour le front-end, sans lire le code.
- **51 scénarios exécutés : 51 OK, 0 KO** (35 sur l'API, 16 sur le front-end). Détail et preuves : `docs/Plan_de_tests_Lot1_Complet.xlsx` et `docs/preuves/`.
- **Qualification structurelle** : 44 tests automatiques (22 pour le `player-service`, 22 pour le front-end) exécutés le 29/10/2026 par Rania Zakraf, tous verts (captures `TA01` et `TA02` dans `docs/preuves/`).
- **6 anomalies signalées** :
  - A01 à A04 (#14, #15, #16, #18) : corrigées le 28/10/2026 et retestées le 29/10/2026 (titre « Erreur de Validation », puis 403 à la place de 401 sur les routes réservées).
  - A05 (#20) : deux messages cumulés sous le champ mot de passe de l'inscription (gravité faible), ouverte.
  - A06 (#21) : le bouton « Précédent » du navigateur réaffiche temporairement le tableau de bord après la déconnexion ; la première interaction renvoie vers la connexion (gravité faible), ouverte.

## 10. Points à confirmer

- **Version de Spring Boot** : d'après un message du groupe (28/10), le `player-service` utilise la version 3.3.3 alors que les autres modules utilisent la 4.1.x ; à vérifier dans les `pom.xml` et à harmoniser si besoin.
- **Adresse de l'interface Swagger** du `player-service`.
- **Données simulées du tableau de bord** : à remplacer par les données du `ladder-service` lors du lot concerné.
- **Anomalies A05 et A06** : correction à prévoir côté front-end (pistes : un seul message par champ ; en-tête `Cache-Control: no-store` sur les pages protégées).
