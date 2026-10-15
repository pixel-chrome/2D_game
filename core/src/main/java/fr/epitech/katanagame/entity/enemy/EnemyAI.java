package fr.epitech.katanagame.entity.enemy;

import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import fr.epitech.katanagame.world.Collision;

/** États d'alerte et déplacements de patrouille d'un ennemi. */
final class EnemyAI {
    private static final float LOSE_SIGHT_TIME = 1.5f;
    private static final float WANDER_RADIUS = 80f;
    private static final float WALK_SPEED = 40f;
    private static final float PAUSE_MIN = 0.8f, PAUSE_MAX = 2f;

    private final Enemy enemy;
    private final Vector2 home = new Vector2();
    private final Vector2 wanderTarget = new Vector2();
    private final Vector2 lastKnown = new Vector2();
    private boolean alerted, seesPlayer, walking;
    private float alertTimer, pauseTimer;

    EnemyAI(Enemy enemy, float x, float y) {
        this.enemy = enemy;
        home.set(x, y);
    }

    void updateAlert(boolean sees, Vector2 target, float delta) {
        seesPlayer = sees;
        if (sees) {
            alerted = true;
            alertTimer = LOSE_SIGHT_TIME;
            lastKnown.set(target);
        } else if (alerted) {
            alertTimer -= delta;
            if (alertTimer <= 0f) {
                alerted = false;
                walking = false;
                pauseTimer = 0.5f;
            }
        }
    }

    void patrol(float delta, TiledMapTileLayer[] walls) {
        if (walking) {
            Vector2 toTarget = new Vector2(wanderTarget).sub(enemy.getPosition());
            float dist = toTarget.len();
            if (dist < 2f) startPause();
            else {
                toTarget.nor();
                enemy.getAim().set(toTarget);
                float stepLen = Math.min(dist, WALK_SPEED * delta);
                if (!enemy.moveForAI(toTarget.x * stepLen, toTarget.y * stepLen, walls)) startPause();
            }
        } else {
            pauseTimer -= delta;
            if (pauseTimer <= 0f) pickWanderTarget(walls);
        }
    }

    private void startPause() {
        walking = false;
        pauseTimer = MathUtils.random(PAUSE_MIN, PAUSE_MAX);
    }

    private void pickWanderTarget(TiledMapTileLayer[] walls) {
        for (int attempt = 0; attempt < 8; attempt++) {
            float angle = MathUtils.random(360f);
            float dist = MathUtils.random(20f, WANDER_RADIUS);
            Vector2 candidate = new Vector2(home).add(
                    MathUtils.cosDeg(angle) * dist, MathUtils.sinDeg(angle) * dist);
            if (Collision.hasLineOfSight(walls, enemy.getPosition(), candidate)) {
                wanderTarget.set(candidate);
                walking = true;
                return;
            }
        }
        pauseTimer = 0.5f;
    }

    void stopWalking() { walking = false; }
    boolean isAlerted() { return alerted; }
    boolean seesPlayer() { return seesPlayer; }
    boolean isWalking() { return walking; }
    Vector2 lastKnown() { return lastKnown; }
    void setSeesPlayer(boolean value) { seesPlayer = value; }
}
