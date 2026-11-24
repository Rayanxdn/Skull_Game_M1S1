package fr.miage.skull.game.dto;

import jakarta.validation.constraints.Size;

/** Motifs choisis pour rejoindre ; absents, ils sont tirés au hasard. */
public record JoinGameRequest(
        @Size(max = 30, message = "Nom de motif trop long")
        String flowerTheme,

        @Size(max = 30, message = "Nom de motif trop long")
        String skullTheme
) {}
