package fr.epitech.katanagame.entity.player;

import fr.epitech.katanagame.entity.Character;
import fr.epitech.katanagame.entity.Direction;
import fr.epitech.katanagame.entity.enemy.Enemy;
import fr.epitech.katanagame.entity.projectile.Bullet;
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
    /** Parry (clic droit) : fenêtre active, recharge, rayon et décalage de la zone devant le joueur. */
    private static final float PARRY_DURATION = 0.22f, PARRY_COOLDOWN = 0.45f;
    /** Animation complète du parry (ATTACK 2), fente vers l'avant au début, éclat blanc après un parry réussi. */
    static final float PARRY_ANIM = 0.32f;
    private static final float LUNGE_TIME = 0.09f, LUNGE_SPEED = 260f, FLASH_TIME = 0.14f;
    /** Fente du coup, et recharge entre deux attaques (pour ne pas les enchaîner en boucle). */
    private static final float ATTACK_LUNGE_TIME = 0.14f, ATTACK_LUNGE_SPEED = 340f, ATTACK_COOLDOWN = 0.5f;
    private static final float PARRY_RADIUS = 34f, PARRY_FORWARD = 14f, PARRY_MELEE_BONUS = 10f;

    private final PlayerAnimations animations = new PlayerAnimations();
    private final Vector2 dashDir = new Vector2();
    private final Rectangle attackBounds = new Rectangle();
    private Direction dashFacing = Direction.DOWN;
    private float dashTimer, dashCooldown, attackTimer, stateTime;
    private float parryTimer, parryCooldown, parryAnim, flash;
    private boolean moving, dashStarted, parrySucceeded;
    private final Vector2 parryCenter = new Vector2();
    private final Vector2 lungeDir = new Vector2();
    private float lungeTimer, attackCooldown;
    private boolean attackHit;
    private final Corpse corpse = new Corpse();

    /** Initialise le joueur et ses dimensions de collision. */
    public Player() { super(320, 180, HITBOX_W, HITBOX_H, FEET_W, FEET_H, FEET_OFFSET_Y); }

    @Override public boolean isInvincible() { return isDashing(); }

    @Override public void respawn(float x, float y) {
        super.respawn(x, y);
        dashTimer = dashCooldown = attackTimer = parryTimer = parryCooldown = parryAnim = flash = lungeTimer = attackCooldown = 0f;
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

    /** Vrai une seule fois par parry réussi (arrêt sur image, secousse de caméra). */
    public boolean pollParrySuccess() {
        boolean ok = parrySucceeded;
        parrySucceeded = false;
        return ok;
    }

    /** Indique si la fenêtre de parry est active. */
    public boolean isParrying() { return parryTimer > 0f; }

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
        parryTimer = Math.max(0f, parryTimer - delta);
        parryCooldown = Math.max(0f, parryCooldown - delta);
        parryAnim = Math.max(0f, parryAnim - delta);
        flash = Math.max(0f, flash - delta);
        lungeTimer = Math.max(0f, lungeTimer - delta);
        attackCooldown = Math.max(0f, attackCooldown - delta);

        Vector2 dir = PlayerInput.readMove();
        moving = !dir.isZero();
        if (moving) dir.nor();
        PlayerInput.updateAim(this, world);
        handleDash(dir);

        if (isDashing()) move(dashDir.x * DASH_SPEED * delta, dashDir.y * DASH_SPEED * delta, world.moveLayers);
        else if (lungeTimer > 0f) {
            move(lungeDir.x * ATTACK_LUNGE_SPEED * delta, lungeDir.y * ATTACK_LUNGE_SPEED * delta, world.moveLayers);
            world.slashFx.speedLine(position, lungeDir);
            hitEnemies(world);                   // le sabre touche tout le long de la fente
        } else if (isLunging()) move(aim.x * LUNGE_SPEED * delta, aim.y * LUNGE_SPEED * delta, world.moveLayers);
        else if (moving) move(dir.x * SPEED * delta, dir.y * SPEED * delta, world.moveLayers);

        if (Gdx.input.isButtonJustPressed(Input.Buttons.RIGHT) && parryCooldown <= 0f && !isDashing()) startParry(world);
        if (isParrying()) deflect(world);
        else if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT) && attackCooldown <= 0f) attack(world);
    }

    /** Lance le parry : croissant visuel et début de la fenêtre où balles et ennemis sont repoussés. */
    private void startParry(World world) {
        parryTimer = PARRY_DURATION;
        parryAnim = PARRY_ANIM;
        attackTimer = 0f;
        parryCooldown = PARRY_COOLDOWN;
        world.parryFx.swing(parryZone(), aim.angleDeg());
    }

    /** Petite fente vers l'avant au tout début du parry. */
    private boolean isLunging() { return parryTimer > PARRY_DURATION - LUNGE_TIME; }

    /** Centre de la zone de parry, devant le joueur dans la direction de visée. */
    private Vector2 parryZone() {
        return parryCenter.set(position).mulAdd(aim, PARRY_FORWARD);
    }

    /** Renvoie les balles de la zone vers la souris et repousse les ennemis au contact. */
    private void deflect(World world) {
        Vector2 zone = parryZone();
        boolean hit = false;
        for (Bullet bullet : world.bullets) {
            if (bullet.isAlive() && !bullet.isReflected() && bullet.getPosition().dst(zone) <= PARRY_RADIUS) {
                bullet.reflect(aim);
                world.parryFx.impact(bullet.getPosition(), aim);
                hit = true;
            }
        }
        for (Enemy enemy : world.enemies) {
            if (!enemy.isAlive() || enemy.isStunned()) continue;
            Vector2 at = enemy.getBounds().getCenter(new Vector2());
            if (at.dst(zone) <= PARRY_RADIUS + PARRY_MELEE_BONUS) {
                enemy.parried(at.cpy().sub(position));
                world.parryFx.impact(at, aim);
                hit = true;
            }
        }
        if (hit) {
            parrySucceeded = true;
            parryCooldown = 0f;          // un parry réussi se rejoue tout de suite
            flash = FLASH_TIME;
        }
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

    /** Déclenche l’attaque : fente vers la souris, puis vérifie les ennemis touchés. */
    private void attack(World world) {
        attackTimer = ATTACK_DURATION;
        attackCooldown = ATTACK_COOLDOWN;
        lungeTimer = ATTACK_LUNGE_TIME;
        lungeDir.set(aim);
        hitEnemies(world);
    }

    /** Frappe les ennemis dans la hitbox du coup. */
    private void hitEnemies(World world) {
        attackBounds.set(position.x + aim.x * ATTACK_REACH + ATTACK_OFFSET,
                position.y + aim.y * ATTACK_REACH + ATTACK_OFFSET, ATTACK_W, ATTACK_H);
        for (Enemy enemy : world.enemies) {
            if (enemy.isAlive() && attackBounds.overlaps(enemy.getBounds())) {
                enemy.takeDamage();
                Vector2 at = enemy.getBounds().getCenter(new Vector2());
                Vector2 dir = at.cpy().sub(position);
                world.slashFx.hit(at, dir);
                attackHit = true;
                flash = FLASH_TIME;
                if (!enemy.isAlive()) world.blood.splatter(at, dir, world.bulletLayers);
            }
        }
    }

    /** Vrai une seule fois par coup qui touche (arrêt sur image, secousse de caméra). */
    public boolean pollAttackHit() {
        boolean hit = attackHit;
        attackHit = false;
        return hit;
    }

    /** Hitbox de l'attaque, visible uniquement pendant son animation. */
    public Rectangle getAttackBounds() { return attackTimer > 0f ? attackBounds : null; }

    @Override public void draw(SpriteBatch batch) {
        animations.draw(this, batch, stateTime, attackTimer, moving, isDashing(), dashFacing,
                parryAnim > 0f ? PARRY_ANIM - parryAnim : -1f, flash > 0f,
                isAlive() ? null : corpse);
    }

    @Override public void dispose() { animations.dispose(); }
}
