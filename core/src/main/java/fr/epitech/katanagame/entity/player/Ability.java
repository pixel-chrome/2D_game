package fr.epitech.katanagame.entity.player;

import com.badlogic.gdx.math.Vector2;
import fr.epitech.katanagame.world.World;

/**
 * Capacité du joueur (dash, coup, parry) : une durée d'action et une recharge.
 * Une capacité peut aussi imposer un déplacement au joueur le temps de son action.
 */
abstract class Ability {
    protected final Player player;
    /** Temps restant de l'action en cours (s) et temps restant avant de pouvoir recommencer (s). */
    protected float timer, cooldown;

    Ability(Player player) { this.player = player; }

    /** Fait avancer les durées (à appeler une fois par image, avant tout déclenchement). */
    void tick(float delta) {
        timer = Math.max(0f, timer - delta);
        cooldown = Math.max(0f, cooldown - delta);
    }

    /** Vrai pendant l'action. */
    boolean isActive() { return timer > 0f; }

    /** Vrai quand la recharge est terminée. */
    boolean isReady() { return cooldown <= 0f; }

    /** Temps restant de l'action en cours. */
    float remaining() { return timer; }

    /** Vitesse (px/s) que la capacité impose au joueur pendant l'image, ou null si elle ne le déplace pas. */
    Vector2 steer(World world) { return null; }

    /** Effet appliqué à chaque image pendant l'action (après le déplacement). */
    void act(World world) {}

    /** Coupe l'action et la recharge (réapparition). */
    void reset() { timer = cooldown = 0f; }
}
