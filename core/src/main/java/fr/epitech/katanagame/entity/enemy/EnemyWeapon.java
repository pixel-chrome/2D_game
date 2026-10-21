package fr.epitech.katanagame.entity.enemy;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import fr.epitech.katanagame.audio.Sounds;
import fr.epitech.katanagame.entity.projectile.Bullet;
import fr.epitech.katanagame.world.World;

import java.util.List;

/** Gestion de la visée, de la canalisation et du rythme de tir de l'ennemi. */
final class EnemyWeapon {
    private static final float LEAD_FACTOR = 0.3f;
    private static final int FAN_BULLETS = 1;
    private static final float FAN_SPREAD = 14f;
    private static final int VOLLEYS_PER_SALVO = 1;
    private static final float VOLLEY_INTERVAL = 0.3f;
    private static final float WINDUP = 0.2f;
    private static final float FIRE_INTERVAL = 1.2f;
    private static final float GUNSHOT_RADIUS = 260f;   // portée du bruit d'un tir
    private static final float SHOOT_EFFECT_TIME = 0.24f;
    private static final float CHARGE_ADVANCE_SPEED = 28f;
    private static final float RECOIL_SPEED = 78f;
    private static final float RECOIL_DURATION = 0.12f;

    private final ShooterEnemy enemy;
    private final Vector2 lastTarget = new Vector2();
    private final Vector2 playerVel = new Vector2();
    private final Vector2 chargeDirection = new Vector2(0f, -1f);
    private final Vector2 chargePosition = new Vector2();
    private final Vector2 recoilDirection = new Vector2();
    private boolean hasLastTarget;
    private boolean winding;
    private float windupTimer;
    private float cooldown;
    private float volleyTimer;
    private float shootTimer;
    private float recoilTimer;
    private int volleysLeft;

    /** Associe un système de tir à un ennemi. */
    EnemyWeapon(ShooterEnemy enemy) {
        this.enemy = enemy;
    }

    /** Estime la vitesse du joueur pour anticiper sa position. */
    void trackPlayerVelocity(Vector2 target, float delta) {
        if (hasLastTarget && delta > 0f) {
            playerVel.set(target).sub(lastTarget).scl(1f / delta);
        } else {
            playerVel.setZero();
        }
        lastTarget.set(target);
        hasLastTarget = true;
    }

    /** Met à jour les délais de tir et l'effet visuel. */
    void updateTimers(float delta) {
        if (shootTimer > 0f) {
            shootTimer = Math.max(0f, shootTimer - delta);
        }
        cooldown = Math.max(0f, cooldown - delta);
    }

    /** Interrompt la séquence d'attaque. */
    void stopAttacking() {
        winding = false;
        volleysLeft = 0;
        recoilTimer = 0f;
    }

    /** Casse l'attaque en cours (parry) et retarde le prochain tir. */
    void stagger(float cooldownTime) {
        stopAttacking();
        cooldown = Math.max(cooldown, cooldownTime);
    }

    /** Réinitialise l'effet visuel de tir. */
    void clearShootEffect() {
        shootTimer = 0f;
    }

    /** Indique si une séquence de tir est en cours (visée, salve ou recul). */
    boolean isAttacking() {
        return winding || volleysLeft > 0 || recoilTimer > 0f;
    }

    /** Indique si l'arme est en phase de préparation. */
    boolean isWinding() {
        return winding;
    }

    /** Progression de la canalisation, entre 0 et 1. */
    float chargeProgress() {
        return winding ? 1f - Math.max(0f, windupTimer) / WINDUP : 0f;
    }

    /** Position du point lumineux affiché devant l'ennemi. */
    Vector2 chargePosition() {
        return chargePosition.set(enemy.getPosition()).mulAdd(chargeDirection, 17f);
    }

    /** Intensité de la déformation après le tir, entre 0 et 1. */
    float shootProgress() {
        return shootTimer <= 0f ? 0f : Math.min(1f, shootTimer / SHOOT_EFFECT_TIME);
    }

    /** Retourne le temps restant de l'effet de tir. */
    float shootTimer() {
        return shootTimer;
    }

    /** Avance légèrement pendant la canalisation puis recule à la décharge. */
    void updateMotion(float delta, TiledMapTileLayer[] walls) {
        if (winding) {
            // L'ennemi s'approche doucement pendant qu'il prépare son tir.
            enemy.moveForAI(chargeDirection.x * CHARGE_ADVANCE_SPEED * delta,
                    chargeDirection.y * CHARGE_ADVANCE_SPEED * delta, walls);
        }
        if (recoilTimer > 0f) {
            float step = Math.min(delta, recoilTimer);
            enemy.moveForAI(recoilDirection.x * RECOIL_SPEED * step,
                    recoilDirection.y * RECOIL_SPEED * step, walls);
            recoilTimer = Math.max(0f, recoilTimer - delta);
        }
    }

    /** Gère la canalisation, les salves et le délai entre les tirs. */
    void updateAttack(float delta, Vector2 target, World world) {
        if (volleysLeft > 0) {
            volleyTimer -= delta;
            if (volleyTimer <= 0f) {
                fireVolley(world);
                volleysLeft--;
                volleyTimer = VOLLEY_INTERVAL;
                if (volleysLeft == 0) {
                    cooldown = FIRE_INTERVAL;
                }
            }
        } else if (winding) {
            windupTimer -= delta;
            if (windupTimer <= 0f) {
                winding = false;
                volleysLeft = VOLLEYS_PER_SALVO;
                volleyTimer = 0f;
            }
        } else if (cooldown <= 0f) {
            beginCharge(target);
        }
    }

    /** Fixe la direction et commence une courte canalisation. */
    private void beginCharge(Vector2 target) {
        float dist = enemy.getPosition().dst(target);
        float leadTime = Math.min(dist / Bullet.SPEED, 0.6f) * LEAD_FACTOR;
        Vector2 predicted = new Vector2(target).mulAdd(playerVel, leadTime);
        chargeDirection.set(predicted).sub(enemy.getPosition()).nor();
        if (chargeDirection.isZero()) {
            chargeDirection.set(1f, 0f);
        }
        winding = true;
        windupTimer = WINDUP;
    }

    /** Crée une salve dans la direction annoncée pendant la canalisation. */
    private void fireVolley(World world) {
        Sounds.play(Sounds.Effect.SHOOT);
        float baseAngle = chargeDirection.angleDeg();
        int half = FAN_BULLETS / 2;

        for (int i = -half; i <= half; i++) {
            Vector2 dir = new Vector2(1f, 0f).setAngleDeg(baseAngle + i * FAN_SPREAD);
            world.bullets.add(new Bullet(
                    new Vector2(enemy.getPosition()),
                    dir,
                    enemy.getLaserTexture()
            ));
        }
        world.makeNoise(enemy.getPosition(), GUNSHOT_RADIUS, enemy);
        shootTimer = SHOOT_EFFECT_TIME;
        recoilDirection.set(chargeDirection).scl(-1f);
        recoilTimer = RECOIL_DURATION;
    }
}
