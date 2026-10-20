package fr.epitech.katanagame.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.utils.Disposable;

/** Viseur qui remplace le curseur de la souris pendant la partie : anneau, quatre traits et point central. */
public final class Crosshair implements Disposable {
    private static final int SIZE = 32, C = SIZE / 2;
    private static final int RING = 8;

    private final Cursor cursor;

    /** Dessine le viseur (liseré sombre sous le blanc pour qu'il reste visible sur tous les fonds). */
    public Crosshair() {
        Pixmap pm = new Pixmap(SIZE, SIZE, Pixmap.Format.RGBA8888);
        draw(pm, new Color(0f, 0f, 0f, 0.85f), 1);
        draw(pm, Color.WHITE, 0);
        cursor = Gdx.graphics.newCursor(pm, C, C);
        pm.dispose();
    }

    private static void draw(Pixmap pm, Color color, int grow) {
        pm.setColor(color);
        for (int d = -RING - grow; d <= RING + grow; d++) {          // anneau fait de pixels
            for (int e = -RING - grow; e <= RING + grow; e++) {
                double r = Math.sqrt(d * d + e * e);
                if (r >= RING - grow && r <= RING + grow) pm.drawPixel(C + d, C + e);
            }
        }
        for (int i = 3; i <= 13; i++) {                              // traits qui dépassent de l'anneau
            if (i > RING - 3 && i < RING + 1) continue;
            for (int w = -grow; w <= grow; w++) {
                pm.drawPixel(C + i, C + w);
                pm.drawPixel(C - i, C + w);
                pm.drawPixel(C + w, C + i);
                pm.drawPixel(C + w, C - i);
            }
        }
        pm.fillRectangle(C - 1 - grow, C - 1 - grow, 2 + 2 * grow, 2 + 2 * grow);   // point central
    }

    /** Remplace le curseur de la fenêtre par le viseur. */
    public void apply() { Gdx.graphics.setCursor(cursor); }

    /** Remet la flèche normale (menus, pause). */
    public static void restore() { Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Arrow); }

    @Override public void dispose() { cursor.dispose(); }
}
