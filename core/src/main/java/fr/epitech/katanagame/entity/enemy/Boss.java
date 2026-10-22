package fr.epitech.katanagame.entity.enemy;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.math.Vector2;

/**
 * Ennemi à points de vie : encaisse plusieurs coups, brièvement invincible après chacun, et ignore les bruits.
 * Il ne se pare pas : seuls ses projectiles se renvoient, et ils lui font plus de dégâts qu'un coup de sabre.
 */
public abstract class Boss extends Enemy {
    /** Invincibilité après un dégât : un coup de sabre touche plusieurs images de suite et ne doit compter qu'une fois. */
    private static final float HIT_COOLDOWN = 0.4f;
    /** Dégâts d'un de ses projectiles renvoyés. */
    private static final int REFLECTED_DAMAGE = 2;

    private final int maxHealth;
    private int health;
    private float hitCooldown;

    /**
     * Crée un boss.
     *
     * @param x abscisse initiale
     * @param y ordonnée initiale
     * @param runSpeed vitesse de déplacement (px/s)
     * @param maxHealth points de vie
     * @param width largeur du corps
     * @param height hauteur du corps
     * @param feetW largeur des pieds
     * @param feetH hauteur des pieds
     * @param feetOffsetY décalage centre du sprite → centre des pieds
     */
    protected Boss(float x, float y, float runSpeed, int maxHealth,
                   float width, float height, float feetW, float feetH, float feetOffsetY) {
        super(x, y, runSpeed, width, height, feetW, feetH, feetOffsetY);
        this.maxHealth = maxHealth;
        this.health = maxHealth;
    }

    /**
     * Points de vie restants.
     *
     * @return les PV, de 0 à {@link #getMaxHealth()}
     */
    public int getHealth() { return health; }

    /**
     * Points de vie au départ.
     *
     * @return les PV maximum
     */
    public int getMaxHealth() { return maxHealth; }

    /**
     * Vrai une fois le combat lancé (la barre de vie n'apparaît qu'à ce moment).
     *
     * @return vrai si le boss combat
     */
    public abstract boolean isEngaged();

    /** Appelé quand un dégât est encaissé sans tuer le boss. */
    protected void onHurt() {}

    /**
     * Vrai juste après un dégât (clignotement du sprite).
     *
     * @return vrai pendant l'invincibilité
     */
    public boolean isHurt() { return hitCooldown > 0f; }

    @Override
    public boolean isInvincible() { return hitCooldown > 0f; }

    @Override
    public void takeDamage() {
        hurt(1);
    }

    @Override
    public void takeReflectedHit() {
        hurt(REFLECTED_DAMAGE);
    }

    private void hurt(int damage) {
        if (!isAlive() || isInvincible()) return;
        health = Math.max(0, health - damage);
        hitCooldown = HIT_COOLDOWN;
        if (health == 0) kill();
        else onHurt();
    }

    /** Un boss ne se pare pas. */
    @Override
    public boolean canBeParried() {
        return false;
    }

    @Override
    protected void tick(float delta) {
        hitCooldown = Math.max(0f, hitCooldown - delta);
    }

    /** Le boss n'est jamais attiré par un bruit. */
    @Override
    public void hearNoise(Vector2 at) {}

    /** Pas de cône de vision : le boss voit toute l'arène. */
    @Override
    public void drawVision(ShapeRenderer shapes, TiledMapTileLayer[] walls) {}
}
