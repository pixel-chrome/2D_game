package fr.epitech.katanagame.entity.player;

import fr.epitech.katanagame.entity.Character;
import fr.epitech.katanagame.settings.GameSettings;
import fr.epitech.katanagame.world.World;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Disposable;

/** Joueur : coordonne les commandes, les capacités (dash, coup, parry), les déplacements et le rendu. */
public class Player extends Character implements Disposable {
    private static final float HITBOX_W = 20f, HITBOX_H = 28f;
    private static final float FEET_W = 12f, FEET_H = 8f, FEET_OFFSET_Y = 16f;
    /** Vitesse normale du joueur. */
    private static final float SPEED = 200f;
    /** Durée de l'éclat blanc après un coup ou un parry qui touche. */
    private static final float FLASH_TIME = 0.14f;

    private final PlayerAnimations animations = new PlayerAnimations();
    private final DashAbility dash = new DashAbility(this);
    private final MeleeAttack melee = new MeleeAttack(this);
    private final ParryAbility parry = new ParryAbility(this);
    /** Capacités par ordre de priorité sur le déplacement : la première qui en impose un l'emporte. */
    private final Ability[] abilities = {dash, melee, parry};
    private final Corpse corpse = new Corpse();
    private float stateTime, flash;
    private boolean moving;
    /** Mode dieu (touche F2 dans GameScreen) : le joueur ne peut plus mourir. */
    private boolean godMode;

    /** Initialise le joueur et ses dimensions de collision. */
    public Player() { super(320, 180, HITBOX_W, HITBOX_H, FEET_W, FEET_H, FEET_OFFSET_Y); }

    /** Active ou désactive le mode dieu. */
    public void setGodMode(boolean godMode) { this.godMode = godMode; }

    /** Invincible pendant le dash, ou en permanence en mode dieu. */
    @Override public boolean isInvincible() { return dash.isActive() || godMode; }

    @Override public void respawn(float x, float y) {
        super.respawn(x, y);
        for (Ability ability : abilities) ability.reset();
        flash = 0f;
        corpse.reset();
    }

    /** Touché par un tir venant dans la direction dir : le corps sera projeté dans ce sens. */
    public void hit(Vector2 dir) {
        if (!isAlive() || isInvincible()) return;
        corpse.fall(dir);
        takeDamage();
    }

    @Override protected void onDeath() { dash.reset(); }

    /** Éclat blanc sur le sprite (coup ou parry qui touche). */
    void flash() { flash = FLASH_TIME; }

    /** Direction (normalisée) du dernier dash. */
    public Vector2 getDashDir() { return dash.direction(); }

    /** Vrai une seule fois par dash, au moment où il démarre (secousse de caméra). */
    public boolean pollDashStart() { return dash.pollStart(); }

    /** Vrai une seule fois par parry réussi (arrêt sur image, secousse de caméra). */
    public boolean pollParrySuccess() { return parry.pollSuccess(); }

    /** Vrai une seule fois par coup qui touche (arrêt sur image, secousse de caméra). */
    public boolean pollAttackHit() { return melee.pollHit(); }

    /** Indique si la fenêtre de parry est active. */
    public boolean isParrying() { return parry.isActive(); }

    /** Indique si le dash est en cours. */
    public boolean isDashing() { return dash.isActive(); }

    @Override public void update(float delta, World world) {
        stateTime += delta;   // le temps continue après la mort : les effets (traînée) finissent de s'éteindre
        if (!isAlive()) {                       // le corps glisse puis s'immobilise
            Vector2 step = corpse.advance(delta);
            move(step.x, step.y, world.moveLayers);
            return;
        }
        for (Ability ability : abilities) ability.tick(delta);
        flash = Math.max(0f, flash - delta);

        Vector2 dir = PlayerInput.readMove();
        moving = !dir.isZero();
        if (moving) dir.nor();
        PlayerInput.updateAim(this, world);
        if (GameSettings.justPressed(GameSettings.Action.DASH)) dash.trigger(moving ? dir : aim);

        Vector2 velocity = steering(world);
        if (velocity != null) move(velocity.x * delta, velocity.y * delta, world.moveLayers);
        else if (moving) move(dir.x * SPEED * delta, dir.y * SPEED * delta, world.moveLayers);

        if (Gdx.input.isButtonJustPressed(Input.Buttons.RIGHT) && !dash.isActive() && parry.trigger(world)) melee.cancel();
        if (parry.isActive()) parry.act(world);
        else if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) melee.trigger(world);
    }

    /** Déplacement imposé par la première capacité qui en demande un (dash, fente du coup, fente du parry). */
    private Vector2 steering(World world) {
        for (Ability ability : abilities) {
            Vector2 velocity = ability.steer(world);
            if (velocity != null) return velocity;
        }
        return null;
    }

    /** Hitbox de l'attaque, visible uniquement pendant son animation. */
    public Rectangle getAttackBounds() { return melee.bounds(); }

    @Override public void draw(SpriteBatch batch) {
        animations.draw(this, batch, stateTime, melee.remaining(), moving, dash.isActive(), dash.facing(),
                parry.animElapsed(), flash > 0f, isAlive() ? null : corpse);
    }

    @Override public void dispose() { animations.dispose(); }
}