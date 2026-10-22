package fr.epitech.katanagame.entity.player;

import com.badlogic.gdx.math.Vector2;
import fr.epitech.katanagame.audio.Sounds;
import fr.epitech.katanagame.entity.enemy.Enemy;
import fr.epitech.katanagame.entity.projectile.Bullet;
import fr.epitech.katanagame.world.World;

/** Parry (clic droit) : renvoie les balles vers la souris et repousse les ennemis au contact. */
final class ParryAbility extends Ability {
    /** Fenêtre active, recharge, durée de l'animation complète (ATTACK 2). */
    private static final float DURATION = 0.22f, COOLDOWN = 0.45f;
    static final float ANIM = 0.32f;
    /** Petite fente vers l'avant au début du parry. */
    private static final float LUNGE_TIME = 0.09f, LUNGE_SPEED = 260f;
    /** Zone devant le joueur : rayon, décalage, bonus de rayon pour les ennemis, rayon du bruit du choc. */
    private static final float RADIUS = 34f, FORWARD = 14f, MELEE_BONUS = 10f, NOISE_RADIUS = 120f;

    private final Vector2 zone = new Vector2();
    private final Vector2 velocity = new Vector2();
    private float animTimer;
    private boolean succeeded;

    ParryAbility(Player player) { super(player); }

    @Override void tick(float delta) {
        super.tick(delta);
        animTimer = Math.max(0f, animTimer - delta);
    }

    /** Lance le parry. Vrai s'il a démarré. */
    boolean trigger(World world) {
        if (!isReady()) return false;
        timer = DURATION;
        animTimer = ANIM;
        cooldown = COOLDOWN;
        Sounds.play(Sounds.Effect.PARRY);
        world.parryFx.swing(zone(), player.getAim().angleDeg());
        return true;
    }

    @Override Vector2 steer(World world) {
        boolean lunging = timer > DURATION - LUNGE_TIME;
        return lunging ? velocity.set(player.getAim()).scl(LUNGE_SPEED) : null;
    }

    @Override void act(World world) {
        Vector2 aim = player.getAim(), z = zone();
        boolean any = false;
        for (Bullet bullet : world.bullets) {
            if (bullet.isAlive() && !bullet.isReflected() && bullet.getPosition().dst(z) <= RADIUS) {
                bullet.reflect(aim);
                world.parryFx.impact(bullet.getPosition(), aim);
                any = true;
            }
        }
        for (Enemy enemy : world.enemies) {
            if (!enemy.isAlive() || enemy.isStunned() || !enemy.canBeParried()) continue;
            Vector2 at = enemy.getBounds().getCenter(new Vector2());
            if (at.dst(z) > RADIUS + MELEE_BONUS) continue;
            Vector2 from = player.getPosition();
            enemy.parried(at.cpy().sub(from), from);
            world.makeNoise(from, NOISE_RADIUS, enemy);          // le bruit du choc attire les autres
            world.parryFx.impact(at, aim);
            any = true;
        }
        if (any) {
            succeeded = true;
            Sounds.play(Sounds.Effect.PARRY_HIT);
            cooldown = 0f;                                        // un parry réussi se rejoue tout de suite
            player.flash();
        }
    }

    /** Centre de la zone, devant le joueur dans la direction de visée. */
    private Vector2 zone() {
        return zone.set(player.getPosition()).mulAdd(player.getAim(), FORWARD);
    }

    /** Vrai une seule fois par parry réussi. */
    boolean pollSuccess() {
        boolean was = succeeded;
        succeeded = false;
        return was;
    }

    /** Temps écoulé de l'animation, ou -1 si elle ne joue pas. */
    float animElapsed() { return animTimer > 0f ? ANIM - animTimer : -1f; }

    @Override void reset() {
        super.reset();
        animTimer = 0f;
    }
}
