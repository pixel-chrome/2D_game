package fr.epitech.katanagame.entity.enemy;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import fr.epitech.katanagame.entity.player.Player;
import fr.epitech.katanagame.world.Collision;
import fr.epitech.katanagame.world.World;

/** Ennemi au corps à corps : court (plus vite que les tireurs) sur le joueur et le frappe à l'épée. */
public class SkullKnight extends Enemy {
    private static final float RUN_SPEED = 130f;        // les tireurs courent à 70
    private static final float ATTACK_RANGE = 40f;      // distance à laquelle il déclenche son coup
    private static final float HIT_REACH = 48f;         // portée réelle du coup
    private static final float HIT_ARC_COS = 0.17f;     // le coup touche ±80° autour de sa visée
    /** Durée de préparation du coup : le joueur peut encore esquiver (dash) avant ce moment. */
    static final float HIT_TIME = 0.2f;
    static final float ARC_TIME = 0.14f;            // durée d'affichage du gros arc
    private static final float ATTACK_TOTAL = HIT_TIME + ARC_TIME + 0.24f;
    private static final float ATTACK_COOLDOWN = 0.6f;
    private static final float LUNGE_SPEED = 70f;

    private float attackTime = -1f;     // < 0 : n'attaque pas
    private boolean hitDone;
    private float cooldown;

    /** Crée un chevalier à la position donnée. */
    public SkullKnight(float x, float y) {
        super(x, y, RUN_SPEED);
    }

    @Override
    protected void tick(float delta) {
        cooldown = Math.max(0f, cooldown - delta);
    }

    @Override
    protected void think(float delta, World world) {
        Player player = world.player;
        Rectangle pb = player.getBounds();
        Vector2 target = new Vector2(pb.x + pb.width / 2f, pb.y + pb.height / 2f);

        // Le coup va toujours au bout, même si le joueur vient de mourir (on voit l'arc).
        if (attackTime >= 0f) {
            updateAttack(delta, world, player, target);
            return;
        }
        if (!player.isAlive()) {
            ai.setSeesPlayer(false);
            return;
        }

        boolean sees = EnemyVision.canSee(this, target, world.sightLayers);
        ai.updateAlert(sees, target, delta);

        if (!ai.isAlerted()) {
            ai.patrol(delta, world.moveLayers);
            return;
        }

        float dist = position.dst(target);
        if (sees && dist <= ATTACK_RANGE) {
            ai.stopWalking();
            aim.set(target).sub(position).nor();
            if (cooldown <= 0f) {
                attackTime = 0f;
                hitDone = false;
            }
            return;
        }
        ai.chaseLastKnown(delta, world.moveLayers);   // il court vers le joueur (ou le bruit)
    }

    /** Parry : le coup en cours est annulé et il met un moment avant de recommencer. */
    @Override
    protected void onParried() {
        attackTime = -1f;
        cooldown = ATTACK_COOLDOWN + 0.5f;
    }

    /** Déroulé du coup : préparation, impact à HIT_TIME, puis récupération. */
    private void updateAttack(float delta, World world, Player player, Vector2 target) {
        attackTime += delta;
        // petite fente vers l'avant autour de l'impact
        if (attackTime >= HIT_TIME - 0.05f && attackTime <= HIT_TIME + 0.1f) {
            moveForAI(aim.x * LUNGE_SPEED * delta, aim.y * LUNGE_SPEED * delta, world.moveLayers);
        }
        if (!hitDone && attackTime >= HIT_TIME) {
            hitDone = true;
            if (player.isAlive()) strike(world, player, target);
        }
        if (attackTime >= ATTACK_TOTAL) {
            attackTime = -1f;
            cooldown = ATTACK_COOLDOWN;
        }
    }

    /** Touche le joueur s'il est dans la portée et l'arc du coup, sans mur entre eux. */
    private void strike(World world, Player player, Vector2 target) {
        Vector2 toPlayer = new Vector2(target).sub(position);
        float dist = toPlayer.len();
        if (dist > HIT_REACH || dist <= 0.01f) return;
        Vector2 dir = toPlayer.cpy().nor();
        if (dir.dot(aim) < HIT_ARC_COS) return;
        if (!Collision.hasLineOfSight(world.bulletLayers, position, target)) return;

        player.hit(dir);   // sans effet si le joueur est en dash
        if (!player.isAlive()) {
            world.blood.burst(target, dir, world.bulletLayers);
        }
    }

    @Override
    public void draw(SpriteBatch batch) {
        if (!isAlive()) return;   // pas d'animation de mort : le sang reste au sol
        boolean winding = attackTime >= 0f && attackTime < HIT_TIME;
        // Teinte rouge pendant la préparation du coup (comme les tireurs qui visent)
        SkullKnightAnimations.draw(this, batch, 1f, winding ? 0.7f : 1f, winding ? 0.7f : 1f);
    }

    /** Temps d'attaque écoulé, négatif s'il n'attaque pas. */
    float attackTime() {
        return attackTime;
    }

    /** Temps écoulé pour les animations. */
    float time() {
        return stateTime();
    }
}
