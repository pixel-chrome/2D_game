package fr.epitech.katanagame.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.math.Rectangle;
import org.junit.jupiter.api.Test;

class DoorTest {
    private final Door door = new Door(new Rectangle(0f, 0f, 10f, 10f), "maps/etage_2.tmx", "entry_a", true);

    @Test
    void exposeSaCibleEtSesConditions() {
        assertEquals("maps/etage_2.tmx", door.getTargetMap());
        assertEquals("entry_a", door.getTargetEntry());
        assertTrue(door.requiresClear());
    }

    @Test
    void touchee_siUnRectangleLaChevauche() {
        assertTrue(door.isTouchedBy(new Rectangle(5f, 5f, 10f, 10f)));
    }

    @Test
    void pasTouchee_siLeRectangleEstLoin() {
        assertFalse(door.isTouchedBy(new Rectangle(50f, 50f, 10f, 10f)));
    }
}
