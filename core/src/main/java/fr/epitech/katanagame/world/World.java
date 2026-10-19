package fr.epitech.katanagame.world;

import fr.epitech.katanagame.entity.enemy.Enemy;
import fr.epitech.katanagame.entity.player.Player;
import fr.epitech.katanagame.entity.projectile.Bullet;
import fr.epitech.katanagame.render.BloodEffects;

import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.utils.viewport.Viewport;

import java.util.ArrayList;
import java.util.List;

/**
 * Tout ce dont une entité a besoin pour se mettre à jour :
 * les calques de collision, le joueur, les ennemis, les balles.
 * Évite de passer 5 paramètres différents à chaque update().
 */
public class World {
    /** Viewport de la partie. */
    public final Viewport viewport;
    public final TiledMapTileLayer[] moveLayers;     // bloquent les corps (joueur + gardes)
    /** Calques qui bloquent les projectiles. */
    public final TiledMapTileLayer[] bulletLayers;   // bloquent les balles
    /** Calques qui bloquent la vision des ennemis. */
    public final TiledMapTileLayer[] sightLayers;    // bloquent la vue des gardes

    /** Joueur contrôlé par l’utilisateur. */
    public final Player player;
    public final List<Enemy> enemies = new ArrayList<>();
    public final List<Bullet> bullets = new ArrayList<>();
    /** Sang et flash d'impact (mort du joueur). */
    public final BloodEffects blood = new BloodEffects();

    /** Initialise les références vers le joueur, le viewport et les calques de collision. */
    public World(Viewport viewport, Player player,
                 TiledMapTileLayer[] moveLayers,
                 TiledMapTileLayer[] bulletLayers,
                 TiledMapTileLayer[] sightLayers) {
        this.viewport = viewport;
        this.player = player;
        this.moveLayers = moveLayers;
        this.bulletLayers = bulletLayers;
        this.sightLayers = sightLayers;
    }
    public boolean allEnemiesDead() {
        for (Enemy e : enemies) {
            if (e.isAlive()) return false;
        }
        return true;
    }
}
