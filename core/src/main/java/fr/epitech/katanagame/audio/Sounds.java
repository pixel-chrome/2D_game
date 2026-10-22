package fr.epitech.katanagame.audio;

import fr.epitech.katanagame.settings.GameSettings;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.math.MathUtils;

import java.util.EnumMap;
import java.util.Map;

/** Effets sonores du jeu : chargés à la demande, au volume des paramètres. Sans effet si l'audio n'est pas disponible (tests). */
public final class Sounds {
    /** Effets sonores ; chacun peut avoir plusieurs variantes, tirées au hasard. */
    public enum Effect {
        /** Coup de sabre. */
        ATTACK("attaque1", "attaque2"),
        /** Esquive. */
        DASH("dash", "dash2"),
        /** Parry lancé. */
        PARRY("parade1"),
        /** Parry qui touche. */
        PARRY_HIT("parade2"),
        /** Un ennemi repère le joueur. */
        DETECT("detection1"),
        /** Mort d'un ennemi ou du joueur. */
        DEATH("mort_enemi1", "mort_enemi2", "mort_enemi3"),
        /** Tir d'un ennemi. */
        SHOOT("piou", "piou2"),
        /** Porte qui s'ouvre. */
        DOOR("porteOuverte"),
        /** Clic dans un menu. */
        MENU("bruitmenu1"),
        /** Début du ralenti. */
        SLOWMO("zawarudo2"),
        /** Tous les ennemis de l'étage sont morts. */
        VICTORY("victoireFinale"),
        /** Pas du joueur (en boucle tant qu'il marche). */
        STEP("pas"),
        /** Mort du joueur. */
        FATALITY("fatality1", "fatality2"),
        /** Coup de sabre du boss (balayage). */
        BOSS_ATTACK("attaqueboss1"),
        /** Tirs du boss. */
        BOSS_SHOOT("attaqueboss_pppiou"),
        /** Le boss charge son attaque. */
        BOSS_CHARGE("attaquechargeeboss"),
        /** Un pas du boss. */
        BOSS_STEP("bruitpasboss"),
        /** Charge du boss. */
        BOSS_DASH("dashboss");

        private final String[] files;

        Effect(String... files) { this.files = files; }
    }

    private static final Map<Effect, Sound[]> LOADED = new EnumMap<>(Effect.class);
    private static long stepId = -1L;

    private Sounds() {}

    /**
     * Joue un effet (une variante au hasard s'il y en a plusieurs).
     *
     * @param effect effet à jouer
     */
    public static void play(Effect effect) {
        if (Gdx.audio == null) return;
        Sound[] variants = variants(effect);
        variants[MathUtils.random(variants.length - 1)].play(GameSettings.sfxVolume);
    }

    /**
     * Lance ou arrête le bruit de pas en boucle.
     *
     * @param walking vrai tant que le joueur marche
     */
    public static void walking(boolean walking) {
        if (Gdx.audio == null) return;
        Sound step = variants(Effect.STEP)[0];
        if (walking && stepId < 0L) {
            stepId = step.loop(GameSettings.sfxVolume);
        } else if (!walking && stepId >= 0L) {
            step.stop(stepId);
            stepId = -1L;
        }
    }

    private static Sound[] variants(Effect effect) {
        Sound[] sounds = LOADED.get(effect);
        if (sounds == null) {
            sounds = new Sound[effect.files.length];
            for (int i = 0; i < sounds.length; i++) {
                sounds[i] = Gdx.audio.newSound(Gdx.files.internal("sounds/" + effect.files[i] + ".mp3"));
            }
            LOADED.put(effect, sounds);
        }
        return sounds;
    }

    /** Libère tous les sons chargés. */
    public static void dispose() {
        for (Sound[] sounds : LOADED.values()) {
            for (Sound s : sounds) s.dispose();
        }
        LOADED.clear();
        stepId = -1L;
    }
}
