package fr.epitech.katanagame.entity.enemy;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import fr.epitech.katanagame.audio.Sounds;
import fr.epitech.katanagame.entity.player.Player;
import fr.epitech.katanagame.entity.projectile.LaserAssets;
import fr.epitech.katanagame.world.World;

/**
 * Le Chapelier Noir, boss de la dernière salle. Il se réveille quand le joueur approche, puis enchaîne ses attaques
 * (annonce, action, récupération). Trois phases selon ses PV : plus de patterns et un rythme plus rapide.
 */
public final class FinalBoss extends Boss {
    /** Points de vie. */
    public static final int HEALTH = 12;
    private static final float WAKE_DISTANCE = 380f, INTRO_TIME = 1.2f;
    private static final float WALK_SPEED = 70f, ENRAGED_SPEED = 100f;
    private static final float NEAR = 90f, FAR = 170f, CLOSE_SWEEP = 80f;

    private enum State { DORMANT, IDLE, WINDUP, ACTIVE, RECOVER }

    private final Texture laser = LaserAssets.getRandomTexture();
    private final BossAttack fan = new FanAttack(), dash = new DashStrike(),
            sweep = new SweepAttack(), radial = new RadialBurst();
    private final Vector2 target = new Vector2();

    private State state = State.DORMANT;
    private BossAttack attack, last;
    private float timer, cooldown, strafeTimer, strafeSide = 1f;
    private boolean moving;

    /**
     * Crée le boss à la position donnée.
     *
     * @param x abscisse initiale
     * @param y ordonnée initiale
     */
    public FinalBoss(float x, float y) {
        super(x, y, WALK_SPEED, HEALTH, 30f, 66f, 20f, 8f, 27f);
    }

    // --- Phases ---

    /**
     * Phase du combat selon les PV restants.
     *
     * @return 1 (PV > 2/3), 2 (PV > 1/3) ou 3
     */
    int phase() {
        int third = HEALTH / 3;
        if (getHealth() > 2 * third) return 1;
        return getHealth() > third ? 2 : 3;
    }

    /** Multiplicateur de durée des annonces et pauses : le boss accélère de phase en phase. */
    private float tempo() {
        return phase() == 1 ? 1f : phase() == 2 ? 0.85f : 0.7f;
    }

    // --- Comportement ---

    @Override
    public boolean isEngaged() { return state != State.DORMANT; }

    @Override
    protected void think(float delta, World world) {
        Player player = world.player;
        Rectangle pb = player.getBounds();
        target.set(pb.x + pb.width / 2f, pb.y + pb.height / 2f);
        moving = false;
        if (!player.isAlive()) {
            if (state == State.ACTIVE || state == State.WINDUP) endAttack();
            return;
        }
        if (state == State.DORMANT) {
            if (position.dst(target) > WAKE_DISTANCE) return;
            Sounds.play(Sounds.Effect.DETECT);
            state = State.IDLE;
            cooldown = INTRO_TIME;
        }
        switch (state) {
            case IDLE: idle(delta, world); break;
            case WINDUP: windup(delta, world); break;
            case ACTIVE: active(delta, world); break;
            default: recover(delta); break;
        }
    }

    private void idle(float delta, World world) {
        aimAtPlayer();
        float dist = position.dst(target);
        float speed = phase() == 3 ? ENRAGED_SPEED : WALK_SPEED;
        strafeTimer -= delta;
        if (strafeTimer <= 0f) {
            strafeSide = -strafeSide;
            strafeTimer = 1.5f;
        }
        Vector2 toPlayer = new Vector2(target).sub(position).nor();
        Vector2 move = new Vector2();
        if (dist > FAR) move.set(toPlayer);
        else if (dist < NEAR) move.set(toPlayer).scl(-1f);
        else move.set(-toPlayer.y * strafeSide, toPlayer.x * strafeSide);
        step(move.x * speed * delta, move.y * speed * delta, world);
        moving = true;

        cooldown -= delta;
        if (cooldown <= 0f) startAttack(dist);
    }

    private void startAttack(float dist) {
        attack = pickAttack(dist);
        last = attack;
        state = State.WINDUP;
        timer = 0f;
        aimAtPlayer();
        attack.begin(this);
    }

    /** Choisit une attaque selon la phase et la distance, sans répéter la précédente. */
    private BossAttack pickAttack(float dist) {
        if (dist < CLOSE_SWEEP && last != sweep && MathUtils.randomBoolean(0.7f)) return sweep;
        BossAttack[] pool = phase() == 1
                ? new BossAttack[] {fan, dash}
                : new BossAttack[] {fan, dash, radial, sweep};
        BossAttack pick;
        do {
            pick = pool[MathUtils.random(pool.length - 1)];
        } while (pick == last);
        return pick;
    }

    private void windup(float delta, World world) {
        timer += delta;
        if (!attack.lockAim()) aimAtPlayer();
        if (timer < attack.windup * tempo()) return;
        state = State.ACTIVE;
        timer = 0f;
        attack.fire(this, world);
    }

    private void active(float delta, World world) {
        timer += delta;
        attack.tick(this, delta, timer, world);
        if (timer >= attack.active) {
            state = State.RECOVER;
            timer = 0f;
        }
    }

    private void recover(float delta) {
        timer += delta;
        if (timer >= attack.recover * tempo()) endAttack();
    }

    /** Retour au repos après une attaque, avec la pause de la phase. */
    private void endAttack() {
        state = State.IDLE;
        cooldown = 1.1f * tempo();
    }

    private void aimAtPlayer() {
        aim.set(target).sub(position);
        if (aim.isZero()) aim.set(0f, -1f);
        aim.nor();
    }

    /** Déplacement avec collisions, utilisé par les attaques. */
    void step(float dx, float dy, World world) {
        moveForAI(dx, dy, world.moveLayers);
    }

    /** Texture des lasers du boss. */
    Texture laserTexture() { return laser; }

    // --- Affichage ---

    @Override
    public void drawCharge(ShapeRenderer shapes) {
        if (isAlive() && state == State.WINDUP) {
            attack.drawTelegraph(shapes, this, windupProgress());
        }
    }

    @Override
    public void draw(SpriteBatch batch) {
        BossAnimations.draw(this, batch);
    }

    /** Avancement de l'annonce (0 à 1), ou -1 hors annonce. */
    float windupProgress() {
        return state == State.WINDUP ? Math.min(1f, timer / (attack.windup * tempo())) : -1f;
    }

    /** Avancement de l'action (0 à 1), ou -1 hors action. */
    float activeProgress() {
        return state == State.ACTIVE ? Math.min(1f, timer / attack.active) : -1f;
    }

    /** Vrai pendant les déplacements normaux (animation de marche). */
    boolean isMoving() { return moving; }

    /** Temps écoulé pour les animations. */
    float time() { return stateTime(); }
}
