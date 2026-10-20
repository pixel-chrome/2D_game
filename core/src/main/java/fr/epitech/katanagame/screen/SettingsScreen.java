package fr.epitech.katanagame.screen;

import fr.epitech.katanagame.Main;
import fr.epitech.katanagame.settings.GameSettings;
import fr.epitech.katanagame.ui.Theme;
import fr.epitech.katanagame.ui.MenuBackground;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;

import java.util.function.Consumer;

/**
 * Paramètres : volumes et plein écran, sauvegardés automatiquement.
 * 'backdrop' : la partie en cours (quand on arrive depuis la pause), sinon null
 * et on affiche le fond du menu. 'onBack' dit où retourner.
 */
public class SettingsScreen extends ScreenAdapter {
    private final Main game;
    private final GameScreen backdrop;
    private final Runnable onBack;
    private final Stage stage = new Stage(new FitViewport(MenuBackground.W, MenuBackground.H));
    private boolean leaving = false;
    private final java.util.Map<GameSettings.Action, TextButton> keyButtons = new java.util.EnumMap<>(GameSettings.Action.class);
    private GameSettings.Action waiting = null;
    private boolean consumedKey = false;   // la touche vient d'être prise par le réglage : pas de retour

    /**
     * Construit l'écran des paramètres.
     *
     * @param game jeu qui possède l'écran
     * @param backdrop partie à afficher derrière (null : fond du menu)
     * @param onBack action à lancer au retour
     */
    public SettingsScreen(Main game, GameScreen backdrop, Runnable onBack) {
        this.game = game;
        this.backdrop = backdrop;
        this.onBack = onBack;
        Theme t = game.theme;

        Label title = new Label(Theme.spaced("PARAMETRES"), t.titleStyle);

        Label musicValue = new Label("", t.labelStyle);
        Slider music = volumeSlider(GameSettings.musicVolume, musicValue, v -> GameSettings.musicVolume = v);
        Label sfxValue = new Label("", t.labelStyle);
        Slider sfx = volumeSlider(GameSettings.sfxVolume, sfxValue, v -> GameSettings.sfxVolume = v);

        CheckBox fullscreen = new CheckBox("PLEIN ECRAN", t.checkStyle);
        fullscreen.getLabelCell().padLeft(16f);
        fullscreen.setChecked(GameSettings.fullscreen);   // avant le listener, pour ne pas le déclencher
        fullscreen.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                GameSettings.fullscreen = fullscreen.isChecked();
                GameSettings.applyDisplay();
            }
        });

        Table rows = new Table();
        rows.defaults().padBottom(20f);
        rows.add(new Label("MUSIQUE", t.labelStyle)).left().width(150f);
        rows.add(music).width(220f).padRight(16f);
        rows.add(musicValue).left().width(80f).row();
        rows.add(new Label("EFFETS", t.labelStyle)).left().width(150f);
        rows.add(sfx).width(220f).padRight(16f);
        rows.add(sfxValue).left().width(80f).row();
        rows.add(fullscreen).colspan(3).left().row();

        // Touches : clic sur une touche, puis appuyer sur la nouvelle (Échap annule)
        Table keys = new Table();
        keys.defaults().padBottom(8f);
        Label keysTitle = new Label("COMMANDES", t.hintStyle);
        keys.add(keysTitle).left().colspan(2).padBottom(14f).row();
        for (GameSettings.Action a : GameSettings.Action.values()) {
            TextButton kb = t.button(GameSettings.keyName(a), true);
            kb.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    waiting = a;
                    kb.setText("...");
                }
            });
            keyButtons.put(a, kb);
            keys.add(new Label(a.label, t.labelStyle)).left().width(150f);
            keys.add(kb).width(200f).height(36f).left().row();
        }
        TextButton reset = t.button("PAR DEFAUT", false);
        reset.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                GameSettings.resetKeys();
                waiting = null;
                refreshKeys();
            }
        });
        keys.add(reset).colspan(2).left().width(300f).height(36f).row();

        TextButton back = t.button("RETOUR", false);
        back.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                goBack();
            }
        });
        Label hint = new Label("ECHAP : RETOUR", t.hintStyle);

        // Colonne gauche : son et affichage
        Label audioTitle = new Label("SON ET AFFICHAGE", t.hintStyle);
        Table left = new Table();
        left.top().left();
        left.add(audioTitle).left().padBottom(14f).row();
        left.add(rows).left().row();

        Table columns = new Table();
        columns.top().left();
        columns.add(left).top().left().padRight(90f);
        columns.add(keys).top().left();

        Table root = new Table();
        root.setFillParent(true);
        root.top().left().pad(50f, 50f, 30f, 60f);
        root.add(title).left().padBottom(30f).row();
        root.add(columns).left().expandY().top().row();
        Table footer = new Table();
        footer.add(back).width(240f).height(44f).left();
        footer.add(hint).padLeft(30f);
        root.add(footer).left();
        stage.addActor(root);
    }

    private Slider volumeSlider(float initial, Label valueLabel, Consumer<Float> setter) {
        Slider slider = new Slider(0f, 1f, 0.05f, false, game.theme.sliderStyle);
        slider.setValue(initial);   // avant le listener
        valueLabel.setText(Math.round(initial * 100f) + "%");
        slider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                setter.accept(slider.getValue());
                valueLabel.setText(Math.round(slider.getValue() * 100f) + "%");
            }
        });
        return slider;
    }

    private void refreshKeys() {
        for (GameSettings.Action a : GameSettings.Action.values()) keyButtons.get(a).setText(GameSettings.keyName(a));
    }

    private void goBack() {
        if (leaving) return;
        leaving = true;
        GameSettings.save();
        Gdx.app.postRunnable(onBack);   // jamais détruire l'écran pendant un clic
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(new InputMultiplexer(new InputAdapter() {
            @Override
            public boolean keyDown(int keycode) {
                if (waiting == null) return false;
                consumedKey = true;
                if (keycode != Input.Keys.ESCAPE) GameSettings.setKey(waiting, keycode);
                waiting = null;
                refreshKeys();
                return true;
            }
        }, stage));
        stage.getRoot().getColor().a = 0f;
        stage.getRoot().addAction(Actions.fadeIn(0.3f));
    }

    @Override
    public void render(float delta) {
        if (!consumedKey && waiting == null && Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) goBack();
        consumedKey = false;

        ScreenUtils.clear(0f, 0f, 0f, 1f);
        if (backdrop != null) {
            // arrivé depuis la pause : la partie figée reste visible derrière
            backdrop.drawFrame();
            stage.getViewport().apply();
            game.film.draw(delta, stage.getCamera().combined, 0.7f, 1f);
        } else {
            stage.getViewport().apply();
            game.background.render(delta, stage.getCamera().combined);
        }
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
        if (backdrop != null) backdrop.resize(width, height);
    }

    @Override
    public void dispose() {
        stage.dispose();
    }
}
