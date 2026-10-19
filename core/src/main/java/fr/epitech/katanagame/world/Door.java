package fr.epitech.katanagame.world;

import com.badlogic.gdx.math.Rectangle;

/** Une porte : une zone de la map qui envoie le joueur vers une entrée d'un autre étage. */
public final class Door {
    private final Rectangle area;
    private final String targetMap;      // ex : "maps/etage_2.tmx"
    private final String targetEntry;    // nom de l'objet d'arrivée dans la map cible
    private final boolean requiresClear; // vrai : la porte ne s'ouvre que si tous les ennemis sont morts

    public Door(Rectangle area, String targetMap, String targetEntry, boolean requiresClear) {
        this.area = area;
        this.targetMap = targetMap;
        this.targetEntry = targetEntry;
        this.requiresClear = requiresClear;
    }

    public boolean isTouchedBy(Rectangle other) {
        return area.overlaps(other);
    }

    public Rectangle getArea() {
        return area;
    }

    public String getTargetMap() {
        return targetMap;
    }

    public String getTargetEntry() {
        return targetEntry;
    }

    public boolean requiresClear() {
        return requiresClear;
    }
}