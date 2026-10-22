package fr.epitech.katanagame.entity.enemy;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import fr.epitech.katanagame.entity.player.Player;
import fr.epitech.katanagame.entity.projectile.LaserAssets;
import fr.epitech.katanagame.world.Collision;
import fr.epitech.katanagame.world.World;

/** Ennemi à distance : patrouille, repère le joueur, court dans sa direction puis tire des lasers. */
public class ShooterEnemy extends Enemy {
    /** Vitesse de course (px/s) et distance sous laquelle il s'arrête de courir pour tirer. */
    private static final float RUN_SPEED = 70f;
    private static final float FIRE_DISTANCE = 150f;

    /** Système de tir de l'ennemi. */
    private final EnemyWeapon weapon;
    /** Texture laser conservée par cet ennemi pendant toute la partie. */
    private final Texture laserTexture;

    /**
     * Initialise un tireur, son arme et la couleur de son laser.
     *
     * @param x abscisse initiale
     * @param y ordonnée initiale
     */
    public ShooterEnemy(float x, float y) {
        super(x, y, RUN_SPEED);
        laserTexture = LaserAssets.getRandomTexture();
        weapon = new EnemyWeapon(this);
    }

    @Override
    protected void onDeadTick() {
        weapon.stopAttacking();
        weapon.clearShootEffect();
    }

    @Override
    protected void onStunned() {
        weapon.stopAttacking();
    }

    @Override
    protected void onParried() {
        weapon.stagger(Stun.TIME + 0.5f);
    }

    @Override
    protected void think(float delta, World world) {
        Player player = world.player;
        if (!player.isAlive()) {
            weapon.stopAttacking();
            ai.setSeesPlayer(false);
            return;
        }

        weapon.updateTimers(delta);
        Rectangle pb = player.getBounds();
        Vector2 target = new Vector2(pb.x + pb.width / 2f, pb.y + pb.height / 2f);
        weapon.trackPlayerVelocity(target, delta);
        boolean sees = EnemyVision.canSee(this, target, world.sightLayers);
        ai.updateAlert(sees, target, delta);

        if (!ai.isAlerted()) {
            weapon.stopAttacking();
            ai.patrol(delta, world.moveLayers);
            return;
        }

        // Il ne tire que s'il voit le joueur, qu'il est à portée et que les balles ne seraient pas bloquées.
        boolean canShoot = sees
                && position.dst(ai.lastKnown()) <= FIRE_DISTANCE
                && Collision.hasLineOfSight(world.bulletLayers, position, target);
        if (!canShoot && !(sees && weapon.isAttacking())) {
            // Il fonce sur le joueur (ou sur le dernier endroit vu / entendu).
            weapon.stopAttacking();
            ai.chaseLastKnown(delta, world.moveLayers);
            return;
        }
        ai.stopWalking();
        aim.set(ai.lastKnown()).sub(position).nor();
        weapon.updateAttack(delta, target, world);
        weapon.updateMotion(delta, world.moveLayers);
    }

    @Override
    public void draw(SpriteBatch batch) {
        EnemyAnimations.draw(this, batch);
    }

    /**
     * Retourne la texture laser attribuée à cet ennemi.
     *
     * @return la texture du laser de cet ennemi
     */
    public Texture getLaserTexture() {
        return laserTexture;
    }

    /** Affiche un petit point d'énergie pendant la canalisation du tir. */
    @Override
    public void drawCharge(ShapeRenderer shapes) {
        if (!isAlive() || !weapon.isWinding()) {
            return;
        }

        float progress = weapon.chargeProgress();
        Vector2 point = weapon.chargePosition();
        float radius = 1.5f + progress * 2.2f;
        float alpha = 0.35f + progress * 0.55f;

        shapes.setColor(0.35f, 0.85f, 1f, alpha * 0.35f);
        shapes.circle(point.x, point.y, radius + 2f);
        shapes.setColor(0.65f, 0.95f, 1f, alpha);
        shapes.circle(point.x, point.y, radius);
    }

    /** Retourne le temps restant de l'effet de tir. */
    float shootTimer() {
        return weapon.shootTimer();
    }

    /** Indique si l'ennemi prépare un tir. */
    boolean isWinding() {
        return weapon.isWinding();
    }

    /** Échelle horizontale utilisée pour déformer légèrement le sprite pendant le tir. */
    float firingScaleX() {
        float charge = weapon.chargeProgress();
        float shot = weapon.shootProgress();
        return 1f + 0.10f * charge - 0.04f * shot;
    }

    /** Échelle verticale utilisée pour déformer légèrement le sprite pendant le tir. */
    float firingScaleY() {
        float charge = weapon.chargeProgress();
        float shot = weapon.shootProgress();
        return 1f - 0.08f * charge + 0.16f * shot;
    }
}
