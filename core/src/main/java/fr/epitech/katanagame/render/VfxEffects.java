package fr.epitech.katanagame.render;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import java.util.ArrayList;
import java.util.List;

/** Effets visuels ponctuels (explosions, étincelles, ondes de choc), dessinés au-dessus des sprites. */
public final class VfxEffects {
    private static final class Instance {
        final Vfx.Kind kind;
        final float x, y, size;
        float delay, age;

        Instance(Vfx.Kind kind, float x, float y, float size, float delay) {
            this.kind = kind;
            this.x = x;
            this.y = y;
            this.size = size;
            this.delay = delay;
        }
    }

    private final List<Instance> active = new ArrayList<>();

    /** Crée un gestionnaire vide. */
    public VfxEffects() {}

    /**
     * Lance un effet.
     *
     * @param kind effet
     * @param x abscisse du centre
     * @param y ordonnée du centre
     * @param size côté dessiné (px)
     */
    public void spawn(Vfx.Kind kind, float x, float y, float size) {
        spawn(kind, x, y, size, 0f);
    }

    /**
     * Lance un effet après un délai.
     *
     * @param kind effet
     * @param x abscisse du centre
     * @param y ordonnée du centre
     * @param size côté dessiné (px)
     * @param delay attente avant le début (s)
     */
    public void spawn(Vfx.Kind kind, float x, float y, float size, float delay) {
        active.add(new Instance(kind, x, y, size, delay));
    }

    /**
     * Fait avancer les effets et retire ceux qui sont finis.
     *
     * @param delta temps écoulé (s)
     */
    public void update(float delta) {
        for (Instance i : active) {
            if (i.delay > 0f) i.delay -= delta;
            else i.age += delta;
        }
        active.removeIf(i -> i.age >= Vfx.duration());
    }

    /**
     * Dessine les effets en cours.
     *
     * @param batch batch déjà ouvert
     */
    public void draw(SpriteBatch batch) {
        for (Instance i : active) {
            if (i.delay <= 0f) Vfx.draw(batch, i.kind, i.x, i.y, i.size, i.age, 1f);
        }
    }

    /** Efface tous les effets (recommencer une partie). */
    public void clear() { active.clear(); }
}
