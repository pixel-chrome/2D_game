package fr.epitech.katanagame;

import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;

/** Fabrique de petites cartes pour les tests (sans OpenGL ni fichier .tmx). */
public final class TestMaps {
    /** Taille d'une tuile, identique à celle du jeu. */
    public static final int TILE = 16;

    private TestMaps() {}

    /**
     * Construit un calque à partir de lignes de texte, la première ligne étant le haut de la carte.
     *
     * @param rows lignes de même longueur : '#' = tuile solide, tout autre caractère = vide
     * @return le calque correspondant
     */
    public static TiledMapTileLayer grid(String... rows) {
        int width = rows[0].length();
        int height = rows.length;
        TiledMapTileLayer layer = new TiledMapTileLayer(width, height, TILE, TILE);
        for (int row = 0; row < height; row++) {
            for (int x = 0; x < width; x++) {
                if (rows[row].charAt(x) == '#') {
                    layer.setCell(x, height - 1 - row, new TiledMapTileLayer.Cell());
                }
            }
        }
        return layer;
    }

    /**
     * Même chose, rangé dans un tableau comme le attend le jeu.
     *
     * @param rows lignes du calque, voir {@link #grid(String...)}
     * @return un tableau d'un seul calque
     */
    public static TiledMapTileLayer[] layers(String... rows) {
        return new TiledMapTileLayer[] { grid(rows) };
    }
}
