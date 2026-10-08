package fr.epitech.katanagame;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

public class Bullet {
    private static final float SPEED = 180f;     // px/s (le dash fait 600 : il est possible de l'esquiver)
    private static final float RADIUS = 2f;
    private static final float LIFETIME = 3f;    // secondes

    private final Vector2 position = new Vector2();
    private final Vector2 velocity = new Vector2();
    private final Rectangle bounds = new Rectangle();
    private float life = 0f;
    private boolean alive = true;

    public Bullet(Vector2 from, Vector2 direction) {
        position.set(from);
        velocity.set(direction).nor().scl(SPEED);
    }

    public void update(float delta, TiledMapTileLayer[] walls) {
        position.mulAdd(velocity, delta);
        life += delta;
        if (life > LIFETIME || Collision.isSolid(walls, position.x, position.y)) {
            alive = false;   // la balle disparaît contre un mur
        }
    }

    public void draw(ShapeRenderer shapes) {
        shapes.setColor(Color.YELLOW);
        shapes.circle(position.x, position.y, RADIUS);
    }

    public Rectangle getBounds() {
        bounds.set(position.x - RADIUS, position.y - RADIUS, RADIUS * 2, RADIUS * 2);
        return bounds;
    }

    public boolean isAlive() { return alive; }

    public void kill() { alive = false; }
}