package fr.epitech.katanagame.entity.enemy;

import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.math.Vector2;

/** Stupeur d'un ennemi repoussé par un parry : il glisse en arrière en ralentissant, puis se reprend. */
final class Stun {
    static final float TIME = 0.7f;
    private static final float KNOCKBACK_SPEED = 260f, KNOCKBACK_DRAG = 7f;

    private final Vector2 knockback = new Vector2();
    private float timer;

    /** Démarre la stupeur avec un recul dans la direction dir. */
    void start(Vector2 dir) {
        timer = TIME;
        knockback.set(dir).nor().scl(KNOCKBACK_SPEED);
    }

    boolean isActive() { return timer > 0f; }

    /** Fait glisser l'ennemi en arrière ; vrai tant qu'il est sonné. */
    boolean advance(float delta, Enemy enemy, TiledMapTileLayer[] walls) {
        if (timer <= 0f) return false;
        timer -= delta;
        enemy.moveForAI(knockback.x * delta, knockback.y * delta, walls);
        knockback.scl(Math.max(0f, 1f - KNOCKBACK_DRAG * delta));
        return true;
    }
}
