package fr.epitech.katanagame.entity.enemy;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import fr.epitech.katanagame.world.World;

/** Ennemi minimal pour les tests : il ne fait rien et ne dessine rien. */
public class TestEnemy extends Enemy {
    /** Vitesse de course arbitraire. */
    private static final float RUN_SPEED = 50f;

    /**
     * Crée un ennemi de test.
     *
     * @param x abscisse initiale
     * @param y ordonnée initiale
     */
    public TestEnemy(float x, float y) {
        super(x, y, RUN_SPEED);
    }

    @Override
    protected void think(float delta, World world) {}

    @Override
    public void draw(SpriteBatch batch) {}

    /**
     * Indique si l'IA de cet ennemi est en alerte.
     *
     * @return vrai si l'ennemi est alerté
     */
    public boolean alerted() {
        return ai.isAlerted();
    }

    /**
     * Retourne le dernier endroit connu du joueur.
     *
     * @return le dernier endroit connu
     */
    public Vector2 lastKnown() {
        return ai.lastKnown();
    }
}
