package fr.epitech.katanagame.world;

import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Recherche de chemin A* sur la grille de la map, pour contourner les murs.
 * Toutes les coordonnées sont celles des PIEDS (là où se fait la collision).
 */
public final class Pathfinder {
    private static final float TILE = 16f;
    private static final float BODY_W = 11f, BODY_H = 7f;   // pieds + petite marge
    private static final int MAX_EXPANSIONS = 6000;
    private static final float SAMPLE_STEP = 3f;
    private static final float DIAG_COST = 1.4142f;
    private static final int[][] DIRS = {
            {1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}};

    /** Classe utilitaire : pas d'instance. */
    private Pathfinder() {}

    /** Vrai si un corps de la taille des pieds peut aller de 'from' à 'to' en ligne droite. */
    public static boolean isClear(TiledMapTileLayer[] layers, Vector2 from, Vector2 to) {
        float dist = from.dst(to);
        int steps = Math.max(1, (int) Math.ceil(dist / SAMPLE_STEP));
        Rectangle box = new Rectangle(0, 0, BODY_W, BODY_H);
        for (int i = 1; i <= steps; i++) {
            float t = i / (float) steps;
            box.setCenter(from.x + (to.x - from.x) * t, from.y + (to.y - from.y) * t);
            if (Collision.overlapsSolid(layers, box)) return false;
        }
        return true;
    }

    /**
     * Chemin de 'from' à 'to' (sans le point de départ), lissé en quelques points de passage.
     * Liste vide si déjà en ligne droite ; null si aucun chemin.
     */
    public static List<Vector2> findPath(TiledMapTileLayer[] layers, Vector2 from, Vector2 to) {
        if (isClear(layers, from, to)) return new ArrayList<>();
        if (layers.length == 0) return null;

        int w = layers[0].getWidth(), h = layers[0].getHeight();
        int sx = tile(from.x), sy = tile(from.y);
        int[] goal = nearestFree(layers, tile(to.x), tile(to.y), w, h);
        if (goal == null || !inMap(sx, sy, w, h)) return null;

        float[] g = new float[w * h];
        int[] parent = new int[w * h];
        boolean[] closed = new boolean[w * h];
        java.util.Arrays.fill(g, Float.MAX_VALUE);
        java.util.Arrays.fill(parent, -1);

        PriorityQueue<float[]> open = new PriorityQueue<>((a, b) -> Float.compare(a[0], b[0]));
        int start = sy * w + sx, end = goal[1] * w + goal[0];
        g[start] = 0f;
        open.add(new float[]{heuristic(sx, sy, goal[0], goal[1]), start});

        int expansions = 0;
        while (!open.isEmpty() && expansions++ < MAX_EXPANSIONS) {
            int cur = (int) open.poll()[1];
            if (closed[cur]) continue;
            closed[cur] = true;
            if (cur == end) return smooth(layers, from, to, rebuild(parent, cur, w));

            int cx = cur % w, cy = cur / w;
            for (int[] d : DIRS) {
                int nx = cx + d[0], ny = cy + d[1];
                if (!inMap(nx, ny, w, h) || isWall(layers, nx, ny)) continue;
                boolean diag = d[0] != 0 && d[1] != 0;
                // pas de passage en diagonale entre deux coins de mur
                if (diag && (isWall(layers, cx + d[0], cy) || isWall(layers, cx, cy + d[1]))) continue;
                int n = ny * w + nx;
                float cost = g[cur] + (diag ? DIAG_COST : 1f);
                if (cost < g[n]) {
                    g[n] = cost;
                    parent[n] = cur;
                    open.add(new float[]{cost + heuristic(nx, ny, goal[0], goal[1]), n});
                }
            }
        }
        return null;
    }

    /** Retire les points inutiles : on saute directement au point le plus loin encore atteignable. */
    private static List<Vector2> smooth(TiledMapTileLayer[] layers, Vector2 from, Vector2 to, List<Vector2> raw) {
        raw.add(to.cpy());
        List<Vector2> result = new ArrayList<>();
        Vector2 anchor = from;
        int i = 0;
        while (i < raw.size()) {
            int far = i;
            for (int j = raw.size() - 1; j > i; j--) {
                if (isClear(layers, anchor, raw.get(j))) { far = j; break; }
            }
            anchor = raw.get(far);
            result.add(anchor);
            i = far + 1;
        }
        return result;
    }

    private static List<Vector2> rebuild(int[] parent, int end, int w) {
        List<Vector2> path = new ArrayList<>();
        for (int n = end; n != -1; n = parent[n]) {
            path.add(new Vector2((n % w + 0.5f) * TILE, (n / w + 0.5f) * TILE));
        }
        Collections.reverse(path);
        if (!path.isEmpty()) path.remove(0);   // la case de départ
        return path;
    }

    private static int[] nearestFree(TiledMapTileLayer[] layers, int tx, int ty, int w, int h) {
        for (int r = 0; r <= 3; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dy = -r; dy <= r; dy++) {
                    int x = tx + dx, y = ty + dy;
                    if (inMap(x, y, w, h) && !isWall(layers, x, y)) return new int[]{x, y};
                }
            }
        }
        return null;
    }

    private static float heuristic(int x, int y, int gx, int gy) {
        float dx = Math.abs(x - gx), dy = Math.abs(y - gy);
        return Math.max(dx, dy) + (DIAG_COST - 1f) * Math.min(dx, dy);
    }

    private static boolean isWall(TiledMapTileLayer[] layers, int tx, int ty) {
        for (TiledMapTileLayer layer : layers) if (layer.getCell(tx, ty) != null) return true;
        return false;
    }

    private static boolean inMap(int x, int y, int w, int h) {
        return x >= 0 && y >= 0 && x < w && y < h;
    }

    private static int tile(float px) {
        return (int) Math.floor(px / TILE);
    }
}
