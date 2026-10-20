package fr.epitech.katanagame.entity.projectile;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import fr.epitech.katanagame.world.Collision;

import java.util.ArrayDeque;
import java.util.Deque;

/** Représente un projectile laser, son déplacement et sa traînée visuelle. */
public class Bullet {
    /** Vitesse d'un laser (px/s). */
    public static final float SPEED = 500f;
    private static final float RADIUS = 2f;
    private static final float REFLECT_BOOST = 1.6f;
    private static final float LIFETIME = 3f;
    private static final float DRAW_WIDTH = 18f;
    private static final float DRAW_HEIGHT = 10f;

    /** Nombre de positions conservées pour dessiner la traînée. */
    private static final int TRAIL_LENGTH = 8;
    private static final float TRAIL_ALPHA = 0.42f;
    private static final float TRAIL_SCALE = 0.82f;

    private final Vector2 position = new Vector2();
    private final Vector2 velocity = new Vector2();
    private final Rectangle bounds = new Rectangle();
    private final Texture texture;
    private final Deque<Vector2> trail = new ArrayDeque<>();
    private float life = 0f;
    private boolean alive = true;
    private boolean reflected;

    /**
     * Crée un projectile à une position et dans une direction données.
     *
     * @param from position de départ
     * @param direction direction du tir
     * @param texture texture du laser
     */
    public Bullet(Vector2 from, Vector2 direction, Texture texture) {
        position.set(from);
        velocity.set(direction).nor().scl(SPEED);
        this.texture = texture;
    }

    /**
     * Déplace le projectile, mémorise sa trajectoire et vérifie les collisions.
     *
     * @param delta temps écoulé (s)
     * @param walls calques qui arrêtent le laser
     */
    public void update(float delta, TiledMapTileLayer[] walls) {
        position.mulAdd(velocity, delta);
        life += delta;

        // Mémorise les dernières positions pour créer une traînée de la même couleur que le laser.
        trail.addFirst(new Vector2(position));
        while (trail.size() > TRAIL_LENGTH) {
            trail.removeLast();
        }

        if (life > LIFETIME || Collision.isSolid(walls, position.x, position.y)) {
            alive = false;
        }
    }

    /**
     * Dessine la traînée lumineuse puis le laser dans sa direction de déplacement.
     *
     * @param batch batch déjà ouvert
     */
    public void draw(SpriteBatch batch) {
        float angle = velocity.angleDeg() + 180f;
        int index = 0;

        // Les copies deviennent progressivement plus transparentes vers l'arrière.
        for (Vector2 point : trail) {
            float progress = 1f - (float) index / TRAIL_LENGTH;
            float alpha = TRAIL_ALPHA * progress;
            drawSprite(batch, point, angle, TRAIL_SCALE, alpha);
            index++;
        }

        drawSprite(batch, position, angle, 1f, 1f);
        batch.setColor(1f, 1f, 1f, 1f);
    }

    /** Dessine une copie du sprite avec une échelle et une transparence données. */
    private void drawSprite(SpriteBatch batch, Vector2 point, float angle, float scale, float alpha) {
        float width = DRAW_WIDTH * scale;
        float height = DRAW_HEIGHT * scale;
        batch.setColor(1f, 1f, 1f, alpha);
        batch.draw(
                texture,
                point.x - width / 2f,
                point.y - height / 2f,
                width / 2f,
                height / 2f,
                width,
                height,
                1f,
                1f,
                angle,
                0,
                0,
                texture.getWidth(),
                texture.getHeight(),
                false,
                false
        );
    }

    /**
     * Renvoie le projectile (parry) : plus rapide, il blesse désormais les ennemis et plus le joueur.
     *
     * @param direction nouvelle direction
     */
    public void reflect(Vector2 direction) {
        velocity.set(direction).nor().scl(SPEED * REFLECT_BOOST);
        reflected = true;
        life = 0f;
        trail.clear();
    }

    /**
     * Indique si le projectile a été renvoyé par le joueur.
     *
     * @return vrai si le joueur l'a renvoyé
     */
    public boolean isReflected() {
        return reflected;
    }

    /**
     * Position actuelle du projectile.
     *
     * @return la position (modifiable)
     */
    public Vector2 getPosition() {
        return position;
    }

    /**
     * Direction de déplacement (vecteur unitaire).
     *
     * @return une copie du vecteur unitaire de déplacement
     */
    public Vector2 getDirection() {
        return new Vector2(velocity).nor();
    }

    /**
     * Retourne la hitbox actuelle du projectile.
     *
     * @return la hitbox, recalculée à chaque appel
     */
    public Rectangle getBounds() {
        bounds.set(position.x - RADIUS, position.y - RADIUS, RADIUS * 2f, RADIUS * 2f);
        return bounds;
    }

    /**
     * Indique si le projectile est encore actif.
     *
     * @return vrai tant que le projectile existe
     */
    public boolean isAlive() {
        return alive;
    }

    /** Désactive le projectile. */
    public void kill() {
        alive = false;
    }
}
