package fr.miage.skull.game.dto;

import jakarta.validation.constraints.NotBlank;

public record JoinGameRequest(
        @NotBlank(message = "Le pseudo est obligatoire")
        String displayName,

        String flowerTheme,
        String skullTheme
) {}
