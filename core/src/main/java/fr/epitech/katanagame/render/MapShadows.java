package fr.epitech.katanagame.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.maps.tiled.TiledMapTile;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.utils.Disposable;

/**
 * Ombres des murs et objets de la map.
 *
 * Au chargement, la silhouette de chaque tuile "qui projette une ombre" est dessinée
 * en noir OPAQUE dans un FrameBuffer (donc pas de zones plus sombres là où les ombres
 * se chevauchent). Ensuite, on affiche simplement ce buffer avec une faible opacité.
 *
 * Pour un mur de plusieurs tuiles empilées, l'ombre part de la base de la pile
 * (plus c'est haut, plus l'ombre est longue).
 *
 * Nécessite ShadowRenderer.initialize() déjà appelé.
 */
public final class MapShadows implements Disposable {
    private static final float TILE_SIZE = 16f;
    private static final float ALPHA = 0.25f;

    /**
     * Ombre latérale (vue de dessus, lumière venant du haut-gauche) : la silhouette de chaque tuile
     * est aussi dessinée décalée de ce nombre de pixels. Y négatif = vers le bas.
     */
    private static final float SIDE_OFFSET_X = 6f;
    private static final float SIDE_OFFSET_Y = -3f;

    private static final float SIDE_SHEAR = 0f;

    private final FrameBuffer buffer;
    private final TextureRegion shadows;

    /**
     * @param casters calques dont les tuiles projettent une ombre (Mur, Objets, Chaises...)
     * @param mapWidthPx,mapHeightPx taille de la map en pixels
     */
    public MapShadows(TiledMapTileLayer[] casters, int mapWidthPx, int mapHeightPx) {
        buffer = new FrameBuffer(Pixmap.Format.RGBA8888, mapWidthPx, mapHeightPx, false);
        bake(casters, mapWidthPx, mapHeightPx);

        Texture texture = buffer.getColorBufferTexture();
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        shadows = new TextureRegion(texture);
        shadows.flip(false, true);   // un FrameBuffer est stocké à l'envers
    }

    /** À appeler entre les calques de sol et les calques de murs/objets. */
    public void draw(Batch batch) {
        batch.begin();
        batch.enableBlending();
        batch.setColor(1f, 1f, 1f, ALPHA);
        batch.draw(shadows, 0f, 0f);
        batch.setColor(1f, 1f, 1f, 1f);
        batch.end();
    }

    // ------------------------------------------------------------------

    private void bake(TiledMapTileLayer[] casters, int width, int height) {
        OrthographicCamera camera = new OrthographicCamera();
        camera.setToOrtho(false, width, height);

        SpriteBatch batch = new SpriteBatch();
        try {
            buffer.begin();
            Gdx.gl.glClearColor(0f, 0f, 0f, 0f);
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

            batch.setProjectionMatrix(camera.combined);
            batch.setShader(ShadowRenderer.getShader());   // un seul changement de shader pour toutes les tuiles
            batch.begin();

            for (TiledMapTileLayer layer : casters) {
                for (int tx = 0; tx < layer.getWidth(); tx++) {
                    for (int ty = 0; ty < layer.getHeight(); ty++) {
                        TiledMapTileLayer.Cell cell = layer.getCell(tx, ty);
                        if (cell != null && cell.getTile() != null) {
                            drawTileShadow(batch, layer, cell, tx, ty);
                        }
                    }
                }
            }

            batch.end();
            buffer.end();
        } finally {
            batch.setShader(null);   // le shader d'ombre n'appartient pas à ce batch
            batch.dispose();
        }
    }

    private void drawTileShadow(
        SpriteBatch batch,
        TiledMapTileLayer layer,
        TiledMapTileLayer.Cell cell,
        int tx,
        int ty
    ) {
        TiledMapTile tile = cell.getTile();

        String shadowType = layer.getProperties().get(
                "shadowType", "wall", String.class
        );

        // Les petits objets et les types inconnus n'ont aucune ombre.
        if ("small".equalsIgnoreCase(shadowType)) {
            return;
        }

        // Seuls les murs et les gros objets projettent une ombre latérale.
        boolean isWall = "wall".equalsIgnoreCase(shadowType);
        boolean isBigObject = "big".equalsIgnoreCase(shadowType);

        if (!isWall && !isBigObject) {
            return;
        }

        TextureRegion region = new TextureRegion(tile.getTextureRegion());

        if (cell.getFlipHorizontally()) {
            region.flip(true, false);
        }

        if (cell.getFlipVertically()) {
            region.flip(false, true);
        }

        float width = region.getRegionWidth();
        float height = region.getRegionHeight();

        float x = tx * TILE_SIZE + tile.getOffsetX();
        float y = ty * TILE_SIZE + tile.getOffsetY();
        float footY = ty * TILE_SIZE;

        // Les murs ont une ombre au sol.
        if (isWall) {
            ShadowRenderer.drawQuad(
                    batch,
                    region,
                    x,
                    y,
                    width,
                    height,
                    footY,
                    1f
            );
        }

        // Les murs et les gros objets ont une ombre latérale.
        int base = stackBase(layer, tx, ty);

        float bottomDepth = (ty - base) * TILE_SIZE;
        float topDepth = bottomDepth + height;

        float bottomShear = bottomDepth * SIDE_SHEAR;
        float topShear = topDepth * SIDE_SHEAR;

        float left = x + SIDE_OFFSET_X;
        float bottom = y + SIDE_OFFSET_Y;

        float color = com.badlogic.gdx.graphics.Color.toFloatBits(
                1f, 1f, 1f, 1f
        );

        float[] vertices = new float[20];

        // Bas-gauche
        putVertex(
                vertices, 0,
                left + bottomShear, bottom,
                color, region.getU(), region.getV2()
        );

        // Haut-gauche
        putVertex(
                vertices, 5,
                left + topShear, bottom + height,
                color, region.getU(), region.getV()
        );

        // Haut-droite
        putVertex(
                vertices, 10,
                left + width + topShear, bottom + height,
                color, region.getU2(), region.getV()
        );

        // Bas-droite
        putVertex(
                vertices, 15,
                left + width + bottomShear, bottom,
                color, region.getU2(), region.getV2()
        );

        batch.draw(
                region.getTexture(),
                vertices,
                0,
                vertices.length
        );
    }

    /** Ligne de la tuile la plus basse de la pile (dans le même calque) qui contient (tx, ty). */
    private static int stackBase(TiledMapTileLayer layer, int tx, int ty) {
        int base = ty;
        while (base > 0 && layer.getCell(tx, base - 1) != null) base--;
        return base;
    }

    @Override
    public void dispose() {
        buffer.dispose();
    }
    private static void putVertex(
            float[] vertices,
            int i,
            float x,
            float y,
            float color,
            float u,
            float v
    ) {
        vertices[i] = x;
        vertices[i + 1] = y;
        vertices[i + 2] = color;
        vertices[i + 3] = u;
        vertices[i + 4] = v;
    }
}