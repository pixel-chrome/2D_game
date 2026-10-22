package fr.epitech.katanagame.entity.enemy;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import fr.epitech.katanagame.audio.Sounds;
import fr.epitech.katanagame.entity.player.Player;
import fr.epitech.katanagame.world.Collision;
import fr.epitech.katanagame.world.World;

/** Balayage circulaire autour du boss : un disque rouge annonce la zone. Se fuit en dash (le boss ne se pare pas). */
final class SweepAttack extends BossAttack {
    static final float RADIUS = 72f;

    SweepAttack() { super(0.55f, 0.25f, 0.5f); }

    @Override void fire(FinalBoss boss, World world) {
        Sounds.play(Sounds.Effect.ATTACK);
        Player player = world.player;
        if (!player.isAlive()) return;
        Vector2 center = player.getBounds().getCenter(new Vector2());
        Vector2 from = boss.getPosition();
        if (center.dst(from) > RADIUS) return;
        if (!Collision.hasLineOfSight(world.bulletLayers, from, center)) return;
        Vector2 dir = center.cpy().sub(from).nor();
        player.hit(dir);
        if (!player.isAlive()) world.blood.burst(center, dir, world.bulletLayers);
    }

    @Override void drawTelegraph(ShapeRenderer shapes, FinalBoss boss, float progress) {
        Vector2 p = boss.getPosition();
        shapes.setColor(1f, 0.15f, 0.15f, 0.08f + 0.12f * progress);
        shapes.circle(p.x, p.y, RADIUS);
        shapes.setColor(1f, 0.15f, 0.15f, 0.12f + 0.35f * progress);
        shapes.circle(p.x, p.y, RADIUS * progress);
    }
}
