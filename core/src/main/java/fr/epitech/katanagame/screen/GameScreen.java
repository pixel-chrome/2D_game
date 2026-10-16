package fr.epitech.katanagame.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.utils.ScreenUtils;
import fr.epitech.katanagame.Main;
import fr.epitech.katanagame.camera.GameCamera;
import fr.epitech.katanagame.entity.enemy.Enemy;
import fr.epitech.katanagame.entity.player.Player;
import fr.epitech.katanagame.entity.projectile.Bullet;
import fr.epitech.katanagame.render.GameRenderer;
import fr.epitech.katanagame.render.ShadowRenderer;
import fr.epitech.katanagame.world.MapLoader;
import fr.epitech.katanagame.world.World;
import com.badlogic.gdx.math.Vector2;

/** Coordonne la partie sans gérer directement le chargement de carte ni le rendu détaillé. */
public class GameScreen extends ScreenAdapter {
    private final Main game;
    /** Charge la carte et ses calques. */
    private final MapLoader mapLoader;
    /** Caméra de la partie. */
    private final GameCamera camera;
    /** Joueur contrôlé par l’utilisateur. */
    private final Player player;
    /** État partagé par les entités. */
    private final World world;
    /** Gère le dessin de la partie. */
    private final GameRenderer renderer;

    /** Active ou désactive l’affichage des hitbox. */
    private boolean debug = true;

    /** Délai (s, court : style Hotline Miami) entre la mort du joueur et l’écran de game over. */
    private final DeathSequence death = new DeathSequence();

    /** Charge la carte et prépare les éléments de la partie. */
    public GameScreen(Main game) {
        this.game = game;
        mapLoader = new MapLoader();
        camera = new GameCamera();
        player = new Player();
        world = new World(
                camera.getViewport(),
                player,
                mapLoader.getMoveLayers(),
                mapLoader.getBulletLayers(),
                mapLoader.getSightLayers());
        renderer = new GameRenderer(mapLoader.getRenderer());

        Vector2 spawn = mapLoader.getPlayerSpawn();
        player.respawn(spawn.x, spawn.y);
        camera.centerOn(player.getPosition());

        Enemy.loadAssets();
        ShadowRenderer.initialize();
        spawnEnemies();
    }

    /** Crée les ennemis aux positions définies dans la carte. */
    private void spawnEnemies() {
        world.enemies.clear();
        world.bullets.clear();
        for (Vector2 spawn : mapLoader.getEnemySpawns()) {
            world.enemies.add(new Enemy(spawn.x, spawn.y));
        }
    }

    /** Réinitialise le joueur, les ennemis et les projectiles (touche R ou « Recommencer » de la pause). */
    public void restart() {
        Vector2 spawn = mapLoader.getPlayerSpawn();
        player.respawn(spawn.x, spawn.y);
        spawnEnemies();
        camera.centerOn(player.getPosition());
        death.reset();
        world.blood.clear();
    }

    /** Prépare l’écran de jeu lorsqu’il devient actif. */
    @Override
    public void show() {
        Gdx.input.setInputProcessor(null); // Le joueur lit directement clavier et souris.
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
        update(delta);
        if (!player.isAlive()) {
            if (death.isOver()) {
                game.setScreen(new GameOverScreen(game, this));
                return;
            }
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.F1)) debug = !debug;
        drawFrame();
    }

    /** Dessine une image de la partie sans rien faire avancer (utilisé par la pause). */
    public void drawFrame() {
        renderer.render(world, player, camera.getViewport(), debug);
    }

    /** Fait vivre le corps et le sang pendant l'écran de game over. */
    public void updateEffects(float delta) {
        player.update(delta, world);
        world.blood.update(delta, world.bulletLayers);
    }

    /** Met à jour le joueur, les ennemis et l’état du monde. */
    private void update(float delta) {
        float live = delta, worldDt = delta;
        if (!player.isAlive()) {
            death.advance(delta);
            live = delta * death.effectsScale();
            worldDt = delta * death.worldScale();
        }
        player.update(live, world);
        if (player.pollDashStart()) camera.shake(2.5f);
        world.blood.update(live, world.bulletLayers);
        if (!player.isAlive() && Gdx.input.isKeyJustPressed(Input.Keys.R)) restart();

        if (worldDt > 0f) {
            for (Enemy enemy : world.enemies) enemy.update(worldDt, world);
            updateBullets(worldDt);
        }
        camera.update(delta, player.getPosition(), mapLoader.getWidth(), mapLoader.getHeight());
    }

    /** Déplace les projectiles et gère leurs collisions. */
    private void updateBullets(float delta) {
        for (Bullet bullet : world.bullets) {
            bullet.update(delta, world.bulletLayers);
            if (bullet.isAlive() && player.isAlive() && !player.isInvincible()
                    && bullet.getBounds().overlaps(player.getBounds())) {
                Vector2 dir = bullet.getDirection();
                player.hit(dir);
                world.blood.burst(player.getBounds().getCenter(new Vector2()), dir, world.bulletLayers);
                death.start();
                bullet.kill();
            }
        }
        world.bullets.removeIf(bullet -> !bullet.isAlive());
    }

    /** Libère les ressources utilisées par la partie. */
    @Override
    public void dispose() {
        renderer.dispose();
        player.dispose();
        mapLoader.dispose();
        Enemy.disposeAssets();
        ShadowRenderer.dispose();
    }
}
