package fr.epitech.katanagame;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

public class Main extends ApplicationAdapter {
    private SpriteBatch batch;
    private Viewport viewport;
    private Texture playerTexture;
    private Player player;

    @Override
    public void create() {
        batch = new SpriteBatch();
        viewport = new FitViewport(640, 360);
        playerTexture = new Texture("player.png");
        player = new Player(playerTexture);
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override 
    public void render() {
        player.update(Gdx.graphics.getDeltaTime(), viewport);

        ScreenUtils.clear(0.15f, 0.15f, 0.2f, 1f);
        viewport.apply();
        batch.setProjectionMatrix(viewport.getCamera().combined);
        batch.begin();
        player.draw(batch);
        batch.end();
    }

    @Override 
    public void dispose() {
        batch.dispose();
        playerTexture.dispose();
    }
}