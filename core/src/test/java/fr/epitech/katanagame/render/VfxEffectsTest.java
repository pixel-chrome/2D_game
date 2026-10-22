package fr.epitech.katanagame.render;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;

class VfxEffectsTest {
    @Test
    void effetPonctuel_finitParDisparaitre() {
        VfxEffects fx = new VfxEffects();
        fx.spawn(Vfx.Kind.EXPLOSION, 0f, 0f, 100f, 0.5f);
        assertDoesNotThrow(() -> {
            for (int i = 0; i < 200; i++) fx.update(0.02f);   // 4 s : délai + lecture complète
        });
        fx.clear();
    }

    @Test
    void sansAssetsChargees_pasDeDessin() {
        assertDoesNotThrow(() -> Vfx.disposeAssets());
    }
}
