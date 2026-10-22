package fr.epitech.katanagame.entity.enemy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.math.Vector2;
import fr.epitech.katanagame.world.World;
import org.junit.jupiter.api.Test;

class BossTest {
    private final TiledMapTileLayer[] none = new TiledMapTileLayer[0];
    private final World world = new World(null, null, none, none, none);
    private final Boss boss = new Boss(0f, 0f, 50f, 10, 20f, 20f, 10f, 5f, 5f) {
        @Override public boolean isEngaged() { return true; }
        @Override protected void think(float delta, World w) {}
        @Override public void draw(SpriteBatch batch) {}
    };

    @Test
    void unCoup_netuePas() {
        boss.takeDamage();
        assertTrue(boss.isAlive());
        assertEquals(9, boss.getHealth());
    }

    @Test
    void apresUnDegat_invincibleUnCourtInstant() {
        boss.takeDamage();
        boss.takeDamage();
        assertEquals(9, boss.getHealth());
        boss.update(0.5f, world);
        boss.takeDamage();
        assertEquals(8, boss.getHealth());
    }

    @Test
    void ProjectileRenvoye_faitDeuxDegats() {
        boss.takeReflectedHit();
        assertEquals(8, boss.getHealth());
    }

    @Test
    void neSePareDirectementPas() {
        assertFalse(boss.canBeParried());
    }

    @Test
    void dernierPV_leTue() {
        for (int i = 0; i < 10; i++) {
            boss.takeDamage();
            boss.update(0.5f, world);
        }
        assertFalse(boss.isAlive());
        assertEquals(0, boss.getHealth());
    }

    @Test
    void kill_ignoreLInvincibilite() {
        boss.takeDamage();
        boss.kill();
        assertFalse(boss.isAlive());
    }

}
