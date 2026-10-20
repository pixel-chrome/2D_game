package fr.epitech.katanagame.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.math.Vector2;
import fr.epitech.katanagame.TestMaps;
import fr.epitech.katanagame.world.World;
import org.junit.jupiter.api.Test;

class CharacterTest {
    /** Personnage de test : 10 x 10, pieds de 8 x 4 au centre, invincibilité réglable. */
    private static final class Dummy extends Character {
        boolean invincible;
        int deaths;

        Dummy(float x, float y) {
            super(x, y, 10f, 10f, 8f, 4f, 0f);
        }

        @Override public boolean isInvincible() { return invincible; }
        @Override protected void onDeath() { deaths++; }
        @Override public void update(float delta, World world) {}
        @Override public void draw(SpriteBatch batch) {}

        boolean push(float dx, float dy, TiledMapTileLayer[] solids) { return move(dx, dy, solids); }
    }

    /** Mur vertical (colonne 4, lignes 1 à 3) dans une carte de 10 x 5 tuiles. */
    private final TiledMapTileLayer[] layers = TestMaps.layers(
            "..........",
            "....#.....",
            "....#.....",
            "....#.....",
            "..........");

    @Test
    void unCoup_tue() {
        Dummy dummy = new Dummy(40f, 24f);
        dummy.takeDamage();
        assertFalse(dummy.isAlive());
        assertEquals(1, dummy.deaths);
    }

    @Test
    void mortDejaSubie_neSeReproduitPas() {
        Dummy dummy = new Dummy(40f, 24f);
        dummy.takeDamage();
        dummy.takeDamage();
        assertEquals(1, dummy.deaths);
    }

    @Test
    void invincible_neMeurtPas() {
        Dummy dummy = new Dummy(40f, 24f);
        dummy.invincible = true;
        dummy.takeDamage();
        assertTrue(dummy.isAlive());
        assertEquals(0, dummy.deaths);
    }

    @Test
    void respawn_remetEnVieALaBonnePosition() {
        Dummy dummy = new Dummy(40f, 24f);
        dummy.takeDamage();
        dummy.respawn(100f, 30f);
        assertTrue(dummy.isAlive());
        assertEquals(100f, dummy.getPosition().x, 0.001f);
        assertEquals(30f, dummy.getPosition().y, 0.001f);
    }

    @Test
    void deplacementLibre_arriveAuBout() {
        Dummy dummy = new Dummy(24f, 24f);
        assertTrue(dummy.push(20f, 0f, layers));
        assertEquals(44f, dummy.getPosition().x, 0.001f);
    }

    @Test
    void mur_bloqueLeDeplacement() {
        Dummy dummy = new Dummy(40f, 24f);
        assertFalse(dummy.push(60f, 0f, layers));
        // les pieds (4 px de demi-largeur) s'arrêtent avant la colonne du mur (x = 64)
        assertTrue(dummy.getPosition().x <= 60f);
        assertTrue(dummy.getPosition().x > 40f);
    }

    @Test
    void mur_laisseGlisserSurLAutreAxe() {
        Dummy dummy = new Dummy(40f, 24f);
        dummy.push(60f, 10f, layers);
        assertEquals(34f, dummy.getPosition().y, 0.001f);
    }

    @Test
    void visee_donneLaDirectionDeSprite() {
        Dummy dummy = new Dummy(40f, 24f);
        dummy.getAim().set(0f, 1f);
        assertEquals(Direction.UP, dummy.getFacing());
    }

    @Test
    void deuxHitbox_sontCentreesSurLaPosition() {
        Dummy dummy = new Dummy(40f, 24f);
        assertEquals(35f, dummy.getBounds().x, 0.001f);
        assertEquals(36f, dummy.getFeetBounds().x, 0.001f);
        assertEquals(new Vector2(40f, 24f), dummy.getPosition());
    }
}
