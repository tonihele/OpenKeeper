package toniarts.openkeeper.tools.convert.map;

import com.jme3.asset.DesktopAssetManager;
import com.jme3.asset.TextureKey;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.texture.image.ColorSpace;
import com.jme3.util.BufferUtils;
import java.lang.reflect.Proxy;
import java.util.EnumSet;
import java.util.List;
import toniarts.openkeeper.utils.Color;

/** Parsed definitions for native carrier-effect regression tests. */
public final class PortalGemEffectFixtures {
    private PortalGemEffectFixtures() {
    }

    public static IKwdFile level() {
        Effect effect = new Effect();
        effect.setName("Gem dust sparkles");
        effect.setFlags(EnumSet.of(Effect.EffectFlag.GENERATE_EFFECT_ELEMENTS));
        effect.setGenerateIds(List.of(444));
        effect.setMinHp(2);
        effect.setMaxHp(2);
        effect.setElementsPerTurn((short) 2);
        effect.setGenerationType(Effect.GenerationType.CUBE_GEN);
        ArtResource art = new ArtResource();
        art.setName("TestGemTexture");
        art.setType(ArtResource.ArtResourceType.ADDITIVE_ALPHA);
        art.setFlags(EnumSet.noneOf(ArtResource.ArtResourceFlag.class));
        art.setData(ArtResource.KEY_FRAMES, 16);
        EffectElement element = new EffectElement();
        element.setName("Gem dust sparkle element");
        element.setArtResource(art);
        element.setFlags(EnumSet.of(EffectElement.EffectElementFlag.FADE));
        element.setColor(new Color(180, 180, 250));
        element.setMinHp(40);
        element.setMaxHp(40);
        element.setMinScale(0.1f);
        element.setMaxScale(0.15f);
        return (IKwdFile) Proxy.newProxyInstance(IKwdFile.class.getClassLoader(), new Class<?>[]{IKwdFile.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getEffect" -> (int) args[0] == 257 ? effect : null;
                    case "getEffectElement" -> (int) args[0] == 444 ? element : null;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    public static DesktopAssetManager assets() {
        return new DesktopAssetManager(true) {
            @Override
            public Texture loadTexture(TextureKey key) {
                return new Texture2D(new Image(Image.Format.RGBA8, 1, 1,
                        BufferUtils.createByteBuffer(new byte[]{-1, -1, -1, -1}), ColorSpace.Linear));
            }
        };
    }

    public static Creature creature() {
        Creature creature = new Creature();
        creature.setFlags(EnumSet.noneOf(Creature.CreatureFlag.class));
        return creature;
    }
}
