package fr.epitech.katanagame.render;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import fr.epitech.katanagame.entity.enemy.Boss;

/** Barre de vie du boss, en haut de l'écran, avec un cran à chaque tiers (les phases). */
final class BossHealthBar {
    private BossHealthBar() {}

    /** Dessine la barre (ShapeRenderer déjà ouvert en mode Filled). */
    static void draw(ShapeRenderer shapes, OrthographicCamera cam, Boss boss) {
        float w = cam.viewportWidth * cam.zoom, h = cam.viewportHeight * cam.zoom;
        float barW = w * 0.5f, barH = 6f, x = cam.position.x - barW / 2f, y = cam.position.y + h / 2f - 18f;
        float ratio = (float) boss.getHealth() / boss.getMaxHealth();
        shapes.setColor(0f, 0f, 0f, 0.65f);
        shapes.rect(x - 2f, y - 2f, barW + 4f, barH + 4f);
        shapes.setColor(0.25f, 0.05f, 0.1f, 1f);
        shapes.rect(x, y, barW, barH);
        shapes.setColor(0.85f, 0.15f, 0.55f, 1f);
        shapes.rect(x, y, barW * ratio, barH);
        shapes.setColor(0f, 0f, 0f, 0.8f);
        shapes.rect(x + barW / 3f - 0.5f, y, 1f, barH);
        shapes.rect(x + 2f * barW / 3f - 0.5f, y, 1f, barH);
    }
}
