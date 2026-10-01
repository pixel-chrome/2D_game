package fr.epitech.katanagame;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;


public class Main extends ApplicationAdapter {
    private static final float VIEW_W = 320f;        // plus petit = plus zoomé
    private static final float VIEW_H = 180f;
    private static final float CAMERA_SMOOTH = 8f;   // plus grand = caméra plus collée au joueur
    private static final float LOOK_AHEAD = 0.3f;    // décalage vers la souris (0 = toujours centrée sur le joueur)
    private SpriteBatch batch;
    private Viewport viewport;
    private Player player;
    private ShapeRenderer shapeRenderer;
    private boolean debug = true;

    @Override
    public void create() {
        batch = new SpriteBatch();
        viewport = new FitViewport(VIEW_W, VIEW_H);
        player = new Player();
        viewport.getCamera().position.set(player.getPosition().x, player.getPosition().y, 0);
        shapeRenderer = new ShapeRenderer();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, false);
    }

    private void updateCamera(float delta) {
        Camera cam = viewport.getCamera();
        Vector2 p = player.getPosition();

        // Position de la souris par rapport au centre de l'écran, de -0.5 à +0.5
        float mx = Gdx.input.getX() / (float) Gdx.graphics.getWidth() - 0.5f;
        float my = 0.5f - Gdx.input.getY() / (float) Gdx.graphics.getHeight();

        float targetX = p.x + mx * VIEW_W * LOOK_AHEAD;
        float targetY = p.y + my * VIEW_H * LOOK_AHEAD;

        float t = Math.min(1f, CAMERA_SMOOTH * delta);
        cam.position.x += (targetX - cam.position.x) * t;
        cam.position.y += (targetY - cam.position.y) * t;
        cam.update();
    }

    @Override
    public void render() {
        float delta = Gdx.graphics.getDeltaTime();
        player.update(delta, viewport);
        updateCamera(delta);

        ScreenUtils.clear(0.15f, 0.15f, 0.2f, 1f);
        viewport.apply();
        batch.setProjectionMatrix(viewport.getCamera().combined);
        batch.begin();
        player.draw(batch);
        batch.end();
        if (Gdx.input.isKeyJustPressed(Input.Keys.F1)) debug = !debug;
        if (debug) {
            shapeRenderer.setProjectionMatrix(viewport.getCamera().combined);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
            shapeRenderer.setColor(Color.RED);
            Rectangle b = player.getBounds();
            shapeRenderer.rect(b.x, b.y, b.width, b.height);
            shapeRenderer.end();
        }
    }

    @Override
    public void dispose() {
        batch.dispose();
        player.dispose();
        shapeRenderer.dispose();
    }
}