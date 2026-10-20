package fr.epitech.katanagame.world;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import fr.epitech.katanagame.TestMaps;
import org.junit.jupiter.api.Test;

class CollisionTest {
    /** Un mur vertical (colonne 4, lignes 1 à 3 depuis le haut) dans une carte de 10 x 5 tuiles. */
    private final TiledMapTileLayer[] layers = TestMaps.layers(
            "..........",
            "....#.....",
            "....#.....",
            "....#.....",
            "..........");

    @Test
    void caseVide_nEstPasSolide() {
        assertFalse(Collision.isSolid(layers, 8f, 8f));
    }

    @Test
    void caseDeMur_estSolide() {
        assertTrue(Collision.isSolid(layers, 72f, 40f));
    }

    @Test
    void horsDeLaCarte_estSolide() {
        assertTrue(Collision.isSolid(layers, -1f, 8f));
        assertTrue(Collision.isSolid(layers, 8f, -1f));
        assertTrue(Collision.isSolid(layers, 500f, 8f));
        assertTrue(Collision.isSolid(layers, 8f, 500f));
    }

    @Test
    void ligneLibre_aUneLigneDeVue() {
        assertTrue(Collision.hasLineOfSight(layers, new Vector2(8f, 8f), new Vector2(150f, 8f)));
    }

    @Test
    void murEntreDeuxPoints_bloqueLaLigneDeVue() {
        assertFalse(Collision.hasLineOfSight(layers, new Vector2(8f, 40f), new Vector2(150f, 40f)));
    }

    @Test
    void rectangleDansLeVide_netouchePasDeMur() {
        assertFalse(Collision.overlapsSolid(layers, new Rectangle(8f, 8f, 6f, 6f)));
    }

    @Test
    void rectangleSurUnMur_toucheUnMur() {
        assertTrue(Collision.overlapsSolid(layers, new Rectangle(60f, 40f, 10f, 6f)));
    }

    @Test
    void rectangleQuiDepasseLaCarte_toucheUnMur() {
        assertTrue(Collision.overlapsSolid(layers, new Rectangle(-5f, 8f, 6f, 6f)));
    }

    @Test
    void sansCalque_rienNeBloque() {
        TiledMapTileLayer[] none = new TiledMapTileLayer[0];
        assertFalse(Collision.isSolid(none, -50f, -50f));
        assertFalse(Collision.overlapsSolid(none, new Rectangle(0f, 0f, 5f, 5f)));
    }
}
