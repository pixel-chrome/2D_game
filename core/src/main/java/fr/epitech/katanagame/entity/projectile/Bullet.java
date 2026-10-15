package fr.epitech.katanagame.entity.projectile;

import fr.epitech.katanagame.world.Collision;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

/** Représente un projectile et gère son déplacement et sa durée de vie. */
public class Bullet {
    public static final float SPEED = 500f;     // px/s (le dash fait 600 : il est possible de l'esquiver)
    private static final float RADIUS = 2f;
    /** Durée de vie maximale du projectile. */
    private static final float LIFETIME = 3f;    // secondes

    private final Vector2 position = new Vector2();
    private final Vector2 velocity = new Vector2();
    private final Rectangle bounds = new Rectangle();
    private float life = 0f;
    private boolean alive = true;

    /** Crée un projectile à une position et dans une direction données. */
    public Bullet(Vector2 from, Vector2 direction) {
        position.set(from);
        velocity.set(direction).nor().scl(SPEED);
    }

    /** Déplace le projectile et le désactive en cas de collision ou d’expiration. */
    public void update(float delta, TiledMapTileLayer[] walls) {
        position.mulAdd(velocity, delta);
        life += delta;
        if (life > LIFETIME || Collision.isSolid(walls, position.x, position.y)) {
            alive = false;   // la balle disparaît contre un mur
        }
    }

    /** Dessine le projectile. */
    public void draw(ShapeRenderer shapes) {
        shapes.setColor(Color.YELLOW);
        shapes.circle(position.x, position.y, RADIUS);
        shapes.rectLine(position.x, position.y,
                position.x - velocity.x * 0.025f, position.y - velocity.y * 0.025f, RADIUS * 1.5f);
    }

    /** Retourne la hitbox actuelle du projectile. */
    public Rectangle getBounds() {
        bounds.set(position.x - RADIUS, position.y - RADIUS, RADIUS * 2, RADIUS * 2);
        return bounds;
    }

    /** Indique si le projectile est encore actif. */
    public boolean isAlive() { return alive; }

    /** Désactive le projectile. */
    public void kill() { alive = false; }
}
