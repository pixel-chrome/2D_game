package fr.epitech.katanagame.entity.enemy;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import fr.epitech.katanagame.entity.Character;
import fr.epitech.katanagame.entity.player.Player;
import fr.epitech.katanagame.entity.projectile.LaserAssets;
import fr.epitech.katanagame.world.Collision;
import fr.epitech.katanagame.world.World;

/** Entité ennemie : gère l'IA, la vision, l'arme et le rendu. */
public class Enemy extends Character {
    private static final float WIDTH = 14f;
    private static final float HEIGHT = 24f;
    private static final float FEET_W = 10f;
    private static final float FEET_H = 6f;
    private static final float FEET_OFFSET_Y = 10f;
    private static final float STUN_TIME = 0.7f, KNOCKBACK_SPEED = 260f, KNOCKBACK_DRAG = 7f;
    /** Distance sous laquelle il s'arrête de courir pour tirer. */
    /** Vitesse de course des tireurs (px/s). */
    private static final float SHOOTER_RUN_SPEED = 70f;
    private static final float FIRE_DISTANCE = 150f;

    /** Logique de comportement et de patrouille. */
    protected final EnemyAI ai;
    /** Système de tir de l'ennemi. */
    private final EnemyWeapon weapon;
    /** Texture laser conservée par cet ennemi pendant toute la partie. */
    private final Texture laserTexture;

    private float stateTime;
    private float deathTime;
    private float stunTimer;
    private final Vector2 knockback = new Vector2();

    /** Initialise un ennemi, son IA, son arme et la couleur de son laser. */
    public Enemy(float x, float y) {
        this(x, y, SHOOTER_RUN_SPEED, true);
    }

    /** Constructeur commun aux sous-classes : vitesse de course et présence d'une arme à distance. */
    protected Enemy(float x, float y, float runSpeed, boolean ranged) {
        super(x, y, WIDTH, HEIGHT, FEET_W, FEET_H, FEET_OFFSET_Y);
        aim.set(0f, -1f);
        laserTexture = ranged ? LaserAssets.getRandomTexture() : null;
        ai = new EnemyAI(this, x, y, runSpeed);
        weapon = new EnemyWeapon(this);
    }

    /** Charge les ressources graphiques partagées des ennemis et des lasers. */
    public static void loadAssets() {
        EnemyAnimations.loadAssets();
        SkullKnightAnimations.loadAssets();
        LaserAssets.loadAssets();
    }

    /** Libère les ressources graphiques partagées des ennemis et des lasers. */
    public static void disposeAssets() {
        EnemyAnimations.disposeAssets();
        SkullKnightAnimations.disposeAssets();
        LaserAssets.disposeAssets();
    }

    @Override
    protected void onDeath() {
        deathTime = 0f;
    }

    /** Repoussé par un parry : l'attaque est interrompue et l'ennemi reste sonné un instant. */
    public void parried(Vector2 dir) {
        if (!isAlive()) return;
        stunTimer = STUN_TIME;
        knockback.set(dir).nor().scl(KNOCKBACK_SPEED);
        weapon.stagger(STUN_TIME + 0.5f);
        onParried();
    }

    /** Point d'extension : les sous-classes annulent ici leur attaque propre. */
    protected void onParried() {}

    /** Indique si l'ennemi est sonné par un parry. */
    public boolean isStunned() {
        return stunTimer > 0f;
    }

    /** Avance le recul et la stupeur d'un parry ; vrai tant que l'ennemi est sonné (il ne fait rien d'autre). */
    protected boolean updateStun(float delta, World world) {
        if (stunTimer <= 0f) return false;
        stunTimer -= delta;
        moveForAI(knockback.x * delta, knockback.y * delta, world.moveLayers);
        knockback.scl(Math.max(0f, 1f - KNOCKBACK_DRAG * delta));
        weapon.stopAttacking();
        return true;
    }

    @Override
    public void update(float delta, World world) {
        stateTime += delta;
        if (!isAlive()) {
            deathTime += delta;
            weapon.stopAttacking();
            weapon.clearShootEffect();
            ai.setSeesPlayer(false);
            return;
        }

        if (updateStun(delta, world)) return;

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

    /** Retourne la texture laser attribuée à cet ennemi. */
    public Texture getLaserTexture() {
        return laserTexture;
    }

    /** Affiche le champ de vision de l'ennemi. */
    public void drawVision(ShapeRenderer shapes, TiledMapTileLayer[] walls) {
        EnemyVision.draw(this, shapes, walls);
    }

    /** Affiche un petit point d'énergie pendant la canalisation du tir. */
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

    /** Affiche l'indicateur d'alerte de l'ennemi. */
    public void drawAlert(ShapeRenderer shapes) {
        if (!isAlive() || !ai.seesPlayer()) {
            return;
        }

        float bob = Math.abs(MathUtils.sin(stateTime * 8f)) * 2f;
        float x = position.x;
        float y = position.y + EnemyAnimations.FRAME_SIZE / 2f + 4f + bob;
        shapes.setColor(Color.BLACK);
        shapes.rect(x - 3f, y - 1f, 6f, 16f);
        shapes.setColor(1f, 0.2f, 0.2f, 1f);
        shapes.rect(x - 2f, y + 5f, 4f, 9f);
        shapes.rect(x - 2f, y, 4f, 4f);
    }

    /** Déplace l'ennemi en appliquant les collisions de la carte. */
    boolean moveForAI(float dx, float dy, TiledMapTileLayer[] walls) {
        return move(dx, dy, walls);
    }

    /** Retourne le temps écoulé pour l'animation courante. */
    float stateTime() {
        return stateTime;
    }

    /** Retourne le temps écoulé depuis la mort de l'ennemi. */
    float deathTime() {
        return deathTime;
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

    /** Décalage entre le centre du sprite et le centre des pieds. */
    Vector2 feetOffset() {
        Rectangle feet = getFeetBounds();
        return new Vector2(feet.x + feet.width / 2f - position.x, feet.y + feet.height / 2f - position.y);
    }

    /** L'ennemi entend un bruit à cet endroit et vient voir. */
    public void hearNoise(Vector2 at) {
        if (isAlive()) {
            ai.hearNoise(at);
        }
    }

    /** Indique si l'ennemi court vers sa cible. */
    boolean isRunning() {
        return ai.isRunning();
    }

    /** Indique si l'ennemi est en déplacement. */
    boolean isWalking() {
        return ai.isWalking();
    }

    /** Indique si l'ennemi est alerté. */
    boolean isAlerted() {
        return ai.isAlerted();
    }
}
