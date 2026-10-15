package fr.epitech.katanagame.entity;

import fr.epitech.katanagame.world.Collision;
import fr.epitech.katanagame.world.World;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

/**
 * Tout ce qui existe dans le monde : une position et deux hitbox
 * (corps entier pour les dégâts, pieds pour les collisions avec les murs).
 */
public abstract class Entity {
    protected final Vector2 position = new Vector2();

    private final Rectangle bounds = new Rectangle();
    private final Rectangle feet = new Rectangle();
    private final float width, height;
    private final float feetW, feetH, feetOffsetY;

    /**
     * @param feetOffsetY distance entre le centre du sprite et le centre des pieds
     */
    protected Entity(float x, float y, float width, float height,
                     float feetW, float feetH, float feetOffsetY) {
        position.set(x, y);
        this.width = width;
        this.height = height;
        this.feetW = feetW;
        this.feetH = feetH;
        this.feetOffsetY = feetOffsetY;
    }

    public abstract void update(float delta, World world);

    public abstract void draw(SpriteBatch batch);

    public Vector2 getPosition() {
        return position;
    }

    /** Hitbox du corps entier (dégâts). */
    public Rectangle getBounds() {
        bounds.set(position.x - width / 2f, position.y - height / 2f, width, height);
        return bounds;
    }

    /** Hitbox des pieds (collisions avec les murs). */
    public Rectangle getFeetBounds() {
        feet.set(position.x - feetW / 2f, position.y - feetOffsetY - feetH / 2f, feetW, feetH);
        return feet;
    }

    /** Vrai si les pieds touchent une case solide (ou sortent de la map). */
    protected boolean collidesWithSolid(TiledMapTileLayer[] solids) {
        return Collision.overlapsSolid(solids, getFeetBounds());
    }
}
