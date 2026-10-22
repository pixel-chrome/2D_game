package fr.epitech.katanagame.render;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.math.Vector2;
import fr.epitech.katanagame.world.Collision;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Random;

/**
 * Sang façon Hotline Miami : une gerbe de gouttes qui giclent puis restent au sol,
 * une mare qui s'étale sous le corps et un flash blanc à l'impact.
 * Tout est dessiné en formes pleines, sous les personnages.
 */
public final class BloodEffects {
    private static final int MAX_STAINS = 900;
    private static final float DRAG = 5f;                // freinage des gouttes en vol
    private static final float TRAIL_STEP = 5f;          // une tache laissée tous les N px de vol
    private static final float FLASH_DECAY = 7f;
    private static final float DRY_TIME = 2.5f;          // temps pour passer du rouge vif au rouge sombre
    private static final int POOL_BLOBS = 8;

    private static final Color FRESH = new Color(0.85f, 0.04f, 0.05f, 1f);
    private static final Color DRIED = new Color(0.36f, 0.02f, 0.04f, 1f);
    private static final Color RIM = new Color(0.2f, 0.01f, 0.02f, 1f);

    private final Random random = new Random();
    private final List<Drop> drops = new ArrayList<>();
    private final Deque<Stain> stains = new ArrayDeque<>();
    private final Color tmp = new Color();

    // Mare sous le corps : plusieurs disques qui grossissent en s'étalant.
    private final Vector2 poolCenter = new Vector2();
    private final float[] blobX = new float[POOL_BLOBS], blobY = new float[POOL_BLOBS], blobR = new float[POOL_BLOBS];
    private boolean pool;
    private float poolTime;
    private float flash;

    private static final class Drop {
        float x, y, vx, vy, size, age, trail;
    }

    private static final class Stain {
        final float x, y, size;
        float age;
        Stain(float x, float y, float size) { this.x = x; this.y = y; this.size = size; }
    }

    /** Crée un système de sang vide. */
    public BloodEffects() {}

    /**
     * Gerbe de sang depuis un point, projetée surtout dans la direction du tir.
     *
     * @param at point d'impact
     * @param dir direction du tir
     * @param walls calques qui arrêtent les gouttes
     */
    public void burst(Vector2 at, Vector2 dir, TiledMapTileLayer[] walls) {
        spray(at, dir, 80, 0.65f, 70f, 1f);
        for (int i = 0; i < 10; i++) {                                // gros morceaux, plus lents
            addDrop(at, random.nextFloat() * 360f, 30f + random.nextFloat() * 110f, 3.5f + random.nextFloat() * 2f);
        }

        pool = true;
        poolTime = 0f;
        poolCenter.set(at);
        for (int i = 0; i < POOL_BLOBS; i++) {
            float a = random.nextFloat() * (float) (2 * Math.PI);
            float d = random.nextFloat() * 10f;
            blobX[i] = (float) Math.cos(a) * d;
            blobY[i] = (float) Math.sin(a) * d * 0.7f;               // un peu aplati : vu de dessus en perspective
            blobR[i] = 7f + random.nextFloat() * 9f;
        }
        flash = 0.55f;
    }

    /**
     * Sang d'un ennemi tué : gerbe plus petite et quelques taches au sol, sans flash ni mare.
     *
     * @param at point d'impact
     * @param dir direction du coup
     * @param walls calques qui arrêtent les gouttes
     */
    public void splatter(Vector2 at, Vector2 dir, TiledMapTileLayer[] walls) {
        spray(at, dir, 45, 0.7f, 60f, 0.87f);
        for (int i = 0; i < 6; i++) {                                 // petite flaque faite de grosses taches
            float a = random.nextFloat() * (float) (2 * Math.PI), d = random.nextFloat() * 8f;
            addStain(at.x + (float) Math.cos(a) * d, at.y + (float) Math.sin(a) * d * 0.7f, 4f + random.nextFloat() * 4f);
        }
    }

    /**
     * Lance count gouttes : une part (focus) en jet serré dans l'axe de dir, le reste dans toutes
     * les directions, plus lentement. power règle la vitesse globale.
     */
    private void spray(Vector2 at, Vector2 dir, int count, float focus, float spreadDeg, float power) {
        float base = dir.isZero() ? random.nextFloat() * 360f : dir.angleDeg();
        for (int i = 0; i < count; i++) {
            boolean jet = random.nextFloat() < focus;
            float angle = jet ? base + (random.nextFloat() - 0.5f) * spreadDeg : random.nextFloat() * 360f;
            float speed = jet ? 140f + random.nextFloat() * 300f : 40f + random.nextFloat() * 160f;
            addDrop(at, angle, speed * power, 1.2f + random.nextFloat() * 2.4f);
        }
    }

    private void addDrop(Vector2 at, float angleDeg, float speed, float size) {
        Drop d = new Drop();
        d.x = at.x;
        d.y = at.y;
        float rad = angleDeg * (float) (Math.PI / 180.0);
        d.vx = (float) Math.cos(rad) * speed;
        d.vy = (float) Math.sin(rad) * speed;
        d.size = size;
        drops.add(d);
    }

    /**
     * Fait avancer les gouttes (qui retombent en taches), la mare et le flash.
     *
     * @param delta temps écoulé (s)
     * @param walls calques qui arrêtent les gouttes
     */
    public void update(float delta, TiledMapTileLayer[] walls) {
        if (delta <= 0f) return;
        flash = Math.max(0f, flash - FLASH_DECAY * delta);
        if (pool) poolTime += delta;
        for (Stain s : stains) s.age += delta;

        for (int i = drops.size() - 1; i >= 0; i--) {
            Drop d = drops.get(i);
            float nx = d.x + d.vx * delta, ny = d.y + d.vy * delta;
            float k = Math.max(0f, 1f - DRAG * delta);
            d.vx *= k;
            d.vy *= k;
            d.age += delta;

            boolean blocked = Collision.isSolid(walls, nx, ny);
            if (!blocked) {
                d.trail += (float) Math.hypot(nx - d.x, ny - d.y);
                d.x = nx;
                d.y = ny;
                if (d.trail >= TRAIL_STEP) { d.trail = 0f; addStain(d.x, d.y, d.size * 0.45f); }
            }
            if (blocked || d.age > 0.6f || Math.hypot(d.vx, d.vy) < 18f) {
                addStain(d.x, d.y, d.size);
                drops.remove(i);
            }
        }
    }

    private void addStain(float x, float y, float size) {
        stains.addLast(new Stain(x, y, size));
        if (stains.size() > MAX_STAINS) stains.removeFirst();
    }

    /** Remet tout à zéro (recommencer une partie). */
    public void clear() {
        drops.clear();
        stains.clear();
        pool = false;
        flash = 0f;
    }

    /**
     * Taches, mare et gouttes en vol. À dessiner sous les sprites, ShapeRenderer en mode Filled.
     *
     * @param shapes Le ShapeRenderer, déjà ouvert en mode Filled.
     */
    public void draw(ShapeRenderer shapes) {
        if (pool) {
            float grow = 1f - (float) Math.exp(-poolTime * 2.5f);
            mix(0.8f * FRESH.r, 0.8f * FRESH.g, 0.8f * FRESH.b, Math.min(1f, poolTime / DRY_TIME));
            float r = tmp.r, g = tmp.g, b = tmp.b;
            shapes.setColor(RIM);                                     // liseré sombre autour de la mare
            for (int i = 0; i < POOL_BLOBS; i++) {
                shapes.circle(poolCenter.x + blobX[i] * grow, poolCenter.y + blobY[i] * grow, blobR[i] * grow + 1.5f, 16);
            }
            shapes.setColor(r, g, b, 1f);
            for (int i = 0; i < POOL_BLOBS; i++) {
                shapes.circle(poolCenter.x + blobX[i] * grow, poolCenter.y + blobY[i] * grow, blobR[i] * grow, 16);
            }
        }
        for (Stain s : stains) {
            mix(FRESH.r, FRESH.g, FRESH.b, Math.min(1f, s.age / DRY_TIME));
            shapes.setColor(tmp.r, tmp.g, tmp.b, 1f);
            shapes.circle(s.x, s.y, s.size, 8);
        }
        shapes.setColor(FRESH);
        for (Drop d : drops) shapes.circle(d.x, d.y, d.size, 8);
    }

    /**
     * Flash blanc teinté de rouge à l'impact, couvrant toute la vue.
     *
     * @param shapes Le ShapeRenderer, déjà ouvert en mode Filled.
     * @param camera caméra de la partie
     */
    public void drawFlash(ShapeRenderer shapes, Camera camera) {
        if (flash <= 0f) return;
        shapes.setColor(1f, 0.9f, 0.9f, Math.min(1f, flash));
        shapes.rect(camera.position.x - camera.viewportWidth, camera.position.y - camera.viewportHeight,
                camera.viewportWidth * 2f, camera.viewportHeight * 2f);
    }

    /** Couleur intermédiaire entre (r,g,b) frais et le rouge séché, rangée dans tmp. */
    private void mix(float r, float g, float b, float dry) {
        tmp.set(r + (DRIED.r - r) * dry, g + (DRIED.g - g) * dry, b + (DRIED.b - b) * dry, 1f);
    }
}
