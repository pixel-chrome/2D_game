package fr.epitech.katanagame.entity.player;

import com.badlogic.gdx.math.Vector2;
import fr.epitech.katanagame.audio.Sounds;
import fr.epitech.katanagame.entity.Direction;
import fr.epitech.katanagame.world.World;

/** Dash : élan court pendant lequel le joueur est invincible. */
final class DashAbility extends Ability {
    private static final float SPEED = 460f, DURATION = 0.12f, COOLDOWN = 0.75f;

    private final Vector2 dir = new Vector2();
    private final Vector2 velocity = new Vector2();
    private Direction facing = Direction.DOWN;
    private boolean started;

    DashAbility(Player player) { super(player); }

    /** Lance le dash dans la direction donnée, si la recharge le permet. */
    void trigger(Vector2 direction) {
        if (!isReady() || isActive()) return;
        dir.set(direction);
        facing = Direction.of(dir);
        started = true;
        Sounds.play(Sounds.Effect.DASH);
        timer = DURATION;
        cooldown = COOLDOWN;
    }

    @Override Vector2 steer(World world) {
        return isActive() ? velocity.set(dir).scl(SPEED) : null;
    }

    /** Vrai une seule fois par dash, au moment où il démarre. */
    boolean pollStart() {
        boolean was = started;
        started = false;
        return was;
    }

    Vector2 direction() { return dir; }

    Direction facing() { return facing; }
}
