package fr.epitech.katanagame.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Disposable;

/**
 * Fond animé des menus, entièrement dessiné en code : un ciel de crépuscule,
 * un « soleil » technologique cyan, des ruines de tours, une sentinelle mécanique
 * sur l'horizon et des braises qui montent.
 * Coordonnées logiques : 960 x 540.
 */
public class MenuBackground implements Disposable {
    public static final float W = 960f;
    public static final float H = 540f;

    private static final Color SKY_TOP = Color.valueOf("0a0d12");
    private static final Color SKY_LOW = Color.valueOf("4a2a18");
    private static final Color SILHOUETTE = Color.valueOf("07090c");

    private static final float SUN_X = 780f;
    private static final float SUN_Y = 300f;
    private static final int EMBERS = 45;

    // Ruines : x, largeur, hauteur (laissées libres au centre pour les boutons)
    private static final float[][] TOWERS = {
            {20, 90, 240}, {120, 60, 160}, {190, 70, 200},
            {700, 70, 170}, {790, 120, 260}, {910, 60, 190}
    };

    private final ShapeRenderer shapes = new ShapeRenderer();
    private final float[] ex = new float[EMBERS];
    private final float[] ey = new float[EMBERS];
    private final float[] es = new float[EMBERS];
    private final float[] ep = new float[EMBERS];
    private float time = 0f;

    /** Initialise les particules décoratives du fond de menu. */
    public MenuBackground() {
        for (int i = 0; i < EMBERS; i++) respawn(i, true);
    }

    /** Place une braise à une nouvelle position. */
    private void respawn(int i, boolean anywhere) {
        ex[i] = MathUtils.random(W);
        ey[i] = anywhere ? MathUtils.random(H) : -5f;
        es[i] = MathUtils.random(12f, 40f);
        ep[i] = MathUtils.random(6.28f);
    }

    /** À appeler à chaque frame, avant de dessiner l'interface. */
    public void render(float delta, Matrix4 projection) {
        time += delta;
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.setProjectionMatrix(projection);

        float pulse = MathUtils.sin(time * 1.4f);

        // 1. Ciel et halo
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.rect(0, 0, W, H, SKY_LOW, SKY_LOW, SKY_TOP, SKY_TOP);
        for (int i = 0; i < 6; i++) {
            float r = 210f - i * 30f + pulse * 5f;
            shapes.setColor(Theme.CYAN.r, Theme.CYAN.g, Theme.CYAN.b, 0.025f + i * 0.012f);
            shapes.circle(SUN_X, SUN_Y, r, 64);
        }
        shapes.end();

        // 2. Anneau technologique qui tourne lentement
        Gdx.gl.glLineWidth(2f);
        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(Theme.CYAN.r, Theme.CYAN.g, Theme.CYAN.b, 0.45f);
        shapes.circle(SUN_X, SUN_Y, 135f, 64);
        for (int i = 0; i < 36; i++) {
            float a = time * 8f + i * 10f;
            float c = MathUtils.cosDeg(a);
            float s = MathUtils.sinDeg(a);
            float r1 = (i % 3 == 0) ? 148f : 156f;
            shapes.line(SUN_X + c * r1, SUN_Y + s * r1, SUN_X + c * 168f, SUN_Y + s * 168f);
        }
        shapes.end();

        // 3. Sol, ruines, sentinelle, braises
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(SILHOUETTE);
        shapes.rect(0, 0, W, 60f);
        for (float[] t : TOWERS) drawTower(t[0], t[1], t[2]);
        drawSentinel(215f, 330f, pulse);
        drawEmbers(delta);
        shapes.end();
    }

    /** Dessine une silhouette de bâtiment. */
    private void drawTower(float x, float w, float h) {
        shapes.setColor(SILHOUETTE);
        shapes.rect(x, 0, w, h);
        for (float cx = x; cx < x + w - 6f; cx += 20f) {
            shapes.rect(cx, h, 10f, 12f);   // créneaux
        }
        // Fenêtres éclairées qui vacillent : une cyan (technologie), une orange (feu)
        float flicker = 0.55f + 0.35f * MathUtils.sin(time * 2.2f + x);
        shapes.setColor(Theme.CYAN.r, Theme.CYAN.g, Theme.CYAN.b, flicker);
        shapes.rect(x + w * 0.5f - 4f, h * 0.6f, 8f, 14f);
        shapes.setColor(Theme.EMBER.r, Theme.EMBER.g, Theme.EMBER.b, flicker);
        shapes.rect(x + w * 0.5f - 4f, h * 0.3f, 8f, 14f);
    }

    /** Silhouette d'une machine à longues pattes, avec un œil cyan qui pulse. */
    private void drawSentinel(float cx, float cy, float pulse) {
        shapes.setColor(SILHOUETTE);
        shapes.ellipse(cx - 55f, cy - 14f, 110f, 28f);
        for (int i = -1; i <= 1; i++) {
            shapes.rectLine(cx + i * 28f, cy - 8f, cx + i * 58f, 60f, 5f);
        }
        shapes.rectLine(cx + 40f, cy + 6f, cx + 62f, cy + 30f, 6f);
        shapes.setColor(Theme.CYAN.r, Theme.CYAN.g, Theme.CYAN.b, 0.7f + 0.3f * pulse);
        shapes.circle(cx + 64f, cy + 32f, 5f, 16);
    }

    /** Anime et dessine les braises flottantes. */
    private void drawEmbers(float delta) {
        for (int i = 0; i < EMBERS; i++) {
            ey[i] += es[i] * delta;
            if (ey[i] > H + 5f) respawn(i, false);
            float x = ex[i] + MathUtils.sin(time * 0.8f + ep[i]) * 12f;
            float fade = 1f - ey[i] / H;
            shapes.setColor(Theme.EMBER.r, Theme.EMBER.g, Theme.EMBER.b, 0.15f + 0.6f * fade);
            shapes.rect(x, ey[i], 2f, 2f);
        }
    }

    /** Libère les ressources graphiques du fond. */
    @Override
    public void dispose() {
        shapes.dispose();
    }
}
