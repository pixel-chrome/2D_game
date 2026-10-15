package fr.epitech.katanagame;

import fr.epitech.katanagame.screen.GameScreen;
import fr.epitech.katanagame.screen.MainMenuScreen;
import fr.epitech.katanagame.settings.GameSettings;
import fr.epitech.katanagame.ui.MenuBackground;
import fr.epitech.katanagame.ui.Theme;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Screen;

/**
 * Point d'entrée : ne contient plus le gameplay, seulement la gestion des écrans.
 * Le jeu en lui-même vit dans GameScreen.
 */
public class Main extends Game {
    public static final String TITLE = "LAME D'AUBE";   // nom provisoire : à changer

    public Theme theme;
    public MenuBackground background;

    @Override
    public void create() {
        GameSettings.load();
        theme = new Theme();
        background = new MenuBackground();
        setScreen(new MainMenuScreen(this));
    }

    /** Change d'écran et libère l'ancien. */
    public void switchTo(Screen next) {
        Screen old = getScreen();
        setScreen(next);
        if (old != null) old.dispose();
    }

    @Override
    public void dispose() {
        super.dispose();
        Screen current = getScreen();
        if (current != null) current.dispose();
        theme.dispose();
        background.dispose();
    }
}
