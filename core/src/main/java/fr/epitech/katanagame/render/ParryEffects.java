package fr.epitech.katanagame.render;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

import java.util.ArrayList;
import java.util.List;

/**
 * Effets du parry façon Katana Zero : croissant blanc qui balaie devant le joueur,
 * puis à l'impact un anneau de choc, des étincelles et un flash blanc.
 * Tout est en formes pleines, au-dessus des sprites.
 */
public final class ParryEffects implements ShapeEffect {
    private static final float SWING_TIME = 0.2f, SWING_SPAN = 150f, SWING_RADIUS = 30f;
    private static final float RING_TIME = 0.3f, RING_MAX = 46f;
    private static final float SPARK_TIME = 0.28f, FLASH_DECAY = 9f;
    private static final int ARC_SEGMENTS = 14, SPARKS = 16;

    private final List<Ring> rings = new ArrayList<>();
    private final List<Spark> sparks = new ArrayList<>();
    private final Vector2 swingAt = new Vector2();
    private float swingAngle, swingAge = SWING_TIME;
    private float flash;

    private static final class Ring {
        final float x, y;
        float age;
        Ring(float x, float y) { this.x = x; this.y = y; }
    }

    private static final class Spark {
        float x, y, vx, vy, age;
    }

    /** Crée des effets de parry vides. */
    public ParryEffects() {}

    /**
     * Lance le croissant du parry depuis at, orienté vers angleDeg (direction de visée).
     *
     * @param at centre du balayage
     * @param angleDeg angle de visée (degrés)
     */
    public void swing(Vector2 at, float angleDeg) {
        swingAt.set(at);
        swingAngle = angleDeg;
        swingAge = 0f;
    }

    /**
     * Impact d'un parry réussi en at : anneau, étincelles dans la direction dir et flash.
     *
     * @param at point d'impact
     * @param dir direction du choc
     */
    public void impact(Vector2 at, Vector2 dir) {
        rings.add(new Ring(at.x, at.y));
        float base = dir.isZero() ? 0f : dir.angleDeg();
        for (int i = 0; i < SPARKS; i++) {
            Spark s = new Spark();
            s.x = at.x;
            s.y = at.y;
            float a = (base + MathUtils.random(-70f, 70f)) * MathUtils.degreesToRadians;
            float speed = MathUtils.random(120f, 320f);
            s.vx = MathUtils.cos(a) * speed;
            s.vy = MathUtils.sin(a) * speed;
            sparks.add(s);
        }
        flash = 0.5f;
    }

    /** Fait avancer les effets (delta réel : ils continuent pendant l'arrêt sur image). */
    @Override public void update(float delta) {
        swingAge += delta;
        flash = Math.max(0f, flash - FLASH_DECAY * delta);
        for (int i = rings.size() - 1; i >= 0; i--) {
            Ring r = rings.get(i);
            r.age += delta;
            if (r.age > RING_TIME) rings.remove(i);
        }
        for (int i = sparks.size() - 1; i >= 0; i--) {
            Spark s = sparks.get(i);
            s.age += delta;
            s.x += s.vx * delta;
            s.y += s.vy * delta;
            if (s.age > SPARK_TIME) sparks.remove(i);
        }
    }

    @Override public void clear() {
        rings.clear();
        sparks.clear();
        swingAge = SWING_TIME;
        flash = 0f;
    }

    /** Croissant, anneaux et étincelles (ShapeRenderer en mode Filled). */
    @Override public void draw(ShapeRenderer shapes) {
        drawSwing(shapes);
        for (Ring r : rings) {
            float t = r.age / RING_TIME, radius = RING_MAX * (1f - (1f - t) * (1f - t));
            shapes.setColor(1f, 1f, 1f, 1f - t);
            circleLine(shapes, r.x, r.y, radius, 3f * (1f - t) + 0.5f);
        }
        for (Spark s : sparks) {
            float t = s.age / SPARK_TIME;
            shapes.setColor(1f, 1f, 1f, 1f - t);
            shapes.rectLine(s.x, s.y, s.x - s.vx * 0.03f, s.y - s.vy * 0.03f, 1.8f * (1f - t) + 0.4f);
        }
    }

    /**
     * Flash blanc sur toute la vue.
     *
     * @param shapes Le ShapeRenderer, déjà ouvert en mode Filled.
     * @param camera caméra de la partie
     */
    public void drawFlash(ShapeRenderer shapes, Camera camera) {
        if (flash <= 0f) return;
        shapes.setColor(1f, 1f, 1f, Math.min(1f, flash));
        shapes.rect(camera.position.x - camera.viewportWidth, camera.position.y - camera.viewportHeight,
                camera.viewportWidth * 2f, camera.viewportHeight * 2f);
    }

    /** Croissant effilé qui balaie l'arc de gauche à droite puis s'efface. */
    private void drawSwing(ShapeRenderer shapes) {
        if (swingAge >= SWING_TIME) return;
        float t = swingAge / SWING_TIME, head = Math.min(1f, t * 2.2f), alpha = 1f - t * t;
        float start = swingAngle - SWING_SPAN / 2f;
        for (int i = 0; i < ARC_SEGMENTS * head; i++) {
            float k0 = (float) i / ARC_SEGMENTS, k1 = (float) (i + 1) / ARC_SEGMENTS;
            float a0 = (start + SWING_SPAN * k0) * MathUtils.degreesToRadians;
            float a1 = (start + SWING_SPAN * k1) * MathUtils.degreesToRadians;
            float width = 1f + 6f * MathUtils.sin(k1 * MathUtils.PI) * (1f - t);   // plus épais au milieu
            shapes.setColor(1f, 1f, 1f, alpha);
            shapes.rectLine(swingAt.x + MathUtils.cos(a0) * SWING_RADIUS, swingAt.y + MathUtils.sin(a0) * SWING_RADIUS,
                    swingAt.x + MathUtils.cos(a1) * SWING_RADIUS, swingAt.y + MathUtils.sin(a1) * SWING_RADIUS, width);
        }
    }

    /** Cercle creux fait de segments épais. */
    private static void circleLine(ShapeRenderer shapes, float cx, float cy, float radius, float width) {
        int n = 28;
        for (int i = 0; i < n; i++) {
            float a0 = MathUtils.PI2 * i / n, a1 = MathUtils.PI2 * (i + 1) / n;
            shapes.rectLine(cx + MathUtils.cos(a0) * radius, cy + MathUtils.sin(a0) * radius,
                    cx + MathUtils.cos(a1) * radius, cy + MathUtils.sin(a1) * radius, width);
        }
    }
}
