package fr.epitech.katanagame.render;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

/** Effet visuel dessiné en formes pleines au-dessus des sprites (parry, coup de sabre). */
public interface ShapeEffect {
    /** Fait avancer l'effet (temps réel : il continue pendant l'arrêt sur image). */
    void update(float delta);

    /** Dessine l'effet (ShapeRenderer en mode Filled). */
    void draw(ShapeRenderer shapes);

    /** Efface l'effet (recommencer une partie). */
    void clear();
}
