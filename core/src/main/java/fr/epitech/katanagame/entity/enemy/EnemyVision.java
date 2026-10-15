package fr.epitech.katanagame.entity.enemy;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import fr.epitech.katanagame.world.Collision;

/** Détection du joueur et dessin du cône de vision. */
final class EnemyVision {
    static final float DETECTION_RANGE = 180f;
    private static final float FOV = 90f;
    private static final int CONE_RAYS = 24;
    private EnemyVision() {}

    static boolean canSee(Enemy enemy, Vector2 target, TiledMapTileLayer[] sight) {
        Vector2 toTarget = new Vector2(target).sub(enemy.getPosition());
        float angleDiff = Math.abs(((toTarget.angleDeg() - enemy.getAim().angleDeg() + 540f) % 360f) - 180f);
        return angleDiff <= FOV / 2f && toTarget.len() <= DETECTION_RANGE
                && Collision.hasLineOfSight(sight, enemy.getPosition(), target);
    }

    static void draw(Enemy enemy, ShapeRenderer shapes, TiledMapTileLayer[] walls) {
        if (!enemy.isAlive()) return;
        if (enemy.isAlerted()) shapes.setColor(1f, 0.1f, 0.1f, 0.28f);
        else shapes.setColor(1f, 0.9f, 0.2f, 0.18f);
        float start = enemy.getAim().angleDeg() - FOV / 2f;
        float step = FOV / CONE_RAYS;
        float prevX = 0f, prevY = 0f;
        for (int i = 0; i <= CONE_RAYS; i++) {
            float angle = start + i * step;
            float dx = MathUtils.cosDeg(angle), dy = MathUtils.sinDeg(angle);
            float len = DETECTION_RANGE;
            for (float d = 4f; d <= DETECTION_RANGE; d += 4f) {
                if (Collision.isSolid(walls, enemy.getPosition().x + dx * d, enemy.getPosition().y + dy * d)) {
                    len = d - 2f; break;
                }
            }
            float x = enemy.getPosition().x + dx * len;
            float y = enemy.getPosition().y + dy * len;
            if (i > 0) shapes.triangle(enemy.getPosition().x, enemy.getPosition().y, prevX, prevY, x, y);
            prevX = x; prevY = y;
        }
    }
}
