package fr.epitech.katanagame.entity.enemy;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import fr.epitech.katanagame.entity.Direction;
import fr.epitech.katanagame.render.ShadowRenderer;
import fr.epitech.katanagame.render.Vfx;

import java.util.EnumMap;
import java.util.Map;

/**
 * Sprites du Chapelier Noir (assets/enemy/DarkHatman). Chaque feuille a 4 colonnes (bas, haut, droite, gauche)
 * et une image par ligne : 4 pour le repos et l'attaque, 8 pour la marche.
 */
final class BossAnimations {
    private static final String FOLDER = "enemy/DarkHatman/";
    private static final int FRAME = 64;
    /** Agrandissement du sprite (le boss est plus grand que les gardes). */
    private static final float SCALE = 1.8f;
    /** Ombre un peu plus sombre que celle des gardes : le boss est grand. */
    private static final float SHADOW_ALPHA = 0.35f;
    private static final float IDLE_TIME = 0.2f, WALK_TIME = 0.1f, FADE_TIME = 1f;

    private static Texture idleSheet, walkSheet, attackSheet;
    private static final Map<Direction, Animation<TextureRegion>> IDLE = new EnumMap<>(Direction.class);
    private static final Map<Direction, Animation<TextureRegion>> WALK = new EnumMap<>(Direction.class);
    private static final Map<Direction, TextureRegion[]> ATTACK = new EnumMap<>(Direction.class);

    private BossAnimations() {}

    /** Charge les feuilles (une seule fois). */
    static void loadAssets() {
        if (idleSheet != null) return;
        idleSheet = sheet("idle.png");
        walkSheet = sheet("walk.png");
        attackSheet = sheet("attack.png");
        TextureRegion[][] idle = TextureRegion.split(idleSheet, FRAME, FRAME);
        TextureRegion[][] walk = TextureRegion.split(walkSheet, FRAME, FRAME);
        TextureRegion[][] attack = TextureRegion.split(attackSheet, FRAME, FRAME);
        for (Direction d : Direction.values()) {
            int col = column(d);
            IDLE.put(d, loop(idle, col, IDLE_TIME));
            WALK.put(d, loop(walk, col, WALK_TIME));
            ATTACK.put(d, column(attack, col));
        }
    }

    private static Texture sheet(String file) {
        Texture t = new Texture(FOLDER + file);
        t.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        return t;
    }

    private static int column(Direction d) {
        switch (d) {
            case DOWN: return 0;
            case UP: return 1;
            case RIGHT: return 2;
            default: return 3;
        }
    }

    private static TextureRegion[] column(TextureRegion[][] grid, int col) {
        TextureRegion[] frames = new TextureRegion[grid.length];
        for (int row = 0; row < grid.length; row++) frames[row] = grid[row][col];
        return frames;
    }

    private static Animation<TextureRegion> loop(TextureRegion[][] grid, int col, float frameTime) {
        Animation<TextureRegion> anim = new Animation<>(frameTime, column(grid, col));
        anim.setPlayMode(Animation.PlayMode.LOOP);
        return anim;
    }

    /** Dessine le boss : annonce (garde levée, rougit), action (coup), marche ou repos ; fondu à la mort. */
    static void draw(FinalBoss boss, SpriteBatch batch) {
        if (idleSheet == null) return;
        Direction dir = boss.getFacing();
        float red = 1f, green = 1f, blue = 1f, alpha = 1f;
        TextureRegion frame;
        if (!boss.isAlive()) {
            alpha = 1f - boss.deathTime() / FADE_TIME;
            if (alpha <= 0f) return;
            frame = IDLE.get(dir).getKeyFrame(0f);
        } else if (boss.activeProgress() >= 0f) {
            TextureRegion[] swing = ATTACK.get(dir);
            frame = swing[1 + Math.min(2, (int) (boss.activeProgress() * 3f))];
        } else if (boss.windupProgress() >= 0f) {
            frame = ATTACK.get(dir)[0];
            green = blue = 1f - 0.45f * boss.windupProgress();
        } else if (boss.isMoving()) {
            frame = WALK.get(dir).getKeyFrame(boss.time());
        } else {
            frame = IDLE.get(dir).getKeyFrame(boss.time());
        }
        if (boss.isAlive() && boss.phase() >= 3) green = blue = Math.min(green, blue) * 0.8f;   // enragé : rougit
        if (boss.isHurt() && ((int) (boss.time() * 30f) & 1) == 0) alpha = 0.45f;               // clignote après un dégât
        float x = boss.getPosition().x, y = boss.getPosition().y, size = FRAME * SCALE;
        Rectangle feet = boss.getFeetBounds();
        float footY = feet.y + feet.height / 2f;
        // Ombre au sol (même projection que les gardes), qui s'efface avec le corps.
        ShadowRenderer.draw(batch, frame, x - size / 2f, y - size / 2f, size, size, footY, SHADOW_ALPHA * alpha);
        // Flammes bleues autour du boss enragé, sous le sprite.
        if (boss.isAlive() && boss.phase() >= 3) {
            Vfx.draw(batch, Vfx.Kind.ANIMA, x, footY + 38f, 150f, boss.time(), 0.55f);
        }
        batch.setColor(red, green, blue, alpha);
        batch.draw(frame, x - FRAME / 2f, y - FRAME / 2f, FRAME / 2f, FRAME / 2f, FRAME, FRAME, SCALE, SCALE, 0f);
        batch.setColor(1f, 1f, 1f, 1f);
        // Anneau électrique qui enfle pendant l'annonce d'une attaque.
        if (boss.isAlive() && boss.windupProgress() >= 0f) {
            Vfx.draw(batch, Vfx.Kind.ELECTRIC_SHIELD, x, footY + 30f, 70f + 70f * boss.windupProgress(), boss.time(), 0.9f);
        }
    }

    /** Libère les feuilles. */
    static void disposeAssets() {
        if (idleSheet == null) return;
        idleSheet.dispose();
        walkSheet.dispose();
        attackSheet.dispose();
        idleSheet = walkSheet = attackSheet = null;
        IDLE.clear();
        WALK.clear();
        ATTACK.clear();
    }
}
