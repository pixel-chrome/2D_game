package fr.epitech.katanagame.entity;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import java.util.EnumMap;
import java.util.Map;

/** Une animation pour chacune des 4 directions, construite à partir de lignes d'une feuille de sprites. */
public final class DirectionalAnimation {
    private final Map<Direction, Animation<TextureRegion>> anims = new EnumMap<>(Direction.class);

    private DirectionalAnimation() {}

    /** Lignes bas / côté droit / haut ; la gauche est le côté droit retourné. */
    public static DirectionalAnimation sideRight(TextureRegion[][] grid, int downRow, int sideRow, int upRow,
                                                 int count, float frameTime, Animation.PlayMode mode) {
        DirectionalAnimation result = new DirectionalAnimation();
        result.anims.put(Direction.DOWN, build(grid[downRow], count, frameTime, mode, false));
        result.anims.put(Direction.RIGHT, build(grid[sideRow], count, frameTime, mode, false));
        result.anims.put(Direction.LEFT, build(grid[sideRow], count, frameTime, mode, true));
        result.anims.put(Direction.UP, build(grid[upRow], count, frameTime, mode, false));
        return result;
    }

    /** Lignes bas / côté gauche / haut ; la droite est le côté gauche retourné. */
    public static DirectionalAnimation sideLeft(TextureRegion[][] grid, int downRow, int sideRow, int upRow,
                                                int count, float frameTime, Animation.PlayMode mode) {
        DirectionalAnimation result = sideRight(grid, downRow, sideRow, upRow, count, frameTime, mode);
        result.anims.put(Direction.LEFT, build(grid[sideRow], count, frameTime, mode, false));
        result.anims.put(Direction.RIGHT, build(grid[sideRow], count, frameTime, mode, true));
        return result;
    }

    /** Mêmes images avec un autre rythme (course = marche plus rapide). */
    public DirectionalAnimation withFrameTime(float frameTime) {
        DirectionalAnimation result = new DirectionalAnimation();
        for (Map.Entry<Direction, Animation<TextureRegion>> e : anims.entrySet()) {
            Animation<TextureRegion> copy = new Animation<>(frameTime, e.getValue().getKeyFrames());
            copy.setPlayMode(e.getValue().getPlayMode());
            result.anims.put(e.getKey(), copy);
        }
        return result;
    }

    public Animation<TextureRegion> get(Direction direction) { return anims.get(direction); }

    private static Animation<TextureRegion> build(TextureRegion[] row, int count, float frameTime,
                                                  Animation.PlayMode mode, boolean flip) {
        TextureRegion[] frames = new TextureRegion[count];
        for (int i = 0; i < count; i++) {
            frames[i] = new TextureRegion(row[i]);
            if (flip) frames[i].flip(true, false);
        }
        Animation<TextureRegion> animation = new Animation<>(frameTime, frames);
        animation.setPlayMode(mode);
        return animation;
    }
}
