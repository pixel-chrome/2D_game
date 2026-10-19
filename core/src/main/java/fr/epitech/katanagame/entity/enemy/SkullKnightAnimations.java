package fr.epitech.katanagame.entity.enemy;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import fr.epitech.katanagame.entity.Direction;

import java.util.EnumMap;
import java.util.Map;

/**
 * Sprites du Skull Knight (assets/enemy/SkullKnight/skull knight.png).
 * Lignes de la feuille : course bas/gauche/haut, idle bas/gauche/haut, coup bas/haut/gauche.
 * La droite est la gauche retournée.
 */
final class SkullKnightAnimations {
    private static final String SHEET_PATH = "enemy/SkullKnight/skull knight.png";
    static final int FRAME_W = 65, FRAME_H = 64;
    private static final int RUN_FRAMES = 6, IDLE_FRAMES = 6, SLASH_FRAMES = 5;

    /** Agrandissement du sprite, identique dans tous les états (l'arc du coup est petit dans la feuille). */
    static final float SCALE = 2f;
    static final float SLASH_FRAME_TIME = 0.07f;
    private static final float RUN_FRAME_TIME = 0.07f, IDLE_FRAME_TIME = 0.16f;

    private static Texture sheet;
    private static final Map<Direction, Animation<TextureRegion>> run = new EnumMap<>(Direction.class);
    private static final Map<Direction, Animation<TextureRegion>> idle = new EnumMap<>(Direction.class);
    private static final Map<Direction, Animation<TextureRegion>> slash = new EnumMap<>(Direction.class);

    private SkullKnightAnimations() {}

    /** Charge la feuille de sprites (une seule fois). */
    static void loadAssets() {
        if (sheet != null) return;
        sheet = new Texture(SHEET_PATH);
        sheet.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        TextureRegion[][] grid = TextureRegion.split(sheet, FRAME_W, FRAME_H);

        fill(run, grid, 0, 1, 2, RUN_FRAMES, RUN_FRAME_TIME, Animation.PlayMode.LOOP);
        fill(idle, grid, 3, 4, 5, IDLE_FRAMES, IDLE_FRAME_TIME, Animation.PlayMode.LOOP);
        // Coups : bas = ligne 6, haut = ligne 7, gauche = ligne 8
        fill(slash, grid, 6, 8, 7, SLASH_FRAMES, SLASH_FRAME_TIME, Animation.PlayMode.NORMAL);
    }

    /** Remplit une animation pour les 4 directions à partir des lignes bas / gauche / haut. */
    private static void fill(Map<Direction, Animation<TextureRegion>> target, TextureRegion[][] grid,
                             int downRow, int leftRow, int upRow, int count,
                             float frameTime, Animation.PlayMode mode) {
        target.put(Direction.DOWN, animation(grid[downRow], count, frameTime, mode, false));
        target.put(Direction.LEFT, animation(grid[leftRow], count, frameTime, mode, false));
        target.put(Direction.RIGHT, animation(grid[leftRow], count, frameTime, mode, true));
        target.put(Direction.UP, animation(grid[upRow], count, frameTime, mode, false));
    }

    private static Animation<TextureRegion> animation(TextureRegion[] row, int count, float frameTime,
                                                      Animation.PlayMode mode, boolean flip) {
        TextureRegion[] frames = new TextureRegion[count];
        for (int i = 0; i < count; i++) {
            frames[i] = new TextureRegion(row[i]);
            if (flip) frames[i].flip(true, false);
        }
        Animation<TextureRegion> anim = new Animation<>(frameTime, frames);
        anim.setPlayMode(mode);
        return anim;
    }

    /** Image du coup : préparation, gros arc (tenu un peu plus longtemps), puis dissipation. */
    private static int slashFrame(float sinceHit) {
        if (sinceHit < 0f) return 0;
        if (sinceHit < SkullKnight.ARC_TIME) return 1;
        return Math.min(SLASH_FRAMES - 1, 2 + (int) ((sinceHit - SkullKnight.ARC_TIME) / SLASH_FRAME_TIME));
    }

    /** Dessine le chevalier : coup si attackTime >= 0, sinon course ou idle. */
    static void draw(SkullKnight knight, SpriteBatch batch, float red, float green, float blue) {
        if (sheet == null) return;
        Direction dir = knight.getFacing();
        TextureRegion frame;
        if (knight.attackTime() >= 0f) {
            frame = slash.get(dir).getKeyFrames()[slashFrame(knight.attackTime() - SkullKnight.HIT_TIME)];
        } else if (knight.isRunning() || knight.isWalking()) {
            frame = run.get(dir).getKeyFrame(knight.time(), true);
        } else {
            frame = idle.get(dir).getKeyFrame(knight.time(), true);
        }
        batch.setColor(red, green, blue, 1f);
        batch.draw(frame, knight.getPosition().x - FRAME_W / 2f, knight.getPosition().y - FRAME_H / 2f,
                FRAME_W / 2f, FRAME_H / 2f, FRAME_W, FRAME_H, SCALE, SCALE, 0f);
        batch.setColor(1f, 1f, 1f, 1f);
    }

    /** Libère la feuille de sprites. */
    static void disposeAssets() {
        if (sheet != null) {
            sheet.dispose();
            sheet = null;
        }
        run.clear();
        idle.clear();
        slash.clear();
    }
}
