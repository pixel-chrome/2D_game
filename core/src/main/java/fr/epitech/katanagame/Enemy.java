package fr.epitech.katanagame;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Enemy {
    // --- Comportement ---
    private static final float WIDTH = 14f;             // hitbox (à ajuster à la taille du corps)
    private static final float HEIGHT = 24f;
    private static final float DETECTION_RANGE = 180f;  // distance de vue (px)
    private static final float WINDUP = 0.4f;           // temps de visée avant le tir
    private static final float FIRE_INTERVAL = 1.5f;    // délai minimum entre deux tirs

    // --- Sprites ---
    private static final String PATH = "enemy/Free8DirRobot/IdleAim/LowPolyManny_Blue_rig_IdleAim_dir";  // + numéro + ".png"
    private static final int GRID_COLS = 5;             // grille 5 x 5
    private static final int GRID_ROWS = 5;
    private static final int FRAME_COUNT = 24;          // la 25e case est vide
    private static final float FRAME_TIME = 0.08f;
    private static final float DRAW_SCALE = 0.4f;       // 256 px * 0.3 = ~77 px à l'écran (à ajuster)
    private static final float FOOT_OFFSET_Y = -10f;      // décalage vertical du sprite (à ajuster)

    // Quel fichier (1 à 8) afficher pour chaque secteur d'angle.
    // Indices : 0=Est, 1=Nord-Est, 2=Nord, 3=Nord-Ouest, 4=Ouest, 5=Sud-Ouest, 6=Sud, 7=Sud-Est
    private static final int[] FILE_FOR_SECTOR = {6, 5, 4, 3, 2, 1, 8, 7};

    private static final Map<Integer, Animation<TextureRegion>> idleAnims = new HashMap<>();
    private static final List<Texture> textures = new ArrayList<>();

    private final Vector2 position = new Vector2();
    private final Vector2 aim = new Vector2(0, -1);
    private final Rectangle bounds = new Rectangle();

    private boolean alive = true;
    private boolean winding = false;    // en train de viser
    private float windupTimer = 0f;
    private float cooldown = 0f;
    private float stateTime = 0f;

    public Enemy(float x, float y) {
        position.set(x, y);
    }

    /** À appeler une fois dans Main.create(), avant de créer les ennemis. */
    public static void loadAssets() {
        for (int i = 1; i <= 8; i++) {
            Texture t = new Texture(PATH + i + ".png");
            t.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            textures.add(t);

            int frameW = t.getWidth() / GRID_COLS;
            int frameH = t.getHeight() / GRID_ROWS;
            TextureRegion[][] grid = TextureRegion.split(t, frameW, frameH);

            // on lit la grille ligne par ligne, et on s'arrête à FRAME_COUNT
            Array<TextureRegion> frames = new Array<>();
            for (int row = 0; row < GRID_ROWS; row++) {
                for (int col = 0; col < GRID_COLS; col++) {
                    if (frames.size < FRAME_COUNT) frames.add(grid[row][col]);
                }
            }

            Animation<TextureRegion> anim = new Animation<>(FRAME_TIME, frames);
            anim.setPlayMode(Animation.PlayMode.LOOP);
            idleAnims.put(i, anim);
        }
    }

    /** À appeler dans Main.dispose(). */
    public static void disposeAssets() {
        for (Texture t : textures) t.dispose();
        textures.clear();
        idleAnims.clear();
    }

    public void update(float delta, Player player, TiledMapTileLayer[] walls, List<Bullet> bullets) {
        stateTime += delta;

        if (!alive || !player.isAlive()) {
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
                cooldown = FIRE_INTERVAL;
            }
        } else if (cooldown <= 0f) {
            winding = true;
            windupTimer = WINDUP;
        }
    }

    public void draw(SpriteBatch batch) {
        if (!alive) return;

        // angle de visée (0° = droite, sens anti-horaire) découpé en 8 secteurs de 45°
        int sector = Math.round(aim.angleDeg() / 45f) % 8;
        Animation<TextureRegion> anim = idleAnims.get(FILE_FOR_SECTOR[sector]);
        TextureRegion frame = anim.getKeyFrame(stateTime);

        float w = frame.getRegionWidth() * DRAW_SCALE;
        float h = frame.getRegionHeight() * DRAW_SCALE;

        if (winding) batch.setColor(1f, 0.6f, 0.6f, 1f);   // teinte rouge quand il vise
        batch.draw(frame, position.x - w / 2f, position.y - h / 2f + FOOT_OFFSET_Y, w, h);
        batch.setColor(1f, 1f, 1f, 1f);
    }

    public Rectangle getBounds() {
        bounds.set(position.x - WIDTH / 2f, position.y - HEIGHT / 2f, WIDTH, HEIGHT);
        return bounds;
    }

    public boolean isAlive() { return alive; }

    public void takeDamage() { alive = false; }   // servira avec l'attaque au katana
}