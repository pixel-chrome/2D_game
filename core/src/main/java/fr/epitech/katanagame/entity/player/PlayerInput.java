package fr.epitech.katanagame.entity.player;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import fr.epitech.katanagame.settings.GameSettings;
import fr.epitech.katanagame.settings.GameSettings.Action;
import fr.epitech.katanagame.world.World;

/** Centralise la lecture des commandes du joueur. */
final class PlayerInput {
    /** Empêche l’instanciation de cette classe utilitaire. */
    private PlayerInput() {}

    /** Direction demandée, selon les touches choisies dans les paramètres. */
    static Vector2 readMove() {
        Vector2 dir = new Vector2();
        if (GameSettings.pressed(Action.UP)) dir.y += 1;
        if (GameSettings.pressed(Action.DOWN)) dir.y -= 1;
        if (GameSettings.pressed(Action.LEFT)) dir.x -= 1;
        if (GameSettings.pressed(Action.RIGHT)) dir.x += 1;
        return dir;
    }

    /** Oriente le personnage vers la souris en coordonnées du monde. */
    static void updateAim(Player player, World world) {
        Vector2 mouse = world.viewport.unproject(new Vector2(Gdx.input.getX(), Gdx.input.getY()));
        player.getAim().set(mouse).sub(player.getPosition()).nor();
    }
}
