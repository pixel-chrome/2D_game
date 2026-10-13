package fr.epitech.katanagame;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Disposable;

import java.util.ArrayList;
import java.util.List;

/**
 * Palette et styles d'interface : « médiéval futuriste »
 * (acier sombre, bronze, braises orange, lueur cyan des machines).
 * Tout est généré en code : aucun fichier d'image nécessaire.
 */
public class Theme implements Disposable {
    public static final Color SLATE  = Color.valueOf("12161c");
    public static final Color STEEL  = Color.valueOf("232b36");
    public static final Color BRONZE = Color.valueOf("b87333");
    public static final Color EMBER  = Color.valueOf("e8833a");
    public static final Color CYAN   = Color.valueOf("3fd0e0");
    public static final Color TEAL   = Color.valueOf("1f7f8a");
    public static final Color BONE   = Color.valueOf("e9e1cf");

    public final BitmapFont font = new BitmapFont();
    public final BitmapFont titleFont = new BitmapFont();

    public final TextButton.TextButtonStyle buttonStyle = new TextButton.TextButtonStyle();
    public final Label.LabelStyle labelStyle;
    public final Label.LabelStyle titleStyle;
    public final Label.LabelStyle hintStyle;
    public final Slider.SliderStyle sliderStyle;
    public final CheckBox.CheckBoxStyle checkStyle;
    public final Drawable panel;

    private final List<Texture> textures = new ArrayList<>();

    public Theme() {
        // Polices : la police par défaut, agrandie, pour un rendu « pixel »
        font.getData().setScale(2f);
        titleFont.getData().setScale(5f);
        font.getRegion().getTexture().setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        titleFont.getRegion().getTexture().setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

        // Boutons : plaques d'acier bordées de bronze, lueur cyan au survol
        buttonStyle.font = font;
        buttonStyle.fontColor = BONE;
        buttonStyle.overFontColor = CYAN;
        buttonStyle.downFontColor = EMBER;
        buttonStyle.up = plate(STEEL, BRONZE);
        buttonStyle.over = plate(new Color(0.16f, 0.26f, 0.31f, 1f), CYAN);
        buttonStyle.down = plate(SLATE, EMBER);

        // Textes
        labelStyle = new Label.LabelStyle(font, BONE);
        titleStyle = new Label.LabelStyle(titleFont, Color.WHITE); // la couleur est animée sur l'acteur
        hintStyle = new Label.LabelStyle(font, new Color(BONE.r, BONE.g, BONE.b, 0.65f));

        // Panneau de fond
        panel = plate(new Color(0.07f, 0.09f, 0.11f, 0.9f), BRONZE);

        // Curseur de volume
        NinePatchDrawable track = plate(SLATE, BRONZE);
        track.setMinHeight(14f);
        TextureRegionDrawable knob = solid(CYAN);
        knob.setMinWidth(14f);
        knob.setMinHeight(32f);
        TextureRegionDrawable filled = solid(TEAL);
        filled.setMinHeight(6f);
        sliderStyle = new Slider.SliderStyle(track, knob);
        sliderStyle.knobBefore = filled;

        // Case à cocher
        NinePatchDrawable off = plate(SLATE, BRONZE);
        off.setMinWidth(30f);
        off.setMinHeight(30f);
        NinePatchDrawable on = plate(CYAN, BRONZE);
        on.setMinWidth(30f);
        on.setMinHeight(30f);
        checkStyle = new CheckBox.CheckBoxStyle(off, on, font, BONE);
    }

    /** Petite plaque (bordure + remplissage) étirable sans déformer la bordure. */
    private NinePatchDrawable plate(Color fill, Color border) {
        Pixmap pm = new Pixmap(16, 16, Pixmap.Format.RGBA8888);
        pm.setColor(border);
        pm.fill();
        pm.setColor(fill);
        pm.fillRectangle(2, 2, 12, 12);
        Texture tex = new Texture(pm);
        tex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        pm.dispose();
        textures.add(tex);
        return new NinePatchDrawable(new NinePatch(tex, 4, 4, 4, 4));
    }

    /** Aplat d'une seule couleur. */
    private TextureRegionDrawable solid(Color color) {
        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(color);
        pm.fill();
        Texture tex = new Texture(pm);
        pm.dispose();
        textures.add(tex);
        return new TextureRegionDrawable(new TextureRegion(tex));
    }

    @Override
    public void dispose() {
        font.dispose();
        titleFont.dispose();
        for (Texture t : textures) t.dispose();
        textures.clear();
    }
}
