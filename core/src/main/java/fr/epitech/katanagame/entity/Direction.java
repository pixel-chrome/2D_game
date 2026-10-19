package fr.epitech.katanagame.entity;

import com.badlogic.gdx.math.Vector2;

/** Les 4 directions de sprite, partagées par le joueur et les ennemis. */
public enum Direction {
    UP, DOWN, LEFT, RIGHT;

    /** Direction dominante d'un vecteur. */
    public static Direction of(Vector2 v) {
        if (Math.abs(v.x) > Math.abs(v.y)) {
            return v.x > 0 ? RIGHT : LEFT;
        }
        return v.y > 0 ? UP : DOWN;
    }
}
