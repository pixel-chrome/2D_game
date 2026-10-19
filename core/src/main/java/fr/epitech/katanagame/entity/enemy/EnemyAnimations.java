package fr.epitech.katanagame.entity.enemy;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import fr.epitech.katanagame.entity.Direction;

import fr.epitech.katanagame.render.ShadowRenderer;

import java.util.EnumMap;
import java.util.Map;

final class EnemyAnimations {

    private static final String SPRITESHEET_PATH =
            "Pixel_Hot/Characters_Sprite_Sheet.png";

    /** Idle dans les 4 directions : ligne 0 face, 1 côté droit, 2 dos. */
    private static final String IDLE_SHEET_PATH = "Pixel_Hot/Enemy_Idle.png";

    static final int FRAME_SIZE = 32;

    private static final float FRAME_TIME_IDLE = 0.15f;
    private static final float FRAME_TIME_SHOOT = 0.08f;
    private static final float FRAME_TIME_DEATH = 0.08f;
    private static final float FRAME_TIME_WALK = 0.12f;
    private static final float FRAME_TIME_RUN = 0.06f;

    private static Texture sheetTexture;

    private static Texture idleTexture;

    private static Animation<TextureRegion> deathAnim;

    private static final Map<Direction, Animation<TextureRegion>> idleAnims =
            new EnumMap<>(Direction.class);

    private static final Map<Direction, Animation<TextureRegion>> shootAnims =
            new EnumMap<>(Direction.class);

    private static final Map<Direction, Animation<TextureRegion>> walkAnims =
            new EnumMap<>(Direction.class);

    private static final Map<Direction, Animation<TextureRegion>> runAnims =
            new EnumMap<>(Direction.class);

    private EnemyAnimations() {
    }

    static void loadAssets() {
        if (sheetTexture != null) {
            return;
        }

        sheetTexture = new Texture(SPRITESHEET_PATH);

        sheetTexture.setFilter(
                Texture.TextureFilter.Nearest,
                Texture.TextureFilter.Nearest
        );

        TextureRegion[][] grid = TextureRegion.split(
                sheetTexture,
                FRAME_SIZE,
                FRAME_SIZE
        );

        idleTexture = new Texture(IDLE_SHEET_PATH);
        idleTexture.setFilter(
                Texture.TextureFilter.Nearest,
                Texture.TextureFilter.Nearest
        );
        TextureRegion[][] idleGrid = TextureRegion.split(
                idleTexture,
                FRAME_SIZE,
                FRAME_SIZE
        );
        idleAnims.put(Direction.DOWN, animation(
                FRAME_TIME_IDLE, frames(idleGrid, 0, 4, false), Animation.PlayMode.LOOP));
        idleAnims.put(Direction.RIGHT, animation(
                FRAME_TIME_IDLE, frames(idleGrid, 1, 4, false), Animation.PlayMode.LOOP));
        idleAnims.put(Direction.LEFT, animation(
                FRAME_TIME_IDLE, frames(idleGrid, 1, 4, true), Animation.PlayMode.LOOP));
        idleAnims.put(Direction.UP, animation(
                FRAME_TIME_IDLE, frames(idleGrid, 2, 4, false), Animation.PlayMode.LOOP));

        deathAnim = animation(
                FRAME_TIME_DEATH,
                frames(grid, 3, 5, false),
                Animation.PlayMode.NORMAL
        );

        // Animations de tir
        shootAnims.put(
                Direction.DOWN,
                animation(
                        FRAME_TIME_SHOOT,
                        frames(grid, 7, 3, false),
                        Animation.PlayMode.NORMAL
                )
        );

        shootAnims.put(
                Direction.RIGHT,
                animation(
                        FRAME_TIME_SHOOT,
                        frames(grid, 8, 3, false),
                        Animation.PlayMode.NORMAL
                )
        );

        shootAnims.put(
                Direction.LEFT,
                animation(
                        FRAME_TIME_SHOOT,
                        frames(grid, 8, 3, true),
                        Animation.PlayMode.NORMAL
                )
        );

        shootAnims.put(
                Direction.UP,
                animation(
                        FRAME_TIME_SHOOT,
                        frames(grid, 9, 3, false),
                        Animation.PlayMode.NORMAL
                )
        );

        // Animations de déplacement
        walkAnims.put(
                Direction.DOWN,
                animation(
                        FRAME_TIME_WALK,
                        frames(grid, 22, 8, false),
                        Animation.PlayMode.LOOP
                )
        );

        walkAnims.put(
                Direction.RIGHT,
                animation(
                        FRAME_TIME_WALK,
                        frames(grid, 23, 8, false),
                        Animation.PlayMode.LOOP
                )
        );

        walkAnims.put(
                Direction.LEFT,
                animation(
                        FRAME_TIME_WALK,
                        frames(grid, 23, 8, true),
                        Animation.PlayMode.LOOP
                )
        );

        walkAnims.put(
                Direction.UP,
                animation(
                        FRAME_TIME_WALK,
                        frames(grid, 24, 8, false),
                        Animation.PlayMode.LOOP
                )
        );

        // Course : mêmes images que la marche, jouées deux fois plus vite
        for (Map.Entry<Direction, Animation<TextureRegion>> e : walkAnims.entrySet()) {
            runAnims.put(e.getKey(), animation(
                    FRAME_TIME_RUN, e.getValue().getKeyFrames(), Animation.PlayMode.LOOP));
        }
    }
    private static TextureRegion[] frames(
            TextureRegion[][] grid,
            int row,
            int count,
            boolean flipX
    ) {
        TextureRegion[] result = new TextureRegion[count];

        for (int i = 0; i < count; i++) {
            TextureRegion region = new TextureRegion(grid[row][i]);

            if (flipX) {
                region.flip(true, false);
            }

            result[i] = region;
        }

        return result;
    }

    private static Animation<TextureRegion> animation(
            float frameTime,
            TextureRegion[] frames,
            Animation.PlayMode playMode
    ) {
        Animation<TextureRegion> animation =
                new Animation<>(frameTime, frames);

        animation.setPlayMode(playMode);

        return animation;
    }

    static void draw(Enemy enemy, SpriteBatch batch) {
        if (idleAnims.isEmpty()) {
            return;
        }

        if (!enemy.isAlive()) {
            if (deathAnim != null
                    && !deathAnim.isAnimationFinished(enemy.deathTime())) {

                TextureRegion frame =
                        deathAnim.getKeyFrame(enemy.deathTime(), false);

                drawFrame(enemy, batch, frame, 1f, 1f, 1f, 1f);
            }

            return;
        }

        Direction direction = enemy.getFacing();

        Animation<TextureRegion> shoot = shootAnims.get(direction);
        Animation<TextureRegion> idleAnim = idleAnims.get(direction);

        TextureRegion frame;
        float red = 1f;
        float green = 1f;
        float blue = 1f;

        if (enemy.shootTimer() > 0f) {
            frame = shoot != null
                    ? shoot.getKeyFrame(
                            0.24f - enemy.shootTimer(),
                            false
                    )
                    : idleAnim.getKeyFrame(enemy.stateTime());

        } else if (enemy.isWinding()) {
            frame = shoot != null
                    ? shoot.getKeyFrames()[0]
                    : idleAnim.getKeyFrame(enemy.stateTime());

            // Teinte rouge pendant la préparation du tir.
            red = 1f;
            green = 0.7f;
            blue = 0.7f;

        } else if (enemy.isWalking()) {
            Animation<TextureRegion> walk = (enemy.isRunning() ? runAnims : walkAnims).get(direction);

            frame = walk != null
                    ? walk.getKeyFrame(enemy.stateTime(), true)
                    : idleAnim.getKeyFrame(enemy.stateTime());

        } else if (enemy.isAlerted()) {
            frame = shoot != null
                    ? shoot.getKeyFrames()[0]
                    : idleAnim.getKeyFrame(enemy.stateTime());

        } else {
            frame = idleAnim.getKeyFrame(enemy.stateTime());
        }

        drawFrame(enemy, batch, frame, red, green, blue, 1f);
    }

    private static void drawFrame(
            Enemy enemy,
            SpriteBatch batch,
            TextureRegion frame,
            float red,
            float green,
            float blue,
            float alpha
    ) {
        float x = enemy.getPosition().x - FRAME_SIZE / 2f;
        float y = enemy.getPosition().y - FRAME_SIZE / 2f;

        float scaleX = enemy.firingScaleX();
        float scaleY = enemy.firingScaleY();

        // Ligne des pieds : centre de la hitbox des pieds (monde).
        Rectangle feet = enemy.getFeetBounds();
        float footY = feet.y + feet.height / 2f;

        // Ombre noire de la silhouette ennemie, accrochée aux pieds.
        ShadowRenderer.draw(
                batch,
                frame,
                x,
                y,
                FRAME_SIZE,
                FRAME_SIZE,
                footY
        );

        // Sprite ennemi : on conserve sa déformation et sa couleur.
        batch.setColor(red, green, blue, alpha);

        batch.draw(
                frame,
                x,
                y,
                FRAME_SIZE / 2f,
                FRAME_SIZE / 2f,
                FRAME_SIZE,
                FRAME_SIZE,
                scaleX,
                scaleY,
                0f
        );

        batch.setColor(1f, 1f, 1f, 1f);
    }

    static void disposeAssets() {
        if (sheetTexture != null) {
            sheetTexture.dispose();
            sheetTexture = null;
        }

        if (idleTexture != null) {
            idleTexture.dispose();
            idleTexture = null;
        }

        shootAnims.clear();
        walkAnims.clear();
        runAnims.clear();
        idleAnims.clear();

        deathAnim = null;
    }
}