package fr.epitech.katanagame.entity;

import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.math.Vector2;

/**
 * Une entité vivante qui bouge, vise et peut mourir.
 * Un coup = mort (comme Katana Zero). Les sous-classes peuvent refuser le coup
 * via isInvincible() et réagir à la mort via onDeath().
 */
public abstract class Character extends Entity {
    private static final float MAX_STEP = 4f;   // pas de déplacement max (px) pour ne pas traverser les murs

    protected final Vector2 aim = new Vector2(1, 0);
    private boolean alive = true;

    protected Character(float x, float y, float width, float height,
                        float feetW, float feetH, float feetOffsetY) {
        super(x, y, width, height, feetW, feetH, feetOffsetY);
    }

    // --- Vie ---

    public boolean isAlive() {
        return alive;
    }

    public boolean isInvincible() {
        return false;
    }

    public void takeDamage() {
        if (!alive || isInvincible()) return;
        alive = false;
        onDeath();
    }

    /** Remet le personnage en vie à une position donnée. */
    public void respawn(float x, float y) {
        position.set(x, y);
        alive = true;
    }

    protected void onDeath() {}

    // --- Visée ---

    public Vector2 getAim() {
        return aim;
    }

    public Direction getFacing() {
        return Direction.of(aim);
    }

    // --- Déplacement ---

    /**
     * Déplace le personnage par petits pas, axe par axe (pour glisser le long des murs).
     * @return false si un mur l'a bloqué
     */
    protected boolean move(float dx, float dy, TiledMapTileLayer[] solids) {
        int steps = Math.max(1, (int) Math.ceil(Math.max(Math.abs(dx), Math.abs(dy)) / MAX_STEP));
        float sx = dx / steps, sy = dy / steps;
        boolean blocked = false;

        for (int i = 0; i < steps; i++) {
            position.x += sx;
            if (collidesWithSolid(solids)) { position.x -= sx; blocked |= sx != 0; }
            position.y += sy;
            if (collidesWithSolid(solids)) { position.y -= sy; blocked |= sy != 0; }
        }
        return !blocked;
    }
}
