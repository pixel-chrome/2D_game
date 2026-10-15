package fr.epitech.katanagame.entity.player;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;
import fr.epitech.katanagame.entity.Direction;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Chargement, sélection et libération des animations du joueur. */
final class PlayerAnimations implements Disposable {
    private static final int FRAME_W = 96;
    private static final int FRAME_H = 80;
    private static final float FRAME_TIME = 0.1f;

    private final Map<Direction, Animation<TextureRegion>> idle = new EnumMap<>(Direction.class);
    private final Map<Direction, Animation<TextureRegion>> run = new EnumMap<>(Direction.class);
    private final Map<Direction, Animation<TextureRegion>> attack = new EnumMap<>(Direction.class);
    private final List<Texture> textures = new ArrayList<>();

    PlayerAnimations() {
        for (Direction d : Direction.values()) {
            String name = d.name().toLowerCase();
            idle.put(d, load("Sprites/IDLE/idle_" + name + ".png", Animation.PlayMode.LOOP));
            run.put(d, load("Sprites/RUN/run_" + name + ".png", Animation.PlayMode.LOOP));
            attack.put(d, load("Sprites/ATTACK 1/attack1_" + name + ".png", Animation.PlayMode.NORMAL));
        }
    }

    private Animation<TextureRegion> load(String path, Animation.PlayMode mode) {
        Texture texture = new Texture(path);
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        textures.add(texture);
        TextureRegion[][] grid = TextureRegion.split(texture, FRAME_W, FRAME_H);
        Animation<TextureRegion> anim = new Animation<>(FRAME_TIME, grid[0]);
        anim.setPlayMode(mode);
        return anim;
    }

    void draw(Player player, SpriteBatch batch, float stateTime, float attackTimer,
              boolean moving, boolean dashing, Direction dashFacing) {
        Animation<TextureRegion> anim;
        float time = stateTime;
        if (attackTimer > 0f) {
            anim = attack.get(player.getFacing());
            time = Player.ATTACK_DURATION - attackTimer;
        } else if (dashing) {
            anim = run.get(dashFacing);
            time = stateTime * 2f;
            batch.setColor(1f, 1f, 1f, 0.5f);
        } else {
            anim = moving ? run.get(player.getFacing()) : idle.get(player.getFacing());
        }
        batch.draw(anim.getKeyFrame(time), player.getPosition().x - FRAME_W / 2f,
                player.getPosition().y - FRAME_H / 2f);
        batch.setColor(1f, 1f, 1f, 1f);
    }

    @Override public void dispose() {
        for (Texture texture : textures) texture.dispose();
        textures.clear();
    }
}
