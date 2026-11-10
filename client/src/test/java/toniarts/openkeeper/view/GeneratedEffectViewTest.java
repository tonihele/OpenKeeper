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
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.event.GeneratedEffect;
import toniarts.openkeeper.tools.convert.map.Effect;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.view.effect.EffectManagerState;
import toniarts.openkeeper.view.effect.VisualEffect;
import toniarts.openkeeper.view.fogofwar.IFogOfWarInformation;
import static org.junit.jupiter.api.Assertions.*;

class GeneratedEffectViewTest {
    @Test
    void finiteEffectsUseExactCoordinatesAndAreSuppressedInFog() throws Exception {
        var data = (IKwdFile) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{IKwdFile.class}, (p, m, a) -> m.getName().equals("getEffect") ? new Effect() : null);
        boolean[] visible = {false};
        var fog = (IFogOfWarInformation) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{IFogOfWarInformation.class}, (p, m, a) -> visible[0]);
        var positions = new ArrayList<Vector3f>();
        var effects = new EffectManagerState(data, null) {
            @Override public VisualEffect load(Node parent, Vector3f position, int id, boolean infinite) {
                assertEquals(123, id);
                assertFalse(infinite);
                positions.add(position.clone());
                return null;
            }
        };
        var app = new SimpleApplication() {
            @Override public void simpleInitApp() { }
        };
        var states = new AppStateManager(app);
        states.attach(effects);
        var entities = new DefaultEntityData();
        try {
            var view = new PlayerEntityViewState(data, null, entities, (short) 3, null, new Node(), null, fog);
            view.initialize(states, app);
            try {
                var show = PlayerEntityViewState.class.getDeclaredMethod("showGeneratedEffect", GeneratedEffect.class);
                show.setAccessible(true);
                var effect = new GeneratedEffect(123, new Vector3f(10.25f, 1f, 20.75f), .5f);
                show.invoke(view, effect);
                assertTrue(positions.isEmpty());
                visible[0] = true;
                show.invoke(view, effect);
                assertEquals(java.util.List.of(effect.position()), positions);
            } finally {
                view.cleanup();
            }
        } finally {
            entities.close();
        }
    }
}
