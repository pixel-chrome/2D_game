package fr.epitech.katanagame.screen;

import fr.epitech.katanagame.Main;
import fr.epitech.katanagame.ui.MenuBackground;
import fr.epitech.katanagame.ui.Theme;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
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
 * Écran de victoire après la mort du boss : la salle reste visible sous un voile, avec un titre doré.
 * Deux choix : rejouer depuis le début ou revenir au menu principal.
 */
public class VictoryScreen extends ScreenAdapter {
    private static final Color GOLD = Color.valueOf("d8b34a");

    private final Main game;
    private final GameScreen level;
    private final Stage stage = new Stage(new FitViewport(MenuBackground.W, MenuBackground.H));
    private boolean leaving = false;

    /**
     * Crée l'écran de victoire par-dessus la partie qui vient de se terminer.
     *
     * @param game jeu qui possède l'écran
     * @param level partie terminée (affichée en fond, puis libérée)
     */
    public VictoryScreen(Main game, GameScreen level) {
        this.game = game;
        this.level = level;
        Theme t = game.theme;

        Label title = new Label(Theme.spaced("VICTOIRE"), new Label.LabelStyle(t.titleFont, GOLD));
        Label subtitle = new Label("LE CHAPELIER NOIR EST TOMBE", t.labelStyle);
        Label hint = new Label("ENTREE : REJOUER     ECHAP : MENU", t.hintStyle);

        TextButton replay = button("REJOUER", this::replay);
        TextButton menu = button("MENU PRINCIPAL", this::toMenu);

        Table root = new Table();
        root.setFillParent(true);
        root.add(title).padBottom(10f).row();
        root.add(subtitle).padBottom(6f).row();
        root.add(hint).padBottom(50f).row();
        root.add(replay).width(320f).height(44f).padBottom(6f).row();
        root.add(menu).width(320f).height(44f).row();
        stage.addActor(root);
    }

    private void replay() {
        level.dispose();
        game.switchTo(new GameScreen(game));
    }

    private void toMenu() {
        level.dispose();
        game.switchTo(new MainMenuScreen(game));
    }

    private TextButton button(String text, Runnable action) {
        TextButton b = game.theme.button(text, true);
        b.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                leave(action);
            }
        });
        return b;
    }

    /** Jamais détruire l'écran pendant un clic ou une touche : on attend la fin de l'image. */
    private void leave(Runnable action) {
        if (leaving) return;
        leaving = true;
        Gdx.app.postRunnable(action);
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
        stage.getRoot().getColor().a = 0f;
        stage.getRoot().addAction(Actions.fadeIn(0.6f));
    }

    @Override
    public void render(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) leave(this::replay);
        else if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) leave(this::toMenu);

        ScreenUtils.clear(0f, 0f, 0f, 1f);
        level.updateEffects(delta);
        level.drawFrame();
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
