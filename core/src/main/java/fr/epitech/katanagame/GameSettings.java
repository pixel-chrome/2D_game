package fr.epitech.katanagame;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;

/** Réglages du joueur, sauvegardés entre deux lancements du jeu. */
public final class GameSettings {
    private static final String FILE = "katanagame-settings";

    public static float musicVolume = 0.7f;
    public static float sfxVolume = 0.8f;
    public static boolean fullscreen = false;

    private static Preferences prefs;

    private GameSettings() {}

    /** À appeler une fois au démarrage (dans Main.create()). */
    public static void load() {
        prefs = Gdx.app.getPreferences(FILE);
        musicVolume = prefs.getFloat("music", 0.7f);
        sfxVolume = prefs.getFloat("sfx", 0.8f);
        fullscreen = prefs.getBoolean("fullscreen", false);
        applyDisplay();
    }

    public static void save() {
        if (prefs == null) prefs = Gdx.app.getPreferences(FILE);
        prefs.putFloat("music", musicVolume);
        prefs.putFloat("sfx", sfxVolume);
        prefs.putBoolean("fullscreen", fullscreen);
        prefs.flush();
    }

    /** Applique le mode plein écran / fenêtré. */
    public static void applyDisplay() {
        if (fullscreen) {
            Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
        } else {
            Gdx.graphics.setWindowedMode(1280, 720);
        }
    }
}
