package fr.epitech.katanagame.entity.enemy;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import fr.epitech.katanagame.audio.Sounds;
import fr.epitech.katanagame.entity.player.Player;
import fr.epitech.katanagame.world.World;

/** Estoc : une ligne rouge annonce la charge, la visée est figée, puis le boss fonce. Se dash de côté (le boss ne se pare pas). */
final class DashStrike extends BossAttack {
    private static final float SPEED = 700f, LENGTH = 230f, GROW = 8f;

    private final Vector2 dir = new Vector2();
    private boolean struck;

    DashStrike() { super(0.7f, 0.33f, 0.7f); }

    @Override boolean lockAim() { return true; }

    @Override void begin(FinalBoss boss) {
        dir.set(boss.getAim());
        struck = false;
    }

    @Override void fire(FinalBoss boss, World world) {
        Sounds.play(Sounds.Effect.ATTACK);
    }

    @Override void tick(FinalBoss boss, float delta, float elapsed, World world) {
        boss.step(dir.x * SPEED * delta, dir.y * SPEED * delta, world);
        Player player = world.player;
        if (struck || !player.isAlive()) return;
        Rectangle reach = new Rectangle(boss.getBounds()).setPosition(boss.getBounds().x - GROW, boss.getBounds().y - GROW);
        reach.setSize(boss.getBounds().width + 2f * GROW, boss.getBounds().height + 2f * GROW);
        if (!reach.overlaps(player.getBounds())) return;
        struck = true;
        player.hit(dir);   // sans effet si le joueur est en dash
        if (!player.isAlive()) {
            world.blood.burst(player.getBounds().getCenter(new Vector2()), dir, world.bulletLayers);
        }
    }

    @Override void drawTelegraph(ShapeRenderer shapes, FinalBoss boss, float progress) {
        Vector2 p = boss.getPosition();
        shapes.setColor(1f, 0.15f, 0.15f, 0.15f + 0.5f * progress);
        shapes.rectLine(p.x, p.y, p.x + dir.x * LENGTH, p.y + dir.y * LENGTH, 6f + 10f * progress);
    }
}
