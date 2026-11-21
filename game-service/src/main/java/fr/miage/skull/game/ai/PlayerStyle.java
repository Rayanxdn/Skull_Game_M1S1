package fr.miage.skull.game.ai;

/** Comment un joueur a joué jusqu'ici (sert à l'option « IA qui apprend »). */
public record PlayerStyle(long gamesPlayed, double bluffRate, double withdrawRate, double averageRaise) {
}
