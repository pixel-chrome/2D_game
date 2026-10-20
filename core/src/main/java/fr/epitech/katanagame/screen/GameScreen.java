package fr.epitech.katanagame.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.math.Vector2;
import fr.epitech.katanagame.Main;
import fr.epitech.katanagame.camera.GameCamera;
import fr.epitech.katanagame.entity.enemy.Enemy;
import fr.epitech.katanagame.entity.enemy.ShooterEnemy;
import fr.epitech.katanagame.entity.enemy.SkullKnight;
import fr.epitech.katanagame.entity.player.Player;
import fr.epitech.katanagame.render.GameRenderer;
import fr.epitech.katanagame.render.MapShadows;
import fr.epitech.katanagame.render.ScreenFade;
import fr.epitech.katanagame.render.ShadowRenderer;
import fr.epitech.katanagame.render.ShapeEffect;
import fr.epitech.katanagame.settings.GameSettings;
import fr.epitech.katanagame.ui.Crosshair;
import fr.epitech.katanagame.world.BulletSystem;
import fr.epitech.katanagame.world.Door;
import fr.epitech.katanagame.world.MapLoader;
import fr.epitech.katanagame.world.World;

import java.util.ArrayList;
import java.util.List;

/** Coordonne la partie : étage courant, mise à jour, changement d'étage, écrans de pause et de game over. */
public class GameScreen extends ScreenAdapter {
    private static final String FIRST_MAP = "maps/etage_1.tmx";
    /** Durée de chaque moitié du fondu (noircir, puis éclaircir). */
    private static final float FADE_TIME = 0.4f;
    /** Calques de tuiles qui projettent une ombre (ceux qui n'existent pas dans une map sont ignorés). */
    private static final String[] SHADOW_LAYERS = { "Mur", "Chaises", "Objets", "Petits Objets" };

    private final Main game;
    /** Caméra de la partie. */
    private final GameCamera camera;
    /** Joueur contrôlé par l’utilisateur (le même d'un étage à l'autre). */
    private final Player player;
    /** Fondu au noir entre deux étages. */
    private final ScreenFade fade = new ScreenFade();
    /** Délai (s, court : style Hotline Miami) entre la mort du joueur et l’écran de game over. */
    private final DeathSequence death = new DeathSequence();
    /** Ralenti du temps (maintenir la touche RALENTI). */
    private final SlowMotion slowMo = new SlowMotion();
    /** Viseur affiché à la place du curseur. */
    private final Crosshair crosshair = new Crosshair();
    /** Arrêt sur image et ralenti après un coup ou un parry qui touche. */
    private final HitStop hitStop = new HitStop();

    // --- Étage courant : remplacés à chaque changement d'étage ---
    /** Charge la carte et ses calques. */
    private MapLoader mapLoader;
    /** Ombres des murs et objets de l'étage. */
    private MapShadows mapShadows;
    /** État partagé par les entités. */
    private World world;
    /** Gère le dessin de la partie. */
    private GameRenderer renderer;
    /** Projectiles : déplacement et impacts (lié au World de l'étage). */
    private BulletSystem bullets;
    /** Entrée par laquelle le joueur est arrivé (sert au restart). */
    private String currentEntry;
    /** Vrai tant que le joueur n'a pas quitté la zone d'arrivée (évite de repartir aussitôt). */
    private boolean doorsLocked;

    /** Active ou désactive l’affichage des hitbox. */
    private boolean debug = false;

    /** Mode dieu (F2) : le joueur ne peut plus mourir. Reste actif d'un étage à l'autre. */
    private boolean godMode = false;

    /** Prépare les éléments de la partie et charge le premier étage. */
    public GameScreen(Main game) {
        this.game = game;
        camera = new GameCamera();
        player = new Player();

        Enemy.loadAssets();
        ShadowRenderer.initialize();
        loadLevel(FIRST_MAP, MapLoader.DEFAULT_ENTRY);
    }

    // =====================================================================
    // Étages
    // =====================================================================

    /**
     * Charge un étage et y place le joueur à l'entrée donnée.
     */
    private void loadLevel(String path, String entryName) {
        // 1. Charger la nouvelle carte avant de détruire l'ancienne
        MapLoader next = new MapLoader(path);

        // 2. Créer les ombres du nouveau niveau
        MapShadows nextShadows = new MapShadows(
                shadowLayersOf(next),
                (int) next.getWidth(),
                (int) next.getHeight()
        );

        // 3. Libérer les ressources de l'ancien niveau
        if (renderer != null) {
            renderer.dispose();
        }
        if (mapShadows != null) {
            mapShadows.dispose();
        }
        if (mapLoader != null) {
            mapLoader.dispose();
        }

        // 4. Remplacer l'ancien niveau par le nouveau
        mapLoader = next;
        mapShadows = nextShadows;

        // 5. Recréer le monde, les projectiles et le renderer
        world = new World(
                camera.getViewport(),
                player,
                mapLoader.getMoveLayers(),
                mapLoader.getBulletLayers(),
                mapLoader.getSightLayers()
        );
        bullets = new BulletSystem(world, death::start);
        renderer = new GameRenderer(
                mapLoader.getRenderer(),
                mapLoader.getDoors(),
                mapShadows
        );

        // 6. Placer le joueur et créer les ennemis
        enterLevel(entryName);
    }

    /** Calques de tuiles de la carte qui projettent une ombre (les calques absents sont ignorés). */
    private static TiledMapTileLayer[] shadowLayersOf(MapLoader map) {
        List<TiledMapTileLayer> layers = new ArrayList<>();
        for (String name : SHADOW_LAYERS) {
            MapLayer layer = map.getMap().getLayers().get(name);
            if (layer instanceof TiledMapTileLayer) {
                layers.add((TiledMapTileLayer) layer);
            }
        }
        return layers.toArray(new TiledMapTileLayer[0]);
    }

    /** Place le joueur à l'entrée donnée et (re)crée les ennemis de l'étage. */
    private void enterLevel(String entryName) {
        currentEntry = entryName;
        Vector2 entry = mapLoader.getEntry(entryName);
        player.respawn(entry.x, entry.y);
        spawnEnemies();
        camera.centerOn(player.getPosition());
        death.reset();
        slowMo.reset();
        world.blood.clear();
        for (ShapeEffect effect : world.effects) effect.clear();
        hitStop.reset();
        doorsLocked = true;
    }

    /** Crée les ennemis aux positions définies dans la carte. */
    private void spawnEnemies() {
        world.enemies.clear();
        world.bullets.clear();
        for (Vector2 spawn : mapLoader.getEnemySpawns()) {
            world.enemies.add(new ShooterEnemy(spawn.x, spawn.y));
        }
        for (Vector2 spawn : mapLoader.getKnightSpawns()) {
            world.enemies.add(new SkullKnight(spawn.x, spawn.y));
        }
    }

    /** Recommence l'étage courant (touche R ou « Recommencer » de la pause). */
    public void restart() {
        enterLevel(currentEntry);
    }

    /**
     * Si le joueur touche une porte ouverte, lance le fondu puis change d'étage.
     * Une porte "requires_clear" reste fermée tant qu'il reste un ennemi vivant.
     */
    private void updateDoors() {
        if (!player.isAlive()) return;

        Door touched = null;
        for (Door door : mapLoader.getDoors()) {
            if (door.isTouchedBy(player.getFeetBounds())) {
                touched = door;
                break;
            }
        }
        if (touched == null) {
            doorsLocked = false;
            return;
        }
        if (doorsLocked) return;
        if (touched.requiresClear() && !world.allEnemiesDead()) return;   // porte verrouillée

        final Door door = touched;
        doorsLocked = true;
        fade.start(FADE_TIME, () -> loadLevel(door.getTargetMap(), door.getTargetEntry()));
    }

    // =====================================================================
    // Cycle de vie de l'écran
    // =====================================================================

    /** Prépare l’écran de jeu lorsqu’il devient actif. */
    @Override
    public void show() {
        Gdx.input.setInputProcessor(null); // Le joueur lit directement clavier et souris.
        crosshair.apply();
    }

    /** Rend la flèche normale quand on quitte la partie (pause, game over). */
    @Override
    public void hide() {
        Crosshair.restore();
    }

    /** Met à jour le viewport lors du redimensionnement. */
    @Override
    public void resize(int width, int height) {
        camera.resize(width, height);
    }

    /** Met à jour puis dessine la partie. */
    @Override
    public void render(float delta) {
        // Échap : pause. La partie n'est pas détruite, juste mise de côté.
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.setScreen(new PauseScreen(game, this));
            return;
        }

        // Pendant le fondu, la partie est figée (mais toujours dessinée).
        if (!fade.isActive()) {
            update(delta);
            if (!player.isAlive() && death.isOver()) {
                game.setScreen(new GameOverScreen(game, this));
                return;
            }
            updateDoors();
        }
        fade.update(delta);   // peut changer d'étage : à faire avant de dessiner

        if (Gdx.input.isKeyJustPressed(Input.Keys.F1)) debug = !debug;
        if (Gdx.input.isKeyJustPressed(Input.Keys.F2)) {
            godMode = !godMode;
            player.setGodMode(godMode);
            Gdx.app.log("GOD", godMode ? "mode dieu activé" : "mode dieu désactivé");
        }
        drawFrame();
        fade.draw();          // en dernier : recouvre toute la fenêtre
    }

    /** Dessine une image de la partie sans rien faire avancer (utilisé par la pause). */
    public void drawFrame() {
        renderer.render(world, player, camera.getViewport(), debug, slowMo.getCharge(), slowMo.getIntensity(), godMode);
    }

    /** Fait vivre le corps et le sang pendant l'écran de game over. */
    public void updateEffects(float delta) {
        player.update(delta, world);
        world.blood.update(delta, world.bulletLayers);
    }

    // =====================================================================
    // Mise à jour
    // =====================================================================

    /** Met à jour le joueur, les ennemis et l’état du monde. */
    private void update(float delta) {
        slowMo.update(delta, player.isAlive() && GameSettings.pressed(GameSettings.Action.SLOWMO));
        float live = delta * slowMo.playerScale(), worldDt = delta * slowMo.worldScale();
        if (!player.isAlive()) {
            death.advance(delta);
            live = delta * death.effectsScale();
            worldDt = delta * death.worldScale();
        }
        if (hitStop.isFrozen(delta)) live = worldDt = 0f;   // tout est figé sauf les effets
        else worldDt *= hitStop.worldScale(delta);          // ralenti après un parry : le joueur garde la main
        player.update(live, world);
        if (player.pollDashStart()) camera.shake(2.5f);
        if (player.pollParrySuccess()) {
            hitStop.onParry();
            camera.shake(4f);
        }
        if (player.pollAttackHit()) {
            hitStop.onAttackHit();
            camera.shake(3f);
        }
        for (ShapeEffect effect : world.effects) effect.update(delta);
        world.blood.update(live, world.bulletLayers);
        if (!player.isAlive() && Gdx.input.isKeyJustPressed(Input.Keys.R)) restart();

        if (worldDt > 0f) {
            boolean wasAlive = player.isAlive();
            for (Enemy enemy : world.enemies) enemy.update(worldDt, world);
            if (wasAlive && !player.isAlive()) death.start();   // tué au corps à corps
            bullets.update(worldDt);
        }
        camera.update(delta, player.getPosition(), mapLoader.getWidth(), mapLoader.getHeight());
    }

    /** Libère les ressources utilisées par la partie. */
    @Override
    public void dispose() {
        Crosshair.restore();
        crosshair.dispose();
        renderer.dispose();
        player.dispose();
        mapLoader.dispose();
        fade.dispose();
        Enemy.disposeAssets();
        ShadowRenderer.dispose();
        mapShadows.dispose();
    }
}