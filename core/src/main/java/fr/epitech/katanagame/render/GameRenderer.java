package fr.epitech.katanagame.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.Viewport;
import fr.epitech.katanagame.entity.enemy.Enemy;
import fr.epitech.katanagame.entity.player.Player;
import fr.epitech.katanagame.entity.projectile.Bullet;
import fr.epitech.katanagame.world.World;

/** S'occupe du rendu de la carte, des entités, des effets et du débogage. */
public final class GameRenderer implements Disposable {
    private final SpriteBatch batch = new SpriteBatch();
    private final ShapeRenderer shapes = new ShapeRenderer();
    private final SlowMoEffects slowMoEffects = new SlowMoEffects();
    /** Renderer de la carte Tiled, fourni par le chargeur de carte. */
    private final OrthogonalTiledMapRenderer mapRenderer;

    /**
     * Prépare le rendu en utilisant le renderer de la carte fourni.
     *
     * @param mapRenderer renderer de la carte Tiled
     */
    public GameRenderer(OrthogonalTiledMapRenderer mapRenderer) {
        this.mapRenderer = mapRenderer;
    }

    /**
     * Dessine la carte, les personnages, les projectiles et les éléments de débogage.
     *
     * @param world monde à dessiner
     * @param player joueur
     * @param viewport viewport de la partie
     * @param debug afficher les hitbox
     * @param slowCharge jauge de ralenti (0 à 1)
     * @param slowIntensity intensité du ralenti (0 à 1)
     */
    public void render(World world, Player player, Viewport viewport, boolean debug,
                       float slowCharge, float slowIntensity) {
        ScreenUtils.clear(0.15f, 0.15f, 0.2f, 1f);
        viewport.apply();
        batch.setProjectionMatrix(viewport.getCamera().combined);
        shapes.setProjectionMatrix(viewport.getCamera().combined);
        mapRenderer.setView((OrthographicCamera) viewport.getCamera());
        mapRenderer.render();

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        // Le sang reste au sol : il passe sous tout le reste.
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        world.blood.draw(shapes);
        shapes.end();

        // Les cônes de vision passent sous les sprites.
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        for (Enemy enemy : world.enemies) {
            enemy.drawVision(shapes, world.sightLayers);
        }
        shapes.end();

        // Petit point lumineux qui annonce brièvement la direction du tir.
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        for (Enemy enemy : world.enemies) {
            enemy.drawCharge(shapes);
        }
        shapes.end();

        // Dessine les ennemis et le joueur au-dessus des ombres.
        batch.begin();
        for (Enemy enemy : world.enemies) {
            enemy.draw(batch);
        }
        player.draw(batch);
        batch.end();

        // Dessine les lasers au-dessus des personnages.
        batch.begin();
        for (Bullet bullet : world.bullets) {
            bullet.draw(batch);
        }
        batch.end();

        // Les indicateurs d'alerte passent au-dessus des sprites et des projectiles.
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        for (Enemy enemy : world.enemies) {
            enemy.drawAlert(shapes);
        }
        for (ShapeEffect effect : world.effects) effect.draw(shapes);
        // Flash d'impact par-dessus tout.
        world.blood.drawFlash(shapes, viewport.getCamera());
        world.parryFx.drawFlash(shapes, viewport.getCamera());
        slowMoEffects.drawBar(shapes, (OrthographicCamera) viewport.getCamera(), slowCharge);
        shapes.end();

        slowMoEffects.drawOverlay(slowIntensity);
        Gdx.gl.glDisable(GL20.GL_BLEND);

        if (debug) {
            DebugRenderer.draw(shapes, world, player);
        }
    }

    /** Libère les ressources graphiques créées par ce renderer. */
    @Override
    public void dispose() {
        batch.dispose();
        shapes.dispose();
        slowMoEffects.dispose();
    }
}
