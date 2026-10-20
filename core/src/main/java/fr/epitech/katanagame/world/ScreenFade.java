package fr.epitech.katanagame.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Disposable;

/**
 * Fondu au noir : l'écran devient noir, une action est exécutée (changer de map...),
 * puis l'écran se rallume.
 */
public final class ScreenFade implements Disposable {
    /** Crée un fondu inactif. */
    public ScreenFade() {}

    private enum Phase { NONE, FADING_OUT, FADING_IN }

    private static final float MAX_DELTA = 1f / 30f;   // un chargement long ne doit pas "manger" le fondu

    private final ShapeRenderer shapes = new ShapeRenderer();
    private final Matrix4 projection = new Matrix4().setToOrtho2D(0f, 0f, 1f, 1f);

    private Phase phase = Phase.NONE;
    private float time = 0f;
    private float duration = 0.4f;
    private Runnable onBlack;

    /**
     * Lance le fondu.
     * @param duration durée de CHAQUE moitié (noircir, puis éclaircir)
     * @param onBlack  exécuté quand l'écran est entièrement noir
     */
    public void start(float duration, Runnable onBlack) {
        if (isActive()) return;
        this.duration = duration;
        this.onBlack = onBlack;
        this.phase = Phase.FADING_OUT;
        this.time = 0f;
    }

    /**
     * Indique si un fondu est en cours.
     *
     * @return vrai pendant le fondu
     */
    public boolean isActive() {
        return phase != Phase.NONE;
    }

    /**
     * Fait avancer le fondu.
     *
     * @param delta temps réel écoulé (s)
     */
    public void update(float delta) {
        if (phase == Phase.NONE) return;

        time += Math.min(delta, MAX_DELTA);
        if (time < duration) return;

        if (phase == Phase.FADING_OUT) {
            Runnable action = onBlack;
            onBlack = null;
            phase = Phase.FADING_IN;
            time = 0f;
            if (action != null) action.run();
        } else {
            phase = Phase.NONE;
        }
    }

    /** À appeler en DERNIER dans render() : recouvre toute la fenêtre. */
    public void draw() {
        if (phase == Phase.NONE) return;

        float progress = Math.min(1f, time / duration);
        float alpha = (phase == Phase.FADING_OUT) ? progress : 1f - progress;

        Gdx.gl.glViewport(0, 0, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.setProjectionMatrix(projection);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0f, 0f, 0f, alpha);
        shapes.rect(0f, 0f, 1f, 1f);
        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    @Override
    public void dispose() {
        shapes.dispose();
    }
}