package fr.epitech.katanagame;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.Viewport;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import fr.epitech.katanagame.Enemy;

public class Player {
    public enum Direction {
        UP, DOWN, LEFT, RIGHT
    }

    private static final float TILE_SIZE = 16f;

    // Hitbox du corps entier (dégâts des ennemis)
    private static final float HITBOX_W = 20f;
    private static final float HITBOX_H = 28f;
    private final Rectangle bounds = new Rectangle(0, 0, HITBOX_W, HITBOX_H);

    // Hitbox des pieds (collisions avec les murs)
    private static final float FEET_W = 12f;
    private static final float FEET_H = 8f;
    private static final float FEET_OFFSET_Y = 16f; // distance centre du sprite -> pieds (à ajuster)
    private final Rectangle feet = new Rectangle(0, 0, FEET_W, FEET_H);

    private static final float DASH_SPEED = 700f;
    private static final float DASH_DURATION = 0.15f;
    private static final float DASH_COOLDOWN = 0.6f;
    private boolean alive = true;

    private final Vector2 dashDir = new Vector2();
    private float dashTimer = 0f; // > 0 : dash en cours
    private float dashCooldown = 0f; // > 0 : dash pas encore réutilisable

    private static final float SPEED = 200f;
    private static final int FRAME_W = 96; // À ADAPTER
    private static final int FRAME_H = 80; // À ADAPTER
    private static final float FRAME_TIME = 0.1f;

    // ATTACK
    private static final float ATTACK_DURATION = 0.56f;
    private float attackTimer = 0f;
    private final Rectangle attackBounds = new Rectangle();
    private final Map<Direction, Animation<TextureRegion>> attack = new EnumMap<>(Direction.class);

    private final Vector2 position = new Vector2(320, 180);
    private final Vector2 aim = new Vector2(1, 0); // direction vers la souris (servira au katana)

    private final Map<Direction, Animation<TextureRegion>> idle = new EnumMap<>(Direction.class);
    private final Map<Direction, Animation<TextureRegion>> run = new EnumMap<>(Direction.class);
    private final List<Texture> textures = new ArrayList<>();

    private Direction facing = Direction.DOWN;
    private boolean moving = false;
    private float stateTime = 0f;

    private Direction dashFacing = Direction.DOWN;

    public Enemy mechant;

    public Player() {
        for (Direction d : Direction.values()) {
            String name = d.name().toLowerCase();
            idle.put(d, load("Sprites/IDLE/idle_" + name + ".png"));
            run.put(d, load("Sprites/RUN/run_" + name + ".png"));
            // ATTACK animation
            Animation<TextureRegion> attackAnimation = load("Sprites/ATTACK 1/attack1_" + name + ".png");
            attackAnimation.setPlayMode(Animation.PlayMode.NORMAL);
            attack.put(d, attackAnimation);
        }
    }

    public void respawn(float x, float y) {
        position.set(x, y);
        alive = true;
        dashTimer = 0f;
        dashCooldown = 0f;
    }

    private Animation<TextureRegion> load(String path) {
        Texture texture = new Texture(path);
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest); // pixel art net
        textures.add(texture);
        TextureRegion[][] grid = TextureRegion.split(texture, FRAME_W, FRAME_H);
        Animation<TextureRegion> anim = new Animation<>(FRAME_TIME, grid[0]); // première ligne
        anim.setPlayMode(Animation.PlayMode.LOOP);
        return anim;
    }

    public void update(float delta, Viewport viewport, TiledMapTileLayer[] solids) {
        dashTimer = Math.max(0f, dashTimer - delta);
        dashCooldown = Math.max(0f, dashCooldown - delta);

        // Direction voulue (Z/W = haut, Q/A = gauche, à cause de l'AZERTY)
        Vector2 dir = new Vector2();
        if (Gdx.input.isKeyPressed(Input.Keys.Z) || Gdx.input.isKeyPressed(Input.Keys.W))
            dir.y += 1;
        if (Gdx.input.isKeyPressed(Input.Keys.S))
            dir.y -= 1;
        if (Gdx.input.isKeyPressed(Input.Keys.Q) || Gdx.input.isKeyPressed(Input.Keys.A))
            dir.x -= 1;
        if (Gdx.input.isKeyPressed(Input.Keys.D))
            dir.x += 1;
        moving = !dir.isZero();
        if (moving)
            dir.nor();

        // Visée : le perso regarde la souris
        Vector2 mouse = viewport.unproject(new Vector2(Gdx.input.getX(), Gdx.input.getY()));
        aim.set(mouse).sub(position).nor();
        if (Math.abs(aim.x) > Math.abs(aim.y)) {
            facing = aim.x > 0 ? Direction.RIGHT : Direction.LEFT;
        } else {
            facing = aim.y > 0 ? Direction.UP : Direction.DOWN;
        }

        // Lancer le dash
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) && dashCooldown <= 0f && !isDashing()) {
            dashDir.set(moving ? dir : aim);
            dashFacing = directionOf(dashDir);
            dashTimer = DASH_DURATION;
            dashCooldown = DASH_COOLDOWN;
        }

        // Mouvement : le dash prend le dessus sur la marche
        if (isDashing()) {
            move(dashDir.x * DASH_SPEED * delta, dashDir.y * DASH_SPEED * delta, solids);
        } else if (moving) {
            move(dir.x * SPEED * delta, dir.y * SPEED * delta, solids);
        }

        attackTimer = Math.max(0f, attackTimer - delta);
        stateTime += delta;
    }

    /**
     * Déplace le joueur par petits pas, axe par axe (pour glisser le long des
     * murs).
     */
    private void move(float dx, float dy, TiledMapTileLayer[] solids) {
        int steps = (int) Math.ceil(Math.max(Math.abs(dx), Math.abs(dy)) / 4f); // pas de 4 px max
        if (steps < 1)
            return;
        float sx = dx / steps, sy = dy / steps;

        for (int i = 0; i < steps; i++) {
            position.x += sx;
            if (collides(solids))
                position.x -= sx;
            position.y += sy;
            if (collides(solids))
                position.y -= sy;
        }
    }

    /**
     * Vrai si les pieds touchent une case non vide d'une couche solide (ou sortent
     * de la map).
     */
    private boolean collides(TiledMapTileLayer[] solids) {
        return Collision.overlapsSolid(solids, getFeetBounds());
    }

    private Direction directionOf(Vector2 v) {
        if (Math.abs(v.x) > Math.abs(v.y))
            return v.x > 0 ? Direction.RIGHT : Direction.LEFT;
        return v.y > 0 ? Direction.UP : Direction.DOWN;
    }

    public boolean isDashing() {
        return dashTimer > 0f;
    }

    public boolean isInvincible() {
        return isDashing();
    } // les ennemis l'utiliseront plus tard

    public void draw(SpriteBatch batch) {
        Animation<TextureRegion> anim;
        float time = stateTime;

        if (attackTimer > 0f) {
            anim = attack.get(facing);
            time = ATTACK_DURATION - attackTimer;
        } else if (isDashing()) {
            anim = run.get(dashFacing);
            time = stateTime * 2f;
            batch.setColor(1f, 1f, 1f, 0.5f);
        } else {
            anim = moving ? run.get(facing) : idle.get(facing);
            time = stateTime;
        }

        batch.draw(anim.getKeyFrame(time), position.x - FRAME_W / 2f, position.y - FRAME_H / 2f);
        batch.setColor(1f, 1f, 1f, 1f);
    }

    public Vector2 getAim() {
        return aim;
    }

    public Vector2 getPosition() {
        return position;
    }

    public void dispose() {
        for (Texture t : textures)
            t.dispose();
    }

    public boolean isAlive() {
        return alive;
    }

    public void takeDamage() {
        if (isInvincible() || !alive)
            return; // le dash annule le coup
        alive = false; // un coup = mort, comme Katana Zero
    }

    /** Hitbox du corps entier (dégâts). */
    public Rectangle getBounds() {
        bounds.setPosition(position.x - HITBOX_W / 2f, position.y - HITBOX_H / 2f);
        return bounds;
    }

    /** Hitbox des pieds (collisions avec les murs). */
    public Rectangle getFeetBounds() {
        feet.setPosition(position.x - FEET_W / 2f, position.y - FEET_OFFSET_Y - FEET_H / 2f);
        return feet;
    }

    public void attack(Enemy enemy) {
        if (enemy == null || !enemy.isAlive()) {
            return;
        }

        attackTimer = ATTACK_DURATION;

        attackBounds.set(
                position.x + aim.x * 20f - 12f,
                position.y + aim.y * 20f - 12f,
                30f,
                24f);

        if (attackBounds.overlaps(enemy.getBounds())) {
            enemy.takeDamage();
        }
    }

    /** Hitbox de l'attaque, visible uniquement pendant son animation. */
    public Rectangle getAttackBounds() {
        return attackTimer > 0f ? attackBounds : null;
    }
}