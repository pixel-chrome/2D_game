package fr.epitech.katanagame;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;

/** Réglages du joueur, sauvegardés entre deux lancements du jeu. */
public final class GameSettings {
    private static final String FILE = "katanagame-settings";
    private static final int WINDOW_W = 1280;
    private static final int WINDOW_H = 720;

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

        // Au démarrage, la fenêtre est déjà créée en mode fenêtré par le lanceur :
        // on ne touche à l'affichage que si le joueur avait choisi le plein écran.
        // (setWindowedMode inutile = erreurs GLFW sur Wayland)
        if (fullscreen) applyDisplay();
    }

    public static void save() {
        if (prefs == null) prefs = Gdx.app.getPreferences(FILE);
        prefs.putFloat("music", musicVolume);
        prefs.putFloat("sfx", sfxVolume);
        prefs.putBoolean("fullscreen", fullscreen);
        prefs.flush();
    }

    /** Applique le mode plein écran / fenêtré (ne fait rien si c'est déjà le bon mode). */
    public static void applyDisplay() {
        if (fullscreen) {
            if (!Gdx.graphics.isFullscreen()) {
                Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
            }
        } else if (Gdx.graphics.isFullscreen()
                || Gdx.graphics.getWidth() != WINDOW_W
                || Gdx.graphics.getHeight() != WINDOW_H) {
            Gdx.graphics.setWindowedMode(WINDOW_W, WINDOW_H);
        }
    }
}