package fr.epitech.katanagame.screen;

/** Arrêt sur image après un coup ou un parry qui touche, et ralenti du monde qui suit un parry. */
final class HitStop {
    private static final float PARRY_FREEZE = 0.09f, ATTACK_FREEZE = 0.06f;
    /** Ralenti du monde juste après un parry réussi (durée en s, échelle de temps). */
    private static final float PARRY_SLOW_TIME = 0.35f, PARRY_SLOW_SCALE = 0.3f;

    private float freeze, slow;

    /** Un parry a réussi : arrêt sur image puis ralenti. */
    void onParry() {
        freeze = PARRY_FREEZE;
        slow = PARRY_SLOW_TIME;
    }

    /** Un coup a touché : arrêt sur image un peu plus court. */
    void onAttackHit() {
        freeze = Math.max(freeze, ATTACK_FREEZE);
    }

    /** Fait avancer l'arrêt sur image (temps réel) ; vrai tant que tout doit rester figé. */
    boolean isFrozen(float delta) {
        if (freeze <= 0f) return false;
        freeze -= delta;
        return true;
    }

    /** Échelle de temps du monde (1 = normal) ; le temps réel fait avancer le ralenti. */
    float worldScale(float delta) {
        if (slow <= 0f) return 1f;
        slow -= delta;
        return PARRY_SLOW_SCALE;
    }

    void reset() {
        freeze = slow = 0f;
    }
}
