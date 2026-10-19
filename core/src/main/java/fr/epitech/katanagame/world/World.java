package fr.epitech.katanagame.world;

import fr.epitech.katanagame.entity.enemy.Enemy;
import fr.epitech.katanagame.entity.player.Player;
import fr.epitech.katanagame.entity.projectile.Bullet;
import fr.epitech.katanagame.render.BloodEffects;
import fr.epitech.katanagame.render.ParryEffects;
import fr.epitech.katanagame.render.SlashEffects;

import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.math.Vector2;
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
    /** Croissant, anneau et flash du parry. */
    public final ParryEffects parryFx = new ParryEffects();
    /** Croissant, traits de vitesse et entailles de l'attaque. */
    public final SlashEffects slashFx = new SlashEffects();

    /** Fait du bruit : les ennemis vivants à portée viennent voir (sauf la source). */
    public void makeNoise(Vector2 at, float radius, Enemy source) {
        for (Enemy enemy : enemies) {
            if (enemy != source && enemy.getPosition().dst(at) <= radius) {
                enemy.hearNoise(at);
            }
        }
    }

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
}
