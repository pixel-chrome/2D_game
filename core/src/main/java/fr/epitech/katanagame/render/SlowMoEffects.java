package fr.epitech.katanagame.render;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Disposable;
import fr.epitech.katanagame.ui.FilmOverlay;
import fr.epitech.katanagame.ui.MenuBackground;

/** Rendu du ralenti : vignette et image assombrie, plus la jauge de ralenti en bas de l'écran. */
final class SlowMoEffects implements Disposable {
    private static final float DIM_MAX = 0.18f;

    private final FilmOverlay overlay = new FilmOverlay();
    private final Matrix4 overlayProjection = new Matrix4().setToOrtho2D(0f, 0f, MenuBackground.W, MenuBackground.H);

    /** Jauge fine en bas de l'écran, visible seulement si elle n'est pas pleine (ShapeRenderer déjà ouvert). */
    void drawBar(ShapeRenderer shapes, OrthographicCamera cam, float charge) {
        if (charge >= 1f) return;
        float w = cam.viewportWidth * cam.zoom, h = cam.viewportHeight * cam.zoom;
        float barW = w * 0.2f, barH = 3f, x = cam.position.x - barW / 2f, y = cam.position.y - h / 2f + 10f;
        shapes.setColor(0f, 0f, 0f, 0.5f);
        shapes.rect(x - 1f, y - 1f, barW + 2f, barH + 2f);
        shapes.setColor(1f, 1f, 1f, 0.85f);
        shapes.rect(x, y, barW * charge, barH);
    }

    /** Bords qui s'assombrissent et image légèrement assombrie, sans teinte (hors ShapeRenderer). */
    void drawOverlay(float intensity) {
        if (intensity > 0f) overlay.draw(0f, overlayProjection, DIM_MAX * intensity, intensity);
    }

    @Override public void dispose() { overlay.dispose(); }
}
