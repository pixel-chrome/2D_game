package fr.epitech.katanagame;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class Enemy {
    public enum Direction { RIGHT, LEFT, DOWN, UP }

    // --- Tir ---
    private static final float LEAD_FACTOR = 0.3f;     // 0 = vise où tu es, 1 = anticipe à fond
    private static final int FAN_BULLETS = 1;          // nombre impair
    private static final float FAN_SPREAD = 14f;       // degrés entre deux balles
    private static final int VOLLEYS_PER_SALVO = 1;
    private static final float VOLLEY_INTERVAL = 0.3f;

    // --- Comportement ---
    private static final float WIDTH = 14f;             // hitbox (largeur)
    private static final float HEIGHT = 24f;            // hitbox (hauteur)
    private static final float DETECTION_RANGE = 180f;  // distance de vue (px)
    private static final float WINDUP = 0.4f;           // temps de visée avant le tir
    private static final float FIRE_INTERVAL = 1.2f;    // délai minimum entre deux tirs
    private static final float SHOOT_EFFECT_TIME = 0.24f; // temps d'affichage du tir

    // --- Cône de vision ---
    private static final float FOV = 90f;               // angle total du cône (degrés)
    private static final int CONE_RAYS = 24;            // finesse du dessin (plus = plus précis)

    // --- Sprites Pixel_Hot ---
    private static final String SPRITESHEET_PATH = "Pixel_Hot/Characters_Sprite_Sheet.png";
    private static final int FRAME_SIZE = 32;
    private static final float FRAME_TIME_IDLE = 0.15f;
    private static final float FRAME_TIME_SHOOT = 0.08f;
    private static final float FRAME_TIME_DEATH = 0.08f;

    // --- Patrouille ---
    private static final float WANDER_RADIUS = 80f;    // rayon autour du point de départ
    private static final float WALK_SPEED = 40f;
    private static final float PAUSE_MIN = 0.8f;
    private static final float PAUSE_MAX = 2f;
    private static final float FRAME_TIME_WALK = 0.12f;

    private static final float LOSE_SIGHT_TIME = 1.5f; // temps avant de renoncer

    private static Texture sheetTexture;
    private static Animation<TextureRegion> idleAnim;
    private static Animation<TextureRegion> deathAnim;
    private static final Map<Direction, Animation<TextureRegion>> shootAnims = new EnumMap<>(Direction.class);
    private static final Map<Direction, Animation<TextureRegion>> walkAnims = new EnumMap<>(Direction.class);

    private final Vector2 position = new Vector2();
    private final Vector2 aim = new Vector2(0, -1);
    private final Rectangle bounds = new Rectangle();
    private final Vector2 home = new Vector2();
    private final Vector2 wanderTarget = new Vector2();
    private final Vector2 lastKnown = new Vector2();
    private final Vector2 lastTarget = new Vector2();
    private final Vector2 playerVel = new Vector2();
    private boolean hasLastTarget = false;

    private boolean alerted = false;
    private boolean seesPlayer = false;  // il voit le joueur à cet instant (sert au "!")
    private float alertTimer = 0f;
    private boolean walking = false;
    private float pauseTimer = 0f;

    private int volleysLeft = 0;
    private float volleyTimer = 0f;

    private boolean alive = true;
    private boolean winding = false;    // en train de viser
    private float windupTimer = 0f;
    private float cooldown = 0f;
    private float shootTimer = 0f;      // flash de tir
    private float stateTime = 0f;
    private float deathTime = 0f;

    private static final float FEET_W = 10f;
    private static final float FEET_H = 6f;
    private static final float FEET_OFFSET_Y = 10f;   // centre du sprite -> pieds (à ajuster)
    private final Rectangle feet = new Rectangle();

    public Enemy(float x, float y) {
        position.set(x, y);
        home.set(x, y);   // centre de la zone de patrouille
    }
    public Rectangle getFeetBounds() {
        feet.set(position.x - FEET_W / 2f, position.y - FEET_OFFSET_Y - FEET_H / 2f, FEET_W, FEET_H);
        return feet;
    }

    /** Avance axe par axe ; renvoie false si le garde est bloqué. */
    private boolean tryMove(float dx, float dy, TiledMapTileLayer[] solids) {
        boolean blocked = false;

        position.x += dx;
        if (Collision.overlapsSolid(solids, getFeetBounds())) { position.x -= dx; blocked |= dx != 0; }

        position.y += dy;
        if (Collision.overlapsSolid(solids, getFeetBounds())) { position.y -= dy; blocked |= dy != 0; }

        return !blocked;
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

        // Tir / visée vers la Droite (Ligne 8, colonnes 0 à 2)
        TextureRegion[] shootRight = new TextureRegion[3];
        for (int i = 0; i < 3; i++) {
            shootRight[i] = grid[8][i];
        }
        shootAnims.put(Direction.RIGHT, new Animation<>(FRAME_TIME_SHOOT, shootRight));

        // Tir / visée vers la Gauche (Ligne 8 inversée horizontalement)
        TextureRegion[] shootLeft = new TextureRegion[3];
        for (int i = 0; i < 3; i++) {
            shootLeft[i] = new TextureRegion(grid[8][i]);
            shootLeft[i].flip(true, false);
        }
        shootAnims.put(Direction.LEFT, new Animation<>(FRAME_TIME_SHOOT, shootLeft));

        // Tir / visée vers le Bas (Ligne 7, colonnes 0 à 2)
        TextureRegion[] shootDown = new TextureRegion[3];
        for (int i = 0; i < 3; i++) {
            shootDown[i] = grid[7][i];
        }
        shootAnims.put(Direction.DOWN, new Animation<>(FRAME_TIME_SHOOT, shootDown));

        // Tir / visée vers le Haut (Ligne 9, colonnes 0 à 2)
        TextureRegion[] shootUp = new TextureRegion[3];
        for (int i = 0; i < 3; i++) {
            shootUp[i] = grid[9][i];
        }
        shootAnims.put(Direction.UP, new Animation<>(FRAME_TIME_SHOOT, shootUp));

        // --- Marche vers le bas : ligne 22, colonnes 0 à 7 ---
        TextureRegion[] walkDown = new TextureRegion[8];

        for (int i = 0; i < 8; i++) {
            walkDown[i] = grid[22][i];
        }

        Animation<TextureRegion> walkDownAnim =
                new Animation<>(FRAME_TIME_WALK, walkDown);

        walkDownAnim.setPlayMode(Animation.PlayMode.LOOP);

        walkAnims.put(Direction.DOWN, walkDownAnim);


        // --- Marche vers la droite : ligne 23, colonnes 0 à 7 ---
        TextureRegion[] walkRight = new TextureRegion[8];

        for (int i = 0; i < 8; i++) {
            walkRight[i] = grid[23][i];
        }

        Animation<TextureRegion> walkRightAnim =
                new Animation<>(FRAME_TIME_WALK, walkRight);

        walkRightAnim.setPlayMode(Animation.PlayMode.LOOP);

        walkAnims.put(Direction.RIGHT, walkRightAnim);


        // --- Marche vers la gauche : miroir de la marche à droite ---
        TextureRegion[] walkLeft = new TextureRegion[8];

        for (int i = 0; i < 8; i++) {
            walkLeft[i] = new TextureRegion(grid[23][i]);
            walkLeft[i].flip(true, false);
        }

        Animation<TextureRegion> walkLeftAnim =
                new Animation<>(FRAME_TIME_WALK, walkLeft);

        walkLeftAnim.setPlayMode(Animation.PlayMode.LOOP);

        walkAnims.put(Direction.LEFT, walkLeftAnim);


        // --- Marche vers le haut : ligne 24, colonnes 0 à 7 ---
        TextureRegion[] walkUp = new TextureRegion[8];

        for (int i = 0; i < 8; i++) {
            walkUp[i] = grid[24][i];
        }

        Animation<TextureRegion> walkUpAnim =
                new Animation<>(FRAME_TIME_WALK, walkUp);

        walkUpAnim.setPlayMode(Animation.PlayMode.LOOP);

        walkAnims.put(Direction.UP, walkUpAnim);

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

    public void update(float delta, Player player, TiledMapTileLayer[] sight,
                   TiledMapTileLayer[] move, List<Bullet> bullets) {
        stateTime += delta;

        if (!alive) {
            deathTime += delta;
            winding = false;
            volleysLeft = 0;
            shootTimer = 0f;
            seesPlayer = false;
            return;
        }

        if (shootTimer > 0f) shootTimer -= delta;

        if (!player.isAlive()) {
            winding = false;
            volleysLeft = 0;
            seesPlayer = false;
            return;
        }
        cooldown = Math.max(0f, cooldown - delta);

        Rectangle pb = player.getBounds();
        Vector2 target = new Vector2(pb.x + pb.width / 2f, pb.y + pb.height / 2f);

        // Vitesse du joueur, estimée d'une frame à l'autre (sert à anticiper)
        if (hasLastTarget && delta > 0f) playerVel.set(target).sub(lastTarget).scl(1f / delta);
        else playerVel.setZero();
        lastTarget.set(target);
        hasLastTarget = true;

        // Il voit le joueur seulement s'il est dans le cône, à portée, et sans mur entre eux
        Vector2 toTarget = new Vector2(target).sub(position);
        float angleDiff = Math.abs(((toTarget.angleDeg() - aim.angleDeg() + 540f) % 360f) - 180f);
        boolean inCone = angleDiff <= FOV / 2f;

        boolean sees = inCone
                && toTarget.len() <= DETECTION_RANGE
                && Collision.hasLineOfSight(sight, position, target);
        seesPlayer = sees;

        if (sees) {
            alerted = true;
            alertTimer = LOSE_SIGHT_TIME;
            lastKnown.set(target);
        } else if (alerted) {
            alertTimer -= delta;
            if (alertTimer <= 0f) {
                alerted = false;
                walking = false;
                pauseTimer = 0.5f;
            }
        }

        if (!alerted) {
            patrol(delta, move);
            return;
        }
        walking = false;   // alerté : il arrête de marcher
        // Alerté : il fixe le joueur (ou le dernier endroit où il l'a vu)
        aim.set(lastKnown).sub(position).nor();

        if (!sees) {            // pas de tir à travers les murs
            winding = false;
            volleysLeft = 0;
            return;
        }

        if (volleysLeft > 0) {
            volleyTimer -= delta;
            if (volleyTimer <= 0f) {
                fireVolley(target, bullets);
                volleysLeft--;
                volleyTimer = VOLLEY_INTERVAL;
                if (volleysLeft == 0) cooldown = FIRE_INTERVAL;
            }
        } else if (winding) {
            windupTimer -= delta;
            if (windupTimer <= 0f) {
                winding = false;
                volleysLeft = VOLLEYS_PER_SALVO;
                volleyTimer = 0f;   // la première volée part tout de suite
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
                batch.draw(frame, position.x - FRAME_SIZE / 2f,
                        position.y - FRAME_SIZE / 2f);
            }
            return;
        }

        Direction dir = getAimDirection();
        TextureRegion frame;

        if (shootTimer > 0f) {

            Animation<TextureRegion> anim = shootAnims.get(dir);

            // Jouer l'animation de tir
            frame = anim != null
                    ? anim.getKeyFrame(
                            SHOOT_EFFECT_TIME - shootTimer,
                            false
                    )
                    : idleAnim.getKeyFrame(stateTime);

        } else if (winding) {

            Animation<TextureRegion> anim = shootAnims.get(dir);

            // Première image : position de visée
            frame = anim != null
                    ? anim.getKeyFrames()[0]
                    : idleAnim.getKeyFrame(stateTime);

            batch.setColor(1f, 0.7f, 0.7f, 1f);

        } else if (walking) {

            // Animation de marche
            Animation<TextureRegion> anim = walkAnims.get(dir);

            frame = anim != null
                    ? anim.getKeyFrame(stateTime, true)
                    : idleAnim.getKeyFrame(stateTime);
        } else if (alerted) {
            // Alerté : il pointe son arme dans ta direction (côté, haut ou bas)
            Animation<TextureRegion> anim = shootAnims.get(dir);
            frame = anim != null
                    ? anim.getKeyFrames()[0]
                    : idleAnim.getKeyFrame(stateTime);
        } else {

            // Animation d'attente
            frame = idleAnim.getKeyFrame(stateTime);
        }

        if (frame != null) {
            batch.draw(
                    frame,
                    position.x - FRAME_SIZE / 2f,
                    position.y - FRAME_SIZE / 2f
            );
        }

        batch.setColor(1f, 1f, 1f, 1f);
    }

    /**
     * Dessine le cône de vision (ShapeRenderer en mode Filled, avec le blending activé).
     * Jaune = il n'a rien vu, rouge = il est alerté. Les murs coupent le cône.
     */
    public void drawVision(ShapeRenderer shapes, TiledMapTileLayer[] walls) {
        if (!alive) return;

        if (alerted) shapes.setColor(1f, 0.1f, 0.1f, 0.28f);
        else shapes.setColor(1f, 0.9f, 0.2f, 0.18f);

        float start = aim.angleDeg() - FOV / 2f;
        float step = FOV / CONE_RAYS;
        float prevX = 0f, prevY = 0f;

        for (int i = 0; i <= CONE_RAYS; i++) {
            float a = start + i * step;
            float dx = MathUtils.cosDeg(a);
            float dy = MathUtils.sinDeg(a);

            // on avance le long du rayon jusqu'à un mur ou la portée maximale
            float len = DETECTION_RANGE;
            for (float d = 4f; d <= DETECTION_RANGE; d += 4f) {
                if (Collision.isSolid(walls, position.x + dx * d, position.y + dy * d)) {
                    len = d - 2f;
                    break;
                }
            }

            float x = position.x + dx * len;
            float y = position.y + dy * len;
            if (i > 0) shapes.triangle(position.x, position.y, prevX, prevY, x, y);
            prevX = x;
            prevY = y;
        }
    }

    /** Dessine un "!" au-dessus de la tête quand il voit le joueur. */
    public void drawAlert(ShapeRenderer shapes) {
        if (!alive || !seesPlayer) return;

        float bob = Math.abs(MathUtils.sin(stateTime * 8f)) * 2f;   // petit rebond
        float x = position.x;
        float y = position.y + FRAME_SIZE / 2f + 4f + bob;

        shapes.setColor(Color.BLACK);                  // contour
        shapes.rect(x - 3f, y - 1f, 6f, 16f);
        shapes.setColor(1f, 0.2f, 0.2f, 1f);           // le trait du "!"
        shapes.rect(x - 2f, y + 5f, 4f, 9f);
        shapes.rect(x - 2f, y, 4f, 4f);                // le point
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

    private void fireVolley(Vector2 target, List<Bullet> bullets) {
        float dist = position.dst(target);
        float leadTime = Math.min(dist / Bullet.SPEED, 0.6f) * LEAD_FACTOR;
        Vector2 predicted = new Vector2(target).mulAdd(playerVel, leadTime);
        float baseAngle = new Vector2(predicted).sub(position).angleDeg();

        int half = FAN_BULLETS / 2;
        for (int i = -half; i <= half; i++) {
            Vector2 dir = new Vector2(1, 0).setAngleDeg(baseAngle + i * FAN_SPREAD);
            bullets.add(new Bullet(new Vector2(position), dir));
        }
        shootTimer = SHOOT_EFFECT_TIME;
    }

    private void patrol(float delta, TiledMapTileLayer[] walls) {
        winding = false;
        volleysLeft = 0;

        if (walking) {
            Vector2 toTarget = new Vector2(wanderTarget).sub(position);
            float dist = toTarget.len();
            if (dist < 2f) {
                walking = false;
                pauseTimer = MathUtils.random(PAUSE_MIN, PAUSE_MAX);
            } else {
                toTarget.nor();
                aim.set(toTarget);
                Vector2 step = new Vector2(toTarget).scl(Math.min(dist, WALK_SPEED * delta));
                if (!tryMove(step.x, step.y, walls)) {     // "walls" = le tableau move reçu en paramètre
                    walking = false;                       // bloqué : il s'arrête et fait une pause
                    pauseTimer = MathUtils.random(PAUSE_MIN, PAUSE_MAX);
                }
            }
        } else {
            pauseTimer -= delta;
            if (pauseTimer <= 0f) pickWanderTarget(walls);
        }
    }

    private void pickWanderTarget(TiledMapTileLayer[] walls) {
        for (int attempt = 0; attempt < 8; attempt++) {
            float angle = MathUtils.random(360f);
            float dist = MathUtils.random(20f, WANDER_RADIUS);
            Vector2 candidate = new Vector2(home).add(
                    MathUtils.cosDeg(angle) * dist, MathUtils.sinDeg(angle) * dist);
            if (Collision.hasLineOfSight(walls, position, candidate)) {   // pas à travers un mur
                wanderTarget.set(candidate);
                walking = true;
                return;
            }
        }
        pauseTimer = 0.5f;   // aucun point valide, on réessaie bientôt
    }
}