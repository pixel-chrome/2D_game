package fr.epitech.katanagame.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;

/**
 * Dessine l'ombre d'un sprite : sa silhouette retournée verticalement autour
 * de la ligne des pieds (donc "couchée" sur le sol), aplatie, et penchée vers le bas-droite.
 */
public final class ShadowRenderer {

    private static final float ALPHA = 0.25f;
    private static final float FLATTEN_Y = 0.30f;  // longueur de l'ombre (1 = aussi longue que le sprite)
    private static final float SHEAR_X = 0.80f;    // inclinaison vers la droite (0 = droit vers le bas)

    private static final float[] VERTICES = new float[20];   // 4 sommets x (x, y, couleur, u, v)

    private static ShaderProgram shader;

    private ShadowRenderer() {}

    /** À appeler une fois, avant tout dessin d'ombre (ex : au début du constructeur de GameScreen). */
    public static void initialize() {
        if (shader != null) return;

        String vertexShader =
                "attribute vec4 a_position;\n" +
                "attribute vec4 a_color;\n" +
                "attribute vec2 a_texCoord0;\n" +
                "uniform mat4 u_projTrans;\n" +
                "varying vec4 v_color;\n" +
                "varying vec2 v_texCoords;\n" +
                "void main() {\n" +
                "    v_color = a_color;\n" +
                "    v_texCoords = a_texCoord0;\n" +
                "    gl_Position = u_projTrans * a_position;\n" +
                "}\n";

        String fragmentShader =
                "#ifdef GL_ES\n" +
                "precision mediump float;\n" +
                "#endif\n" +
                "uniform sampler2D u_texture;\n" +
                "varying vec4 v_color;\n" +
                "varying vec2 v_texCoords;\n" +
                "void main() {\n" +
                "    float alpha = texture2D(u_texture, v_texCoords).a;\n" +
                "    gl_FragColor = vec4(0.0, 0.0, 0.0, alpha) * v_color;\n" +
                "}\n";

        shader = new ShaderProgram(vertexShader, fragmentShader);
        if (!shader.isCompiled()) {
            String log = shader.getLog();
            shader.dispose();
            shader = null;
            throw new IllegalStateException("Impossible de compiler le shader d'ombre : " + log);
        }
    }

    /** Le shader d'ombre, pour dessiner beaucoup de quads d'un coup avec drawQuad(). */
    public static ShaderProgram getShader() {
        requireInitialized();
        return shader;
    }

    /**
     * Ombre d'un sprite (opacité par défaut). À appeler AVANT de dessiner le sprite.
     *
     * @param x,y,width,height rectangle où le sprite est dessiné (coin bas-gauche)
     * @param footY            Y monde de la ligne des pieds (ex : position.y - FEET_OFFSET_Y)
     */
    public static void draw(SpriteBatch batch, TextureRegion frame,
                            float x, float y, float width, float height, float footY) {
        draw(batch, frame, x, y, width, height, footY, ALPHA);
    }

    /** Pareil, avec une opacité choisie. */
    public static void draw(SpriteBatch batch, TextureRegion frame,
                            float x, float y, float width, float height, float footY, float alpha) {
        requireInitialized();
        ShaderProgram previous = batch.getShader();
        batch.setShader(shader);                      // vide le batch avec l'ancien shader
        drawQuad(batch, frame, x, y, width, height, footY, alpha);
        batch.setShader(previous);                    // vide l'ombre avec le shader d'ombre
    }

    /**
     * Dessine l'ombre SANS changer de shader : l'appelant a déjà fait
     * batch.setShader(ShadowRenderer.getShader()). Évite un flush par tuile.
     */
    public static void drawQuad(SpriteBatch batch, TextureRegion frame,
                                float x, float y, float width, float height,
                                float footY, float alpha) {
        float color = Color.toFloatBits(1f, 1f, 1f, alpha);
        float left = x, right = x + width, bottom = y, top = y + height;

        // Ordre des sommets : bas-gauche, haut-gauche, haut-droite, bas-droite
        put(0,  left,  bottom, color, frame.getU(),  frame.getV2(), footY);
        put(5,  left,  top,    color, frame.getU(),  frame.getV(),  footY);
        put(10, right, top,    color, frame.getU2(), frame.getV(),  footY);
        put(15, right, bottom, color, frame.getU2(), frame.getV2(), footY);

        batch.draw(frame.getTexture(), VERTICES, 0, 20);
    }

    /** Transforme un coin du sprite : miroir vertical autour des pieds + aplatissement + inclinaison. */
    private static void put(int i, float vx, float vy, float color, float u, float v, float footY) {
        float depth = (vy - footY) * FLATTEN_Y;   // hauteur au-dessus des pieds, aplatie
        VERTICES[i]     = vx + depth * SHEAR_X;   // plus c'est haut, plus l'ombre part à droite...
        VERTICES[i + 1] = footY - depth;          // ...et vers le bas (retournement vertical)
        VERTICES[i + 2] = color;
        VERTICES[i + 3] = u;
        VERTICES[i + 4] = v;
    }

    private static void requireInitialized() {
        if (shader == null) {
            throw new IllegalStateException("ShadowRenderer.initialize() doit être appelé avant draw().");
        }
    }

    public static void dispose() {
        if (shader != null) {
            shader.dispose();
            shader = null;
        }
    }
}