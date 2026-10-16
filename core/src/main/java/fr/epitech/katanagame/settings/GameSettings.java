package fr.epitech.katanagame.settings;

import fr.epitech.katanagame.Main;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Preferences;

/** Réglages du joueur, sauvegardés entre deux lancements du jeu. */
public final class GameSettings {
    private static final String FILE = "katanagame-settings";

    public static float musicVolume = 0.7f;
    public static float sfxVolume = 0.8f;
    public static boolean fullscreen = false;

    /** Actions dont la touche peut être changée. */
    public enum Action {
        UP("HAUT", Input.Keys.Z), DOWN("BAS", Input.Keys.S),
        LEFT("GAUCHE", Input.Keys.Q), RIGHT("DROITE", Input.Keys.D),
        DASH("ESQUIVE", Input.Keys.SPACE),
        SLOWMO("RALENTI", Input.Keys.SHIFT_LEFT);

        public final String label;
        final int defaultKey;
        int key;

        Action(String label, int defaultKey) {
            this.label = label;
            this.defaultKey = defaultKey;
            this.key = defaultKey;
        }
    }

    private static Preferences prefs;

    public static int key(Action a) { return a.key; }

    public static void setKey(Action a, int key) { a.key = key; }

    public static boolean pressed(Action a) { return Gdx.input.isKeyPressed(a.key); }

    public static boolean justPressed(Action a) { return Gdx.input.isKeyJustPressed(a.key); }

    public static String keyName(Action a) {
        String n = Input.Keys.toString(a.key);
        return (n == null ? "?" : n).toUpperCase();
    }

    public static void resetKeys() {
        for (Action a : Action.values()) a.key = a.defaultKey;
    }

    private GameSettings() {}

    /** À appeler une fois au démarrage (dans Main.create()). */
    public static void load() {
        prefs = Gdx.app.getPreferences(FILE);
        musicVolume = prefs.getFloat("music", 0.7f);
        sfxVolume = prefs.getFloat("sfx", 0.8f);
        fullscreen = prefs.getBoolean("fullscreen", false);
        for (Action a : Action.values()) a.key = prefs.getInteger("key_" + a.name(), a.defaultKey);
        applyDisplay();
    }

    public static void save() {
        if (prefs == null) prefs = Gdx.app.getPreferences(FILE);
        prefs.putFloat("music", musicVolume);
        prefs.putFloat("sfx", sfxVolume);
        prefs.putBoolean("fullscreen", fullscreen);
        for (Action a : Action.values()) prefs.putInteger("key_" + a.name(), a.key);
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
