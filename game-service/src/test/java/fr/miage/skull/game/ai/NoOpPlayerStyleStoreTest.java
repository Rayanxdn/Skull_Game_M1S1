package fr.miage.skull.game.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class NoOpPlayerStyleStoreTest {

    @Test
    void knowsNobody() {
        assertTrue(new NoOpPlayerStyleStore().find(1L).isEmpty());
    }
}
