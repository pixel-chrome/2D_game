package fr.epitech.katanagame.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.badlogic.gdx.math.Vector2;
import org.junit.jupiter.api.Test;

class DirectionTest {
    @Test
    void axeHorizontalDominant() {
        assertEquals(Direction.RIGHT, Direction.of(new Vector2(1f, 0.5f)));
        assertEquals(Direction.LEFT, Direction.of(new Vector2(-1f, 0.5f)));
    }

    @Test
    void axeVerticalDominant() {
        assertEquals(Direction.UP, Direction.of(new Vector2(0.1f, 2f)));
        assertEquals(Direction.DOWN, Direction.of(new Vector2(0f, -3f)));
    }

    @Test
    void vecteurNul_donneBas() {
        assertEquals(Direction.DOWN, Direction.of(new Vector2(0f, 0f)));
    }

    @Test
    void egalite_departageParLAxeVertical() {
        assertEquals(Direction.UP, Direction.of(new Vector2(1f, 1f)));
        assertEquals(Direction.DOWN, Direction.of(new Vector2(1f, -1f)));
    }
}
