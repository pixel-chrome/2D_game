package fr.epitech.katanagame.screen;

import fr.epitech.katanagame.Main;
import fr.epitech.katanagame.ui.MenuBackground;
import fr.epitech.katanagame.ui.Theme;

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

/** Menu principal : Jouer / Paramètres / Quitter. */
public class MainMenuScreen extends ScreenAdapter {
    private final Main game;
    private final Stage stage = new Stage(new FitViewport(MenuBackground.W, MenuBackground.H));

    /** Construit l’interface du menu principal. */
    public MainMenuScreen(Main game) {
        this.game = game;
        Theme t = game.theme;

        // Titre qui passe lentement de l'os à la braise
        Label title = new Label(Main.TITLE, t.titleStyle);
        title.setColor(Theme.BONE);
        title.addAction(Actions.forever(Actions.sequence(
                Actions.color(Theme.EMBER, 2.5f),
                Actions.color(Theme.BONE, 2.5f))));
        Label subtitle = new Label("UNE LAME. UN DASH. UN SEUL COUP.", t.hintStyle);

        TextButton play = button("JOUER", () -> game.switchTo(new GameScreen(game)));
        TextButton settings = button("PARAMETRES", () ->
                game.switchTo(new SettingsScreen(game, () -> game.switchTo(new MainMenuScreen(game)))));
        TextButton quit = button("QUITTER", () -> Gdx.app.exit());

        Table root = new Table();
        root.setFillParent(true);
        root.add(title).padBottom(8f).row();
        root.add(subtitle).padBottom(50f).row();
        root.add(play).width(360f).height(56f).padBottom(16f).row();
        root.add(settings).width(360f).height(56f).padBottom(16f).row();
        root.add(quit).width(360f).height(56f).row();
        stage.addActor(root);
    }

    /**
     * Bouton dont l'action est exécutée à la frame suivante : on ne peut pas
     * détruire l'écran (et son Stage) pendant qu'il est en train de traiter un clic.
     */
    private TextButton button(String text, Runnable action) {
        TextButton b = new TextButton(text, game.theme.buttonStyle);
        b.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                Gdx.app.postRunnable(action);
            }
        });
        return b;
    }

    /** Active les entrées de l’interface. */
    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
        stage.getRoot().getColor().a = 0f;
        stage.getRoot().addAction(Actions.fadeIn(0.5f));
    }

    /** Dessine et met à jour le menu. */
    @Override
    public void render(float delta) {
        ScreenUtils.clear(0f, 0f, 0f, 1f);
        stage.getViewport().apply();
        game.background.render(delta, stage.getCamera().combined);
        stage.act(delta);
        stage.draw();
    }

    /** Adapte l’interface aux dimensions de la fenêtre. */
    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    /** Libère les ressources du menu. */
    @Override
    public void dispose() {
        stage.dispose();
    }
}
