package fr.epitech.katanagame.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.math.Vector2;
import fr.epitech.katanagame.TestMaps;
import java.util.List;
import org.junit.jupiter.api.Test;

class PathfinderTest {
    /** Mur vertical (colonne 4) avec un passage en haut et en bas. */
    private final TiledMapTileLayer[] gap = TestMaps.layers(
            "............",
            "....#.......",
            "....#.......",
            "....#.......",
            "....#.......",
            "....#.......",
            "............");

    /** Mur vertical sur toute la hauteur : aucun passage. */
    private final TiledMapTileLayer[] sealed = TestMaps.layers(
            "....#.......",
            "....#.......",
            "....#.......",
            "....#.......",
            "....#.......");

    private final Vector2 left = new Vector2(24f, 56f);
    private final Vector2 right = new Vector2(150f, 56f);

    @Test
    void isClear_vraiDansLeVide() {
        assertTrue(Pathfinder.isClear(gap, new Vector2(24f, 8f), new Vector2(150f, 8f)));
    }

    @Test
    void isClear_fauxAuTraversDunMur() {
        assertFalse(Pathfinder.isClear(gap, left, right));
    }

    @Test
    void ligneDroiteLibre_donneUneListeVide() {
        List<Vector2> path = Pathfinder.findPath(gap, new Vector2(24f, 8f), new Vector2(150f, 8f));
        assertNotNull(path);
        assertTrue(path.isEmpty());
    }

    @Test
    void murAvecPassage_lecheminContourneEtArriveALaCible() {
        List<Vector2> path = Pathfinder.findPath(gap, left, right);
        assertNotNull(path);
        assertFalse(path.isEmpty());
        assertEquals(right.x, path.get(path.size() - 1).x, 0.01f);
        assertEquals(right.y, path.get(path.size() - 1).y, 0.01f);
    }

    @Test
    void chaqueSegmentDuChemin_estLibre() {
        List<Vector2> path = Pathfinder.findPath(gap, left, right);
        assertNotNull(path);
        Vector2 anchor = left;
        for (Vector2 point : path) {
            assertTrue(Pathfinder.isClear(gap, anchor, point), "segment bloqué vers " + point);
            anchor = point;
        }
    }

    @Test
    void murSansPassage_pasDeChemin() {
        assertNull(Pathfinder.findPath(sealed, left, right));
    }
}
