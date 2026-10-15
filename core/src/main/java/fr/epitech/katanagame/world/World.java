package fr.epitech.katanagame.world;

import fr.epitech.katanagame.entity.enemy.Enemy;
import fr.epitech.katanagame.entity.player.Player;
import fr.epitech.katanagame.entity.projectile.Bullet;

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
    public final Viewport viewport;
    public final TiledMapTileLayer[] moveLayers;     // bloquent les corps (joueur + gardes)
    public final TiledMapTileLayer[] bulletLayers;   // bloquent les balles
    public final TiledMapTileLayer[] sightLayers;    // bloquent la vue des gardes

    public final Player player;
    public final List<Enemy> enemies = new ArrayList<>();
    public final List<Bullet> bullets = new ArrayList<>();

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
}
