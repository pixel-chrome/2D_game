package fr.epitech.katanagame.screen;

import fr.epitech.katanagame.Main;
import fr.epitech.katanagame.ui.Theme;
import fr.epitech.katanagame.ui.MenuBackground;

import com.badlogic.gdx.Gdx;
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

/** Menu principal : titre à gauche, katana à droite, trois choix. */
public class MainMenuScreen extends ScreenAdapter {
    private final Main game;
    private final Stage stage = new Stage(new FitViewport(MenuBackground.W, MenuBackground.H));
    private boolean leaving = false;

    /**
     * Construit le menu principal.
     *
     * @param game jeu qui possède l'écran
     */
    public MainMenuScreen(Main game) {
        this.game = game;
        Theme t = game.theme;

        Label title = new Label(Theme.spaced(Main.TITLE), t.titleStyle);
        Label subtitle = new Label("UNE LAME. UN DASH. UN SEUL COUP.", t.hintStyle);

        TextButton play = button("JOUER", () -> game.switchTo(new GameScreen(game)));
        TextButton settings = button("PARAMETRES", () ->
                game.switchTo(new SettingsScreen(game, null, () -> game.switchTo(new MainMenuScreen(game)))));
        TextButton quit = button("QUITTER", () -> Gdx.app.exit());

        Table root = new Table();
        root.setFillParent(true);
        root.left().padLeft(110f);
        root.add(title).left().padBottom(10f).row();
        root.add(subtitle).left().padLeft(4f).padBottom(70f).row();
        root.add(play).left().width(300f).height(44f).padBottom(6f).row();
        root.add(settings).left().width(300f).height(44f).padBottom(6f).row();
        root.add(quit).left().width(300f).height(44f).row();
        stage.addActor(root);
    }

    /**
     * L'action part à la frame suivante : on ne peut pas détruire l'écran (et son
     * Stage) pendant qu'il traite un clic. Le garde-fou évite le double clic.
     */
    private TextButton button(String text, Runnable action) {
        TextButton b = game.theme.button(text, false);
        b.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (leaving) return;
                leaving = true;
                Gdx.app.postRunnable(action);
            }
        });
        return b;
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
        stage.getRoot().getColor().a = 0f;
        stage.getRoot().addAction(Actions.fadeIn(0.9f));
    }

    @Override
    public void render(float delta) {
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
