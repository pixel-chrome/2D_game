package fr.epitech.katanagame.world;

import com.badlogic.gdx.math.Rectangle;

/** Une porte : une zone de la map qui envoie le joueur vers une entrée d'un autre étage. */
public final class Door {
    private final Rectangle area;
    private final String targetMap;      // ex : "maps/etage_2.tmx"
    private final String targetEntry;    // nom de l'objet d'arrivée dans la map cible
    private final boolean requiresClear; // vrai : la porte ne s'ouvre que si tous les ennemis sont morts

    /**
     * Crée une porte.
     *
     * @param area zone de la porte
     * @param targetMap carte de destination (ex : maps/etage_2.tmx)
     * @param targetEntry nom de l'entrée d'arrivée dans la carte cible
     * @param requiresClear vrai si la porte ne s'ouvre que lorsque tous les ennemis sont morts
     */
    public Door(Rectangle area, String targetMap, String targetEntry, boolean requiresClear) {
        this.area = area;
        this.targetMap = targetMap;
        this.targetEntry = targetEntry;
        this.requiresClear = requiresClear;
    }

    /**
     * Teste si un rectangle touche la porte.
     *
     * @param other rectangle à tester
     * @return vrai en cas de chevauchement
     */
    public boolean isTouchedBy(Rectangle other) {
        return area.overlaps(other);
    }

    /**
     * Zone de la porte.
     *
     * @return la zone de la porte
     */
    public Rectangle getArea() {
        return area;
    }

    /**
     * Carte vers laquelle mène la porte.
     *
     * @return le chemin de la carte cible
     */
    public String getTargetMap() {
        return targetMap;
    }

    /**
     * Entrée d'arrivée dans la carte cible.
     *
     * @return le nom de l'entrée
     */
    public String getTargetEntry() {
        return targetEntry;
    }

    /**
     * Indique si la porte exige que tous les ennemis soient morts.
     *
     * @return vrai si la porte peut être verrouillée
     */
    public boolean requiresClear() {
        return requiresClear;
    }
}