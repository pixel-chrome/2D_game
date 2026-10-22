package fr.epitech.katanagame.entity.enemy;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import fr.epitech.katanagame.audio.Sounds;
import fr.epitech.katanagame.entity.projectile.Bullet;
import fr.epitech.katanagame.render.Vfx;
import fr.epitech.katanagame.world.World;

/** Éventail de lasers vers le joueur ; en phase 3, une seconde salve suit. Un parry renvoie les lasers sur le boss. */
final class FanAttack extends BossAttack {
    private static final float SPREAD = 12f, REFIRE_AT = 0.3f, LENGTH = 200f;

    private boolean refired;

    FanAttack() { super(0.75f, 0.5f, 0.5f); }

    @Override void fire(FinalBoss boss, World world) {
        refired = false;
        volley(boss, world);
    }

    @Override void tick(FinalBoss boss, float delta, float elapsed, World world) {
        if (boss.phase() >= 3 && !refired && elapsed >= REFIRE_AT) {
            refired = true;
            volley(boss, world);
        }
    }

    private void volley(FinalBoss boss, World world) {
        Sounds.play(Sounds.Effect.BOSS_SHOOT);
        int half = boss.phase() >= 2 ? 3 : 2;
        float base = boss.getAim().angleDeg();
        world.vfx.spawn(Vfx.Kind.IMPACT, boss.getPosition().x + boss.getAim().x * 24f, boss.getPosition().y + boss.getAim().y * 24f, 90f);
        for (int i = -half; i <= half; i++) {
            Vector2 dir = new Vector2(1f, 0f).setAngleDeg(base + i * SPREAD);
            world.bullets.add(new Bullet(new Vector2(boss.getPosition()), dir, boss.laserTexture()));
        }
    }

    @Override void drawTelegraph(ShapeRenderer shapes, FinalBoss boss, float progress) {
        Vector2 p = boss.getPosition();
        float base = boss.getAim().angleDeg();
        shapes.setColor(0.85f, 0.2f, 0.9f, 0.1f + 0.3f * progress);
        for (int i = -1; i <= 1; i++) {
            Vector2 d = new Vector2(1f, 0f).setAngleDeg(base + i * SPREAD * 2f).scl(LENGTH);
            shapes.rectLine(p.x, p.y, p.x + d.x, p.y + d.y, 2f + 3f * progress);
        }
    }
}
