package fr.epitech.katanagame;

import com.badlogic.gdx.graphics.GL20;
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
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapProperties;
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
    private static final float VIEW_W = 480f;
    private static final float VIEW_H = 270f;
    private static final float CAMERA_SMOOTH = 8f;
    private static final float LOOK_AHEAD = 0.3f;

    private SpriteBatch batch;
    private Viewport viewport;
    private Player player;
    private ShapeRenderer shapeRenderer;
    private boolean debug = true;

    private TiledMap map;
    private OrthogonalTiledMapRenderer mapRenderer;
    private TiledMapTileLayer[] solidLayers;   // couches qui bloquent le joueur
    private TiledMapTileLayer[] wallLayers;    // couches qui bloquent les balles et la vue des gardes

    private float mapW, mapH;                                    // taille de la map en pixels (lue dans le .tmx)
    private final Vector2 playerSpawn = new Vector2(100, 100);   // valeur de secours si pas de point "player"
    private final List<Vector2> enemySpawns = new ArrayList<>();

    private final List<Enemy> enemies = new ArrayList<>();
    private final List<Bullet> bullets = new ArrayList<>();

    /** Lit la taille de la map et les points de départ du calque d'objets "Spawns". */
    private void loadMapInfo() {
        MapProperties p = map.getProperties();
        mapW = p.get("width", Integer.class) * p.get("tilewidth", Integer.class);
        mapH = p.get("height", Integer.class) * p.get("tileheight", Integer.class);

        MapLayer spawns = map.getLayers().get("Spawns");
        if (spawns == null) {
            Gdx.app.log("MAP", "Pas de calque 'Spawns' dans la map : valeurs par défaut utilisées");
            return;
        }
        for (MapObject obj : spawns.getObjects()) {
            MapProperties props = obj.getProperties();
            // coin bas-gauche (libGDX a déjà inversé l'axe Y) + moitié de la taille = centre de l'objet
            // (pour un point, la taille vaut 0 : on obtient directement sa position)
            float x = props.get("x", 0f, Float.class) + props.get("width", 0f, Float.class) / 2f;
            float y = props.get("y", 0f, Float.class) + props.get("height", 0f, Float.class) / 2f;

            if ("player_spawn".equals(obj.getName())) {
                playerSpawn.set(x, y);
            } else {
                enemySpawns.add(new Vector2(x, y));   // tout le reste = un garde
            }
        }
        Gdx.app.log("MAP", "map " + mapW + "x" + mapH + " px, " + enemySpawns.size() + " garde(s)");
    }

    private void spawnEnemies() {
        enemies.clear();
        bullets.clear();
        for (Vector2 s : enemySpawns) enemies.add(new Enemy(s.x, s.y));
    }

    private void restart() {
        player.respawn(playerSpawn.x, playerSpawn.y);
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
        map = new TmxMapLoader().load("maps/etage_1.tmx", params);
        mapRenderer = new OrthogonalTiledMapRenderer(map);   // 1 unité = 1 pixel
        loadMapInfo();

        solidLayers = new TiledMapTileLayer[] {
            (TiledMapTileLayer) map.getLayers().get("Mur"),
            (TiledMapTileLayer) map.getLayers().get("Objets"),
            (TiledMapTileLayer) map.getLayers().get("Chaises"),
            (TiledMapTileLayer) map.getLayers().get("Petits Objets")
        };
        wallLayers = new TiledMapTileLayer[] {
            (TiledMapTileLayer) map.getLayers().get("Mur")
        };

        player = new Player();
        player.respawn(playerSpawn.x, playerSpawn.y);
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

        // décalage de la caméra vers la souris
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
        cam.position.x = (VIEW_W >= mapW) ? mapW / 2f
                : MathUtils.clamp(cam.position.x, halfW, mapW - halfW);
        cam.position.y = (VIEW_H >= mapH) ? mapH / 2f
                : MathUtils.clamp(cam.position.y, halfH, mapH - halfH);
        cam.update();
    }

    @Override
    public void render() {
        float delta = Gdx.graphics.getDeltaTime();

        // --- Logique ---
        if (player.isAlive()) {
            player.update(delta, viewport, solidLayers);
            if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
                for (Enemy enemy : enemies) {
                    player.attack(enemy);
                }
            }
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

        mapRenderer.render();

        // cônes de vision (transparents, sous les sprites)
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapeRenderer.setProjectionMatrix(viewport.getCamera().combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (Enemy e : enemies) e.drawVision(shapeRenderer, wallLayers);
        shapeRenderer.end();

        batch.begin();
        for (Enemy e : enemies) e.draw(batch);
        player.draw(batch);
        batch.end();

        // balles et points d'exclamation (par-dessus les sprites)
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (Bullet b : bullets) b.draw(shapeRenderer);
        for (Enemy e : enemies) e.drawAlert(shapeRenderer);
        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

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