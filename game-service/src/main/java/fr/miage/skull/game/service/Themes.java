package fr.miage.skull.game.service;

import java.util.List;
import java.util.Random;

/** Motifs de disques utilisables sans achat. Le marketplace (Lot 3) en ajoutera d'autres. */
public final class Themes {

    public static final List<String> BASE = List.of("CLASSIC", "MUERTOS", "CELTIQUE");

    private Themes() {
    }

    public static boolean isKnown(String theme) {
        return BASE.contains(theme);
    }

    public static String random(Random random) {
        return BASE.get(random.nextInt(BASE.size()));
    }
}
