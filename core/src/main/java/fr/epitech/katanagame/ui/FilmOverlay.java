package fr.epitech.katanagame.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Disposable;

/**
 * Le « look pellicule » : un voile sombre optionnel, une vignette (bords noirs). Sert au menu principal, à la pause et aux paramètres.
 * Coordonnées logiques : 960 x 540 (comme les menus).
 */
public class FilmOverlay implements Disposable {
    private static final float W = MenuBackground.W;
    private static final float H = MenuBackground.H;
    private static final float VIGNETTE_MAX = 0.78f; // noirceur des coins (0 = aucune vignette, 1 = noir total)

    private final SpriteBatch batch = new SpriteBatch();
    private final Texture white;
    private final Texture vignette;

    /** Crée les textures du voile et de la vignette. */
    public FilmOverlay() {
        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(Color.WHITE);
        pm.fill();
        white = new Texture(pm);
        pm.dispose();

        // Vignette : transparente au centre, noire dans les coins
        int n = 256;
        pm = new Pixmap(n, n, Pixmap.Format.RGBA8888);
        pm.setBlending(Pixmap.Blending.None);
        for (int py = 0; py < n; py++) {
            for (int px = 0; px < n; px++) {
                float dx = (px + 0.5f) / n * 2f - 1f;
                float dy = (py + 0.5f) / n * 2f - 1f;
                float d = (float) Math.sqrt(dx * dx + dy * dy) / 1.4142f;
                float t = MathUtils.clamp((d - 0.30f) / 0.70f, 0f, 1f);
                float a = t * t * (3f - 2f * t);   // smoothstep
                pm.setColor(0f, 0f, 0f, a * VIGNETTE_MAX);
                pm.drawPixel(px, py);
            }
        }
        vignette = new Texture(pm);
        pm.dispose();
        vignette.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

    }

    /**
     * Dessine le voile sombre puis la vignette.
     *
     * @param dim voile noir (0 = aucun, 0.6 = pause)
     * @param vignetteAlpha intensité de la vignette (0 à 1)
     * @param delta temps écoulé (s)
     * @param projection matrice de projection des menus
     */
    public void draw(float delta, Matrix4 projection, float dim, float vignetteAlpha) {
        batch.setProjectionMatrix(projection);
        batch.begin();
        if (dim > 0f) {
            batch.setColor(0f, 0f, 0f, dim);
            batch.draw(white, 0f, 0f, W, H);
        }
        batch.setColor(1f, 1f, 1f, vignetteAlpha);
        batch.draw(vignette, 0f, 0f, W, H);
        batch.setColor(1f, 1f, 1f, 1f);
        batch.end();
    }

    @Override
    public void dispose() {
        batch.dispose();
        white.dispose();
        vignette.dispose();
    }
}
