package fr.epitech.katanagame.audio;

import fr.epitech.katanagame.settings.GameSettings;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.math.MathUtils;

/** Musique d'ambiance : une piste au hasard, puis une autre quand elle finit. Sans effet si l'audio n'est pas disponible (tests). */
public final class Jukebox {
    /** Ensembles de pistes. */
    public enum Mode {
        /** Menus. */
        MENU("musiqueMenu1", "C418Alpha"),
        /** Partie. */
        GAME("aurasound", "musiczelda", "musicjeumegalovania", "musiccrazy", "musictest", "HarryPoter");

        private final String[] tracks;

        Mode(String... tracks) { this.tracks = tracks; }
    }

    private static Mode mode;
    private static String current;
    private static Music music;

    private Jukebox() {}

    /**
     * Passe sur les pistes d'un mode ; sans effet si ce mode joue déjà.
     *
     * @param next mode voulu
     */
    public static void play(Mode next) {
        if (Gdx.audio == null || next == mode) return;
        mode = next;
        playRandom();
    }

    /** Applique le volume des paramètres à la piste en cours. */
    public static void refreshVolume() {
        if (music != null) music.setVolume(GameSettings.musicVolume);
    }

    private static void playRandom() {
        String[] tracks = mode.tracks;
        String pick;
        do {
            pick = tracks[MathUtils.random(tracks.length - 1)];
        } while (tracks.length > 1 && pick.equals(current));
        current = pick;
        if (music != null) music.dispose();
        music = Gdx.audio.newMusic(Gdx.files.internal("music/" + pick + ".mp3"));
        music.setVolume(GameSettings.musicVolume);
        music.setOnCompletionListener(m -> playRandom());
        music.play();
    }

    /** Arrête et libère la musique. */
    public static void dispose() {
        if (music != null) music.dispose();
        music = null;
        mode = null;
        current = null;
    }
}
