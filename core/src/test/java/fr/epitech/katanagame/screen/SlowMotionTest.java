package fr.epitech.katanagame.screen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SlowMotionTest {
    private final SlowMotion slowMo = new SlowMotion();

    private void hold(float seconds, boolean wants) {
        float step = 0.05f;
        for (float t = 0f; t < seconds; t += step) slowMo.update(step, wants);
    }

    @Test
    void auDepart_tempsNormalEtJaugePleine() {
        assertEquals(1f, slowMo.worldScale(), 0.001f);
        assertEquals(1f, slowMo.playerScale(), 0.001f);
        assertEquals(1f, slowMo.getCharge(), 0.001f);
    }

    @Test
    void maintenir_ralentitLeMondePlusQueLeJoueur() {
        hold(0.3f, true);
        assertEquals(0.2f, slowMo.worldScale(), 0.001f);
        assertEquals(0.6f, slowMo.playerScale(), 0.001f);
        assertEquals(1f, slowMo.getIntensity(), 0.001f);
    }

    @Test
    void maintenir_videLaJauge() {
        hold(0.6f, true);
        assertTrue(slowMo.getCharge() < 0.6f);
    }

    @Test
    void jaugeVide_leRalentiS_arrete() {
        hold(2f, true);
        assertEquals(0f, slowMo.getIntensity(), 0.001f);
        assertEquals(1f, slowMo.worldScale(), 0.001f);
    }

    @Test
    void jaugeTropBasse_nePeutPasRedemarrer() {
        hold(1.25f, true);              // jauge vide (1,2 s)
        hold(0.5f, false);              // recharge à ~0,14 : sous le minimum (0,3)
        hold(0.2f, true);
        assertEquals(0f, slowMo.getIntensity(), 0.001f);
    }

    @Test
    void relacher_rechargeLaJauge() {
        hold(1f, true);
        hold(5f, false);
        assertEquals(1f, slowMo.getCharge(), 0.001f);
        assertEquals(0f, slowMo.getIntensity(), 0.001f);
    }

    @Test
    void reset_remetTout() {
        hold(1f, true);
        slowMo.reset();
        assertEquals(1f, slowMo.getCharge(), 0.001f);
        assertEquals(1f, slowMo.worldScale(), 0.001f);
    }
}
