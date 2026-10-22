package fr.epitech.katanagame.entity.enemy;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import fr.epitech.katanagame.audio.Sounds;
import fr.epitech.katanagame.entity.projectile.Bullet;
import fr.epitech.katanagame.world.World;

/** Anneau de lasers dans toutes les directions ; en phase 3, un second anneau décalé suit. À passer en se glissant entre les rayons. */
final class RadialBurst extends BossAttack {
    private static final int RAYS = 14;
    private static final float REFIRE_AT = 0.4f, RING_RADIUS = 46f;

    private boolean refired;

    RadialBurst() { super(0.9f, 0.7f, 0.6f); }

    @Override void fire(FinalBoss boss, World world) {
        refired = false;
        ring(boss, world, 0f);
    }

    @Override void tick(FinalBoss boss, float delta, float elapsed, World world) {
        if (boss.phase() >= 3 && !refired && elapsed >= REFIRE_AT) {
            refired = true;
            ring(boss, world, 180f / RAYS);
        }
    }

    private void ring(FinalBoss boss, World world, float offset) {
        Sounds.play(Sounds.Effect.SHOOT);
        for (int i = 0; i < RAYS; i++) {
            Vector2 dir = new Vector2(1f, 0f).setAngleDeg(offset + i * 360f / RAYS);
            world.bullets.add(new Bullet(new Vector2(boss.getPosition()), dir, boss.laserTexture()));
        }
    }

    @Override void drawTelegraph(ShapeRenderer shapes, FinalBoss boss, float progress) {
        Vector2 p = boss.getPosition();
        shapes.setColor(0.85f, 0.2f, 0.9f, 0.12f + 0.3f * progress);
        shapes.circle(p.x, p.y, RING_RADIUS * progress);
        shapes.setColor(0.85f, 0.2f, 0.9f, 0.5f);
        for (int i = 0; i < RAYS; i++) {
            Vector2 d = new Vector2(1f, 0f).setAngleDeg(i * 360f / RAYS).scl(RING_RADIUS + 4f);
            shapes.circle(p.x + d.x, p.y + d.y, 1.5f + 2f * progress);
        }
    }
}
