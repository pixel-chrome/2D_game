package fr.epitech.katanagame.entity.projectile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.math.Vector2;
import fr.epitech.katanagame.TestMaps;
import org.junit.jupiter.api.Test;

class BulletTest {
    private final TiledMapTileLayer[] open = new TiledMapTileLayer[0];

    private Bullet bullet(float dx, float dy) {
        return new Bullet(new Vector2(0f, 0f), new Vector2(dx, dy), null);
    }

    @Test
    void avance_alaVitesseDuLaser() {
        Bullet bullet = bullet(1f, 0f);
        bullet.update(0.1f, open);
        assertEquals(Bullet.SPEED * 0.1f, bullet.getPosition().x, 0.01f);
        assertEquals(0f, bullet.getPosition().y, 0.01f);
    }

    @Test
    void direction_estNormalisee() {
        Bullet bullet = bullet(10f, 0f);
        assertEquals(1f, bullet.getDirection().len(), 0.001f);
    }

    @Test
    void expire_apresTroisSecondes() {
        Bullet bullet = bullet(1f, 0f);
        for (int i = 0; i < 30; i++) bullet.update(0.1f, open);
        assertTrue(bullet.isAlive());
        bullet.update(0.2f, open);
        assertFalse(bullet.isAlive());
    }

    @Test
    void mur_leDetruit() {
        TiledMapTileLayer[] wall = TestMaps.layers("..#...");
        Bullet bullet = new Bullet(new Vector2(8f, 8f), new Vector2(1f, 0f), null);
        bullet.update(0.1f, wall);   // 50 px plus loin : dans la tuile 3, puis...
        bullet.update(0.1f, wall);
        assertFalse(bullet.isAlive());
    }

    @Test
    void reflect_changeDeCampEtAccelere() {
        Bullet bullet = bullet(1f, 0f);
        assertFalse(bullet.isReflected());
        bullet.reflect(new Vector2(0f, 1f));
        assertTrue(bullet.isReflected());
        bullet.update(0.1f, open);
        assertEquals(0f, bullet.getPosition().x, 0.01f);
        assertEquals(Bullet.SPEED * 1.6f * 0.1f, bullet.getPosition().y, 0.01f);
    }

    @Test
    void reflect_remetLaDureeDeVieAZero() {
        Bullet bullet = bullet(1f, 0f);
        for (int i = 0; i < 25; i++) bullet.update(0.1f, open);
        bullet.reflect(new Vector2(-1f, 0f));
        for (int i = 0; i < 25; i++) bullet.update(0.1f, open);
        assertTrue(bullet.isAlive());
    }

    @Test
    void kill_ledesactive() {
        Bullet bullet = bullet(1f, 0f);
        bullet.kill();
        assertFalse(bullet.isAlive());
    }

    @Test
    void hitbox_estCentreeSurLaBalle() {
        Bullet bullet = bullet(1f, 0f);
        assertEquals(-2f, bullet.getBounds().x, 0.001f);
        assertEquals(4f, bullet.getBounds().width, 0.001f);
    }
}
