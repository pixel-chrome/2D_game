package fr.epitech.katanagame.render;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;


/**
 * Effets visuels du pack « VFX Free Pack » (assets/vfx). Chaque feuille est une grille 6 x 5 de 30 images à 30 i/s,
 * soit une seconde d'animation. Les effets « en boucle » servent d'aura tant que le boss est dans l'état voulu.
 */
public final class Vfx {
    private static final int COLS = 6, ROWS = 5, FRAMES = COLS * ROWS;
    private static final float FPS = 30f;

    /** Effets disponibles. */
    public enum Kind {
        /** Explosion (mort du boss). */
        EXPLOSION("explosion", false),
        /** Petite gerbe d'étincelles (dégât). */
        SMALL_HIT("small_hit", false),
        /** Grosse gerbe d'étincelles (projectile renvoyé). */
        BIG_HIT("big_hit", false),
        /** Éclats blancs (impact d'une attaque). */
        IMPACT("impact", false),
        /** Onde de choc en roue (tirs, balayage). */
        WHEEL("wheel", false),
        /** Anneau électrique : le boss charge une attaque. */
        ELECTRIC_SHIELD("electric_shield", true),
        /** Flammes bleues : le boss est enragé. */
        ANIMA("anima", true),
        /** Anneau de feu (changement de phase). */
        ELDEN_RING("elden_ring", false);

        private final String file;
        private final boolean loops;
        private Texture texture;
        private TextureRegion[] frames;

        Kind(String file, boolean loops) {
            this.file = file;
            this.loops = loops;
        }
    }


    private Vfx() {}

    /** Charge toutes les feuilles (une seule fois). */
    public static void loadAssets() {
        for (Kind kind : Kind.values()) {
            if (kind.texture != null) continue;
            kind.texture = new Texture("vfx/" + kind.file + ".png");
            kind.texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            TextureRegion[][] grid = TextureRegion.split(kind.texture, kind.texture.getWidth() / COLS, kind.texture.getHeight() / ROWS);
            kind.frames = new TextureRegion[FRAMES];
            for (int i = 0; i < FRAMES; i++) kind.frames[i] = grid[i / COLS][i % COLS];
        }
    }

    /** Libère les feuilles. */
    public static void disposeAssets() {
        for (Kind kind : Kind.values()) {
            if (kind.texture == null) continue;
            kind.texture.dispose();
            kind.texture = null;
            kind.frames = null;
        }
    }

    /**
     * Durée d'une lecture de l'effet.
     *
     * @return la durée (s)
     */
    public static float duration() { return FRAMES / FPS; }

    /**
     * Dessine l'effet centré en (x, y), carré de côté size.
     *
     * @param batch batch déjà ouvert
     * @param kind effet
     * @param x abscisse du centre
     * @param y ordonnée du centre
     * @param size côté du carré dessiné (px)
     * @param age temps écoulé depuis le début (s) ; en boucle pour les effets qui bouclent
     * @param alpha opacité (0 à 1)
     */
    public static void draw(SpriteBatch batch, Kind kind, float x, float y, float size, float age, float alpha) {
        if (kind.frames == null) return;
        int index = (int) (age * FPS);
        if (kind.loops) index %= FRAMES;
        else if (index >= FRAMES) return;
        TextureRegion frame = kind.frames[index];
        float h = size * frame.getRegionHeight() / frame.getRegionWidth();
        batch.setColor(1f, 1f, 1f, alpha);
        batch.draw(frame, x - size / 2f, y - h / 2f, size, h);
        batch.setColor(1f, 1f, 1f, 1f);
    }

    /**
     * Vrai si l'effet est chargé (faux dans les tests, sans fenêtre).
     *
     * @param kind effet
     * @return vrai si on peut le dessiner
     */
    public static boolean isLoaded(Kind kind) { return kind.frames != null; }
}
