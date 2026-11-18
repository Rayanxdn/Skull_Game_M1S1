package fr.miage.skull.game.ai;

/** Ce que l'IA décide de faire : poser un disque, enchérir ou se retirer. */
public sealed interface AiAction {

    record PlaceDisc(Disc disc) implements AiAction { }

    record Bid(int amount) implements AiAction { }

    record Withdraw() implements AiAction { }
}
