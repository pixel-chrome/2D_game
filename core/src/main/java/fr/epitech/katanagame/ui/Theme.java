package fr.epitech.katanagame.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.BaseDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Disposable;

/**
 * Style des menus : minimaliste, presque monochrome (gris cendre, blanc os),
 * avec une seule touche de rouge sombre pour la sélection. Pas de cadres ni de
 * plaques : du texte, une fine barre au survol, des traits fins.
 * Tout est généré en code, aucun fichier d'image n'est nécessaire.
 */
public class Theme implements Disposable {
    public static final Color INK   = Color.valueOf("0b0c0e");
    public static final Color ASH   = Color.valueOf("7d8084");
    public static final Color BONE  = Color.valueOf("dcd8cc");
    public static final Color BLOOD = Color.valueOf("9b1c1c");

    /** Les polices sont générées en 2x puis réduites de moitié : nettes même en plein écran. */
    private static final float FONT_SCALE = 0.5f;
    private static final String CHARS = FreeTypeFontGenerator.DEFAULT_CHARS + "éèêëàâäçùûüôöîïÉÈÀÇœ°";

    public final BitmapFont font;
    public final BitmapFont smallFont;
    public final BitmapFont titleFont;

    public final TextButton.TextButtonStyle buttonStyle = new TextButton.TextButtonStyle();
    public final Label.LabelStyle labelStyle;
    public final Label.LabelStyle titleStyle;
    public final Label.LabelStyle hintStyle;
    public final Slider.SliderStyle sliderStyle;
    public final CheckBox.CheckBoxStyle checkStyle;

    private final Texture whiteTex;
    private final TextureRegion white;

    public Theme() {
        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(Color.WHITE);
        pm.fill();
        whiteTex = new Texture(pm);
        pm.dispose();
        white = new TextureRegion(whiteTex);

        // Polices vectorielles (FreeType) : bords lissés, pas de flou d'agrandissement
        FreeTypeFontGenerator gen = new FreeTypeFontGenerator(Gdx.files.internal("fonts/menu.ttf"));
        font = makeFont(gen, 48);
        smallFont = makeFont(gen, 34);
        titleFont = makeFont(gen, 100);
        gen.dispose();

        // Boutons : du texte gris qui s'éclaire au survol, avec une fine barre rouge à gauche
        buttonStyle.font = font;
        buttonStyle.fontColor = ASH;
        buttonStyle.overFontColor = BONE;
        buttonStyle.downFontColor = BLOOD;
        buttonStyle.over = new Flat(white, BLOOD, Flat.Mode.LEFT_BAR);

        labelStyle = new Label.LabelStyle(font, BONE);
        titleStyle = new Label.LabelStyle(titleFont, BONE);
        hintStyle = new Label.LabelStyle(smallFont, ASH);

        // Curseur : un trait fin et un petit repère
        Flat track = new Flat(white, new Color(ASH.r, ASH.g, ASH.b, 0.45f), Flat.Mode.FILL);
        track.setMinHeight(2f);
        Flat knob = new Flat(white, BONE, Flat.Mode.FILL);
        knob.setMinWidth(8f);
        knob.setMinHeight(24f);
        Flat before = new Flat(white, BONE, Flat.Mode.FILL);
        before.setMinHeight(2f);
        sliderStyle = new Slider.SliderStyle(track, knob);
        sliderStyle.knobBefore = before;

        // Case à cocher : un carré en contour, rempli quand elle est cochée
        Flat off = new Flat(white, ASH, Flat.Mode.FRAME);
        off.setMinWidth(26f);
        off.setMinHeight(26f);
        Flat on = new Flat(white, BONE, Flat.Mode.FRAME_FILLED);
        on.setMinWidth(26f);
        on.setMinHeight(26f);
        checkStyle = new CheckBox.CheckBoxStyle(off, on, font, BONE);
    }

    private static BitmapFont makeFont(FreeTypeFontGenerator gen, int size) {
        FreeTypeFontGenerator.FreeTypeFontParameter p = new FreeTypeFontGenerator.FreeTypeFontParameter();
        p.size = size;
        p.characters = CHARS;
        p.minFilter = Texture.TextureFilter.Linear;
        p.magFilter = Texture.TextureFilter.Linear;
        p.hinting = FreeTypeFontGenerator.Hinting.Slight;
        BitmapFont f = gen.generateFont(p);
        f.getData().setScale(FONT_SCALE);
        return f;
    }

    /** Bouton de menu : texte aligné à gauche (ou centré). */
    public TextButton button(String text, boolean centered) {
        TextButton b = new TextButton(text, buttonStyle);
        b.getLabel().setAlignment(centered ? Align.center : Align.left);
        if (!centered) b.getLabelCell().padLeft(18f);
        return b;
    }

    /** "LAME D'AUBE" devient "L A M E   D ' A U B E" : un titre aéré, plus cinématographique. */
    public static String spaced(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == ' ') {
                sb.append("   ");
            } else {
                sb.append(c);
                if (i < s.length() - 1 && s.charAt(i + 1) != ' ') sb.append(' ');
            }
        }
        return sb.toString();
    }

    @Override
    public void dispose() {
        font.dispose();
        smallFont.dispose();
        titleFont.dispose();
        whiteTex.dispose();
    }

    /** Petit dessin plat (aplat, barre, contour) à partir d'un pixel blanc teinté. */
    static class Flat extends BaseDrawable {
        enum Mode { FILL, LEFT_BAR, FRAME, FRAME_FILLED }

        private static final float T = 2f;   // épaisseur des traits

        private final TextureRegion region;
        private final Color color;
        private final Mode mode;

        Flat(TextureRegion region, Color color, Mode mode) {
            this.region = region;
            this.color = color;
            this.mode = mode;
        }

        @Override
        public void draw(Batch batch, float x, float y, float w, float h) {
            float packed = batch.getPackedColor();
            float alpha = batch.getColor().a;
            batch.setColor(color.r, color.g, color.b, color.a * alpha);
            switch (mode) {
                case FILL:
                    batch.draw(region, x, y, w, h);
                    break;
                case LEFT_BAR:
                    batch.draw(region, x, y + 6f, 3f, h - 12f);
                    break;
                case FRAME:
                    frame(batch, x, y, w, h);
                    break;
                case FRAME_FILLED:
                    frame(batch, x, y, w, h);
                    batch.draw(region, x + 6f, y + 6f, w - 12f, h - 12f);
                    break;
            }
            batch.setPackedColor(packed);
        }

        private void frame(Batch batch, float x, float y, float w, float h) {
            batch.draw(region, x, y, w, T);
            batch.draw(region, x, y + h - T, w, T);
            batch.draw(region, x, y, T, h);
            batch.draw(region, x + w - T, y, T, h);
        }
    }
}
