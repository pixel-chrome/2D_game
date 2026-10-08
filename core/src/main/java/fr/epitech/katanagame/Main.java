package fr.epitech.katanagame;

import java.util.ArrayList;
import java.util.List;
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

public class Main extends ApplicationAdapter {
    private static final float VIEW_W = 450f;
    private static final float VIEW_H = 310f;
    private static final float CAMERA_SMOOTH = 8f;
    private static final float LOOK_AHEAD = 0.3f;

    private static final float MAP_W = 30 * 16;   // 480
    private static final float MAP_H = 20 * 16;   // 320

    private SpriteBatch batch;
    private Viewport viewport;
    private Player player;
    private ShapeRenderer shapeRenderer;
    private boolean debug = true;

    private TiledMap map;
    private OrthogonalTiledMapRenderer mapRenderer;
    private TiledMapTileLayer[] solidLayers;   // couches qui bloquent le joueur
    private TiledMapTileLayer[] wallLayers;    // couches qui bloquent les balles et la vue des gardes

    private final List<Enemy> enemies = new ArrayList<>();
    private final List<Bullet> bullets = new ArrayList<>();

    private void spawnEnemies() {
        enemies.clear();
        bullets.clear();
        enemies.add(new Enemy(380, 200));
        enemies.add(new Enemy(120, 120));
        enemies.add(new Enemy(300, 60));
    }

    private void restart() {
        player.respawn(320, 180);
        spawnEnemies();
    }

    @Override
    public void create() {
        batch = new SpriteBatch();
        viewport = new FitViewport(VIEW_W, VIEW_H);
        shapeRenderer = new ShapeRenderer();

        TmxMapLoader.Parameters params = new TmxMapLoader.Parameters();
        params.textureMinFilter = Texture.TextureFilter.Nearest;
        params.textureMagFilter = Texture.TextureFilter.Nearest;
        map = new TmxMapLoader().load("maps/map1.tmx", params);
        mapRenderer = new OrthogonalTiledMapRenderer(map);   // 1 unité = 1 pixel

        solidLayers = new TiledMapTileLayer[] {
            (TiledMapTileLayer) map.getLayers().get("mures"),
            (TiledMapTileLayer) map.getLayers().get("Objets")
        };
        wallLayers = new TiledMapTileLayer[] {
            (TiledMapTileLayer) map.getLayers().get("mures")
        };

        player = new Player();
        viewport.getCamera().position.set(player.getPosition().x, player.getPosition().y, 0);

        Enemy.loadAssets();
        spawnEnemies();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, false);
    }

    private void updateCamera(float delta) {
        Camera cam = viewport.getCamera();
        Vector2 p = player.getPosition();

        float mx = Gdx.input.getX() / (float) Gdx.graphics.getWidth() - 0.5f;
        float my = 0.5f - Gdx.input.getY() / (float) Gdx.graphics.getHeight();

        float targetX = p.x + mx * VIEW_W * LOOK_AHEAD;
        float targetY = p.y + my * VIEW_H * LOOK_AHEAD;

        float t = Math.min(1f, CAMERA_SMOOTH * delta);
        cam.position.x += (targetX - cam.position.x) * t;
        cam.position.y += (targetY - cam.position.y) * t;

        // la caméra ne sort pas de la map (centrée si la vue est plus grande que la map)
        float halfW = VIEW_W / 2f;
        float halfH = VIEW_H / 2f;
        cam.position.x = (VIEW_W >= MAP_W) ? MAP_W / 2f
                : MathUtils.clamp(cam.position.x, halfW, MAP_W - halfW);
        cam.position.y = (VIEW_H >= MAP_H) ? MAP_H / 2f
                : MathUtils.clamp(cam.position.y, halfH, MAP_H - halfH);
        cam.update();
    }

    @Override
    public void render() {
        float delta = Gdx.graphics.getDeltaTime();

        // --- Logique ---
        if (player.isAlive()) {
            player.update(delta, viewport, solidLayers);
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            restart();
        }

        for (Enemy e : enemies) e.update(delta, player, wallLayers, bullets);

        for (Bullet b : bullets) {
            b.update(delta, wallLayers);
            if (b.isAlive() && player.isAlive() && !player.isInvincible()
                    && b.getBounds().overlaps(player.getBounds())) {
                player.takeDamage();
                b.kill();
            }
        }
        bullets.removeIf(b -> !b.isAlive());

        updateCamera(delta);

        // --- Dessin ---
        ScreenUtils.clear(0.15f, 0.15f, 0.2f, 1f);
        viewport.apply();
        batch.setProjectionMatrix(viewport.getCamera().combined);
        mapRenderer.setView((OrthographicCamera) viewport.getCamera());

        mapRenderer.render(new int[]{0, 1, 2});   // sol, mures, Objets

        batch.begin();
        for (Enemy e : enemies) e.draw(batch);
        player.draw(batch);
        batch.end();

        // les balles (formes simples)
        shapeRenderer.setProjectionMatrix(viewport.getCamera().combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (Bullet b : bullets) b.draw(shapeRenderer);
        shapeRenderer.end();

        // debug (F1 pour afficher / cacher)
        if (Gdx.input.isKeyJustPressed(Input.Keys.F1)) debug = !debug;
        if (debug) {
            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
            shapeRenderer.setColor(Color.RED);
            Rectangle b = player.getBounds();
            shapeRenderer.rect(b.x, b.y, b.width, b.height);
            shapeRenderer.setColor(Color.YELLOW);
            Rectangle f = player.getFeetBounds();
            shapeRenderer.rect(f.x, f.y, f.width, f.height);
            shapeRenderer.setColor(Color.GREEN);
            for (Enemy e : enemies) {
                if (!e.isAlive()) continue;
                Rectangle eb = e.getBounds();
                shapeRenderer.rect(eb.x, eb.y, eb.width, eb.height);
            }
            shapeRenderer.end();
        }
    }

    @Override
    public void dispose() {
        batch.dispose();
        player.dispose();
        shapeRenderer.dispose();
        map.dispose();
        mapRenderer.dispose();
        Enemy.disposeAssets();
    }
}