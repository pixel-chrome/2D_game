package fr.epitech.katanagame.entity.enemy;

import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import fr.epitech.katanagame.world.Pathfinder;

import java.util.List;

/** États d'alerte et déplacements de patrouille d'un ennemi. */
final class EnemyAI {
    private static final float LOSE_SIGHT_TIME = 1.5f;
    private static final float WANDER_RADIUS = 80f;
    private static final float WALK_SPEED = 40f;
    private static final float REPATH_INTERVAL = 0.4f;
    private static final float PAUSE_MIN = 0.8f, PAUSE_MAX = 2f;

    private final Enemy enemy;
    /** Vitesse de course vers le joueur / le bruit (px/s). */
    private final float runSpeed;
    private final Vector2 home = new Vector2();
    private final Vector2 wanderTarget = new Vector2();
    private final Vector2 lastKnown = new Vector2();
    private boolean alerted, seesPlayer, walking, chasing;
    private float alertTimer, pauseTimer;

    /** Chemin en cours vers la cible (coordonnées sprite), recalculé si la cible bouge ou si on se bloque. */
    private List<Vector2> path;
    private final Vector2 pathGoal = new Vector2();
    private float repathTimer;

    /** Initialise l’IA et la position de référence de l’ennemi. */
    EnemyAI(Enemy enemy, float x, float y, float runSpeed) {
        this.enemy = enemy;
        this.runSpeed = runSpeed;
        home.set(x, y);
    }

    /** Actualise l’alerte et la dernière position connue du joueur. */
    void updateAlert(boolean sees, Vector2 target, float delta) {
        seesPlayer = sees;
        if (sees) {
            alerted = true;
            alertTimer = LOSE_SIGHT_TIME;
            lastKnown.set(target);
        } else if (alerted && !chasing) {   // le délai ne tourne qu'une fois arrivé sur place
            alertTimer -= delta;
            if (alertTimer <= 0f) {
                alerted = false;
                walking = false;
                pauseTimer = 0.5f;
            }
        }
    }

    /** Déplace l’ennemi vers ses destinations de patrouille. */
    void patrol(float delta, TiledMapTileLayer[] walls) {
        if (walking) {
            Vector2 toTarget = new Vector2(wanderTarget).sub(enemy.getPosition());
            float dist = toTarget.len();
            if (dist < 2f) startPause();
            else {
                toTarget.nor();
                enemy.getAim().set(toTarget);
                float stepLen = Math.min(dist, WALK_SPEED * delta);
                if (!enemy.moveForAI(toTarget.x * stepLen, toTarget.y * stepLen, walls)) startPause();
            }
        } else {
            pauseTimer -= delta;
            if (pauseTimer <= 0f) pickWanderTarget(walls);
        }
    }

    /** Démarre une pause avant le prochain déplacement. */
    private void startPause() {
        walking = false;
        pauseTimer = MathUtils.random(PAUSE_MIN, PAUSE_MAX);
    }

    /** Choisit une destination de patrouille accessible. */
    private void pickWanderTarget(TiledMapTileLayer[] walls) {
        for (int attempt = 0; attempt < 8; attempt++) {
            float angle = MathUtils.random(360f);
            float dist = MathUtils.random(20f, WANDER_RADIUS);
            Vector2 candidate = new Vector2(home).add(
                    MathUtils.cosDeg(angle) * dist, MathUtils.sinDeg(angle) * dist);
            if (Pathfinder.isClear(walls, feetOf(enemy.getPosition()), feetOf(candidate))) {
                wanderTarget.set(candidate);
                walking = true;
                return;
            }
        }
        pauseTimer = 0.5f;
    }

    /** Un bruit (tir, mort) attire l'ennemi, sauf s'il a déjà le joueur en vue. */
    void hearNoise(Vector2 at) {
        if (!seesPlayer) alertAt(at);
    }

    /** Alerte l'ennemi et lui indique où aller, même s'il ne voit pas le joueur. */
    void alertAt(Vector2 at) {
        alerted = true;
        alertTimer = LOSE_SIGHT_TIME;
        walking = false;
        path = null;
        repathTimer = 0f;
        lastKnown.set(at);
    }

    /** Court vers le dernier endroit où le joueur a été vu (ou entendu). */
    void chaseLastKnown(float delta, TiledMapTileLayer[] walls) {
        chasing = false;
        if (enemy.getPosition().dst(lastKnown) <= 2f) {
            path = null;
            repathTimer = 0f;
            return;
        }

        repathTimer -= delta;
        if (repathTimer <= 0f || pathGoal.dst(lastKnown) > 8f) {
            path = Pathfinder.findPath(walls, feetOf(enemy.getPosition()), feetOf(lastKnown));
            pathGoal.set(lastKnown);
            repathTimer = REPATH_INTERVAL;
        }
        if (path == null) return;   // aucun chemin : il reste sur place, puis abandonne

        // Prochain point de passage (en coordonnées sprite)
        while (!path.isEmpty() && enemy.getPosition().dst(spriteOf(path.get(0))) < 3f) path.remove(0);
        Vector2 next = path.isEmpty() ? lastKnown : spriteOf(path.get(0));

        Vector2 toNext = new Vector2(next).sub(enemy.getPosition());
        float dist = toNext.len();
        if (dist <= 0.01f) return;
        toNext.nor();
        enemy.getAim().set(toNext);
        float stepLen = Math.min(dist, runSpeed * delta);
        chasing = enemy.moveForAI(toNext.x * stepLen, toNext.y * stepLen, walls);
        if (!chasing) repathTimer = 0f;   // coincé : on recalcule le chemin
    }

    /** Position des pieds correspondant à une position de sprite. */
    private Vector2 feetOf(Vector2 spritePos) {
        return new Vector2(spritePos).add(enemy.feetOffset());
    }

    /** Position de sprite correspondant à une position de pieds. */
    private Vector2 spriteOf(Vector2 feetPos) {
        return new Vector2(feetPos).sub(enemy.feetOffset());
    }

    /** Arrête le déplacement de patrouille et de poursuite. */
    void stopWalking() {
        walking = false;
        chasing = false;
    }
    /** Indique si l’ennemi est alerté. */
    boolean isAlerted() { return alerted; }
    /** Indique si l’ennemi voit actuellement le joueur. */
    boolean seesPlayer() { return seesPlayer; }
    /** Indique si l’ennemi court. */
    boolean isRunning() { return chasing; }
    /** Indique si l’ennemi marche. */
    boolean isWalking() { return walking || chasing; }
    /** Retourne la dernière position connue du joueur. */
    Vector2 lastKnown() { return lastKnown; }
    /** Met à jour l’état de vision du joueur. */
    void setSeesPlayer(boolean value) { seesPlayer = value; }
}
