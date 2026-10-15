package fr.epitech.katanagame.entity.enemy;

import com.badlogic.gdx.math.Vector2;
import fr.epitech.katanagame.entity.projectile.Bullet;
import java.util.List;

/** Gestion de la visée, de l'anticipation et du rythme de tir de l'ennemi. */
final class EnemyWeapon {
    private static final float LEAD_FACTOR = 0.3f;
    private static final int FAN_BULLETS = 1;
    private static final float FAN_SPREAD = 14f;
    private static final int VOLLEYS_PER_SALVO = 1;
    private static final float VOLLEY_INTERVAL = 0.3f;
    private static final float WINDUP = 0.4f;
    private static final float FIRE_INTERVAL = 1.2f;
    private static final float SHOOT_EFFECT_TIME = 0.24f;

    private final Enemy enemy;
    private final Vector2 lastTarget = new Vector2();
    private final Vector2 playerVel = new Vector2();
    private boolean hasLastTarget, winding;
    private float windupTimer, cooldown, volleyTimer, shootTimer;
    private int volleysLeft;

    /** Associe un système de tir à un ennemi. */
    EnemyWeapon(Enemy enemy) { this.enemy = enemy; }

    /** Estime la vitesse du joueur pour anticiper sa position. */
    void trackPlayerVelocity(Vector2 target, float delta) {
        if (hasLastTarget && delta > 0f) playerVel.set(target).sub(lastTarget).scl(1f / delta);
        else playerVel.setZero();
        lastTarget.set(target);
        hasLastTarget = true;
    }

    /** Met à jour les délais de tir et l’effet visuel. */
    void updateTimers(float delta) {
        if (shootTimer > 0f) shootTimer -= delta;
        cooldown = Math.max(0f, cooldown - delta);
    }

    /** Interrompt la séquence d’attaque. */
    void stopAttacking() { winding = false; volleysLeft = 0; }
    /** Réinitialise l’effet visuel de tir. */
    void clearShootEffect() { shootTimer = 0f; }
    /** Indique si l’arme est en phase de préparation. */
    boolean isWinding() { return winding; }
    /** Retourne la durée restante de l’effet de tir. */
    float shootTimer() { return shootTimer; }

    /** Gère la préparation, les salves et le délai entre les tirs. */
    void updateAttack(float delta, Vector2 target, List<Bullet> bullets) {
        if (volleysLeft > 0) {
            volleyTimer -= delta;
            if (volleyTimer <= 0f) {
                fireVolley(target, bullets);
                volleysLeft--;
                volleyTimer = VOLLEY_INTERVAL;
                if (volleysLeft == 0) cooldown = FIRE_INTERVAL;
            }
        } else if (winding) {
            windupTimer -= delta;
            if (windupTimer <= 0f) {
                winding = false;
                volleysLeft = VOLLEYS_PER_SALVO;
                volleyTimer = 0f;
            }
        } else if (cooldown <= 0f) {
            winding = true;
            windupTimer = WINDUP;
        }
    }

    /** Crée une salve de projectiles en direction de la cible. */
    private void fireVolley(Vector2 target, List<Bullet> bullets) {
        float dist = enemy.getPosition().dst(target);
        float leadTime = Math.min(dist / Bullet.SPEED, 0.6f) * LEAD_FACTOR;
        Vector2 predicted = new Vector2(target).mulAdd(playerVel, leadTime);
        float baseAngle = new Vector2(predicted).sub(enemy.getPosition()).angleDeg();
        int half = FAN_BULLETS / 2;
        for (int i = -half; i <= half; i++) {
            Vector2 dir = new Vector2(1, 0).setAngleDeg(baseAngle + i * FAN_SPREAD);
            bullets.add(new Bullet(new Vector2(enemy.getPosition()), dir));
        }
        shootTimer = SHOOT_EFFECT_TIME;
    }
}
