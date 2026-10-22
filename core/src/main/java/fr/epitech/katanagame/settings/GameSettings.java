package fr.epitech.katanagame.settings;

import fr.epitech.katanagame.Main;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Preferences;

/** Réglages du joueur, sauvegardés entre deux lancements du jeu. */
public final class GameSettings {
    private static final String FILE = "katanagame-settings";

    /** Volume de la musique (0 à 1). */
    public static float musicVolume = 0.7f;
    /** Volume des effets sonores (0 à 1). */
    public static float sfxVolume = 0.8f;
    /** Vrai en plein écran. */
    public static boolean fullscreen = false;

    /** Actions dont la touche peut être changée. */
    public enum Action {
        /** Aller vers le haut. */
        UP("HAUT", Input.Keys.Z),
        /** Aller vers le bas. */
        DOWN("BAS", Input.Keys.S),
        /** Aller à gauche. */
        LEFT("GAUCHE", Input.Keys.Q),
        /** Aller à droite. */
        RIGHT("DROITE", Input.Keys.D),
        /** Esquive (dash). */
        DASH("ESQUIVE", Input.Keys.SPACE),
        /** Ralenti du temps (maintenu). */
        SLOWMO("RALENTI", Input.Keys.SHIFT_LEFT);

        /** Nom affiché dans les paramètres. */
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

    /**
     * Touche actuellement associée à une action.
     *
     * @param a action
     * @return le code de la touche (Input.Keys)
     */
    public static int key(Action a) { return a.key; }

    /**
     * Associe une touche à une action.
     *
     * @param a action
     * @param key code de la touche (Input.Keys)
     */
    public static void setKey(Action a, int key) { a.key = key; }

    /**
     * Teste si la touche d'une action est maintenue.
     *
     * @param a action
     * @return vrai si la touche est enfoncée
     */
    public static boolean pressed(Action a) { return Gdx.input.isKeyPressed(a.key); }

    /**
     * Teste si la touche d'une action vient d'être pressée.
     *
     * @param a action
     * @return vrai à l'image où la touche est enfoncée
     */
    public static boolean justPressed(Action a) { return Gdx.input.isKeyJustPressed(a.key); }

    /**
     * Nom lisible de la touche d'une action.
     *
     * @param a action
     * @return le nom en majuscules
     */
    public static String keyName(Action a) {
        String n = Input.Keys.toString(a.key);
        return (n == null ? "?" : n).toUpperCase();
    }

    /** Remet toutes les touches par défaut. */
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

    /** Enregistre les réglages dans les préférences. */
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
