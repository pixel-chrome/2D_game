package fr.epitech.katanagame.entity.player;

import com.badlogic.gdx.math.Vector2;

/** État du joueur mort : temps écoulé, sens de chute et glissade du corps projeté par le tir. */
final class Corpse {
    /** Poussée initiale (px/s) et freinage au sol. */
    private static final float KNOCKBACK = 260f, KNOCK_DRAG = 7f;

    private final Vector2 knock = new Vector2();
    private final Vector2 step = new Vector2();
    private float time;
    private float side = 1f;

    /** Prépare la chute d'un corps touché par un tir venant dans la direction dir. */
    void fall(Vector2 dir) {
        knock.set(dir).nor().scl(KNOCKBACK);
        side = dir.x >= 0f ? -1f : 1f;
        time = 0f;
    }

    void reset() {
        knock.setZero();
        time = 0f;
    }

    /** Fait avancer le temps et retourne le déplacement du corps pour cette image. */
    Vector2 advance(float delta) {
        time += delta;
        knock.scl(Math.max(0f, 1f - KNOCK_DRAG * delta));
        return step.set(knock).scl(delta);
    }

    float time() { return time; }

    /** -1 ou +1 : côté vers lequel le corps bascule. */
    float side() { return side; }
}
