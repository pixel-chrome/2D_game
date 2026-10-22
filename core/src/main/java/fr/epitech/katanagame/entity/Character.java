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

    /** Direction de visée (vecteur unitaire). */
    protected final Vector2 aim = new Vector2(1, 0);
    private boolean alive = true;

    /**
     * Initialise les dimensions et la position du personnage.
     *
     * @param x abscisse initiale
     * @param y ordonnée initiale
     * @param width largeur du corps
     * @param height hauteur du corps
     * @param feetW largeur des pieds
     * @param feetH hauteur des pieds
     * @param feetOffsetY décalage centre du sprite → centre des pieds
     */
    protected Character(float x, float y, float width, float height,
                        float feetW, float feetH, float feetOffsetY) {
        super(x, y, width, height, feetW, feetH, feetOffsetY);
    }

    // --- Vie ---

    /**
     * Indique si le personnage est vivant.
     *
     * @return vrai tant que le personnage n'est pas mort
     */
    public boolean isAlive() {
        return alive;
    }

    /**
     * Indique si le personnage ignore les dégâts.
     *
     * @return vrai si les dégâts sont ignorés (faux par défaut)
     */
    public boolean isInvincible() {
        return false;
    }

    /** Applique un dégât si le personnage peut être touché. */
    public void takeDamage() {
        if (!alive || isInvincible()) return;
        kill();
    }

    /** Tue le personnage quoi qu'il arrive (même invincible). */
    public void kill() {
        if (!alive) return;
        alive = false;
        onDeath();
    }

    /**
     * Remet le personnage en vie à une position donnée.
     *
     * @param x abscisse de réapparition
     * @param y ordonnée de réapparition
     */
    public void respawn(float x, float y) {
        position.set(x, y);
        alive = true;
    }

    /** Point d’extension appelé lorsque le personnage meurt. */
    protected void onDeath() {}

    // --- Visée ---

    /**
     * Retourne le vecteur de visée du personnage.
     *
     * @return le vecteur de visée (modifiable)
     */
    public Vector2 getAim() {
        return aim;
    }

    /**
     * Retourne la direction correspondant à la visée.
     *
     * @return la direction de sprite la plus proche de la visée
     */
    public Direction getFacing() {
        return Direction.of(aim);
    }

    // --- Déplacement ---

    /**
     * Déplace le personnage par petits pas, axe par axe (pour glisser le long des murs).
     *
     * @param dx déplacement en x
     * @param dy déplacement en y
     * @param solids calques qui bloquent le déplacement
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
