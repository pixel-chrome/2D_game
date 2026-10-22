package fr.epitech.katanagame.entity.enemy;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import fr.epitech.katanagame.world.World;

/** Une attaque du boss : annonce (windup), action (active), puis récupération (recover). Les durées sont en secondes. */
abstract class BossAttack {
    final float windup, active, recover;

    BossAttack(float windup, float active, float recover) {
        this.windup = windup;
        this.active = active;
        this.recover = recover;
    }

    /** Vrai si la visée est figée au début de l'annonce (le joueur peut alors esquiver sur le côté). */
    boolean lockAim() { return false; }

    /** Début de l'annonce. */
    void begin(FinalBoss boss) {}

    /** Début de l'action. */
    void fire(FinalBoss boss, World world) {}

    /** Chaque image de l'action ; elapsed = temps écoulé depuis fire. */
    void tick(FinalBoss boss, float delta, float elapsed, World world) {}

    /** Annonce dessinée pendant l'annonce ; progress va de 0 à 1. */
    void drawTelegraph(ShapeRenderer shapes, FinalBoss boss, float progress) {}
}
