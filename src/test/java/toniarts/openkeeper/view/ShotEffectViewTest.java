/*
 * Copyright (C) 2014-2026 OpenKeeper
 *
 * OpenKeeper is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OpenKeeper is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenKeeper.  If not, see <http://www.gnu.org/licenses/>.
 */
package toniarts.openkeeper.view;

import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AppStateManager;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.EnumSet;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.event.GeneratedEffect;
import toniarts.openkeeper.game.component.ShotViewState;
import toniarts.openkeeper.tools.convert.map.Effect;
import toniarts.openkeeper.tools.convert.map.ShotEffectFixtures;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Shot;
import toniarts.openkeeper.tools.convert.map.ShotFixtures;
import toniarts.openkeeper.view.effect.EffectManagerState;
import toniarts.openkeeper.view.effect.VisualEffect;
import toniarts.openkeeper.view.fogofwar.IFogOfWarInformation;

import static org.junit.jupiter.api.Assertions.*;

class ShotEffectViewTest {

    @Test
    void finiteLifetimeEffectCanStartWithoutAShotEntity() throws Exception {
        try (Fixture f = new Fixture()) {
            var show = PlayerEntityViewState.class.getDeclaredMethod("showGeneratedEffect", GeneratedEffect.class);
            show.setAccessible(true);
            show.invoke(f.view, new GeneratedEffect(981, new Vector3f(3, 1, 5), 0, true));
            assertNotNull(f.general);
            assertEquals(1, f.general.generations);
        }
    }

    @Test
    void instantShotStartsFiniteGeneralEffectWhichOutlastsItsModel() throws Exception {
        try (Fixture f = new Fixture()) {
            var id = f.entities.createEntity();
            f.entities.setComponents(id, new Position(0, new Vector3f(3, 1, 5)), new ShotViewState((short) 118));
            var entity = f.entities.getEntity(id, Position.class, ShotViewState.class);
            var create = PlayerEntityViewState.class.getDeclaredMethod("createShotModel", com.simsilica.es.Entity.class);
            create.setAccessible(true);
            Spatial model = (Spatial) create.invoke(f.view, entity);
            assertNotNull(f.general);
            assertEquals(1, f.general.generations);
            assertSame(model, f.general.getEffectNode().getParent());
            model.setLocalTranslation(3, 1, 5);
            Node oldParent = model.getParent();

            f.view.releaseShotEffect(f.general, model, f.shot);
            model.removeFromParent();

            assertSame(oldParent, f.general.getEffectNode().getParent());
            assertEquals(new Vector3f(3, 1, 5), f.general.getEffectNode().getLocalTranslation());
            assertFalse(f.general.removed);
        }
    }

    @Test
    void killEffectsFlagRemovesGeneralEffectWithTheShot() throws Exception {
        try (Fixture f = new Fixture()) {
            f.shot.getFlags().add(Shot.ShotFlag.KILL_EFFECTS_WHEN_DEAD);
            Node model = new Node();
            var effect = new CountingEffect(f.effects, model);
            f.view.releaseShotEffect(effect, model, f.shot);
            assertTrue(effect.removed);
            assertNull(effect.getEffectNode().getParent());
        }
    }

    private static class CountingEffect extends VisualEffect {
        int generations;
        boolean removed;

        CountingEffect(EffectManagerState manager, Node parent) {
            super(manager, parent, ShotEffectFixtures.emptyEffect());
        }

        @Override public void generateThroughoutLifetime() { generations++; }
        @Override public void removeEffect() { removed = true; super.removeEffect(); }
    }

    private static class Fixture implements AutoCloseable {
        final DefaultEntityData entities = new DefaultEntityData();
        final Shot shot = ShotFixtures.shot((short) 118, Shot.ProcessType.FEAR, 3,
                EnumSet.of(Shot.ShotProcessFlag.AT_START));
        final EffectManagerState effects;
        final PlayerEntityViewState view;
        CountingEffect general;

        Fixture() {
            // No model/art resource is needed: this tests the independent general effect.
            IKwdFile data = (IKwdFile) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class<?>[]{IKwdFile.class}, (p, m, a) -> switch (m.getName()) {
                        case "getShotById" -> shot;
                        case "getEffect" -> ShotEffectFixtures.emptyEffect();
                        default -> null;
                    });
            IFogOfWarInformation fog = (IFogOfWarInformation) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class<?>[]{IFogOfWarInformation.class}, (p, m, a) -> true);
            effects = new EffectManagerState(data, null) {
                @Override public VisualEffect load(Node parent, Vector3f position, int id, boolean infinite) {
                    if (id == 0) return null;
                    assertFalse(infinite);
                    general = new CountingEffect(this, parent);
                    return general;
                }
            };
            // Protected map setters are exposed by the synthetic fixture helper.
            ShotFixtures.setGeneralEffect(shot, 981);
            var app = new SimpleApplication() { @Override public void simpleInitApp() { } };
            var states = new AppStateManager(app);
            states.attach(effects);
            view = new PlayerEntityViewState(data, null, entities, (short) 3, null, new Node(), null, fog);
            view.initialize(states, app);
        }

        @Override public void close() { view.cleanup(); entities.close(); }
    }
}
