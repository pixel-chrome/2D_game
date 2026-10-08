package fr.epitech.katanagame;

import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.math.Vector2;

/** Fonctions utilitaires pour tester la map (balles, ligne de vue des ennemis). */
public final class Collision {
    private static final float TILE_SIZE = 16f;

    private Collision() {}

    /** Vrai si le point (px, py) est dans une case non vide d'une des couches (ou hors de la map). */
    public static boolean isSolid(TiledMapTileLayer[] layers, float px, float py) {
        int tx = (int) Math.floor(px / TILE_SIZE);
        int ty = (int) Math.floor(py / TILE_SIZE);
        for (TiledMapTileLayer layer : layers) {
            if (tx < 0 || ty < 0 || tx >= layer.getWidth() || ty >= layer.getHeight()) return true;
            if (layer.getCell(tx, ty) != null) return true;
        }
        return false;
    }

    /** Vrai si aucun mur ne se trouve sur la ligne entre 'from' et 'to'. */
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
}