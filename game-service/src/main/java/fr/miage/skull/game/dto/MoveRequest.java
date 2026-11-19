package fr.miage.skull.game.dto;

import fr.miage.skull.game.engine.DiscType;
import fr.miage.skull.game.engine.MoveType;
import jakarta.validation.constraints.NotNull;

public record MoveRequest(
        @NotNull(message = "Le type de coup est obligatoire")
        MoveType type,

        DiscType discType,

        Integer bidAmount,

        Long targetPlayerId
) {}
