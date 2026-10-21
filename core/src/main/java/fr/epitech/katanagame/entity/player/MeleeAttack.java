package fr.epitech.katanagame.entity.player;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import fr.epitech.katanagame.audio.Sounds;
import fr.epitech.katanagame.entity.enemy.Enemy;
import fr.epitech.katanagame.world.World;

/** Coup de sabre (clic gauche) : fente vers la souris qui tue les ennemis touchés, avec recharge. */
final class MeleeAttack extends Ability {
    /** Durée de l'animation du coup. */
    static final float DURATION = 0.56f;
    private static final float COOLDOWN = 0.5f;
    private static final float LUNGE_TIME = 0.14f, LUNGE_SPEED = 340f;
    private static final float REACH = 20f, OFFSET = -12f, WIDTH = 30f, HEIGHT = 24f;
    /** Un kill attire les gardes proches. */
    private static final float KILL_NOISE_RADIUS = 110f;

    private final Rectangle bounds = new Rectangle();
    private final Vector2 lungeDir = new Vector2();
    private final Vector2 velocity = new Vector2();
    private float lungeTimer;
    private boolean hit;

    MeleeAttack(Player player) { super(player); }

    @Override void tick(float delta) {
        super.tick(delta);
        lungeTimer = Math.max(0f, lungeTimer - delta);
    }

    /** Lance le coup si la recharge le permet. */
    void trigger(World world) {
        if (!isReady()) return;
        timer = DURATION;
        cooldown = COOLDOWN;
        lungeTimer = LUNGE_TIME;
        lungeDir.set(player.getAim());
        Sounds.play(Sounds.Effect.ATTACK);
        strike(world);
    }

    /** Interrompt l'animation du coup (le parry prend la main). */
    void cancel() { timer = 0f; }

    @Override Vector2 steer(World world) {
        if (lungeTimer <= 0f) return null;
        world.slashFx.speedLine(player.getPosition(), lungeDir);
        strike(world);                           // le sabre touche tout le long de la fente
        return velocity.set(lungeDir).scl(LUNGE_SPEED);
    }

    /** Frappe les ennemis dans la hitbox du coup. */
    private void strike(World world) {
        Vector2 pos = player.getPosition(), aim = player.getAim();
        bounds.set(pos.x + aim.x * REACH + OFFSET, pos.y + aim.y * REACH + OFFSET, WIDTH, HEIGHT);
        for (Enemy enemy : world.enemies) {
            if (!enemy.isAlive() || !bounds.overlaps(enemy.getBounds())) continue;
            enemy.takeDamage();
            Vector2 at = enemy.getBounds().getCenter(new Vector2());
            Vector2 dir = at.cpy().sub(pos);
            world.slashFx.hit(at, dir);
            hit = true;
            player.flash();
            if (!enemy.isAlive()) {
                world.blood.splatter(at, dir, world.bulletLayers);
                world.makeNoise(at, KILL_NOISE_RADIUS, enemy);
            }
        }
    }

    /** Vrai une seule fois par coup qui touche. */
    boolean pollHit() {
        boolean was = hit;
        hit = false;
        return was;
    }

    /** Hitbox du coup, visible uniquement pendant l'animation. */
    Rectangle bounds() { return isActive() ? bounds : null; }

    @Override void reset() {
        super.reset();
        lungeTimer = 0f;
    }
}
