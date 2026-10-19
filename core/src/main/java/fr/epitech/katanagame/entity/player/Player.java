package fr.epitech.katanagame.entity.player;

import fr.epitech.katanagame.entity.Character;
import fr.epitech.katanagame.entity.Direction;
import fr.epitech.katanagame.entity.enemy.Enemy;
import fr.epitech.katanagame.settings.GameSettings;
import fr.epitech.katanagame.world.World;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Disposable;

/** Joueur : coordonne les commandes, déplacements, combat et rendu. */
public class Player extends Character implements Disposable {
    private static final float HITBOX_W = 20f, HITBOX_H = 28f;
    private static final float FEET_W = 12f, FEET_H = 8f, FEET_OFFSET_Y = 16f;
    /** Vitesse normale du joueur. */
    private static final float SPEED = 200f, DASH_SPEED = 700f;
    /** Durée du dash en secondes. */
    private static final float DASH_DURATION = 0.15f, DASH_COOLDOWN = 0.6f;
    static final float ATTACK_DURATION = 0.56f;
    private static final float KILL_NOISE_RADIUS = 110f;   // un kill attire les gardes proches
    private static final float ATTACK_REACH = 20f, ATTACK_OFFSET = -12f;
    private static final float ATTACK_W = 30f, ATTACK_H = 24f;

    private final PlayerAnimations animations = new PlayerAnimations();
    private final Vector2 dashDir = new Vector2();
    private final Rectangle attackBounds = new Rectangle();
    private Direction dashFacing = Direction.DOWN;
    private float dashTimer, dashCooldown, attackTimer, stateTime;
    private boolean moving, dashStarted;
    private final Corpse corpse = new Corpse();

    /** Initialise le joueur et ses dimensions de collision. */
    public Player() { super(320, 180, HITBOX_W, HITBOX_H, FEET_W, FEET_H, FEET_OFFSET_Y); }

    @Override public boolean isInvincible() { return isDashing(); }

    @Override public void respawn(float x, float y) {
        super.respawn(x, y);
        dashTimer = dashCooldown = attackTimer = 0f;
        corpse.reset();
    }

    /** Touché par un tir venant dans la direction dir : le corps sera projeté dans ce sens. */
    public void hit(Vector2 dir) {
        if (!isAlive() || isInvincible()) return;
        corpse.fall(dir);
        takeDamage();
    }

    @Override protected void onDeath() { dashTimer = 0f; }

    /** Direction (normalisée) du dernier dash. */
    public Vector2 getDashDir() { return dashDir; }

    /** Vrai une seule fois par dash, au moment où il démarre (secousse de caméra). */
    public boolean pollDashStart() {
        boolean started = dashStarted;
        dashStarted = false;
        return started;
    }

    /** Indique si le dash est en cours. */
    public boolean isDashing() { return dashTimer > 0f; }

    @Override public void update(float delta, World world) {
        stateTime += delta;   // le temps continue après la mort : les effets (traînée) finissent de s'éteindre
        if (!isAlive()) {                       // le corps glisse puis s'immobilise
            Vector2 step = corpse.advance(delta);
            move(step.x, step.y, world.moveLayers);
            return;
        }
        dashTimer = Math.max(0f, dashTimer - delta);
        dashCooldown = Math.max(0f, dashCooldown - delta);
        attackTimer = Math.max(0f, attackTimer - delta);

        Vector2 dir = PlayerInput.readMove();
        moving = !dir.isZero();
        if (moving) dir.nor();
        PlayerInput.updateAim(this, world);
        handleDash(dir);

        if (isDashing()) move(dashDir.x * DASH_SPEED * delta, dashDir.y * DASH_SPEED * delta, world.moveLayers);
        else if (moving) move(dir.x * SPEED * delta, dir.y * SPEED * delta, world.moveLayers);

        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) attack(world);
    }

    /** Déclenche un dash dans la direction choisie si possible. */
    private void handleDash(Vector2 moveDir) {
        if (GameSettings.justPressed(GameSettings.Action.DASH) && dashCooldown <= 0f && !isDashing()) {
            dashDir.set(moving ? moveDir : aim);
            dashFacing = Direction.of(dashDir);
            dashStarted = true;
            dashTimer = DASH_DURATION;
            dashCooldown = DASH_COOLDOWN;
        }
    }

    /** Déclenche l’attaque et vérifie les ennemis touchés. */
    private void attack(World world) {
        attackTimer = ATTACK_DURATION;
        attackBounds.set(position.x + aim.x * ATTACK_REACH + ATTACK_OFFSET,
                position.y + aim.y * ATTACK_REACH + ATTACK_OFFSET, ATTACK_W, ATTACK_H);
        for (Enemy enemy : world.enemies) {
            if (enemy.isAlive() && attackBounds.overlaps(enemy.getBounds())) {
                enemy.takeDamage();
                if (!enemy.isAlive()) {
                    Vector2 at = enemy.getBounds().getCenter(new Vector2());
                    world.blood.splatter(at, at.cpy().sub(position), world.bulletLayers);
                    world.makeNoise(at, KILL_NOISE_RADIUS, enemy);
                }
            }
        }
    }

    /** Hitbox de l'attaque, visible uniquement pendant son animation. */
    public Rectangle getAttackBounds() { return attackTimer > 0f ? attackBounds : null; }

    @Override public void draw(SpriteBatch batch) {
        animations.draw(this, batch, stateTime, attackTimer, moving, isDashing(), dashFacing,
                isAlive() ? null : corpse);
    }

    @Override public void dispose() { animations.dispose(); }
}
