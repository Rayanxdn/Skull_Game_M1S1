package fr.miage.skull.game.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record CreateGameRequest(
        @NotBlank(message = "Le pseudo est obligatoire")
        String displayName,

        @Min(value = 3, message = "Une partie nécessite au moins 3 joueurs")
        @Max(value = 6, message = "Une partie est limitée à 6 joueurs")
        int maxPlayers,

        @Min(value = 0, message = "Le nombre d'IA ne peut pas être négatif")
        @Max(value = 5, message = "Il faut au moins un joueur humain")
        int aiCount,

        String flowerTheme,
        String skullTheme
) {}
