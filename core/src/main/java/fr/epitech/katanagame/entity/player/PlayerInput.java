package fr.epitech.katanagame.entity.player;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.Vector2;
import fr.epitech.katanagame.world.World;

/** Centralise la lecture des commandes du joueur. */
final class PlayerInput {
    private PlayerInput() {}

    /** Z/W = haut, S = bas, Q/A = gauche, D = droite (AZERTY et QWERTY). */
    static Vector2 readMove() {
        Vector2 dir = new Vector2();
        if (Gdx.input.isKeyPressed(Input.Keys.Z) || Gdx.input.isKeyPressed(Input.Keys.W)) dir.y += 1;
        if (Gdx.input.isKeyPressed(Input.Keys.S)) dir.y -= 1;
        if (Gdx.input.isKeyPressed(Input.Keys.Q) || Gdx.input.isKeyPressed(Input.Keys.A)) dir.x -= 1;
        if (Gdx.input.isKeyPressed(Input.Keys.D)) dir.x += 1;
        return dir;
    }

    /** Oriente le personnage vers la souris en coordonnées du monde. */
    static void updateAim(Player player, World world) {
        Vector2 mouse = world.viewport.unproject(new Vector2(Gdx.input.getX(), Gdx.input.getY()));
        player.getAim().set(mouse).sub(player.getPosition()).nor();
    }
}
