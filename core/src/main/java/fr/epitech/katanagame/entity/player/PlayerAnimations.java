package fr.epitech.katanagame.entity.player;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;

import fr.epitech.katanagame.render.ShadowRenderer;
import fr.epitech.katanagame.entity.Direction;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

final class PlayerAnimations implements com.badlogic.gdx.utils.Disposable {

    private static final int FRAME_W = 96;
    private static final int FRAME_H = 80;

    private static final float FRAME_TIME = 0.1f;

    /** Durée de la chute du corps à la mort. */
    private static final float FALL_TIME = 0.18f;

    private final Map<Direction, Animation<TextureRegion>> idle =
            new EnumMap<>(Direction.class);

    private final Map<Direction, Animation<TextureRegion>> run =
            new EnumMap<>(Direction.class);

    private final Map<Direction, Animation<TextureRegion>> attack =
            new EnumMap<>(Direction.class);

    private final List<Texture> textures = new ArrayList<>();

    PlayerAnimations() {
        for (Direction direction : Direction.values()) {
            String name = direction.name().toLowerCase();

            idle.put(
                    direction,
                    load(
                            "Sprites/IDLE/idle_" + name + ".png",
                            Animation.PlayMode.LOOP
                    )
            );

            run.put(
                    direction,
                    load(
                            "Sprites/RUN/run_" + name + ".png",
                            Animation.PlayMode.LOOP
                    )
            );

            attack.put(
                    direction,
                    load(
                            "Sprites/ATTACK 1/attack1_" + name + ".png",
                            Animation.PlayMode.NORMAL
                    )
            );
        }
    }

    private Animation<TextureRegion> load(
            String path,
            Animation.PlayMode mode
    ) {
        Texture texture = new Texture(path);
        texture.setFilter(
                Texture.TextureFilter.Nearest,
                Texture.TextureFilter.Nearest
        );

        textures.add(texture);

        TextureRegion[][] grid =
                TextureRegion.split(texture, FRAME_W, FRAME_H);

        Animation<TextureRegion> animation =
                new Animation<>(FRAME_TIME, grid[0]);

        animation.setPlayMode(mode);

        return animation;
    }

    void draw(
            Player player,
            SpriteBatch batch,
            float stateTime,
            float attackTimer,
            boolean moving,
            boolean dashing,
            Direction dashFacing,
            Corpse corpse
    ) {
        if (corpse != null) {
            drawCorpse(player, batch, corpse);
            return;
        }

        Animation<TextureRegion> animation;
        float time = stateTime;

        if (attackTimer > 0f) {
            animation = attack.get(player.getFacing());
            time = Player.ATTACK_DURATION - attackTimer;

        } else if (dashing) {
            animation = run.get(dashFacing);
            time = stateTime * 2f;

        } else {
            animation = moving
                    ? run.get(player.getFacing())
                    : idle.get(player.getFacing());
        }

        TextureRegion frame = animation.getKeyFrame(time);

        float x = player.getPosition().x - FRAME_W / 2f;
        float y = player.getPosition().y - FRAME_H / 2f;

        // Ligne des pieds : centre de la hitbox des pieds (monde).
        Rectangle feet = player.getFeetBounds();
        float footY = feet.y + feet.height / 2f;

        // Ombre du personnage, accrochée aux pieds.
        ShadowRenderer.draw(
                batch,
                frame,
                x,
                y,
                FRAME_W,
                FRAME_H,
                footY
        );

        // Transparence du dash appliquée au personnage,
        // mais pas à son ombre.
        batch.setColor(
                1f,
                1f,
                1f,
                dashing && attackTimer <= 0f ? 0.5f : 1f
        );

        batch.draw(frame, x, y);

        // Réinitialisation de la couleur pour les prochains sprites.
        batch.setColor(1f, 1f, 1f, 1f);
    }

    /** Corps touché : il bascule en arrière, rebondit un peu, puis reste au sol, assombri. */
    private void drawCorpse(Player player, SpriteBatch batch, Corpse corpse) {
        float deathTime = corpse.time();
        float t = Math.min(1f, deathTime / FALL_TIME);
        float ease = 1f - (1f - t) * (1f - t);
        float hop = (float) Math.sin(Math.PI * t) * 6f;
        float shade = 1f - 0.5f * Math.min(1f, deathTime / 0.3f);

        TextureRegion frame = idle.get(player.getFacing()).getKeyFrame(0f);
        float x = player.getPosition().x - FRAME_W / 2f;
        float y = player.getPosition().y - FRAME_H / 2f;

        batch.setColor(1f, shade, shade, 1f);
        batch.draw(frame, x, y + hop, FRAME_W / 2f, FRAME_H / 2f, FRAME_W, FRAME_H, 1f, 1f, corpse.side() * 80f * ease);
        batch.setColor(1f, 1f, 1f, 1f);
    }

    @Override
    public void dispose() {
        for (Texture texture : textures) {
            texture.dispose();
        }

        textures.clear();
    }
}