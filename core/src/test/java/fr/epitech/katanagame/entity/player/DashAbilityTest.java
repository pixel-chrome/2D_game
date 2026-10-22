package fr.epitech.katanagame.entity.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.math.Vector2;
import fr.epitech.katanagame.entity.Direction;
import org.junit.jupiter.api.Test;

class DashAbilityTest {
    private final DashAbility dash = new DashAbility(null);

    @Test
    void auDepart_pretEtInactif() {
        assertTrue(dash.isReady());
        assertFalse(dash.isActive());
        assertNull(dash.steer(null));
    }

    @Test
    void trigger_lanceUnElanDansLaDirection() {
        dash.trigger(new Vector2(1f, 0f));
        assertTrue(dash.isActive());
        assertFalse(dash.isReady());
        Vector2 velocity = dash.steer(null);
        assertNotNull(velocity);
        assertEquals(460f, velocity.x, 0.001f);
        assertEquals(Direction.RIGHT, dash.facing());
    }

    @Test
    void finit_apres0_12s_maisLaRechargeContinue() {
        dash.trigger(new Vector2(0f, 1f));
        dash.tick(0.13f);
        assertFalse(dash.isActive());
        assertFalse(dash.isReady());
        dash.tick(0.7f);
        assertTrue(dash.isReady());
    }

    @Test
    void pendantLaRecharge_unNouveauDashEstIgnore() {
        dash.trigger(new Vector2(1f, 0f));
        dash.tick(0.2f);
        dash.trigger(new Vector2(0f, -1f));
        assertFalse(dash.isActive());
        assertEquals(Direction.RIGHT, dash.facing());
    }

    @Test
    void pollStart_nEstVraiQuUneFois() {
        dash.trigger(new Vector2(1f, 0f));
        assertTrue(dash.pollStart());
        assertFalse(dash.pollStart());
    }

    @Test
    void reset_coupeTout() {
        dash.trigger(new Vector2(1f, 0f));
        dash.reset();
        assertFalse(dash.isActive());
        assertTrue(dash.isReady());
    }
}
