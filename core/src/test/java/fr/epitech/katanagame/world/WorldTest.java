package fr.epitech.katanagame.world;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import fr.epitech.katanagame.entity.enemy.TestEnemy;
import org.junit.jupiter.api.Test;

class WorldTest {
    private final TiledMapTileLayer[] none = new TiledMapTileLayer[0];
    private final World world = new World(null, null, none, none, none);

    @Test
    void sansEnnemi_ilsSontTousMorts() {
        assertTrue(world.allEnemiesDead());
    }

    @Test
    void ennemiVivant_pasTousMorts() {
        world.enemies.add(new TestEnemy(0f, 0f));
        assertFalse(world.allEnemiesDead());
    }

    @Test
    void ennemiTue_tousMorts() {
        TestEnemy enemy = new TestEnemy(0f, 0f);
        world.enemies.add(enemy);
        enemy.takeDamage();
        assertTrue(world.allEnemiesDead());
    }

    @Test
    void bruit_alerteLesEnnemisAPorteeSaufLaSource() {
        TestEnemy source = new TestEnemy(0f, 0f);
        TestEnemy near = new TestEnemy(50f, 0f);
        TestEnemy far = new TestEnemy(500f, 0f);
        world.enemies.add(source);
        world.enemies.add(near);
        world.enemies.add(far);

        world.makeNoise(source.getPosition(), 100f, source);

        assertTrue(near.alerted());
        assertFalse(far.alerted());
        assertFalse(source.alerted());
    }
}
