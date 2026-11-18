package fr.miage.skull.game.ai;

import java.util.List;

/**
 * Ce que l'IA a le droit de savoir à son tour : ses disques et l'état des enchères,
 * jamais le contenu des piles des autres.
 *
 * @param hand               disques qu'elle a encore en main (qu'elle peut poser)
 * @param ownStack           disques qu'elle a déjà posés sur la table
 * @param currentBid         enchère en cours (0 si personne n'a enchéri)
 * @param totalDiscsOnTable  nombre de disques posés par tous les joueurs
 */
public record AiView(List<Disc> hand, List<Disc> ownStack, int currentBid, int totalDiscsOnTable) {

    public boolean biddingStarted() {
        return currentBid > 0;
    }
}
