package fr.miage.skull.game.engine;

public record Move(
    MoveType type,
    DiscType discType,
    Integer bidAmount,
    String targetPlayerId
){}

