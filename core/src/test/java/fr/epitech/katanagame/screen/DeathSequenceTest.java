package fr.epitech.katanagame.screen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DeathSequenceTest {
    private final DeathSequence death = new DeathSequence();

    @Test
    void auDepart_pasTermine() {
        assertFalse(death.isOver());
    }

    @Test
    void impact_figeTout() {
        death.start();
        death.advance(0.01f);
        assertEquals(0f, death.effectsScale(), 0.001f);
        assertEquals(0f, death.worldScale(), 0.001f);
    }

    @Test
    void apresLImpact_lesEffetsRepartent_etLeMondeResteLent() {
        death.start();
        death.advance(0.05f);
        death.advance(0.05f);
        death.advance(0.05f);
        assertEquals(1f, death.effectsScale(), 0.001f);
        assertEquals(0.25f, death.worldScale(), 0.001f);
    }

    @Test
    void gameOver_apres0_45s() {
        death.start();
        death.advance(0.4f);
        assertFalse(death.isOver());
        death.advance(0.1f);
        assertTrue(death.isOver());
    }

    @Test
    void reset_recommenceAZero() {
        death.start();
        death.advance(1f);
        death.reset();
        assertFalse(death.isOver());
    }
}
