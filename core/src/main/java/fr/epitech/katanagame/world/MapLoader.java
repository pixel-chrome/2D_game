package fr.epitech.katanagame.world;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Disposable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Charge la carte Tiled et expose ses calques, dimensions et points d'apparition. */
public final class MapLoader implements Disposable {
    /** Chemin de la carte Tiled à charger. */
    private static final String MAP_PATH = "maps/etage_1.tmx";

    private final TiledMap map;
    private final OrthogonalTiledMapRenderer renderer;
    private final float width;
    private final float height;
    private final Vector2 playerSpawn = new Vector2(100f, 100f);
    private final List<Vector2> enemySpawns = new ArrayList<>();

    /** Charge la carte, calcule ses dimensions et récupère les points d’apparition. */
    public MapLoader() {
        TmxMapLoader.Parameters params = new TmxMapLoader.Parameters();
        params.textureMinFilter = Texture.TextureFilter.Nearest;
        params.textureMagFilter = Texture.TextureFilter.Nearest;

        map = new TmxMapLoader().load(MAP_PATH, params);
        renderer = new OrthogonalTiledMapRenderer(map); // 1 unité = 1 pixel

        MapProperties properties = map.getProperties();
        width = properties.get("width", Integer.class)
                * properties.get("tilewidth", Integer.class);
        height = properties.get("height", Integer.class)
                * properties.get("tileheight", Integer.class);
        loadSpawns();
    }

    /** Lit les positions de départ définies dans le calque Spawns. */
    private void loadSpawns() {
        MapLayer spawnsLayer = map.getLayers().get("Spawns");
        if (spawnsLayer == null) {
            Gdx.app.log("MAP", "Pas de calque 'Spawns' dans la map : valeurs par défaut utilisées");
            return;
        }

        for (MapObject object : spawnsLayer.getObjects()) {
            MapProperties properties = object.getProperties();
            // Tiled donne le coin bas-gauche : on récupère le centre de l'objet.
            float x = properties.get("x", 0f, Float.class)
                    + properties.get("width", 0f, Float.class) / 2f;
            float y = properties.get("y", 0f, Float.class)
                    + properties.get("height", 0f, Float.class) / 2f;

            if ("player_spawn".equals(object.getName())) {
                playerSpawn.set(x, y);
            } else {
                enemySpawns.add(new Vector2(x, y));
            }
        }
        Gdx.app.log("MAP", "map " + width + "x" + height + " px, "
                + enemySpawns.size() + " garde(s)");
    }

    /** Récupère un calque requis ou signale une erreur s’il manque. */
    private TiledMapTileLayer requireLayer(String name) {
        if (map.getLayers().get(name) == null) {
            throw new IllegalStateException("Calque introuvable dans la map : " + name);
        }
        return (TiledMapTileLayer) map.getLayers().get(name);
    }

    /** Retourne la carte Tiled chargée. */
    public TiledMap getMap() { return map; }
    /** Retourne le renderer associé à la carte. */
    public OrthogonalTiledMapRenderer getRenderer() { return renderer; }
    /** Retourne la largeur de la carte en unités du monde. */
    public float getWidth() { return width; }
    /** Retourne la hauteur de la carte en unités du monde. */
    public float getHeight() { return height; }
    /** Retourne une copie de la position de départ du joueur. */
    public Vector2 getPlayerSpawn() { return new Vector2(playerSpawn); }

    /** Positions de départ des ennemis, exposées en lecture seule. */
    public List<Vector2> getEnemySpawns() {
        return Collections.unmodifiableList(enemySpawns);
    }

    /** Retourne les calques qui bloquent le déplacement. */
    public TiledMapTileLayer[] getMoveLayers() {
        return new TiledMapTileLayer[] {
                requireLayer("Mur"), requireLayer("Objets"), requireLayer("Chaises")
        };
    }

    /** Retourne les calques qui bloquent les projectiles. */
    public TiledMapTileLayer[] getBulletLayers() {
        return new TiledMapTileLayer[] { requireLayer("Mur"), requireLayer("Objets") };
    }

    /** Retourne les calques qui bloquent la ligne de vue. */
    public TiledMapTileLayer[] getSightLayers() {
        return new TiledMapTileLayer[] { requireLayer("Mur") };
    }

    /** Libère le renderer et la carte chargée. */
    @Override
    public void dispose() {
        renderer.dispose();
        map.dispose();
    }
}
