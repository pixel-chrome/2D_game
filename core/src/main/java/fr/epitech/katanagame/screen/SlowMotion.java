package fr.epitech.katanagame.screen;

import com.badlogic.gdx.math.MathUtils;

/**
 * Ralenti à la Katana Zero : maintenir la touche ralentit le monde (ennemis et tirs) et, moins fort, le joueur.
 * Une jauge se vide pendant l'effet et se recharge le reste du temps : l'effet reste court.
 */
final class SlowMotion {
    /** Échelle de temps du monde et du joueur pendant le ralenti. */
    private static final float WORLD_SCALE = 0.2f, PLAYER_SCALE = 0.6f;
    /** Durée maximale d'un ralenti (s), temps de recharge complet (s), jauge minimale pour relancer. */
    private static final float MAX_DURATION = 1.2f, RECHARGE_TIME = 4f, MIN_TO_START = 0.3f;
    /** Vitesse d'entrée/sortie du ralenti (par seconde), pour éviter un à-coup. */
    private static final float BLEND_SPEED = 10f;

    private float charge = 1f;
    private float blend;
    private boolean active;

    /** À appeler une fois par image avec le vrai delta. */
    void update(float delta, boolean wantsSlow) {
        if (wantsSlow && (active || charge >= MIN_TO_START) && charge > 0f) {
            active = true;
            charge = Math.max(0f, charge - delta / MAX_DURATION);
        } else {
            active = false;
            charge = Math.min(1f, charge + delta / RECHARGE_TIME);
        }
        blend = MathUtils.clamp(blend + (active ? 1f : -1f) * BLEND_SPEED * delta, 0f, 1f);
    }

    void reset() {
        charge = 1f;
        blend = 0f;
        active = false;
    }

    /** Échelle de temps des ennemis et des tirs (1 = normal). */
    float worldScale() { return MathUtils.lerp(1f, WORLD_SCALE, blend); }

    /** Échelle de temps du joueur (1 = normal). */
    float playerScale() { return MathUtils.lerp(1f, PLAYER_SCALE, blend); }

    /** Jauge restante, de 0 à 1. */
    float getCharge() { return charge; }

    /** Intensité de l'effet, de 0 (aucun) à 1 (plein ralenti). */
    float getIntensity() { return blend; }
}
