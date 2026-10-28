/* SPDX-License-Identifier: GPL-3.0-or-later */
package toniarts.openkeeper.tools.convert.map;

import java.lang.reflect.Proxy;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.tools.modelviewer.EffectInspector;
import toniarts.openkeeper.utils.Color;
import static org.junit.jupiter.api.Assertions.*;

class EffectInspectorTest {
    @Test
    void entranceNavigationDistinguishesParticleIdsAndPreservesRepeatedSlots() {
        Fixture f = new Fixture();
        var creature = f.inspector.describe(f.goblin);
        assertEquals(181, creature.links().getFirst().id());
        assertEquals(EffectInspector.Kind.EFFECT, creature.links().getFirst().kind());
        var effect = f.inspector.describe(f.effect);
        assertTrue(effect.fields().contains("Used by: Goblin entrance"));
        assertEquals(List.of(354, 354, 161), effect.links().stream().map(EffectInspector.Link::id).toList());
        assertEquals(List.of(EffectInspector.Kind.PARTICLE, EffectInspector.Kind.PARTICLE, EffectInspector.Kind.EFFECT),
                effect.links().stream().map(EffectInspector.Link::kind).toList());
        // The effect and particle catalogs can contain the same numeric ID.
        f.effect.setFlags(EnumSet.of(Effect.EffectFlag.GENERATE_EFFECTS));
        assertEquals(EffectInspector.Kind.EFFECT, f.inspector.describe(f.effect).links().getFirst().kind());
    }

    @Test
    void showsLifetimeAndTextureLoopingAsSeparateSettings() {
        Fixture f = new Fixture();
        var particle = f.inspector.describe(f.smoke);
        assertTrue(particle.fields().contains("Particle lifetime: 32 to 48 steps (~1.6 to 2.4 s at 20 Hz)"));
        assertTrue(particle.fields().contains("Frames: 16"));
        assertTrue(particle.fields().contains("Texture animation: does not loop"));
        assertTrue(f.inspector.describe(f.effect).fields().contains("Count per update: 3"));
        assertTrue(f.inspector.describe(f.effect).fields().contains("Generator lifetime: 100 steps (~5 s at 20 Hz)"));
    }

    @Test
    void creatureWithoutEntranceEffectHasNoNavigationTarget() {
        Fixture f = new Fixture();
        Creature imp = new Creature();
        imp.setName("Imp");
        assertTrue(f.inspector.describe(imp).links().isEmpty());
    }

    private static final class Fixture {
        final Effect effect = new Effect();
        final EffectElement smoke = new EffectElement();
        final Creature goblin = new Creature();
        final EffectInspector inspector;
        Fixture() {
            effect.setEffectId(181); effect.setName("Goblin Portal Entrance");
            effect.setGenerationType(Effect.GenerationType.CUBE_GEN);
            effect.setElementsPerTurn((short) 3); effect.setMinHp(100); effect.setMaxHp(100);
            effect.setFlags(EnumSet.of(Effect.EffectFlag.GENERATE_EFFECT_ELEMENTS));
            effect.setGenerateIds(List.of(354, 354)); effect.setNextEffectId(161);
            smoke.setEffectElementId(354); smoke.setName("Smoke");
            smoke.setFlags(EnumSet.noneOf(EffectElement.EffectElementFlag.class));
            smoke.setColor(new Color(100, 100, 100)); smoke.setMinHp(32); smoke.setMaxHp(48);
            ArtResource art = new ArtResource(); art.setName("sm");
            art.setType(ArtResource.ArtResourceType.ADDITIVE_ALPHA);
            art.setFlags(EnumSet.of(ArtResource.ArtResourceFlag.ANIMATING_TEXTURE, ArtResource.ArtResourceFlag.DOESNT_LOOP));
            art.setData(ArtResource.KEY_FRAMES, 16); smoke.setArtResource(art);
            goblin.setName("Goblin"); goblin.setEntranceEffectId(181);
            Map<Integer, Effect> effects = Map.of(181, effect);
            Map<Integer, EffectElement> particles = Map.of(354, smoke);
            IKwdFile data = (IKwdFile) Proxy.newProxyInstance(IKwdFile.class.getClassLoader(), new Class<?>[]{IKwdFile.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "getCreatureList" -> List.of(goblin);
                        case "getRooms", "getObjectList" -> List.of();
                        case "getEffect" -> effects.get((Integer) args[0]);
                        case "getEffectElement" -> particles.get((Integer) args[0]);
                        default -> throw new UnsupportedOperationException(method.getName());
                    });
            inspector = new EffectInspector(data);
        }
    }
}
