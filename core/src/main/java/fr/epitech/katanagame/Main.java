package fr.epitech.katanagame;

import fr.epitech.katanagame.screen.GameScreen;
import fr.epitech.katanagame.screen.MainMenuScreen;
import fr.epitech.katanagame.settings.GameSettings;
import fr.epitech.katanagame.ui.FilmOverlay;
import fr.epitech.katanagame.ui.MenuBackground;
import fr.epitech.katanagame.ui.Theme;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Screen;

/**
 * Point d'entrée : ne contient plus le gameplay, seulement la gestion des écrans.
 * Le jeu en lui-même vit dans GameScreen.
 */
public class Main extends Game {
    /** Nom affiché du jeu. */
    public static final String TITLE = "LAME D'AUBE";   // nom provisoire : à changer

    /** Styles et ressources partagés de l’interface. */
    public Theme theme;
    /** Voile et vignette de type film. */
    public FilmOverlay film;
    /** Fond animé partagé par les menus. */
    public MenuBackground background;

    /** Crée le jeu ; les ressources sont chargées dans {@link #create()}. */
    public Main() {}

    /** Initialise les paramètres, les ressources communes et le menu principal. */
    @Override
    public void create() {
        GameSettings.load();
        theme = new Theme();
        film = new FilmOverlay();
        background = new MenuBackground(film);
        setScreen(new MainMenuScreen(this));
    }

    /**
     * Change d'écran et libère l'ancien.
     *
     * @param next écran à afficher
     */
    public void switchTo(Screen next) {
        Screen old = getScreen();
        setScreen(next);
        if (old != null && old != next) old.dispose();
    }

    /** Libère les ressources partagées du jeu. */
    @Override
    public void dispose() {
        super.dispose();
        Screen current = getScreen();
        if (current != null) current.dispose();
        theme.dispose();
        background.dispose();
        film.dispose();
    }
}
