package fr.epitech.katanagame.entity.enemy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.math.Vector2;
import fr.epitech.katanagame.world.World;
import org.junit.jupiter.api.Test;

class EnemyTest {
    private final TiledMapTileLayer[] none = new TiledMapTileLayer[0];
    private final World world = new World(null, null, none, none, none);
    private final TestEnemy enemy = new TestEnemy(100f, 100f);

    @Test
    void unCoup_leTue() {
        enemy.takeDamage();
        assertFalse(enemy.isAlive());
    }

    @Test
    void parried_lesonneEtLeTourneVersLeJoueur() {
        enemy.parried(new Vector2(1f, 0f), new Vector2(100f, 200f));
        assertTrue(enemy.isStunned());
        assertEquals(0f, enemy.getAim().x, 0.001f);
        assertEquals(1f, enemy.getAim().y, 0.001f);
        assertTrue(enemy.alerted());
        assertEquals(200f, enemy.lastKnown().y, 0.001f);
    }

    @Test
    void parried_surUnMort_neFaitRien() {
        enemy.takeDamage();
        enemy.parried(new Vector2(1f, 0f), new Vector2(100f, 200f));
        assertFalse(enemy.isStunned());
    }

    @Test
    void sonne_ilRecule_puisSeReprend() {
        enemy.parried(new Vector2(1f, 0f), new Vector2(0f, 100f));
        for (int i = 0; i < 20; i++) enemy.update(0.05f, world);
        assertTrue(enemy.getPosition().x > 100f);
        assertFalse(enemy.isStunned());
    }

    @Test
    void bruit_alerteUnVivant() {
        enemy.hearNoise(new Vector2(30f, 40f));
        assertTrue(enemy.alerted());
        assertEquals(30f, enemy.lastKnown().x, 0.001f);
    }

    @Test
    void bruit_neReveillePasUnMort() {
        enemy.takeDamage();
        enemy.hearNoise(new Vector2(30f, 40f));
        assertFalse(enemy.alerted());
    }

    @Test
    void canBeParried_tantQuIlNeVoitPasLeJoueur() {
        assertTrue(enemy.canBeParried());
    }
}
