package fr.epitech.katanagame.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.Viewport;
import fr.epitech.katanagame.entity.enemy.Enemy;
import fr.epitech.katanagame.entity.player.Player;
import fr.epitech.katanagame.entity.projectile.Bullet;
import fr.epitech.katanagame.world.Door;
import fr.epitech.katanagame.world.World;
import fr.epitech.katanagame.render.MapShadows;

import java.util.List;

/** S'occupe du rendu de la carte, des entités, des effets et du débogage. */
public final class GameRenderer implements Disposable {
    private final SpriteBatch batch = new SpriteBatch();
    private final ShapeRenderer shapes = new ShapeRenderer();
    private final SlowMoEffects slowMoEffects = new SlowMoEffects();
    /** Indique l'état des portes de l'étage (verrouillée / ouverte). */
    private final DoorIndicator doorIndicator;
    /** Renderer de la carte Tiled, fourni par le chargeur de carte. */
    private final OrthogonalTiledMapRenderer mapRenderer;

    /** Sert à animer le halo du mode dieu. */
    private float time = 0f;

    // création des ombres de la map
    private final MapShadows mapShadows;

    /** Prépare le rendu en utilisant le renderer et les portes de la carte fournis. */
    public GameRenderer(
        OrthogonalTiledMapRenderer mapRenderer,
        List<Door> doors,
        MapShadows mapShadows
    ) {
        this.mapRenderer = mapRenderer;
        this.mapShadows = mapShadows;
        this.doorIndicator = new DoorIndicator(doors);
    }

    /** Dessine la carte, les personnages, les projectiles et les éléments de débogage. */
    public void render(World world, Player player, Viewport viewport, boolean debug,
                       float slowCharge, float slowIntensity, boolean godMode) {
        time += Gdx.graphics.getDeltaTime();
        doorIndicator.prepare(world);

        ScreenUtils.clear(0.15f, 0.15f, 0.2f, 1f);
        viewport.apply();
        batch.setProjectionMatrix(viewport.getCamera().combined);
        shapes.setProjectionMatrix(viewport.getCamera().combined);
        mapRenderer.setView((OrthographicCamera) viewport.getCamera());
        // 1. Sol
        mapRenderer.render(new int[] {0});

        // 2. Ombres
        if (mapShadows != null) {
            mapShadows.draw(batch);
        }

        // 3. Toutes les autres couches Tiled
        mapRenderer.render(new int[] {1, 2, 3, 4, 5, 6});

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        // Le sang reste au sol : il passe sous tout le reste, comme le voile des portes et le halo du mode dieu.
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        world.blood.draw(shapes);
        doorIndicator.drawFloor(shapes);
        if (godMode) {
            drawGodHalo(player);
        }
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

        // Les indicateurs d'alerte et les cadenas passent au-dessus des sprites et des projectiles.
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

        // Texte des portes (nombre de gardes restants, "SORTIE").
        batch.begin();
        doorIndicator.drawText(batch, player.getPosition());
        batch.end();

        slowMoEffects.drawOverlay(slowIntensity);
        Gdx.gl.glDisable(GL20.GL_BLEND);

        if (debug) {
            DebugRenderer.draw(shapes, world, player);
        }
    }

    /** Halo doré sous les pieds du joueur quand le mode dieu est actif. */
    private void drawGodHalo(Player player) {
        Rectangle feet = player.getFeetBounds();
        float cx = feet.x + feet.width / 2f;
        float cy = feet.y + feet.height / 2f;
        float pulse = 0.5f + 0.5f * MathUtils.sin(time * 5f);

        shapes.setColor(1f, 0.82f, 0.2f, 0.12f + 0.08f * pulse);
        shapes.ellipse(cx - 16f, cy - 7f, 32f, 14f);
        shapes.setColor(1f, 0.82f, 0.2f, 0.25f + 0.15f * pulse);
        shapes.ellipse(cx - 11f, cy - 4.5f, 22f, 9f);
    }

    /** Libère les ressources graphiques créées par ce renderer. */
    @Override
    public void dispose() {
        batch.dispose();
        shapes.dispose();
        slowMoEffects.dispose();
        doorIndicator.dispose();
    }
}