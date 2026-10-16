package fr.epitech.katanagame.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Disposable;

/**
 * Fond des menus : une nuit grise et douce, une lueur diffuse derrière un katana
 * en silhouette, une brume au ras du sol. Presque monochrome, sans éléments
 * « bruités » : seulement des dégradés lisses, donc aucune coupure visible.
 * Le katana respire à peine et un reflet glisse sur sa lame de temps en temps.
 * Tout est dessiné en code. Coordonnées logiques : 960 x 540.
 */
public class MenuBackground implements Disposable {
    public static final float W = 960f;
    public static final float H = 540f;

    private static final Color SKY_TOP = Color.valueOf("090b0f");
    private static final Color SKY_LOW = Color.valueOf("24282e");

    // Lumière : (centre x, centre y, rayon x, rayon y, opacité, r, g, b)
    private static final float[] HALO = { 705f, 290f, 540f, 420f, 0.36f, 0.784f, 0.816f, 0.847f };
    private static final float[] LEFT_GLOW = { 250f, 270f, 460f, 340f, 0.11f, 0.647f, 0.682f, 0.729f };
    private static final float[] MIST = { 480f, 30f, 720f, 190f, 0.32f, 0.588f, 0.620f, 0.651f };
    // Grands voiles très légers qui dérivent : (x, y, rayon x, rayon y, opacité, décalage)
    private static final float[][] VEILS = {
            { 250f, 190f, 330f, 120f, 0.05f, 0.0f },
            { 720f, 210f, 380f, 130f, 0.05f, 2.1f },
            { 480f, 330f, 420f, 120f, 0.03f, 4.2f }
    };

    // Katana
    private static final Color BLADE_L = Color.valueOf("2e3136");
    private static final Color BLADE_R = Color.valueOf("14161a");
    private static final Color COLLAR = Color.valueOf("34363a");
    private static final Color GUARD = Color.valueOf("2a2c30");
    private static final Color HANDLE = Color.valueOf("0e1012");
    private static final Color WRAP = Color.valueOf("232629");
    private static final float KATANA_X = 700f;   // position de la garde
    private static final float KATANA_Y = 120f;
    private static final float BLADE_LEN = 390f;
    private static final float GLINT_PERIOD = 7f;   // un reflet toutes les 7 secondes
    private static final float GLINT_TIME = 1.3f;   // il met 1,3 s à parcourir la lame

    private final FilmOverlay film;
    private final ShapeRenderer shapes = new ShapeRenderer();
    private final SpriteBatch batch = new SpriteBatch();
    private final Texture softTex;
    private final Color tmp = new Color();

    private float time = 0f;

    // repère du katana (rotation autour de la garde)
    private float kc = 1f, ks = 0f;

    public MenuBackground(FilmOverlay film) {
        this.film = film;

        // Une seule texture sert à toutes les lueurs : un halo doux, transparent sur les bords
        Pixmap pm = new Pixmap(128, 128, Pixmap.Format.RGBA8888);
        pm.setBlending(Pixmap.Blending.None);
        for (int y = 0; y < 128; y++) {
            for (int x = 0; x < 128; x++) {
                float dx = (x + 0.5f) / 128f * 2f - 1f;
                float dy = (y + 0.5f) / 128f * 2f - 1f;
                float d = Math.min(1f, (float) Math.sqrt(dx * dx + dy * dy));
                float a = (1f - d) * (1f - d);
                pm.setColor(1f, 1f, 1f, a);
                pm.drawPixel(x, y);
            }
        }
        softTex = new Texture(pm);
        pm.dispose();
        softTex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
    }

    /** À appeler à chaque frame, avant de dessiner l'interface. */
    public void render(float delta, Matrix4 projection) {
        time += delta;
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.setProjectionMatrix(projection);
        batch.setProjectionMatrix(projection);

        // 1. Ciel : un dégradé vertical lisse
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.rect(0f, 0f, W, H, SKY_LOW, SKY_LOW, SKY_TOP, SKY_TOP);
        shapes.end();

        // 2. Lueur derrière le katana (elle « respire » doucement) et lueur faible à gauche
        batch.begin();
        glow(HALO, 0.36f + 0.025f * MathUtils.sin(time * 0.6f));
        glow(LEFT_GLOW, 0.11f + 0.01f * MathUtils.sin(time * 0.45f + 1.3f));
        batch.setColor(1f, 1f, 1f, 1f);
        batch.end();

        // 3. Katana
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        drawKatana();
        shapes.end();

        // 4. Brume au sol et voiles qui dérivent lentement devant le katana
        batch.begin();
        glow(MIST, MIST[4]);
        for (float[] v : VEILS) {
            float drift = MathUtils.sin(time * 0.08f + v[5]) * 60f;
            batch.setColor(0.667f, 0.698f, 0.729f, v[4] * (0.85f + 0.15f * MathUtils.sin(time * 0.3f + v[5])));
            batch.draw(softTex, v[0] + drift - v[2], v[1] - v[3], v[2] * 2f, v[3] * 2f);
        }
        batch.setColor(1f, 1f, 1f, 1f);
        batch.end();

        // 5. Vignette 
        film.draw(delta, projection, 0f, 1f);
    }

    /** Dessine une lueur : p = {x, y, rayonX, rayonY, opacité, r, g, b}. */
    private void glow(float[] p, float alpha) {
        batch.setColor(p[5], p[6], p[7], alpha);
        batch.draw(softTex, p[0] - p[2], p[1] - p[3], p[2] * 2f, p[3] * 2f);
    }

    // ---------------------------------------------------------------- katana

    private float wx(float lx, float ly) { return KATANA_X + lx * kc - ly * ks; }

    private float wy(float lx, float ly) { return KATANA_Y + lx * ks + ly * kc; }

    private void tri(float ax, float ay, float bx, float by, float cx, float cy, Color col) {
        shapes.triangle(wx(ax, ay), wy(ax, ay), wx(bx, by), wy(bx, by), wx(cx, cy), wy(cx, cy), col, col, col);
    }

    private void quad(float ax, float ay, float bx, float by, float cx, float cy, float dx, float dy, Color col) {
        tri(ax, ay, bx, by, cx, cy, col);
        tri(ax, ay, cx, cy, dx, dy, col);
    }

    private void line(float ax, float ay, float bx, float by, float width, Color col) {
        shapes.setColor(col);
        shapes.rectLine(wx(ax, ay), wy(ax, ay), wx(bx, by), wy(bx, by), width);
    }

    /** Silhouette d'un katana, lame vers le haut, légèrement penché, qui respire. */
    private void drawKatana() {
        float ang = (-7f + MathUtils.sin(time * 0.5f) * 0.8f) * MathUtils.degRad;
        kc = MathUtils.cos(ang);
        ks = MathUtils.sin(ang);
        float shoulder = BLADE_LEN - 30f;

        // Lame : deux faces de teintes différentes pour donner du relief
        quad(-14f, 0f, 0f, 0f, 0f, shoulder, -9f, shoulder, BLADE_L);
        quad(0f, 0f, 14f, 0f, 9f, shoulder, 0f, shoulder, BLADE_R);
        tri(-9f, shoulder, 0f, shoulder, 0f, BLADE_LEN, BLADE_L);
        tri(0f, shoulder, 9f, shoulder, 0f, BLADE_LEN, BLADE_R);

        // Fin reflet permanent le long du tranchant
        tmp.set(0.86f, 0.85f, 0.80f, 0.18f + 0.08f * MathUtils.sin(time * 0.9f));
        line(-13f, 8f, -8.4f, shoulder - 4f, 1.4f, tmp);

        // Reflet brillant qui glisse le long de la lame, de temps en temps
        float g = (time % GLINT_PERIOD) / GLINT_TIME;
        if (g < 1f) {
            float gy = g * shoulder;
            tmp.set(0.94f, 0.93f, 0.89f, 0.6f * MathUtils.sin(g * MathUtils.PI));
            line(-12f, gy - 22f, -9.6f, gy + 22f, 2f, tmp);
        }

        // Collier et garde (tsuba)
        quad(-16f, -5f, 16f, -5f, 16f, 14f, -16f, 14f, COLLAR);
        int seg = 24;
        for (int i = 0; i < seg; i++) {
            float a0 = i / (float) seg * MathUtils.PI2;
            float a1 = (i + 1) / (float) seg * MathUtils.PI2;
            tri(0f, -8f,
                    MathUtils.cos(a0) * 40f, MathUtils.sin(a0) * 9f - 8f,
                    MathUtils.cos(a1) * 40f, MathUtils.sin(a1) * 9f - 8f, GUARD);
        }

        // Manche, avec ses enroulements
        quad(-12f, -14f, 12f, -14f, 12f, -240f, -12f, -240f, HANDLE);
        for (float y = -22f; y > -220f; y -= 15f) {
            line(-12f, y, 12f, y - 12f, 1.6f, WRAP);
        }
    }

    @Override
    public void dispose() {
        shapes.dispose();
        batch.dispose();
        softTex.dispose();
    }
}
