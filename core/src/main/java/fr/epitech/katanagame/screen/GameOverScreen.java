package fr.epitech.katanagame.screen;

import fr.epitech.katanagame.Main;
import fr.epitech.katanagame.ui.MenuBackground;
import fr.epitech.katanagame.ui.Theme;

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
 * Écran de mort : la partie reste visible, figée, sous un voile plus sombre que
 * la pause, avec un titre rouge sang. Deux choix : recommencer ou menu principal.
 */
public class GameOverScreen extends ScreenAdapter {
    private final Main game;
    private final GameScreen level;
    private final Stage stage = new Stage(new FitViewport(MenuBackground.W, MenuBackground.H));
    private boolean leaving = false;

    /**
     * Crée l'écran de game over par-dessus la partie qui vient de se terminer.
     *
     * @param game jeu qui possède l'écran
     * @param level partie à recommencer ou à quitter
     */
    public GameOverScreen(Main game, GameScreen level) {
        this.game = game;
        this.level = level;
        Theme t = game.theme;

        Label.LabelStyle deadStyle = new Label.LabelStyle(t.titleFont, Theme.BLOOD);
        Label title = new Label(Theme.spaced("GAME OVER"), deadStyle);
        Label hint = new Label("R : RECOMMENCER     ECHAP : MENU", t.hintStyle);

        TextButton restart = button("RECOMMENCER", this::restart);
        TextButton menu = button("MENU PRINCIPAL", this::toMenu);

        Table root = new Table();
        root.setFillParent(true);
        root.add(title).padBottom(6f).row();
        root.add(hint).padBottom(50f).row();
        root.add(restart).width(320f).height(44f).padBottom(6f).row();
        root.add(menu).width(320f).height(44f).row();
        stage.addActor(root);
    }

    private void restart() {
        level.restart();
        game.setScreen(level);
    }

    private void toMenu() {
        level.dispose();                           // on quitte vraiment la partie
        game.switchTo(new MainMenuScreen(game));
    }

    /** Raccourcis clavier : on doit pouvoir relancer instantanément. */
    private void shortcut(Runnable action) {
        if (leaving) return;
        leaving = true;
        Gdx.app.postRunnable(action);
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
        stage.getRoot().addAction(Actions.fadeIn(0.12f));
    }

    @Override
    public void render(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.R) || Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) shortcut(this::restart);
        else if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) shortcut(this::toMenu);

        ScreenUtils.clear(0f, 0f, 0f, 1f);
        level.updateEffects(delta);              // le sang continue de couler
        level.drawFrame();                       // la partie, sinon figée
        stage.getViewport().apply();
        game.film.draw(delta, stage.getCamera().combined, 0.7f, 1f);
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
        level.resize(width, height);
    }

    @Override
    public void dispose() {
        stage.dispose();
    }
}
