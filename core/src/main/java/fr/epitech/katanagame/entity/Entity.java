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
    /** Position du centre du sprite, en unités du monde. */
    protected final Vector2 position = new Vector2();

    private final Rectangle bounds = new Rectangle();
    private final Rectangle feet = new Rectangle();
    private final float width, height;
    private final float feetW, feetH, feetOffsetY;

    /**
     * Crée une entité avec sa hitbox de corps et celle de ses pieds.
     *
     * @param feetOffsetY distance entre le centre du sprite et le centre des pieds
     * @param x abscisse initiale du centre du sprite
     * @param y ordonnée initiale du centre du sprite
     * @param width largeur de la hitbox du corps
     * @param height hauteur de la hitbox du corps
     * @param feetW largeur de la hitbox des pieds
     * @param feetH hauteur de la hitbox des pieds
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

    /**
     * Met à jour l’entité ; l’implémentation dépend du type d’entité.
     *
     * @param delta temps écoulé depuis la dernière image (s)
     * @param world état partagé du monde
     */
    public abstract void update(float delta, World world);

    /**
     * Dessine l’entité ; l’implémentation dépend du type d’entité.
     *
     * @param batch batch déjà ouvert
     */
    public abstract void draw(SpriteBatch batch);

    /**
     * Retourne la position de l’entité.
     *
     * @return la position du centre du sprite (modifiable)
     */
    public Vector2 getPosition() {
        return position;
    }

    /**
     * Hitbox du corps entier (dégâts).
     *
     * @return la hitbox du corps, recalculée à chaque appel
     */
    public Rectangle getBounds() {
        bounds.set(position.x - width / 2f, position.y - height / 2f, width, height);
        return bounds;
    }

    /**
     * Hitbox des pieds (collisions avec les murs).
     *
     * @return la hitbox des pieds, recalculée à chaque appel
     */
    public Rectangle getFeetBounds() {
        feet.set(position.x - feetW / 2f, position.y - feetOffsetY - feetH / 2f, feetW, feetH);
        return feet;
    }

    /**
     * Vrai si les pieds touchent une case solide (ou sortent de la map).
     *
     * @param solids calques qui bloquent le déplacement
     * @return vrai si les pieds touchent un obstacle
     */
    protected boolean collidesWithSolid(TiledMapTileLayer[] solids) {
        return Collision.overlapsSolid(solids, getFeetBounds());
    }
}
