package fr.epitech.katanagame.render;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

import java.util.ArrayList;
import java.util.List;

/**
 * Effets de l'attaque du joueur : grand croissant blanc bordé de rouge, traits de vitesse pendant la fente,
 * et à l'impact une double entaille en X sur l'ennemi avec étincelles.
 * Tout est en formes pleines, au-dessus des sprites.
 */
public final class SlashEffects {
    private static final float SLASH_TIME = 0.24f, SLASH_SPAN = 190f, SLASH_RADIUS = 38f;
    private static final float LINE_TIME = 0.16f, CUT_TIME = 0.26f, CUT_LENGTH = 22f;
    private static final float SPARK_TIME = 0.3f;
    private static final int ARC_SEGMENTS = 18, SPARKS = 14;

    private final Vector2 slashAt = new Vector2();
    private float slashAngle, slashAge = SLASH_TIME;
    private final List<Line> lines = new ArrayList<>();
    private final List<Cut> cuts = new ArrayList<>();
    private final List<Spark> sparks = new ArrayList<>();

    private static final class Line {
        final float x, y, dx, dy, length;
        float age;
        Line(float x, float y, float dx, float dy, float length) {
            this.x = x; this.y = y; this.dx = dx; this.dy = dy; this.length = length;
        }
    }

    private static final class Cut {
        final float x, y, angle;
        float age;
        Cut(float x, float y, float angle) { this.x = x; this.y = y; this.angle = angle; }
    }

    private static final class Spark {
        float x, y, vx, vy, age;
    }

    /** Lance le croissant du coup en at, centré sur la direction angleDeg. */
    public void slash(Vector2 at, float angleDeg) {
        slashAt.set(at);
        slashAngle = angleDeg;
        slashAge = 0f;
    }

    /** Ajoute un trait de vitesse derrière le joueur (position at, direction de la fente dir). */
    public void speedLine(Vector2 at, Vector2 dir) {
        float side = MathUtils.random(-9f, 9f);
        Vector2 n = new Vector2(-dir.y, dir.x).scl(side);
        lines.add(new Line(at.x + n.x - dir.x * 6f, at.y + n.y - dir.y * 6f, -dir.x, -dir.y, MathUtils.random(18f, 40f)));
    }

    /** Impact sur un ennemi en at : entaille en X dans la direction dir et étincelles. */
    public void hit(Vector2 at, Vector2 dir) {
        float base = dir.isZero() ? 0f : dir.angleDeg();
        cuts.add(new Cut(at.x, at.y, base + 45f));
        cuts.add(new Cut(at.x, at.y, base - 45f));
        for (int i = 0; i < SPARKS; i++) {
            Spark s = new Spark();
            s.x = at.x;
            s.y = at.y;
            float a = (base + MathUtils.random(-60f, 60f)) * MathUtils.degreesToRadians, speed = MathUtils.random(140f, 340f);
            s.vx = MathUtils.cos(a) * speed;
            s.vy = MathUtils.sin(a) * speed;
            sparks.add(s);
        }
    }

    /** Fait avancer les effets (delta réel : ils continuent pendant l'arrêt sur image). */
    public void update(float delta) {
        slashAge += delta;
        for (int i = lines.size() - 1; i >= 0; i--) {
            Line l = lines.get(i);
            l.age += delta;
            if (l.age > LINE_TIME) lines.remove(i);
        }
        for (int i = cuts.size() - 1; i >= 0; i--) {
            Cut c = cuts.get(i);
            c.age += delta;
            if (c.age > CUT_TIME) cuts.remove(i);
        }
        for (int i = sparks.size() - 1; i >= 0; i--) {
            Spark s = sparks.get(i);
            s.age += delta;
            s.x += s.vx * delta;
            s.y += s.vy * delta;
            if (s.age > SPARK_TIME) sparks.remove(i);
        }
    }

    public void clear() {
        lines.clear();
        cuts.clear();
        sparks.clear();
        slashAge = SLASH_TIME;
    }

    /** Croissant, traits, entailles et étincelles (ShapeRenderer en mode Filled). */
    public void draw(ShapeRenderer shapes) {
        drawSlash(shapes);
        for (Line l : lines) {
            float t = l.age / LINE_TIME, len = l.length * (1f - t);
            shapes.setColor(1f, 1f, 1f, 0.8f * (1f - t));
            shapes.rectLine(l.x + l.dx * l.age * 120f, l.y + l.dy * l.age * 120f,
                    l.x + l.dx * (l.age * 120f + len), l.y + l.dy * (l.age * 120f + len), 1.2f);
        }
        for (Cut c : cuts) {
            float t = c.age / CUT_TIME, grow = Math.min(1f, t * 5f), fade = 1f - t;
            float a = c.angle * MathUtils.degreesToRadians, len = CUT_LENGTH * grow;
            float dx = MathUtils.cos(a) * len, dy = MathUtils.sin(a) * len;
            shapes.setColor(0.9f, 0.1f, 0.12f, fade);
            shapes.rectLine(c.x - dx, c.y - dy, c.x + dx, c.y + dy, 4.5f * fade + 1f);
            shapes.setColor(1f, 1f, 1f, fade);
            shapes.rectLine(c.x - dx, c.y - dy, c.x + dx, c.y + dy, 2f * fade + 0.5f);
        }
        for (Spark s : sparks) {
            float t = s.age / SPARK_TIME;
            shapes.setColor(1f, 0.85f - 0.5f * t, 0.7f - 0.6f * t, 1f - t);
            shapes.rectLine(s.x, s.y, s.x - s.vx * 0.03f, s.y - s.vy * 0.03f, 1.8f * (1f - t) + 0.4f);
        }
    }

    /** Croissant à deux couches (rouge dessous, blanc dessus) qui balaie l'arc puis s'efface. */
    private void drawSlash(ShapeRenderer shapes) {
        if (slashAge >= SLASH_TIME) return;
        float t = slashAge / SLASH_TIME, head = Math.min(1f, t * 3f), alpha = 1f - t * t;
        float start = slashAngle - SLASH_SPAN / 2f;
        for (int pass = 0; pass < 2; pass++) {
            boolean core = pass == 1;
            for (int i = 0; i < ARC_SEGMENTS * head; i++) {
                float k0 = (float) i / ARC_SEGMENTS, k1 = (float) (i + 1) / ARC_SEGMENTS;
                float a0 = (start + SLASH_SPAN * k0) * MathUtils.degreesToRadians;
                float a1 = (start + SLASH_SPAN * k1) * MathUtils.degreesToRadians;
                float tail = Math.max(0f, k1 - t * 0.9f);                // l'arrière du croissant s'amincit
                float width = (1.5f + 9f * MathUtils.sin(k1 * MathUtils.PI)) * (1f - t) * (core ? 0.55f : 1f) * (0.4f + 0.6f * (1f - tail));
                if (core) shapes.setColor(1f, 1f, 1f, alpha); else shapes.setColor(0.95f, 0.15f, 0.15f, alpha * 0.9f);
                shapes.rectLine(slashAt.x + MathUtils.cos(a0) * SLASH_RADIUS, slashAt.y + MathUtils.sin(a0) * SLASH_RADIUS,
                        slashAt.x + MathUtils.cos(a1) * SLASH_RADIUS, slashAt.y + MathUtils.sin(a1) * SLASH_RADIUS, width);
            }
        }
    }
}
