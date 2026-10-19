package fr.epitech.katanagame.camera;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

/** Gère le viewport, le suivi du joueur et les limites de la caméra. */
public final class GameCamera {
    /** Largeur de la vue en unités du monde. */
    public static final float VIEW_WIDTH = 480f;
    /** Hauteur de la vue en unités du monde. */
    public static final float VIEW_HEIGHT = 270f;
    /** Vitesse de rapprochement de la caméra. */
    private static final float SMOOTHING = 8f;
    /** Intensité du décalage vers la souris. */
    private static final float MOUSE_LOOK_AHEAD = 0.15f;

    /** Vitesse à laquelle la secousse s'éteint. */
    private static final float SHAKE_DECAY = 16f;

    private float shake;
    private float shakeX, shakeY;

    private final Viewport viewport = new FitViewport(VIEW_WIDTH, VIEW_HEIGHT);

    /** Retourne le viewport utilisé par la caméra. */
    public Viewport getViewport() { return viewport; }

    /** Centre immédiatement la caméra sur une position. */
    public void centerOn(Vector2 position) {
        viewport.getCamera().position.set(position.x, position.y, 0f);
        viewport.getCamera().update();
    }

    /** Déclenche une courte secousse (en pixels du monde). */
    public void shake(float amount) {
        shake = Math.max(shake, amount);
    }

    /** Adapte le viewport à la taille de la fenêtre. */
    public void resize(int width, int height) {
        viewport.update(width, height, false);
    }

    /** Suit le joueur, anticipe légèrement la souris et limite la caméra à la carte. */
    public void update(float delta, Vector2 playerPosition, float mapWidth, float mapHeight) {
        Camera camera = viewport.getCamera();
        camera.position.x -= shakeX;   // on retire la secousse de la frame précédente
        camera.position.y -= shakeY;

        // Petit décalage vers la souris.
        float screenWidth = Math.max(1, Gdx.graphics.getWidth());
        float screenHeight = Math.max(1, Gdx.graphics.getHeight());
        float mouseX = Gdx.input.getX() / screenWidth - 0.5f;
        float mouseY = 0.5f - Gdx.input.getY() / screenHeight;

        float targetX = playerPosition.x + mouseX * VIEW_WIDTH * MOUSE_LOOK_AHEAD;
        float targetY = playerPosition.y + mouseY * VIEW_HEIGHT * MOUSE_LOOK_AHEAD;
        float smoothing = Math.min(1f, SMOOTHING * delta);
        camera.position.x += (targetX - camera.position.x) * smoothing;
        camera.position.y += (targetY - camera.position.y) * smoothing;

        float halfWidth = VIEW_WIDTH / 2f;
        float halfHeight = VIEW_HEIGHT / 2f;
        camera.position.x = VIEW_WIDTH >= mapWidth
                ? mapWidth / 2f
                : MathUtils.clamp(camera.position.x, halfWidth, mapWidth - halfWidth);
        camera.position.y = VIEW_HEIGHT >= mapHeight
                ? mapHeight / 2f
                : MathUtils.clamp(camera.position.y, halfHeight, mapHeight - halfHeight);

        shakeX = shake > 0.05f ? MathUtils.random(-shake, shake) : 0f;
        shakeY = shake > 0.05f ? MathUtils.random(-shake, shake) : 0f;
        shake = Math.max(0f, shake - shake * SHAKE_DECAY * delta);
        camera.position.x += shakeX;
        camera.position.y += shakeY;
        camera.update();
    }
}
