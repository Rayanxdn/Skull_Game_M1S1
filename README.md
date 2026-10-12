# Skull Game – M1 MIAGE

Application web du jeu de cartes Skull, basée sur des services REST (Spring Boot).

## Services

| Service | Rôle | Port hôte |
|---|---|---|
| player-service | Utilisateurs, inscription, connexion | 8081 |
| game-service | Parties, règles du jeu, IA | 8082 |
| ladder-service | Historique et classement | 8083 |
| market-service | Marketplace d'apparences | 8084 |
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
