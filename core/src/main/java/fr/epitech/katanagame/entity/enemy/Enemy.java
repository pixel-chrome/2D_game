package fr.epitech.katanagame.entity.enemy;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import fr.epitech.katanagame.audio.Sounds;
import fr.epitech.katanagame.entity.Character;
import fr.epitech.katanagame.entity.projectile.LaserAssets;
import fr.epitech.katanagame.world.World;

/**
 * Base de tous les ennemis : IA (patrouille, alerte, poursuite), vision, stupeur du parry et mort.
 * Les sous-classes décident de leur comportement quand ils sont en forme (think) et de leur rendu.
 */
public abstract class Enemy extends Character {
    private static final float WIDTH = 14f, HEIGHT = 24f;
    private static final float FEET_W = 10f, FEET_H = 6f, FEET_OFFSET_Y = 10f;

    /** Logique de comportement et de patrouille. */
    protected final EnemyAI ai;
    private final Stun stun = new Stun();
    private float stateTime;
    private float deathTime;

    /**
     * Initialise un ennemi à une position, avec sa vitesse de course.
     *
     * @param x abscisse initiale
     * @param y ordonnée initiale
     * @param runSpeed vitesse de course (px/s)
     */
    protected Enemy(float x, float y, float runSpeed) {
        this(x, y, runSpeed, WIDTH, HEIGHT, FEET_W, FEET_H, FEET_OFFSET_Y);
    }

    /**
     * Initialise un ennemi avec des hitbox personnalisées (boss plus grand que les gardes).
     *
     * @param x abscisse initiale
     * @param y ordonnée initiale
     * @param runSpeed vitesse de course (px/s)
     * @param width largeur du corps
     * @param height hauteur du corps
     * @param feetW largeur des pieds
     * @param feetH hauteur des pieds
     * @param feetOffsetY décalage centre du sprite → centre des pieds
     */
    protected Enemy(float x, float y, float runSpeed, float width, float height,
                    float feetW, float feetH, float feetOffsetY) {
        super(x, y, width, height, feetW, feetH, feetOffsetY);
        aim.set(0f, -1f);
        ai = new EnemyAI(this, x, y, runSpeed);
    }

    /** Charge les ressources graphiques partagées des ennemis et des lasers. */
    public static void loadAssets() {
        EnemyAnimations.loadAssets();
        SkullKnightAnimations.loadAssets();
        LaserAssets.loadAssets();
        BossAnimations.loadAssets();
    }

    /** Libère les ressources graphiques partagées des ennemis et des lasers. */
    public static void disposeAssets() {
        EnemyAnimations.disposeAssets();
        SkullKnightAnimations.disposeAssets();
        LaserAssets.disposeAssets();
        BossAnimations.disposeAssets();
    }

    // --- Comportement ---

    /**
     * Comportement de l'ennemi vivant et non sonné : voir, se déplacer, attaquer.
     *
     * @param delta temps écoulé (s)
     * @param world état partagé du monde
     */
    protected abstract void think(float delta, World world);

    /**
     * Appelé à chaque image tant que l'ennemi est vivant, même sonné (recharges).
     *
     * @param delta temps écoulé (s)
     */
    protected void tick(float delta) {}

    /** Appelé à chaque image une fois mort (arrête les attaques en cours). */
    protected void onDeadTick() {}

    /** Appelé à chaque image où l'ennemi est sonné. */
    protected void onStunned() {}

    /** Appelé quand un parry l'atteint : les sous-classes annulent ici leur attaque. */
    protected void onParried() {}

    @Override
    public void update(float delta, World world) {
        stateTime += delta;
        if (!isAlive()) {
            deathTime += delta;
            ai.setSeesPlayer(false);
            onDeadTick();
            return;
        }
        tick(delta);
        if (stun.advance(delta, this, world.moveLayers)) {
            onStunned();
            return;
        }
        think(delta, world);
    }

    @Override
    protected void onDeath() {
        deathTime = 0f;
        Sounds.play(Sounds.Effect.DEATH);
    }

    /**
     * Indique si un parry peut l'atteindre en ce moment (faux : le parry passe sans effet ni son).
     * Un ennemi qui a repéré le joueur (point d'exclamation) ne se pare pas : seuls ses tirs se renvoient.
     *
     * @return vrai tant que l'ennemi ne voit pas le joueur
     */
    public boolean canBeParried() {
        return !ai.seesPlayer();
    }

    /** Touché par un projectile renvoyé : un dégât (les boss peuvent encaisser plus). */
    public void takeReflectedHit() {
        takeDamage();
    }

    /**
     * Repoussé par un parry venu de from : l'attaque est interrompue et l'ennemi reste sonné un instant. Même touché dans le dos, il se retourne et sait où est le joueur.
     *
     * @param dir direction du recul
     * @param from position du joueur qui pare
     */
    public void parried(Vector2 dir, Vector2 from) {
        if (!isAlive()) return;
        ai.alertAt(from);
        aim.set(from).sub(position).nor();
        stun.start(dir);
        onParried();
    }

    /**
     * Indique si l'ennemi est sonné par un parry.
     *
     * @return vrai pendant la stupeur
     */
    public boolean isStunned() {
        return stun.isActive();
    }

    /**
     * L'ennemi entend un bruit à cet endroit et vient voir.
     *
     * @param at position du bruit
     */
    public void hearNoise(Vector2 at) {
        if (isAlive()) {
            ai.hearNoise(at);
        }
    }

    // --- Affichage ---

    /**
     * Affiche le champ de vision de l'ennemi.
     *
     * @param shapes Le ShapeRenderer, déjà ouvert en mode Filled.
     * @param walls calques qui bloquent la vue
     */
    public void drawVision(ShapeRenderer shapes, TiledMapTileLayer[] walls) {
        EnemyVision.draw(this, shapes, walls);
    }

    /**
     * Affiche la préparation de l'attaque (par défaut rien).
     *
     * @param shapes Le ShapeRenderer, déjà ouvert en mode Filled.
     */
    public void drawCharge(ShapeRenderer shapes) {}

    /**
     * Affiche l'indicateur d'alerte de l'ennemi.
     *
     * @param shapes Le ShapeRenderer, déjà ouvert en mode Filled.
     */
    public void drawAlert(ShapeRenderer shapes) {
        if (!isAlive() || !ai.seesPlayer()) {
            return;
        }

        float bob = Math.abs(MathUtils.sin(stateTime * 8f)) * 2f;
        float x = position.x;
        float y = position.y + EnemyAnimations.FRAME_SIZE / 2f + 4f + bob;
        shapes.setColor(Color.BLACK);
        shapes.rect(x - 3f, y - 1f, 6f, 16f);
        shapes.setColor(1f, 0.2f, 0.2f, 1f);
        shapes.rect(x - 2f, y + 5f, 4f, 9f);
        shapes.rect(x - 2f, y, 4f, 4f);
    }

    @Override
    public abstract void draw(SpriteBatch batch);

    // --- Accès pour l'IA, les animations et les sous-classes ---

    /** Déplace l'ennemi en appliquant les collisions de la carte. */
    boolean moveForAI(float dx, float dy, TiledMapTileLayer[] walls) {
        return move(dx, dy, walls);
    }

    /** Retourne le temps écoulé pour l'animation courante. */
    float stateTime() {
        return stateTime;
    }

    /** Retourne le temps écoulé depuis la mort de l'ennemi. */
    float deathTime() {
        return deathTime;
    }

    /** Décalage entre le centre du sprite et le centre des pieds. */
    Vector2 feetOffset() {
        Rectangle feet = getFeetBounds();
        return new Vector2(feet.x + feet.width / 2f - position.x, feet.y + feet.height / 2f - position.y);
    }

    /** Indique si l'ennemi court vers sa cible. */
    boolean isRunning() {
        return ai.isRunning();
    }

    /** Indique si l'ennemi est en déplacement. */
    boolean isWalking() {
        return ai.isWalking();
    }

    /** Indique si l'ennemi est alerté. */
    boolean isAlerted() {
        return ai.isAlerted();
    }
}
