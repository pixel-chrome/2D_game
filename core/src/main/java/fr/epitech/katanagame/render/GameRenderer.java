package fr.epitech.katanagame.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.Viewport;
import fr.epitech.katanagame.entity.enemy.Enemy;
import fr.epitech.katanagame.entity.player.Player;
import fr.epitech.katanagame.entity.projectile.Bullet;
import fr.epitech.katanagame.world.World;

/** S'occupe du rendu de la carte, des entités, des effets et du débogage. */
public final class GameRenderer implements Disposable {
    private final SpriteBatch batch = new SpriteBatch();
    private final ShapeRenderer shapes = new ShapeRenderer();
    /** Dessine la carte Tiled. */
    private final OrthogonalTiledMapRenderer mapRenderer;

    /** Prépare le rendu en utilisant le renderer de la carte fourni. */
    public GameRenderer(OrthogonalTiledMapRenderer mapRenderer) {
        this.mapRenderer = mapRenderer;
    }

    /** Dessine la carte, les personnages, les effets et les éléments de débogage. */
    public void render(World world, Player player, Viewport viewport, boolean debug) {
        ScreenUtils.clear(0.15f, 0.15f, 0.2f, 1f);
        viewport.apply();
        batch.setProjectionMatrix(viewport.getCamera().combined);
        shapes.setProjectionMatrix(viewport.getCamera().combined);
        mapRenderer.setView((OrthographicCamera) viewport.getCamera());
        mapRenderer.render();

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        // Les cônes de vision passent sous les sprites.
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        for (Enemy enemy : world.enemies) {
            enemy.drawVision(shapes, world.sightLayers);
        }
        shapes.end();

        batch.begin();
        for (Enemy enemy : world.enemies) enemy.draw(batch);
        player.draw(batch);
        batch.end();

        // Les projectiles et alertes passent au-dessus des sprites.
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        for (Bullet bullet : world.bullets) bullet.draw(shapes);
        for (Enemy enemy : world.enemies) enemy.drawAlert(shapes);
        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        if (debug) DebugRenderer.draw(shapes, world, player);
    }

    /** Libère les ressources graphiques créées par ce renderer. */
    @Override
    public void dispose() {
        batch.dispose();
        shapes.dispose();
    }
}
