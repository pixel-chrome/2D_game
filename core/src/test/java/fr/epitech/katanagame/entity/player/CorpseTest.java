package fr.epitech.katanagame.entity.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.math.Vector2;
import org.junit.jupiter.api.Test;

class CorpseTest {
    private final Corpse corpse = new Corpse();

    @Test
    void tombeDuCoteOppose_auTir() {
        corpse.fall(new Vector2(1f, 0f));
        assertEquals(-1f, corpse.side(), 0.001f);
        corpse.fall(new Vector2(-1f, 0f));
        assertEquals(1f, corpse.side(), 0.001f);
    }

    @Test
    void glisse_dansLeSensDuTir_enRalentissant() {
        corpse.fall(new Vector2(1f, 0f));
        float first = corpse.advance(0.05f).x;
        float second = corpse.advance(0.05f).x;
        assertTrue(first > 0f);
        assertTrue(second < first);
    }

    @Test
    void leTempsAvance() {
        corpse.fall(new Vector2(1f, 0f));
        corpse.advance(0.25f);
        assertEquals(0.25f, corpse.time(), 0.001f);
    }

    @Test
    void reset_arreteLaGlissade() {
        corpse.fall(new Vector2(1f, 0f));
        corpse.reset();
        assertEquals(0f, corpse.advance(0.05f).len(), 0.001f);
    }
}
