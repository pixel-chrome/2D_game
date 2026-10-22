package fr.epitech.katanagame.entity.enemy;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.math.Vector2;
import org.junit.jupiter.api.Test;

class StunTest {
    private final TiledMapTileLayer[] none = new TiledMapTileLayer[0];
    private final TestEnemy enemy = new TestEnemy(100f, 100f);
    private final Stun stun = new Stun();

    @Test
    void auDepart_pasActif() {
        assertFalse(stun.isActive());
        assertFalse(stun.advance(0.1f, enemy, none));
    }

    @Test
    void start_activeLaStupeur() {
        stun.start(new Vector2(1f, 0f));
        assertTrue(stun.isActive());
    }

    @Test
    void recule_dansLaDirectionDuChoc() {
        stun.start(new Vector2(1f, 0f));
        stun.advance(0.05f, enemy, none);
        assertTrue(enemy.getPosition().x > 100f);
        assertTrue(Math.abs(enemy.getPosition().y - 100f) < 0.001f);
    }

    @Test
    void recule_enRalentissant() {
        stun.start(new Vector2(1f, 0f));
        stun.advance(0.05f, enemy, none);
        float first = enemy.getPosition().x - 100f;
        float before = enemy.getPosition().x;
        stun.advance(0.05f, enemy, none);
        float second = enemy.getPosition().x - before;
        assertTrue(second < first);
    }

    @Test
    void finit_apresLaDuree() {
        stun.start(new Vector2(0f, 1f));
        float elapsed = 0f;
        while (stun.advance(0.1f, enemy, none)) {
            elapsed += 0.1f;
            assertTrue(elapsed < Stun.TIME + 0.5f, "la stupeur ne s'arrête jamais");
        }
        assertFalse(stun.isActive());
        assertTrue(elapsed >= Stun.TIME - 0.15f);
    }
}
