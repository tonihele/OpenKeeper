package toniarts.openkeeper.view.effect;

import com.jme3.effect.Particle;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.tools.convert.map.PortalGemEffectFixtures;
import static org.junit.jupiter.api.Assertions.*;

class PortalGemEffectTest {
    private PortalGemEffect effect(Node parent) {
        var emitter = new PortalGemEffect(new EffectManagerState(
                PortalGemEffectFixtures.level(), PortalGemEffectFixtures.assets()));
        parent.attachChild(emitter);
        parent.updateGeometricState();
        return emitter;
    }

    @Test
    void preservesNativeMaterialColorLifetimeAndRandomScale() {
        var emitter = effect(new Node());
        emitter.request(new Vector3f(2, 1, 3));
        emitter.advance(0.05f);
        assertEquals(BlendMode.Additive, emitter.getMaterial().getAdditionalRenderState().getBlendMode());
        assertFalse(emitter.getMaterial().getAdditionalRenderState().isDepthWrite());
        assertEquals(16, emitter.getImagesX());
        assertEquals(0, emitter.getParticlesPerSec());
        Particle p = live(emitter);
        assertEquals(2, p.startlife);
        assertEquals(180 / 255f, p.color.r);
        assertEquals(250 / 255f, p.color.b);
        assertTrue(p.size >= 0.1f && p.size <= 0.15f);
        assertEquals(new Vector3f(2, 1, 3), p.position);
    }

    @Test
    void usesIntegerRgbFadeAndAdvancesTheAnimatedTexture() {
        var root = new Node();
        var emitter = effect(root);
        emitter.request(Vector3f.ZERO);
        emitter.advance(0.05f);
        Particle p = live(emitter);
        int image = p.imageIndex;
        float size = p.size;
        root.updateLogicalState(0.05f);
        assertEquals((180 * 38 / 39) / 255f, p.color.r, 0.00001f);
        assertEquals((250 * 38 / 39) / 255f, p.color.b, 0.00001f);
        assertEquals(1, p.color.a);
        assertEquals((image + 1) % 16, p.imageIndex);
        assertEquals(size, p.size);
    }

    @Test
    void oldParticlesStayInWorldSpaceWhenTheCarrierMoves() {
        var carrier = new Node();
        var emitter = effect(carrier);
        emitter.request(new Vector3f(2, 1, 3));
        emitter.advance(0.05f);
        Particle p = live(emitter);
        carrier.setLocalTranslation(10, 0, 0);
        carrier.updateGeometricState();
        assertEquals(new Vector3f(2, 1, 3), p.position);
        assertTrue(emitter.isInWorldSpace());
        emitter.clear();
        emitter.advance(0.05f);
        assertEquals(0, emitter.getNumVisibleParticles());
    }

    @Test
    void generationQualityTracksNativeFrameRateThreshold() {
        assertEquals(50, PortalGemEffect.generationScale(2));
        assertEquals(123, PortalGemEffect.generationScale(0.2f));
        assertEquals(255, PortalGemEffect.generationScale(1f / 14));
        assertEquals(255, PortalGemEffect.generationScale(1f / 60));
    }

    @Test
    void nativeScalingKeepsAtLeastOneElementAndExpiredGeneratorsStop() {
        assertEquals(1, PortalGemEffect.scaledCount(2, 50));
        assertEquals(2, PortalGemEffect.scaledCount(2, 255));
        assertEquals(0, PortalGemEffect.scaledCount(0, 50));
        var emitter = effect(new Node());
        emitter.request(Vector3f.ZERO);
        emitter.advance(0.05f);
        emitter.advance(0.05f);
        assertEquals(4, emitter.getNumVisibleParticles());
        emitter.advance(0.05f);
        assertEquals(4, emitter.getNumVisibleParticles());
    }

    private static Particle live(PortalGemEffect emitter) {
        for (Particle p : emitter.getParticles()) if (p.life > 0) return p;
        throw new AssertionError("No emitted particle");
    }
}
