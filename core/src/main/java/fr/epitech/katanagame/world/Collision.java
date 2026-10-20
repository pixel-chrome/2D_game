package fr.epitech.katanagame.world;

import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Rectangle;

/** Fonctions utilitaires pour tester la map (balles, ligne de vue des ennemis). */
public final class Collision {
    private static final float TILE_SIZE = 16f;

    /** Empêche l’instanciation de cette classe utilitaire. */
    private Collision() {}

    /**
     * Vrai si le point (px, py) est dans une case non vide d'une des couches (ou hors de la map).
     *
     * @param layers calques à tester
     * @param px abscisse (px)
     * @param py ordonnée (px)
     * @return vrai si le point est dans un obstacle
     */
    public static boolean isSolid(TiledMapTileLayer[] layers, float px, float py) {
        int tx = (int) Math.floor(px / TILE_SIZE);
        int ty = (int) Math.floor(py / TILE_SIZE);
        for (TiledMapTileLayer layer : layers) {
            if (tx < 0 || ty < 0 || tx >= layer.getWidth() || ty >= layer.getHeight()) return true;
            if (layer.getCell(tx, ty) != null) return true;
        }
        return false;
    }

    /**
     * Vrai si aucun mur ne se trouve sur la ligne entre 'from' et 'to'.
     *
     * @param layers calques qui bloquent
     * @param from point de départ
     * @param to point d'arrivée
     * @return vrai si la ligne est dégagée
     */
    public static boolean hasLineOfSight(TiledMapTileLayer[] layers, Vector2 from, Vector2 to) {
        float dist = from.dst(to);
        int steps = (int) Math.ceil(dist / 4f);   // on teste un point tous les 4 px
        for (int i = 1; i < steps; i++) {
            float t = i / (float) steps;
            float x = from.x + (to.x - from.x) * t;
            float y = from.y + (to.y - from.y) * t;
            if (isSolid(layers, x, y)) return false;
        }
        return true;
    }
    /**
     * Vrai si le rectangle touche une case non vide d'une des couches (ou sort de la map).
     *
     * @param layers calques à tester
     * @param r rectangle à tester
     * @return vrai si le rectangle touche un obstacle
     */
    public static boolean overlapsSolid(TiledMapTileLayer[] layers, Rectangle r) {
        int x0 = (int) Math.floor(r.x / TILE_SIZE);
        int x1 = (int) Math.floor((r.x + r.width) / TILE_SIZE);
        int y0 = (int) Math.floor(r.y / TILE_SIZE);
        int y1 = (int) Math.floor((r.y + r.height) / TILE_SIZE);

        for (int tx = x0; tx <= x1; tx++) {
            for (int ty = y0; ty <= y1; ty++) {
                for (TiledMapTileLayer layer : layers) {
                    if (tx < 0 || ty < 0 || tx >= layer.getWidth() || ty >= layer.getHeight()) return true;
                    if (layer.getCell(tx, ty) != null) return true;
                }
            }
        }
        return false;
    }
}
