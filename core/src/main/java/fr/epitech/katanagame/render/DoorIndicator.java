package fr.epitech.katanagame.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Disposable;
import fr.epitech.katanagame.entity.enemy.Enemy;
import fr.epitech.katanagame.world.Door;
import fr.epitech.katanagame.world.World;

import java.util.List;

/**
 * Montre l'état des portes de l'étage :
 *  - rouge + cadenas : verrouillée (il reste des gardes), avec le nombre restant quand on s'approche ;
 *  - vert + flèche   : ouverte, "SORTIE" quand on s'approche.
 */
public final class DoorIndicator implements Disposable {
    /** Distance (px) à partir de laquelle le texte s'affiche. */
    private static final float TEXT_RANGE = 90f;
    private static final float TEXT_SCALE = 0.6f;

    private static final Color LOCKED = new Color(0.95f, 0.25f, 0.25f, 1f);
    private static final Color OPEN = new Color(0.30f, 0.95f, 0.45f, 1f);

    private final List<Door> doors;
    private final BitmapFont font = new BitmapFont();
    private final GlyphLayout layout = new GlyphLayout();

    private float time = 0f;
    private int remaining = 0;   // gardes encore vivants

    public DoorIndicator(List<Door> doors) {
        this.doors = doors;
        font.getData().setScale(TEXT_SCALE);
        font.getRegion().getTexture().setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
    }

    /** À appeler une fois par image, avant les méthodes de dessin. */
    public void prepare(World world) {
        time += Gdx.graphics.getDeltaTime();
        remaining = 0;
        for (Enemy enemy : world.enemies) {
            if (enemy.isAlive()) remaining++;
        }
    }

    private boolean isLocked(Door door) {
        return door.requiresClear() && remaining > 0;
    }

    /** Voile coloré sur la zone de la porte (à dessiner SOUS les personnages, shapes en mode Filled). */
    public void drawFloor(ShapeRenderer shapes) {
        float pulse = 0.5f + 0.5f * MathUtils.sin(time * 4f);
        for (Door door : doors) {
            Rectangle area = door.getArea();
            Color color = isLocked(door) ? LOCKED : OPEN;
            shapes.setColor(color.r, color.g, color.b, 0.12f + 0.12f * pulse);
            shapes.rect(area.x, area.y, area.width, area.height);
        }
    }

    /** Cadenas ou flèche au-dessus de la porte (shapes en mode Filled). */
    public void drawIcons(ShapeRenderer shapes) {
        for (Door door : doors) {
            Rectangle area = door.getArea();
            float x = area.x + area.width / 2f;
            float y = area.y + area.height + 4f;

            if (isLocked(door)) {
                drawPadlock(shapes, x, y);
            } else {
                drawArrow(shapes, x, y + Math.abs(MathUtils.sin(time * 5f)) * 3f);   // petit rebond
            }
        }
    }

    /** Texte au-dessus de la porte, seulement quand le joueur est proche (SpriteBatch déjà démarré). */
    public void drawText(SpriteBatch batch, Vector2 playerPosition) {
        for (Door door : doors) {
            Rectangle area = door.getArea();
            float centerX = area.x + area.width / 2f;
            float centerY = area.y + area.height / 2f;
            if (playerPosition.dst(centerX, centerY) > TEXT_RANGE) continue;

            boolean locked = isLocked(door);
            String text;
            if (!locked) text = "SORTIE";
            else if (remaining == 1) text = "1 GARDE RESTANT";
            else text = remaining + " GARDES RESTANTS";

            layout.setText(font, text);
            float x = centerX - layout.width / 2f;
            float y = area.y + area.height + 16f + layout.height;

            font.setColor(0f, 0f, 0f, 1f);   // ombre pour la lisibilité
            font.draw(batch, text, x + 1f, y - 1f);
            font.setColor(locked ? LOCKED : OPEN);
            font.draw(batch, text, x, y);
        }
    }

    private void drawPadlock(ShapeRenderer shapes, float x, float y) {
        shapes.setColor(Color.BLACK);                       // contour
        shapes.rect(x - 4f, y + 5f, 8f, 8f);
        shapes.rect(x - 7f, y - 1f, 14f, 9f);
        shapes.setColor(0.75f, 0.75f, 0.8f, 1f);           // anse
        shapes.rect(x - 3f, y + 6f, 2f, 6f);
        shapes.rect(x + 1f, y + 6f, 2f, 6f);
        shapes.rect(x - 3f, y + 10f, 6f, 2f);
        shapes.setColor(LOCKED);                            // corps
        shapes.rect(x - 6f, y, 12f, 7f);
        shapes.setColor(Color.BLACK);                       // trou de serrure
        shapes.rect(x - 1f, y + 2f, 2f, 3f);
    }

    private void drawArrow(ShapeRenderer shapes, float x, float y) {
        shapes.setColor(Color.BLACK);                       // contour
        shapes.triangle(x - 8f, y + 11f, x + 8f, y + 11f, x, y - 1f);
        shapes.setColor(OPEN);
        shapes.triangle(x - 6f, y + 10f, x + 6f, y + 10f, x, y + 1f);
    }

    @Override
    public void dispose() {
        font.dispose();
    }
}