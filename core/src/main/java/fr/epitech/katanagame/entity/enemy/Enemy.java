package fr.epitech.katanagame.entity.enemy;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import fr.epitech.katanagame.entity.Character;
import fr.epitech.katanagame.entity.player.Player;
import fr.epitech.katanagame.world.World;

/** Entité ennemie : orchestre l'IA, la vision, l'arme et le rendu. */
public class Enemy extends Character {
    private static final float WIDTH = 14f, HEIGHT = 24f;
    private static final float FEET_W = 10f, FEET_H = 6f, FEET_OFFSET_Y = 10f;
    private final EnemyAI ai;
    private final EnemyWeapon weapon;
    private float stateTime, deathTime;

    public Enemy(float x, float y) {
        super(x, y, WIDTH, HEIGHT, FEET_W, FEET_H, FEET_OFFSET_Y);
        aim.set(0, -1);
        ai = new EnemyAI(this, x, y);
        weapon = new EnemyWeapon(this);
    }

    public static void loadAssets() { EnemyAnimations.loadAssets(); }
    public static void disposeAssets() { EnemyAnimations.disposeAssets(); }

    @Override protected void onDeath() { deathTime = 0f; }

    @Override public void update(float delta, World world) {
        stateTime += delta;
        if (!isAlive()) {
            deathTime += delta;
            weapon.stopAttacking();
            weapon.clearShootEffect();
            ai.setSeesPlayer(false);
            return;
        }

        Player player = world.player;
        if (!player.isAlive()) {
            weapon.stopAttacking();
            ai.setSeesPlayer(false);
            return;
        }
        weapon.updateTimers(delta);
        Rectangle pb = player.getBounds();
        Vector2 target = new Vector2(pb.x + pb.width / 2f, pb.y + pb.height / 2f);
        weapon.trackPlayerVelocity(target, delta);
        boolean sees = EnemyVision.canSee(this, target, world.sightLayers);
        ai.updateAlert(sees, target, delta);

        if (!ai.isAlerted()) {
            weapon.stopAttacking();
            ai.patrol(delta, world.moveLayers);
            return;
        }
        ai.stopWalking();
        aim.set(ai.lastKnown()).sub(position).nor();
        if (!sees) {
            weapon.stopAttacking();
            return;
        }
        weapon.updateAttack(delta, target, world.bullets);
    }

    @Override public void draw(SpriteBatch batch) { EnemyAnimations.draw(this, batch); }

    public void drawVision(ShapeRenderer shapes, TiledMapTileLayer[] walls) {
        EnemyVision.draw(this, shapes, walls);
    }

    public void drawAlert(ShapeRenderer shapes) {
        if (!isAlive() || !ai.seesPlayer()) return;
        float bob = Math.abs(MathUtils.sin(stateTime * 8f)) * 2f;
        float x = position.x;
        float y = position.y + EnemyAnimations.FRAME_SIZE / 2f + 4f + bob;
        shapes.setColor(Color.BLACK);
        shapes.rect(x - 3f, y - 1f, 6f, 16f);
        shapes.setColor(1f, 0.2f, 0.2f, 1f);
        shapes.rect(x - 2f, y + 5f, 4f, 9f);
        shapes.rect(x - 2f, y, 4f, 4f);
    }

    // API interne des composants de déplacement, vision et animation.
    boolean moveForAI(float dx, float dy, TiledMapTileLayer[] walls) { return move(dx, dy, walls); }
    float stateTime() { return stateTime; }
    float deathTime() { return deathTime; }
    float shootTimer() { return weapon.shootTimer(); }
    boolean isWinding() { return weapon.isWinding(); }
    boolean isWalking() { return ai.isWalking(); }
    boolean isAlerted() { return ai.isAlerted(); }
}
