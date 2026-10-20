package fr.epitech.katanagame.world;

import com.badlogic.gdx.math.Vector2;
import fr.epitech.katanagame.entity.enemy.Enemy;
import fr.epitech.katanagame.entity.player.Player;
import fr.epitech.katanagame.entity.projectile.Bullet;

/** Déplace les projectiles du monde et gère leurs impacts : sur le joueur, ou sur les ennemis quand ils sont renvoyés. */
public final class BulletSystem {
    private final World world;
    /** Appelé quand le joueur est tué par une balle. */
    private final Runnable onPlayerKilled;

    /**
     * Associe le système au monde et à l'action à lancer quand le joueur meurt.
     *
     * @param world monde contenant les projectiles
     * @param onPlayerKilled appelé quand une balle tue le joueur
     */
    public BulletSystem(World world, Runnable onPlayerKilled) {
        this.world = world;
        this.onPlayerKilled = onPlayerKilled;
    }

    /**
     * Déplace les projectiles et gère leurs collisions.
     *
     * @param delta temps écoulé (s)
     */
    public void update(float delta) {
        for (Bullet bullet : world.bullets) {
            bullet.update(delta, world.bulletLayers);
            if (bullet.isReflected()) hitEnemies(bullet);
            else hitPlayer(bullet);
        }
        world.bullets.removeIf(bullet -> !bullet.isAlive());
    }

    private void hitPlayer(Bullet bullet) {
        Player player = world.player;
        if (!bullet.isAlive() || !player.isAlive() || player.isInvincible()
                || !bullet.getBounds().overlaps(player.getBounds())) return;
        Vector2 dir = bullet.getDirection();
        player.hit(dir);
        world.blood.burst(player.getBounds().getCenter(new Vector2()), dir, world.bulletLayers);
        onPlayerKilled.run();
        bullet.kill();
    }

    /** Une balle renvoyée tue le premier ennemi qu'elle touche. */
    private void hitEnemies(Bullet bullet) {
        if (!bullet.isAlive()) return;
        for (Enemy enemy : world.enemies) {
            if (enemy.isAlive() && bullet.getBounds().overlaps(enemy.getBounds())) {
                enemy.takeDamage();
                Vector2 at = enemy.getBounds().getCenter(new Vector2());
                world.blood.splatter(at, bullet.getDirection(), world.bulletLayers);
                bullet.kill();
                return;
            }
        }
    }
}
