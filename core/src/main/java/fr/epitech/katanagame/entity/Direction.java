package fr.epitech.katanagame.entity;

import com.badlogic.gdx.math.Vector2;

/** Les 4 directions de sprite, partagées par le joueur et les ennemis. */
public enum Direction {
    /** Vers le haut de l'écran. */
    UP,
    /** Vers le bas de l'écran. */
    DOWN,
    /** Vers la gauche. */
    LEFT,
    /** Vers la droite. */
    RIGHT;

    /**
     * Direction dominante d'un vecteur.
     *
     * @param v vecteur à analyser
     * @return la direction de l'axe dominant (BAS si le vecteur est nul)
     */
    public static Direction of(Vector2 v) {
        if (Math.abs(v.x) > Math.abs(v.y)) {
            return v.x > 0 ? RIGHT : LEFT;
        }
        return v.y > 0 ? UP : DOWN;
    }
}
