# Skull Game – M1 MIAGE

Application web du jeu de cartes Skull, basée sur des services REST (Spring Boot).

## Services

| Service | Rôle | Port hôte |
|---|---|---|
| player-service | Utilisateurs, inscription, connexion | 8081 |
| game-service | Parties, règles du jeu, IA | 8082 |
| ladder-service | Historique et classement | 8083 |
| market-service | Marketplace d'apparences | 8084 |
| frontend | Interface web (Thymeleaf), client des services | 8080 |
| db (PostgreSQL) | Persistance | 5432 |

## Prérequis

Java 21, Docker + Docker Compose, Git.

## Lancer tout le projet

    cp .env.example .env
    docker compose up --build -d
    curl http://localhost:8081/ping

## Développer un seul service

    docker compose up -d db
    cd player-service
    ./mvnw spring-boot:run

En local, chaque service écoute sur son port (8081 à 8084).

## Workflow Git

- `main` : versions livrées (une par lot, avec un tag)
- `develop` : intégration
- `feature/lotX-nom` : une branche par tâche, créée depuis `develop`
- Jamais de push direct sur `main` ni `develop` : Pull Request relue par un autre membre
- Commits : `feat(player): ...`, `fix(game): ...`, `docs: ...`

## Conventions

- Package de base : `fr.miage.skull.<service>`
- Découpage MVC : controller / service / repository / model / dto
- Utiliser `./mvnw` et non `mvn`
- Ne jamais commiter `.env`

## Front-end

Application Spring Boot + Thymeleaf (dossier `frontend/`). Elle appelle le `player-service` côté serveur ;
le JWT est gardé dans la session serveur (cookie HttpOnly), jamais envoyé au navigateur.

    docker compose up --build -d      # front sur http://localhost:8080

Plusieurs instances (plusieurs joueurs) : lancer le front sur d'autres ports, par exemple

    cd frontend
    SERVER_PORT=8090 PLAYER_API_URL=http://localhost:8081 ./mvnw spring-boot:run

Chaque instance a son propre cookie de session (`SKULLSESSION-<port>`).
Parties, historique et statistiques du tableau de bord sont simulés tant que game-service et ladder-service n'existent pas.
Compte admin : variables `ADMIN_EMAIL` / `ADMIN_PASSWORD` du `.env`.
