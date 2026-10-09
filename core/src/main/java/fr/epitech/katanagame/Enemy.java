package fr.epitech.katanagame;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class Enemy {
    public enum Direction { RIGHT, LEFT, DOWN, UP }

    // --- Comportement ---
    private static final float WIDTH = 14f;             // hitbox (largeur)
    private static final float HEIGHT = 24f;            // hitbox (hauteur)
    private static final float DETECTION_RANGE = 180f;  // distance de vue (px)
    private static final float WINDUP = 0.4f;           // temps de visée avant le tir
    private static final float FIRE_INTERVAL = 1.5f;    // délai minimum entre deux tirs
    private static final float SHOOT_EFFECT_TIME = 0.12f; // temps d'affichage du tir

    // --- Sprites Pixel_Hot ---
    private static final String SPRITESHEET_PATH = "Pixel_Hot/Characters_Sprite_Sheet.png";
    private static final int FRAME_SIZE = 32;
    private static final float FRAME_TIME_IDLE = 0.15f;
    private static final float FRAME_TIME_SHOOT = 0.08f;
    private static final float FRAME_TIME_DEATH = 0.08f;

    private static Texture sheetTexture;
    private static Animation<TextureRegion> idleAnim;
    private static Animation<TextureRegion> deathAnim;
    private static final Map<Direction, Animation<TextureRegion>> shootAnims = new EnumMap<>(Direction.class);

    private final Vector2 position = new Vector2();
    private final Vector2 aim = new Vector2(0, -1);
    private final Rectangle bounds = new Rectangle();

    private boolean alive = true;
    private boolean winding = false;    // en train de viser
    private float windupTimer = 0f;
    private float cooldown = 0f;
    private float shootTimer = 0f;      // flash de tir
    private float stateTime = 0f;
    private float deathTime = 0f;

    public Enemy(float x, float y) {
        position.set(x, y);
    }

    /** À appeler une fois dans Main.create(), avant de créer les ennemis. */
    public static void loadAssets() {
        if (sheetTexture != null) return;

        sheetTexture = new Texture(SPRITESHEET_PATH);
        sheetTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

        TextureRegion[][] grid = TextureRegion.split(sheetTexture, FRAME_SIZE, FRAME_SIZE);

        // Idle avec arme (Ligne 28, colonnes 0 à 3)
        TextureRegion[] idleFrames = new TextureRegion[4];
        for (int i = 0; i < 4; i++) {
            idleFrames[i] = grid[28][i];
        }
        idleAnim = new Animation<>(FRAME_TIME_IDLE, idleFrames);
        idleAnim.setPlayMode(Animation.PlayMode.LOOP);

        // Tir / visée vers la Droite (Ligne 7, colonnes 0 à 2)
        TextureRegion[] shootRight = new TextureRegion[3];
        for (int i = 0; i < 3; i++) {
            shootRight[i] = grid[7][i];
        }
        shootAnims.put(Direction.RIGHT, new Animation<>(FRAME_TIME_SHOOT, shootRight));

        // Tir / visée vers la Gauche (Ligne 7 inversée horizontalement)
        TextureRegion[] shootLeft = new TextureRegion[3];
        for (int i = 0; i < 3; i++) {
            shootLeft[i] = new TextureRegion(grid[7][i]);
            shootLeft[i].flip(true, false);
        }
        shootAnims.put(Direction.LEFT, new Animation<>(FRAME_TIME_SHOOT, shootLeft));

        // Tir / visée vers le Bas (Ligne 8, colonnes 0 à 2)
        TextureRegion[] shootDown = new TextureRegion[3];
        for (int i = 0; i < 3; i++) {
            shootDown[i] = grid[8][i];
        }
        shootAnims.put(Direction.DOWN, new Animation<>(FRAME_TIME_SHOOT, shootDown));

        // Tir / visée vers le Haut (Ligne 9, colonnes 0 à 2)
        TextureRegion[] shootUp = new TextureRegion[3];
        for (int i = 0; i < 3; i++) {
            shootUp[i] = grid[9][i];
        }
        shootAnims.put(Direction.UP, new Animation<>(FRAME_TIME_SHOOT, shootUp));

        // Mort / destruction en cristaux (Ligne 3, colonnes 0 à 4)
        TextureRegion[] deathFrames = new TextureRegion[5];
        for (int i = 0; i < 5; i++) {
            deathFrames[i] = grid[3][i];
        }
        deathAnim = new Animation<>(FRAME_TIME_DEATH, deathFrames);
        deathAnim.setPlayMode(Animation.PlayMode.NORMAL);
    }

    /** À appeler dans Main.dispose(). */
    public static void disposeAssets() {
        if (sheetTexture != null) {
            sheetTexture.dispose();
            sheetTexture = null;
        }
        shootAnims.clear();
        idleAnim = null;
        deathAnim = null;
    }

    private Direction getAimDirection() {
        if (Math.abs(aim.x) > Math.abs(aim.y)) {
            return aim.x > 0 ? Direction.RIGHT : Direction.LEFT;
        } else {
            return aim.y > 0 ? Direction.UP : Direction.DOWN;
        }
    }

    public void update(float delta, Player player, TiledMapTileLayer[] walls, List<Bullet> bullets) {
        stateTime += delta;

        if (!alive) {
            deathTime += delta;
            winding = false;
            shootTimer = 0f;
            return;
        }

        if (shootTimer > 0f) {
            shootTimer -= delta;
        }

        if (!player.isAlive()) {
            winding = false;
            return;
        }
        cooldown = Math.max(0f, cooldown - delta);

        // Cible : le centre du corps du joueur
        Rectangle pb = player.getBounds();
        Vector2 target = new Vector2(pb.x + pb.width / 2f, pb.y + pb.height / 2f);

        boolean sees = position.dst(target) <= DETECTION_RANGE
                && Collision.hasLineOfSight(walls, position, target);

        if (!sees) {
            winding = false;
            return;
        }

        aim.set(target).sub(position).nor();

        if (winding) {
            windupTimer -= delta;
            if (windupTimer <= 0f) {
                bullets.add(new Bullet(position, aim));   // tir !
                winding = false;
                shootTimer = SHOOT_EFFECT_TIME;
                cooldown = FIRE_INTERVAL;
            }
        } else if (cooldown <= 0f) {
            winding = true;
            windupTimer = WINDUP;
        }
    }

    public void draw(SpriteBatch batch) {
        if (!alive) {
            if (deathAnim != null && !deathAnim.isAnimationFinished(deathTime)) {
                TextureRegion frame = deathAnim.getKeyFrame(deathTime, false);
                batch.draw(frame, position.x - FRAME_SIZE / 2f, position.y - FRAME_SIZE / 2f);
            }
            return;
        }

        Direction dir = getAimDirection();
        TextureRegion frame;

        if (shootTimer > 0f) {
            Animation<TextureRegion> anim = shootAnims.get(dir);
            frame = anim != null ? anim.getKeyFrames()[1] : idleAnim.getKeyFrame(stateTime);
        } else if (winding) {
            Animation<TextureRegion> anim = shootAnims.get(dir);
            frame = anim != null ? anim.getKeyFrames()[0] : idleAnim.getKeyFrame(stateTime);
            batch.setColor(1f, 0.7f, 0.7f, 1f);
        } else {
            if (dir == Direction.DOWN && idleAnim != null) {
                frame = idleAnim.getKeyFrame(stateTime);
            } else {
                Animation<TextureRegion> anim = shootAnims.get(dir);
                frame = anim != null ? anim.getKeyFrames()[0] : (idleAnim != null ? idleAnim.getKeyFrame(stateTime) : null);
            }
        }

        if (frame != null) {
            batch.draw(frame, position.x - FRAME_SIZE / 2f, position.y - FRAME_SIZE / 2f);
        }
        batch.setColor(1f, 1f, 1f, 1f);
    }

    public Rectangle getBounds() {
        bounds.set(position.x - WIDTH / 2f, position.y - HEIGHT / 2f, WIDTH, HEIGHT);
        return bounds;
    }

    public boolean isAlive() { return alive; }

    public void takeDamage() {
        if (!alive) return;
        alive = false;
        deathTime = 0f;
    }
}