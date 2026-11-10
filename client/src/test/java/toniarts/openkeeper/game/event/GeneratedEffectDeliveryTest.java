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
package toniarts.openkeeper.game.event;

import com.jme3.math.Vector3f;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.state.session.GameSessionListener;
import toniarts.openkeeper.game.state.session.LocalGameSession;
import static org.junit.jupiter.api.Assertions.*;

class GeneratedEffectDeliveryTest {
    @Test
    void serverAndLocalSessionPreserveTheExactPlacementForEveryListener() {
        var session = new LocalGameSession();
        var received = new ArrayList<Object[]>();
        var listener = (GameSessionListener) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{GameSessionListener.class}, (p, method, args) -> {
                    if (method.getName().equals("equals")) {
                        return p == args[0];
                    }
                    assertEquals("onEffect", method.getName());
                    received.add(args);
                    return null;
                });
        session.addGameSessionListener(listener);
        var server = new ServerGameEventService(null, session);
        var position = new Vector3f(10.25f, 1f, 20.75f);
        server.emitEffect(123, position, .75f);
        assertEquals(1, received.size());
        assertArrayEquals(new Object[]{123, position, .75f}, received.getFirst());
        session.removeGameSessionListener(listener);
    }
}
