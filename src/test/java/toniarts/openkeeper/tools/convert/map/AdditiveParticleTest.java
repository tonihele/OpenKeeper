/* SPDX-License-Identifier: GPL-3.0-or-later */
package toniarts.openkeeper.tools.convert.map;

import com.jme3.asset.DesktopAssetManager;
import com.jme3.asset.TextureKey;
import com.jme3.effect.ParticleEmitter;
import com.jme3.effect.ParticleMesh;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Node;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import java.lang.reflect.Proxy;
import java.util.EnumSet;
import java.util.List;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.utils.AssetUtils;
import toniarts.openkeeper.utils.Color;
import toniarts.openkeeper.view.effect.EffectManagerState;
import toniarts.openkeeper.view.effect.VisualEffect;
import static org.junit.jupiter.api.Assertions.*;

class AdditiveParticleTest {
    @Test
    void additiveSparklesUseOneOneRatherThanMultiplyingByTextureAlphaAgain() {
        var assets = new DesktopAssetManager(true) {
            @Override public Texture loadTexture(TextureKey key) { return new Texture2D(); }
        };
        ArtResource art = art(ArtResource.ArtResourceType.ADDITIVE_ALPHA, "synthetic additive sparkle");
        assertEquals(BlendMode.Additive,
                AssetUtils.createParticleMaterial(art, assets).getAdditionalRenderState().getBlendMode());
        ArtResource plain = art(ArtResource.ArtResourceType.ALPHA, "synthetic alpha particle");
        assertEquals(BlendMode.Alpha,
                AssetUtils.createParticleMaterial(plain, assets).getAdditionalRenderState().getBlendMode());
    }

    @Test
    void additiveFadeReducesRgbWhileKeepingAlphaOpaque() throws Exception {
        var effect = new Effect();
        effect.setName("synthetic empty effect");
        effect.setFlags(EnumSet.of(Effect.EffectFlag.GENERATE_EFFECT_ELEMENTS));
        effect.setGenerateIds(List.of());
        var data = (IKwdFile) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{IKwdFile.class},
                (proxy, method, args) -> effect);
        var visual = new VisualEffect(new EffectManagerState(data, null), new Node(), effect);
        var particle = new EffectElement();
        particle.setArtResource(art(ArtResource.ArtResourceType.ADDITIVE_ALPHA, "synthetic sparkle"));
        particle.setColor(new Color(255, 255, 255));
        particle.setFlags(EnumSet.of(EffectElement.EffectElementFlag.FADE));
        particle.setFadePercentage((short) 100);
        var emitter = new ParticleEmitter("synthetic", ParticleMesh.Type.Triangle, 1);
        var apply = VisualEffect.class.getDeclaredMethod("applyParticleColor", ParticleEmitter.class, EffectElement.class);
        apply.setAccessible(true);
        apply.invoke(visual, emitter, particle);
        assertEquals(ColorRGBA.White, emitter.getStartColor());
        assertEquals(new ColorRGBA(0, 0, 0, 1), emitter.getEndColor());
        particle.setFlags(EnumSet.noneOf(EffectElement.EffectElementFlag.class));
        apply.invoke(visual, emitter, particle);
        assertEquals(ColorRGBA.White, emitter.getEndColor());
    }

    private static ArtResource art(ArtResource.ArtResourceType type, String name) {
        var art = new ArtResource();
        art.setType(type);
        art.setName(name);
        art.setFlags(EnumSet.noneOf(ArtResource.ArtResourceFlag.class));
        return art;
    }
}
