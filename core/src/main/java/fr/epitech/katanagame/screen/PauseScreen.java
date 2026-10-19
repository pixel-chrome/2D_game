package fr.epitech.katanagame.screen;

import fr.epitech.katanagame.Main;
import fr.epitech.katanagame.ui.Theme;
import fr.epitech.katanagame.ui.MenuBackground;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;

/**
 * Menu pause : la partie reste visible, figée, sous un voile sombre avec
 * vignette. La partie (GameScreen) n'est ni détruite ni modifiée :
 * « Reprendre » la remet simplement à l'écran.
 */
public class PauseScreen extends ScreenAdapter {
    private final Main game;
    private final GameScreen level;
    private final Stage stage = new Stage(new FitViewport(MenuBackground.W, MenuBackground.H));
    private boolean leaving = false;

    public PauseScreen(Main game, GameScreen level) {
        this.game = game;
        this.level = level;
        Theme t = game.theme;

        Label title = new Label(Theme.spaced("PAUSE"), t.titleStyle);
        Label hint = new Label("ECHAP : REPRENDRE", t.hintStyle);

        TextButton resume = button("REPRENDRE", () -> game.switchTo(level));
        TextButton restart = button("RECOMMENCER", () -> {
            level.restart();
            game.switchTo(level);
        });
        TextButton settings = button("PARAMETRES", () ->
                game.switchTo(new SettingsScreen(game, level, () -> game.switchTo(new PauseScreen(game, level)))));
        TextButton menu = button("MENU PRINCIPAL", () -> {
            level.dispose();                           // on quitte vraiment la partie
            game.switchTo(new MainMenuScreen(game));
        });

        Table root = new Table();
        root.setFillParent(true);
        root.add(title).padBottom(6f).row();
        root.add(hint).padBottom(50f).row();
        root.add(resume).width(320f).height(44f).padBottom(6f).row();
        root.add(restart).width(320f).height(44f).padBottom(6f).row();
        root.add(settings).width(320f).height(44f).padBottom(6f).row();
        root.add(menu).width(320f).height(44f).row();
        stage.addActor(root);
    }

    private TextButton button(String text, Runnable action) {
        TextButton b = game.theme.button(text, true);
        b.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (leaving) return;
                leaving = true;
                Gdx.app.postRunnable(action);   // jamais détruire l'écran pendant un clic
            }
        });
        return b;
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
        stage.getRoot().getColor().a = 0f;
        stage.getRoot().addAction(Actions.fadeIn(0.25f));
    }

    @Override
    public void render(float delta) {
        if (!leaving && Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            leaving = true;
            Gdx.app.postRunnable(() -> game.switchTo(level));
        }

        ScreenUtils.clear(0f, 0f, 0f, 1f);
        level.drawFrame();                       // la partie, figée
        stage.getViewport().apply();
        game.film.draw(delta, stage.getCamera().combined, 0.62f, 1f);
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
        level.resize(width, height);             // sinon la partie s'afficherait à la mauvaise taille
    }

    @Override
    public void dispose() {
        stage.dispose();
    }
}
