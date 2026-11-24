package fr.miage.skull.game.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/** Le pseudo du créateur vient du jeton, jamais de cette requête. Motif absent : tiré au hasard. */
public record CreateGameRequest(
        @Min(value = 3, message = "Une partie nécessite au moins 3 joueurs")
        @Max(value = 6, message = "Une partie est limitée à 6 joueurs")
        int maxPlayers,

        @Min(value = 0, message = "Le nombre d'IA ne peut pas être négatif")
        @Max(value = 5, message = "Il faut au moins un joueur humain")
        int aiCount,

        @Size(max = 30, message = "Nom de motif trop long")
        String flowerTheme,

        @Size(max = 30, message = "Nom de motif trop long")
        String skullTheme
) {}
