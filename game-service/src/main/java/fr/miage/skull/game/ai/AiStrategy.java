package fr.miage.skull.game.ai;

import java.util.Random;

/** Une façon de jouer. Le hasard est fourni de l'extérieur pour pouvoir tester. */
public interface AiStrategy {

    String id();

    AiAction decide(AiView view, Random random);
}
