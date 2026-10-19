package fr.epitech.katanagame.screen;

/**
 * Déroulé de la mort du joueur : arrêt sur image à l'impact, puis ralenti du monde
 * (ennemis et tirs) jusqu'à l'écran de game over. Le sang et le corps restent en temps réel.
 */
final class DeathSequence {
    /** Durée de l'arrêt sur image, ralenti du monde, et délai avant le game over (s). */
    private static final float HIT_STOP = 0.06f, SLOWMO = 0.25f, GAME_OVER_DELAY = 0.45f;

    private float elapsed;
    private float hitStop;
    private boolean frozen;

    /** Lance la séquence au moment où le joueur est touché. */
    void start() {
        hitStop = HIT_STOP;
    }

    void reset() {
        elapsed = 0f;
        hitStop = 0f;
        frozen = false;
    }

    /** À appeler une fois par image tant que le joueur est mort. */
    void advance(float delta) {
        elapsed += delta;
        frozen = hitStop > 0f;
        if (frozen) hitStop -= delta;
    }

    /** Échelle de temps du sang et du corps : à l'arrêt pendant l'impact seulement. */
    float effectsScale() { return frozen ? 0f : 1f; }

    /** Échelle de temps des ennemis et des tirs. */
    float worldScale() { return frozen ? 0f : SLOWMO; }

    /** Vrai quand il est temps d'afficher le game over. */
    boolean isOver() { return elapsed >= GAME_OVER_DELAY; }
}
