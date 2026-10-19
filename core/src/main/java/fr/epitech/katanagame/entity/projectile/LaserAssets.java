package fr.epitech.katanagame.entity.projectile;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.MathUtils;

/**
 * Charge et gère les textures des projectiles laser.
 */
public final class LaserAssets {

    private static final int FIRST_SPRITE = 11;
    private static final int LAST_SPRITE = 29;

    private static Texture[] textures;

    private LaserAssets() {
    }

    /**
     * Charge les textures des lasers si elles ne sont pas déjà chargées.
     */
    public static void loadAssets() {
        if (textures != null) {
            return;
        }

        textures = new Texture[LAST_SPRITE - FIRST_SPRITE + 1];

        for (int i = FIRST_SPRITE; i <= LAST_SPRITE; i++) {
            textures[i - FIRST_SPRITE] =
                    new Texture("Laser Sprites/" + i + ".png");
        }
    }

    /**
     * Retourne une texture laser choisie aléatoirement.
     */
    public static Texture getRandomTexture() {
        loadAssets();
        return textures[MathUtils.random(textures.length - 1)];
    }

    /**
     * Libère les textures chargées.
     */
    public static void disposeAssets() {
        if (textures == null) {
            return;
        }

        for (Texture texture : textures) {
            texture.dispose();
        }

        textures = null;
    }
}