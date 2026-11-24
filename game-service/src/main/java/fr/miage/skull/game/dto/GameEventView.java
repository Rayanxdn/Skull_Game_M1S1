package fr.miage.skull.game.dto;

/**
 * Un événement du journal de la partie, prêt à afficher. Ne contient jamais un disque encore caché :
 * la pose et la perte d'un disque sont annoncées sans leur type.
 *
 * @param type                type d'événement du moteur (DISC_PLACED, BID, PASS, FLOWER_REVEALED, ...)
 * @param participantId       siège qui a agi (null pour NEW_ROUND)
 * @param actorName           nom affiché de ce siège
 * @param targetParticipantId siège dont la pile est retournée (null sinon)
 * @param targetName          nom affiché de ce siège
 * @param value               enchère, fleurs trouvées, points ou numéro de manche selon le type
 */
public record GameEventView(
        String type,
        Long participantId,
        String actorName,
        Long targetParticipantId,
        String targetName,
        Integer value
) {}
