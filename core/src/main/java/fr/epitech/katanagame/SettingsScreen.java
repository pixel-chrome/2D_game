package fr.epitech.katanagame;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
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
 * Écran des paramètres : volumes et plein écran, sauvegardés automatiquement.
 * 'onBack' dit où retourner (menu principal, ou menu pause plus tard).
 */
public class SettingsScreen extends ScreenAdapter {
    private final Main game;
    private final Runnable onBack;
    private final Stage stage = new Stage(new FitViewport(MenuBackground.W, MenuBackground.H));
    private boolean leaving = false;

    public SettingsScreen(Main game, Runnable onBack) {
        this.game = game;
        this.onBack = onBack;
        Theme t = game.theme;

        Label title = new Label("PARAMETRES", t.titleStyle);
        title.setColor(Theme.BONE);

        Label musicValue = new Label("", t.labelStyle);
        Slider music = volumeSlider(GameSettings.musicVolume, musicValue, v -> GameSettings.musicVolume = v);
        Label sfxValue = new Label("", t.labelStyle);
        Slider sfx = volumeSlider(GameSettings.sfxVolume, sfxValue, v -> GameSettings.sfxVolume = v);

        CheckBox fullscreen = new CheckBox("PLEIN ECRAN", t.checkStyle);
        fullscreen.getLabelCell().padLeft(14f);
        fullscreen.setChecked(GameSettings.fullscreen);   // avant le listener, pour ne pas le déclencher
        fullscreen.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                GameSettings.fullscreen = fullscreen.isChecked();
                GameSettings.applyDisplay();
            }
        });

        Table panel = new Table();
        panel.setBackground(t.panel);
        panel.pad(30f, 40f, 30f, 40f);
        panel.defaults().pad(12f);
        panel.add(new Label("VOLUME MUSIQUE", t.labelStyle)).left();
        panel.add(music).width(280f);
        panel.add(musicValue).width(90f).right().row();
        panel.add(new Label("VOLUME EFFETS", t.labelStyle)).left();
        panel.add(sfx).width(280f);
        panel.add(sfxValue).width(90f).right().row();
        panel.add(fullscreen).colspan(3).left().row();

        TextButton back = new TextButton("RETOUR", t.buttonStyle);
        back.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                goBack();
            }
        });

        Label hint = new Label("ECHAP : RETOUR", t.hintStyle);

        Table root = new Table();
        root.setFillParent(true);
        root.add(title).padBottom(30f).row();
        root.add(panel).padBottom(30f).row();
        root.add(back).width(300f).height(52f).padBottom(14f).row();
        root.add(hint);
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

    private void goBack() {
        if (leaving) return;
        leaving = true;
        GameSettings.save();
        Gdx.app.postRunnable(onBack);   // jamais détruire l'écran pendant un clic
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
        stage.getRoot().getColor().a = 0f;
        stage.getRoot().addAction(Actions.fadeIn(0.3f));
    }

    @Override
    public void render(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) goBack();

        ScreenUtils.clear(0f, 0f, 0f, 1f);
        stage.getViewport().apply();
        game.background.render(delta, stage.getCamera().combined);
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void dispose() {
        stage.dispose();
    }
}
