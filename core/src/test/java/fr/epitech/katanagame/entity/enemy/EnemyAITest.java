package fr.epitech.katanagame.entity.enemy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.math.Vector2;
import org.junit.jupiter.api.Test;

class EnemyAITest {
    private final TestEnemy enemy = new TestEnemy(0f, 0f);
    private final EnemyAI ai = enemy.ai;
    private final Vector2 target = new Vector2(50f, 20f);

    @Test
    void auDepart_pasAlerte() {
        assertFalse(ai.isAlerted());
        assertFalse(ai.seesPlayer());
    }

    @Test
    void voirLeJoueur_alerteEtMemoriseSaPosition() {
        ai.updateAlert(true, target, 0.1f);
        assertTrue(ai.isAlerted());
        assertTrue(ai.seesPlayer());
        assertEquals(50f, ai.lastKnown().x, 0.001f);
    }

    @Test
    void perdreDeVue_garderLAlerteUnMoment_puisLaPerdre() {
        ai.updateAlert(true, target, 0.1f);
        ai.updateAlert(false, target, 1.0f);
        assertTrue(ai.isAlerted());      // 1,5 s de délai
        ai.updateAlert(false, target, 1.0f);
        assertFalse(ai.isAlerted());
    }

    @Test
    void bruit_alerteSiLeJoueurNEstPasVu() {
        ai.hearNoise(new Vector2(10f, 10f));
        assertTrue(ai.isAlerted());
        assertEquals(10f, ai.lastKnown().x, 0.001f);
    }

    @Test
    void bruit_ignoreSiLeJoueurEstVu() {
        ai.updateAlert(true, target, 0.1f);
        ai.hearNoise(new Vector2(10f, 10f));
        assertEquals(50f, ai.lastKnown().x, 0.001f);
    }

    @Test
    void alertAt_forceLAlerteMemeSansVoir() {
        ai.updateAlert(true, target, 0.1f);
        ai.alertAt(new Vector2(-5f, -5f));
        assertEquals(-5f, ai.lastKnown().x, 0.001f);
        assertTrue(ai.isAlerted());
    }

    @Test
    void stopWalking_arreteMarcheEtCourse() {
        ai.stopWalking();
        assertFalse(ai.isWalking());
        assertFalse(ai.isRunning());
    }
}
