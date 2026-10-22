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
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Disposable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Charge une carte Tiled (un étage) et expose ses calques, dimensions,
 * points d'apparition et portes.
 *
 * Calques attendus dans chaque .tmx :
 *  - tuiles : "Mur", "Objets", "Chaises"
 *  - objets "Spawns" : "player_spawn" (entrée par défaut), "entry_xxx" (arrivées par une porte),
 *                      tout le reste = un garde
 *  - objets "Doors" (optionnel) : rectangles avec les propriétés
 *                      target_map (texte), target_entry (texte)
 *                      et requires_clear (booléen, vrai par défaut)
 */
public final class MapLoader implements Disposable {
    /** Nom de l'entrée par défaut du joueur dans le calque Spawns. */
    public static final String DEFAULT_ENTRY = "player_spawn";

    /** Carte chargée par le constructeur sans argument. */
    private static final String DEFAULT_MAP_PATH = "maps/etage_1.tmx";
    /** Les objets de Spawns dont le nom commence ainsi sont des arrivées par une porte, pas des gardes. */
    private static final String ENTRY_PREFIX = "entry_";

    private final String path;
    private final TiledMap map;
    private final OrthogonalTiledMapRenderer renderer;
    private final float width;
    private final float height;

    private final Vector2 playerSpawn = new Vector2(100f, 100f);
    private final List<Vector2> enemySpawns = new ArrayList<>();
    private final List<Vector2> knightSpawns = new ArrayList<>();
    private final List<Vector2> bossSpawns = new ArrayList<>();


    private final TiledMapTileLayer[] moveLayers;
    private final TiledMapTileLayer[] bulletLayers;
    private final TiledMapTileLayer[] sightLayers;

    private final Map<String, Vector2> entries = new HashMap<>();
    private final List<Door> doors = new ArrayList<>();

    /** Charge la carte par défaut (étage 1). */
    public MapLoader() {
        this(DEFAULT_MAP_PATH);
    }

    /**
     * Charge la carte donnée, calcule ses dimensions et récupère ses points d’apparition et ses portes.
     *
     * @param path chemin de la carte Tiled (ex : maps/etage_1.tmx)
     */
    public MapLoader(String path) {
        this.path = path;

        TmxMapLoader.Parameters params = new TmxMapLoader.Parameters();
        params.textureMinFilter = Texture.TextureFilter.Nearest;
        params.textureMagFilter = Texture.TextureFilter.Nearest;

        map = new TmxMapLoader().load(path, params);
        renderer = new OrthogonalTiledMapRenderer(map); // 1 unité = 1 pixel

        MapProperties properties = map.getProperties();
        width = properties.get("width", Integer.class)
                * properties.get("tilewidth", Integer.class);
        height = properties.get("height", Integer.class)
                * properties.get("tileheight", Integer.class);

        // Les calques sont vérifiés dès le chargement : une map mal préparée plante tout de suite, avec son nom.
        TiledMapTileLayer walls = requireLayer("Mur");
        TiledMapTileLayer objects = requireLayer("Objets");
        TiledMapTileLayer chairs = requireLayer("Chaises");
        moveLayers = new TiledMapTileLayer[] { walls, objects, chairs };
        bulletLayers = new TiledMapTileLayer[] { walls, objects };
        sightLayers = new TiledMapTileLayer[] { walls };

        loadSpawns();
        loadDoors();
        Gdx.app.log("MAP", path + " : " + width + "x" + height + " px, "
                + enemySpawns.size() + " garde(s), " + doors.size() + " porte(s), entrées "
                + entries.keySet());
    }

    /**
     * Lit les entrées du joueur et les positions des ennemis définies
     * dans le calque Spawns.
     *
     * Noms réservés :
     * - player_spawn : entrée par défaut du joueur ;
     * - entry_xxx    : entrée accessible depuis une porte ;
     * - skull_knight : apparition d'un Skull Knight ;
     * - boss : apparition du boss final ;
     * - tout autre nom : apparition d'un ennemi classique.
     */
    private void loadSpawns() {
        MapLayer spawnsLayer = map.getLayers().get("Spawns");

        if (spawnsLayer == null) {
            Gdx.app.log(
                "MAP",
                "Pas de calque 'Spawns' dans " + path
                    + " : valeurs par défaut utilisées"
            );
        } else {
            for (MapObject object : spawnsLayer.getObjects()) {
                Vector2 center = centerOf(object.getProperties());
                String name = object.getName();

                if (DEFAULT_ENTRY.equals(name)
                        || (name != null && name.startsWith(ENTRY_PREFIX))) {
                    // Entrée du joueur, notamment pour les portes.
                    entries.put(name, center);
                } else if ("boss".equals(name)) {
                    // Apparition du boss final.
                    bossSpawns.add(center);
                } else if ("skull_knight".equals(name)) {
                    // Apparition d'un Skull Knight.
                    knightSpawns.add(center);
                } else {
                    // Apparition d'un ennemi classique.
                    enemySpawns.add(center);
                }
            }
        }

        // Entrée de secours si player_spawn n'existe pas.
        if (!entries.containsKey(DEFAULT_ENTRY)) {
            entries.put(DEFAULT_ENTRY, new Vector2(100f, 100f));
        }
    }

    /** Lit les portes définies dans le calque Doors (facultatif). */
    private void loadDoors() {
        MapLayer doorsLayer = map.getLayers().get("Doors");
        if (doorsLayer == null) return;

        for (MapObject object : doorsLayer.getObjects()) {
            MapProperties properties = object.getProperties();
            String targetMap = properties.get("target_map", String.class);
            if (targetMap == null || targetMap.isEmpty()) {
                Gdx.app.error("MAP", "Porte sans 'target_map' ignorée dans " + path);
                continue;
            }
            String targetEntry = properties.get("target_entry", String.class);
            if (targetEntry == null || targetEntry.isEmpty()) {
                Gdx.app.error("MAP", "Porte vers " + targetMap + " sans 'target_entry' : l'entrée par défaut ("
                        + DEFAULT_ENTRY + ") sera utilisée");
                targetEntry = DEFAULT_ENTRY;
            }
            boolean requiresClear = properties.get("requires_clear", true, Boolean.class);
            Rectangle area = new Rectangle(
                    properties.get("x", 0f, Float.class),
                    properties.get("y", 0f, Float.class),
                    properties.get("width", 0f, Float.class),
                    properties.get("height", 0f, Float.class));
            doors.add(new Door(area, targetMap, targetEntry, requiresClear));
        }
    }

    /** Centre d'un objet Tiled (Tiled donne le coin bas-gauche ; pour un point, la taille vaut 0). */
    private static Vector2 centerOf(MapProperties properties) {
        float x = properties.get("x", 0f, Float.class)
                + properties.get("width", 0f, Float.class) / 2f;
        float y = properties.get("y", 0f, Float.class)
                + properties.get("height", 0f, Float.class) / 2f;
        return new Vector2(x, y);
    }

    /** Récupère un calque requis ou signale une erreur s’il manque. */
    private TiledMapTileLayer requireLayer(String name) {
        if (map.getLayers().get(name) == null) {
            throw new IllegalStateException("Calque introuvable dans " + path + " : " + name);
        }
        return (TiledMapTileLayer) map.getLayers().get(name);
    }

    /**
     * Retourne la carte Tiled chargée.
     *
     * @return la carte Tiled
     */
    public TiledMap getMap() { return map; }
    /**
     * Retourne le renderer associé à la carte.
     *
     * @return le renderer de la carte
     */
    public OrthogonalTiledMapRenderer getRenderer() { return renderer; }
    /**
     * Retourne la largeur de la carte en unités du monde.
     *
     * @return la largeur de la carte (px)
     */
    public float getWidth() { return width; }
    /**
     * Retourne la hauteur de la carte en unités du monde.
     *
     * @return la hauteur de la carte (px)
     */
    public float getHeight() { return height; }
    /**
     * Retourne une copie de la position de départ par défaut du joueur.
     *
     * @return une copie de la position de départ
     */
    public Vector2 getPlayerSpawn() { return getEntry(DEFAULT_ENTRY); }

    /**
     * Retourne une copie de la position d'une entrée (l'entrée par défaut si elle n'existe pas).
     *
     * @param name nom de l'entrée
     * @return une copie de la position de l'entrée
     */
    public Vector2 getEntry(String name) {
        Vector2 entry = entries.get(name);
        if (entry == null) {
            Gdx.app.error("MAP", "Entrée '" + name + "' introuvable dans " + path
                    + " : entrée par défaut utilisée");
            entry = entries.get(DEFAULT_ENTRY);
        }
        Gdx.app.log("MAP", "entrée demandée '" + name + "' -> position " + entry);
        return new Vector2(entry);
    }

    /**
     * Positions de départ des Skull Knights (objets nommés « skull_knight » dans Tiled).
     *
     * @return une liste en lecture seule
     */
    public List<Vector2> getBossSpawns() {
        return Collections.unmodifiableList(bossSpawns);
    }

    /**
     * Positions de départ des Skull Knights.
     *
     * @return la liste des positions (lecture seule)
     */
    public List<Vector2> getKnightSpawns() {
        return Collections.unmodifiableList(knightSpawns);
    }

    /**
     * Positions de départ des ennemis, exposées en lecture seule.
     *
     * @return une liste en lecture seule
     */
    public List<Vector2> getEnemySpawns() {
        return Collections.unmodifiableList(enemySpawns);
    }

    /**
     * Portes de la carte, exposées en lecture seule.
     *
     * @return une liste en lecture seule
     */
    public List<Door> getDoors() {
        return Collections.unmodifiableList(doors);
    }

    /**
     * Retourne les calques qui bloquent le déplacement.
     *
     * @return les calques Mur, Objets et Chaises
     */
    public TiledMapTileLayer[] getMoveLayers() { return moveLayers; }
    /**
     * Retourne les calques qui bloquent les projectiles.
     *
     * @return les calques Mur et Objets
     */
    public TiledMapTileLayer[] getBulletLayers() { return bulletLayers; }
    /**
     * Retourne les calques qui bloquent la ligne de vue.
     *
     * @return le calque Mur
     */
    public TiledMapTileLayer[] getSightLayers() { return sightLayers; }

    /** Libère le renderer et la carte chargée. */
    @Override
    public void dispose() {
        renderer.dispose();
        map.dispose();
    }
}