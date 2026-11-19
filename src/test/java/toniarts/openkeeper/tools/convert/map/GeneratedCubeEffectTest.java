/* SPDX-License-Identifier: GPL-3.0-or-later */
package toniarts.openkeeper.tools.convert.map;

import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AppStateManager;
import com.jme3.effect.ParticleEmitter;
import com.jme3.effect.ParticleMesh;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.event.GeneratedEffect;
import toniarts.openkeeper.view.PlayerEntityViewState;
import toniarts.openkeeper.view.effect.EffectManagerState;
import toniarts.openkeeper.view.effect.VisualEffect;
import toniarts.openkeeper.view.fogofwar.IFogOfWarInformation;
import static org.junit.jupiter.api.Assertions.*;

class GeneratedCubeEffectTest {
    @Test
    @SuppressWarnings("unchecked")
    void serverEventStartsFiniteGeneratorAndItsParticlesExpireIndependently() throws Exception {
        // Synthetic values; no fixtures extracted from the original game.
        Effect definition = new Effect();
        definition.setGenerationType(Effect.GenerationType.CUBE_GEN);
        definition.setFlags(EnumSet.of(Effect.EffectFlag.GENERATE_EFFECT_ELEMENTS));
        definition.setGenerateIds(List.of());
        definition.setElementsPerTurn((short) 3);
        definition.setMinHp(5);
        definition.setMaxHp(5);
        var data = (IKwdFile) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{IKwdFile.class}, (p, m, a) -> m.getName().equals("getEffect") ? definition : null);
        var fog = (IFogOfWarInformation) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{IFogOfWarInformation.class}, (p, m, a) -> true);
        var emitter = new ParticleEmitter("synthetic stars", ParticleMesh.Type.Triangle, 3);
        emitter.setLowLife(1);
        emitter.setHighLife(1);
        VisualEffect[] loaded = {null};
        var effects = new EffectManagerState(data, null) {
            @Override public VisualEffect load(Node parent, Vector3f position, int id, boolean infinite) {
                var visual = super.load(parent, position, id, infinite);
                try {
                    var field = VisualEffect.class.getDeclaredField("effectElements");
                    field.setAccessible(true);
                    var element = new EffectElement();
                    element.setMaxHp(20);
                    ((Map<Spatial, EffectElement>) field.get(visual)).put(emitter, element);
                    visual.getEffectNode().attachChild(emitter);
                } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
                loaded[0] = visual;
                return visual;
            }
        };
        var app = new SimpleApplication() { @Override public void simpleInitApp() { } };
        var states = new AppStateManager(app);
        states.attach(effects);
        var entities = new DefaultEntityData();
        var root = new Node();
        var view = new PlayerEntityViewState(data, null, entities, (short) 3, null, root, null, fog);
        view.initialize(states, app);
        try {
            var show = PlayerEntityViewState.class.getDeclaredMethod("showGeneratedEffect", GeneratedEffect.class);
            show.setAccessible(true);
            show.invoke(view, new GeneratedEffect(900, new Vector3f(2, 1, 3), .4f));
            assertNotNull(loaded[0]);
            assertEquals(new Vector3f(2, 1, 3), loaded[0].getEffectNode().getLocalTranslation());
            for (int step = 0; step < 5; step++) effects.update(.05f);
            assertEquals(15, emitter.getNumVisibleParticles());
            effects.update(.05f);
            assertEquals(15, emitter.getNumVisibleParticles(), "Generator stops; its particles remain");
            for (int frame = 0; frame < 30; frame++) {
                root.updateLogicalState(.05f);
                effects.update(.05f);
            }
            assertNull(loaded[0].getEffectNode().getParent(), "Finite effect cleans up after its particles die");
        } finally {
            view.cleanup();
            effects.clearActiveEffects();
            entities.close();
        }
    }
}
