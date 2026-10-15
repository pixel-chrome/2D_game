package fr.epitech.katanagame.entity.enemy;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import fr.epitech.katanagame.entity.Direction;

import java.util.EnumMap;
import java.util.Map;

/** Ressources et rendu des animations partagées par les ennemis. */
final class EnemyAnimations {
    private static final String SPRITESHEET_PATH = "Pixel_Hot/Characters_Sprite_Sheet.png";
    static final int FRAME_SIZE = 32;
    private static final float FRAME_TIME_IDLE = 0.15f, FRAME_TIME_SHOOT = 0.08f;
    private static final float FRAME_TIME_DEATH = 0.08f, FRAME_TIME_WALK = 0.12f;
    private static Texture sheetTexture;
    private static Animation<TextureRegion> idleAnim, deathAnim;
    private static final Map<Direction, Animation<TextureRegion>> shootAnims = new EnumMap<>(Direction.class);
    private static final Map<Direction, Animation<TextureRegion>> walkAnims = new EnumMap<>(Direction.class);

    /** Empêche l’instanciation de cette classe utilitaire. */
    private EnemyAnimations() {}

    /** Charge les spritesheets et prépare les animations partagées. */
    static void loadAssets() {
        if (sheetTexture != null) return;
        sheetTexture = new Texture(SPRITESHEET_PATH);
        sheetTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        TextureRegion[][] grid = TextureRegion.split(sheetTexture, FRAME_SIZE, FRAME_SIZE);
        idleAnim = animation(FRAME_TIME_IDLE, frames(grid, 28, 4, false), Animation.PlayMode.LOOP);
        deathAnim = animation(FRAME_TIME_DEATH, frames(grid, 3, 5, false), Animation.PlayMode.NORMAL);
        shootAnims.put(Direction.RIGHT, animation(FRAME_TIME_SHOOT, frames(grid, 8, 3, false), Animation.PlayMode.NORMAL));
        shootAnims.put(Direction.LEFT, animation(FRAME_TIME_SHOOT, frames(grid, 8, 3, true), Animation.PlayMode.NORMAL));
        shootAnims.put(Direction.DOWN, animation(FRAME_TIME_SHOOT, frames(grid, 7, 3, false), Animation.PlayMode.NORMAL));
        shootAnims.put(Direction.UP, animation(FRAME_TIME_SHOOT, frames(grid, 9, 3, false), Animation.PlayMode.NORMAL));
        walkAnims.put(Direction.DOWN, animation(FRAME_TIME_WALK, frames(grid, 22, 8, false), Animation.PlayMode.LOOP));
        walkAnims.put(Direction.RIGHT, animation(FRAME_TIME_WALK, frames(grid, 23, 8, false), Animation.PlayMode.LOOP));
        walkAnims.put(Direction.LEFT, animation(FRAME_TIME_WALK, frames(grid, 23, 8, true), Animation.PlayMode.LOOP));
        walkAnims.put(Direction.UP, animation(FRAME_TIME_WALK, frames(grid, 24, 8, false), Animation.PlayMode.LOOP));
    }

    /** Extrait une séquence de frames depuis une ligne du spritesheet. */
    private static TextureRegion[] frames(TextureRegion[][] grid, int row, int count, boolean flipX) {
        TextureRegion[] result = new TextureRegion[count];
        for (int i = 0; i < count; i++) {
            result[i] = new TextureRegion(grid[row][i]);
            if (flipX) result[i].flip(true, false);
        }
        return result;
    }
    /** Construit une animation à partir de ses frames. */
    private static Animation<TextureRegion> animation(float time, TextureRegion[] frames, Animation.PlayMode mode) {
        Animation<TextureRegion> result = new Animation<>(time, frames);
        result.setPlayMode(mode);
        return result;
    }
    /** Libère les textures et animations partagées. */
    static void disposeAssets() {
        if (sheetTexture != null) { sheetTexture.dispose(); sheetTexture = null; }
        shootAnims.clear(); walkAnims.clear(); idleAnim = null; deathAnim = null;
    }
    /** Choisit puis dessine l’animation adaptée à l’état de l’ennemi. */
    static void draw(Enemy enemy, SpriteBatch batch) {
        if (!enemy.isAlive()) {
            if (deathAnim != null && !deathAnim.isAnimationFinished(enemy.deathTime()))
                /** Dessine une frame à la position de l’ennemi. */
                drawFrame(enemy, batch, deathAnim.getKeyFrame(enemy.deathTime(), false));
            return;
        }
        Direction dir = enemy.getFacing();
        Animation<TextureRegion> shoot = shootAnims.get(dir);
        TextureRegion frame;
        if (enemy.shootTimer() > 0f) {
            frame = shoot != null ? shoot.getKeyFrame(0.24f - enemy.shootTimer(), false) : idleAnim.getKeyFrame(enemy.stateTime());
        } else if (enemy.isWinding()) {
            frame = shoot != null ? shoot.getKeyFrames()[0] : idleAnim.getKeyFrame(enemy.stateTime());
            batch.setColor(1f, 0.7f, 0.7f, 1f);
        } else if (enemy.isWalking()) {
            Animation<TextureRegion> walk = walkAnims.get(dir);
            frame = walk != null ? walk.getKeyFrame(enemy.stateTime(), true) : idleAnim.getKeyFrame(enemy.stateTime());
        } else if (enemy.isAlerted()) {
            frame = shoot != null ? shoot.getKeyFrames()[0] : idleAnim.getKeyFrame(enemy.stateTime());
        } else frame = idleAnim.getKeyFrame(enemy.stateTime());
        /** Dessine une frame à la position de l’ennemi. */
        drawFrame(enemy, batch, frame);
        batch.setColor(1f, 1f, 1f, 1f);
    }
    /** Dessine une frame à la position de l’ennemi. */
    private static void drawFrame(Enemy enemy, SpriteBatch batch, TextureRegion frame) {
        batch.draw(frame, enemy.getPosition().x - FRAME_SIZE / 2f, enemy.getPosition().y - FRAME_SIZE / 2f);
    }
}
