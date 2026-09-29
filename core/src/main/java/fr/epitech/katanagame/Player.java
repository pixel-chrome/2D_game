package fr.epitech.katanagame;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.Viewport;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class Player {
    public enum Direction {
        UP, DOWN, LEFT, RIGHT
    }
    private static final float SPEED = 200f;
    private static final int FRAME_W = 32;
    private static final int FRAME_H = 32;
    private static final float FRAME_TIME = 0.1f;

    private final Vector2 position = new Vector2(320, 180);
    private final Vector2 aim = new Vector2(1, 0);
    
    private final Map<Direction, Animation<TextureRegion>> idle = new EnumMap<>(Direction.class);
    private final Map<Direction, Animation<TextureRegion>> run = new EnumMap<>(Direction.class);
    private final List<Texture> textures = new ArrayList<>();

    private Direction facing = Direction.DOWN;
    private boolean moving = false;
    private float stateTime = 0f;

    public Player(Texture texture) {
        for (Direction d : Direction.values()) {
            String name = d.name().toLowerCase();
            idle.put(d, load("Sprites/IDLE/idel_" + name + ".png"));
            run.put(d, load("Sprites/RUN/run_" + name + ".png"));
        }
    }

    public Animation<TextureRegion>

    public void update(float delta, Viewport viewport) {
        // Déplacement
        Vector2 dir = new Vector2();
        if (Gdx.input.isKeyPressed(Input.Keys.Z) || Gdx.input.isKeyPressed(Input.Keys.W)) dir.y += 1;
        if (Gdx.input.isKeyPressed(Input.Keys.S)) dir.y -= 1;
        if (Gdx.input.isKeyPressed(Input.Keys.Q) || Gdx.input.isKeyPressed(Input.Keys.A)) dir.x -= 1;
        if (Gdx.input.isKeyPressed(Input.Keys.D)) dir.x += 1;
        if (!dir.isZero()) position.add(dir.nor().scl(SPEED * delta));

        //Rotation de la souris
        Vector2 mouse = viewport.unproject(new Vector2(Gdx.input.getX(), Gdx.input.getY()));
        float angle = MathUtils.atan2(mouse.y - position.y, mouse.x - position.x) * MathUtils.radiansToDegrees;
        sprite.setRotation(angle); // adapte selon l'orientation du personnage
    }

    public void draw(SpriteBatch batch) {
        sprite.setPosition(position.x - sprite.getWidth() / 2, position.y - sprite.getHeight() / 2);
        sprite.draw(batch);
    }
}