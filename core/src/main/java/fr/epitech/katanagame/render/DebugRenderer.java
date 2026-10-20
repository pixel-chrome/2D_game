package fr.epitech.katanagame.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import fr.epitech.katanagame.entity.enemy.Enemy;
import fr.epitech.katanagame.entity.player.Player;
import fr.epitech.katanagame.world.World;

/** Dessine les hitbox et autres repères utiles pendant le développement. */
public final class DebugRenderer {
    private DebugRenderer() { }

    /**
     * Dessine les hitbox du joueur, des ennemis et des projectiles.
     *
     * @param shapes le ShapeRenderer (fermé en entrée)
     * @param world monde à afficher
     * @param player joueur
     */
    public static void draw(ShapeRenderer shapes, World world, Player player) {
        shapes.begin(ShapeRenderer.ShapeType.Line);

        shapes.setColor(Color.RED);
        drawRectangle(shapes, player.getBounds());

        shapes.setColor(Color.YELLOW);
        drawRectangle(shapes, player.getFeetBounds());

        shapes.setColor(Color.CYAN);
        Rectangle attack = player.getAttackBounds();
        if (attack != null) drawRectangle(shapes, attack);

        shapes.setColor(Color.GREEN);
        for (Enemy enemy : world.enemies) {
            if (enemy.isAlive()) drawRectangle(shapes, enemy.getBounds());
        }
        shapes.end();
    }

    /** Dessine le contour d’un rectangle. */
    private static void drawRectangle(ShapeRenderer shapes, Rectangle rectangle) {
        shapes.rect(rectangle.x, rectangle.y, rectangle.width, rectangle.height);
    }
}
