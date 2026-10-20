package fr.epitech.katanagame.screen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HitStopTest {
    private final HitStop hitStop = new HitStop();

    @Test
    void auDepart_rienNeBouge() {
        assertFalse(hitStop.isFrozen(0.016f));
        assertEquals(1f, hitStop.worldScale(0.016f), 0.001f);
    }

    @Test
    void parry_figeUnInstant_puisCesse() {
        hitStop.onParry();
        assertTrue(hitStop.isFrozen(0.04f));
        assertTrue(hitStop.isFrozen(0.04f));
        assertTrue(hitStop.isFrozen(0.04f));    // 0,09 s : trois images de 0,04 s
        assertFalse(hitStop.isFrozen(0.04f));
    }

    @Test
    void coupQuiTouche_figeMoinsLongtempsQueLeParry() {
        hitStop.onAttackHit();
        assertTrue(hitStop.isFrozen(0.04f));
        assertTrue(hitStop.isFrozen(0.04f));    // 0,06 s : deux images de 0,04 s
        assertFalse(hitStop.isFrozen(0.04f));
    }

    @Test
    void coupQuiTouche_neRaccourcitPasUnParry() {
        hitStop.onParry();
        hitStop.onAttackHit();
        assertTrue(hitStop.isFrozen(0.04f));
        assertTrue(hitStop.isFrozen(0.04f));
        assertTrue(hitStop.isFrozen(0.04f));
    }

    @Test
    void parry_ralentitLeMondeEnsuite_puisRevientANormal() {
        hitStop.onParry();
        for (int i = 0; i < 4; i++) {
            assertEquals(0.3f, hitStop.worldScale(0.1f), 0.001f);   // 0,35 s de ralenti
        }
        assertEquals(1f, hitStop.worldScale(0.1f), 0.001f);
    }

    @Test
    void reset_toutRepart() {
        hitStop.onParry();
        hitStop.reset();
        assertFalse(hitStop.isFrozen(0.016f));
        assertEquals(1f, hitStop.worldScale(0.016f), 0.001f);
    }
}
