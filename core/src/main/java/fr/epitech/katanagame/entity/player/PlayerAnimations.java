package fr.epitech.katanagame.entity.player;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
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

    private final Map<Direction, Animation<TextureRegion>> idle =
            new EnumMap<>(Direction.class);

    private final Map<Direction, Animation<TextureRegion>> run =
            new EnumMap<>(Direction.class);

    private final Map<Direction, Animation<TextureRegion>> attack =
            new EnumMap<>(Direction.class);

    private final List<Texture> textures = new ArrayList<>();

    // Durées calées sur le dash (0,15 s = durée d'invincibilité) : l'effet s'éteint avec elle.
    // --- Effets du dash (traînée d'images rémanentes, trait de lame, poussière) ---
    private static final float GHOST_LIFE = 0.15f, GHOST_STEP = 7f;
    private static final float STREAK_LIFE = 0.1f;
    private static final float DUST_LIFE = 0.32f;
    private static final int DUST_COUNT = 7;
    private static final Color GHOST_START = new Color(0.9f, 0.95f, 1f, 1f);
    private static final Color GHOST_END = new Color(0.61f, 0.11f, 0.11f, 1f);   // BLOOD du thème

    private static final class Ghost {
        TextureRegion frame;
        float x, y, birth;
    }

    private final List<Ghost> ghosts = new ArrayList<>();
    private final TextureRegion pixel;
    private boolean wasDashing;
    private float startX, startY, endX, endY, streakBirth = -10f;
    private float lastGhostX, lastGhostY;
    private float dustBirth = -10f, dustX, dustY;
    private final float[] dustAngle = new float[DUST_COUNT];
    private final float[] dustSpeed = new float[DUST_COUNT];

    PlayerAnimations() {
        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(Color.WHITE);
        pm.fill();
        Texture pixelTex = new Texture(pm);
        pm.dispose();
        textures.add(pixelTex);
        pixel = new TextureRegion(pixelTex);

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
            Direction dashFacing
    ) {
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

        updateDashEffects(player, frame, x, y, dashing, stateTime);
        drawDust(batch, stateTime);
        drawStreakAndGhosts(batch, stateTime, dashing);

        // Pendant le dash : léger étirement dans le sens du mouvement et teinte froide.
        boolean dashBody = dashing && attackTimer <= 0f;
        float sx = 1f, sy = 1f;
        if (dashBody) {
            boolean horizontal = dashFacing == Direction.LEFT || dashFacing == Direction.RIGHT;
            if (horizontal) { sx = 1.18f; sy = 0.92f; } else { sx = 0.92f; sy = 1.14f; }
            batch.setColor(0.85f, 0.92f, 1f, 1f);
        }
        batch.draw(frame, x, y, FRAME_W / 2f, 0f, FRAME_W, FRAME_H, sx, sy, 0f);

        // Réinitialisation de la couleur pour les prochains sprites.
        batch.setColor(1f, 1f, 1f, 1f);
    }

    /** Suit le dash : début, images rémanentes à intervalles réguliers, fin. */
    private void updateDashEffects(Player player, TextureRegion frame, float x, float y,
                                   boolean dashing, float now) {
        if (dashing) {
            float px = player.getPosition().x, py = player.getPosition().y;
            if (!wasDashing) {
                startX = px;
                startY = py;
                lastGhostX = Float.NaN;
                Rectangle feet = player.getFeetBounds();
                dustX = feet.x + feet.width / 2f;
                dustY = feet.y + feet.height / 2f;
                dustBirth = now;
                // Poussière projetée vers l'arrière du dash.
                float back = MathUtils.atan2(-player.getDashDir().y, -player.getDashDir().x);
                for (int i = 0; i < DUST_COUNT; i++) {
                    dustAngle[i] = back + MathUtils.random(-0.9f, 0.9f);
                    dustSpeed[i] = MathUtils.random(10f, 30f);
                }
            }
            endX = px;
            endY = py;
            streakBirth = now;
            if (Float.isNaN(lastGhostX) || Math.hypot(px - lastGhostX, py - lastGhostY) >= GHOST_STEP) {
                Ghost g = new Ghost();
                g.frame = frame;
                g.x = x;
                g.y = y;
                g.birth = now;
                ghosts.add(g);
                lastGhostX = px;
                lastGhostY = py;
            }
        }
        wasDashing = dashing;
        ghosts.removeIf(g -> now - g.birth >= GHOST_LIFE);
    }

    /** Petits nuages de poussière au sol, au départ du dash. */
    private void drawDust(SpriteBatch batch, float now) {
        float age = now - dustBirth;
        if (age < 0f || age >= DUST_LIFE) return;
        float t = age / DUST_LIFE;
        float ease = 1f - (1f - t) * (1f - t);
        for (int i = 0; i < DUST_COUNT; i++) {
            float px = dustX + MathUtils.cos(dustAngle[i]) * dustSpeed[i] * ease;
            float py = dustY + MathUtils.sin(dustAngle[i]) * dustSpeed[i] * ease * 0.6f + 5f * t;
            float size = 1f + 4f * (1f - t);
            batch.setColor(0.86f, 0.85f, 0.8f, 0.55f * (1f - t));   // BONE du thème
            batch.draw(pixel, px - size / 2f, py - size / 2f, size, size);
        }
        batch.setColor(1f, 1f, 1f, 1f);
    }

    /** Trait de lame blanc le long du dash + images rémanentes blanc → rouge sang (mélange additif). */
    private void drawStreakAndGhosts(SpriteBatch batch, float now, boolean dashing) {
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);

        float age = now - streakBirth;
        if (dashing || (age >= 0f && age < STREAK_LIFE)) {
            float fade = dashing ? 1f : 1f - age / STREAK_LIFE;
            float dx = endX - startX, dy = endY - startY;
            float len = (float) Math.hypot(dx, dy);
            if (len > 1f) {
                float angle = MathUtils.atan2(dy, dx) * MathUtils.radiansToDegrees;
                float cy = startY - 2f;   // à hauteur du torse, un peu sous le centre du sprite
                // halo rouge large puis cœur blanc fin
                batch.setColor(GHOST_END.r, GHOST_END.g, GHOST_END.b, 0.45f * fade);
                batch.draw(pixel, startX, cy - 3f, 0f, 3f, len, 6f, 1f, 1f, angle);
                batch.setColor(1f, 1f, 1f, 0.9f * fade);
                batch.draw(pixel, startX, cy - 1f, 0f, 1f, len, 2f, 1f, 1f, angle);
            }
        }

        for (Ghost g : ghosts) {
            float t = (now - g.birth) / GHOST_LIFE;
            batch.setColor(
                    MathUtils.lerp(GHOST_START.r, GHOST_END.r, t),
                    MathUtils.lerp(GHOST_START.g, GHOST_END.g, t),
                    MathUtils.lerp(GHOST_START.b, GHOST_END.b, t),
                    0.6f * (1f - t));
            batch.draw(g.frame, g.x, g.y);
        }

        batch.setColor(1f, 1f, 1f, 1f);
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    }

    @Override
    public void dispose() {
        for (Texture texture : textures) {
            texture.dispose();
        }

        textures.clear();
    }
}