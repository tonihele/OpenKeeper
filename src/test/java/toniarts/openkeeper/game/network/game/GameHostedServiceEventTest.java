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
package toniarts.openkeeper.game.network.game;

import com.jme3.network.HostedConnection;
import com.simsilica.es.EntityId;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.event.GameEvent;
import toniarts.openkeeper.game.event.GameEventType;
import toniarts.openkeeper.game.state.lobby.ClientInfo;
import toniarts.openkeeper.game.state.session.GameSessionListener;
import toniarts.openkeeper.utils.Point;
import static org.junit.jupiter.api.Assertions.*;

class GameHostedServiceEventTest {
    private final GameHostedService service = new GameHostedService();
    private final EntityId entity = new EntityId(42);
    private final Point position = new Point(7, 9);

    @Test
    void broadcastReachesEveryClientOnceWithCompletePayload() throws Exception {
        var first = connect((short) 3);
        var second = connect((short) 4);
        var third = connect((short) 5);

        send(null);

        for (var client : List.of(first, second, third)) {
            assertEquals(1, client.events.size());
            assertArrayEquals(new Object[]{GameEventType.MELEE_HIT,
                GameEvent.ThingFlag.CREATURE, 11, GameEvent.ThingFlag.DOOR, 12,
                entity, position}, client.events.get(0));
        }
    }

    @Test
    void targetedEventReachesOnlyItsKeeper() throws Exception {
        var clients = List.of(connect((short) 3), connect((short) 4), connect((short) 5));
        // Exercise every recipient, independent of the map's iteration order.
        for (short target = 3; target <= 5; target++) {
            clients.forEach(client -> client.events.clear());
            send(target);
            for (var client : clients) {
                assertEquals(client.info.getKeeper().getId() == target ? 1 : 0,
                        client.events.size());
            }
        }
    }

    @Test
    void unknownAndDisconnectedTargetsAreIgnored() throws Exception {
        var connected = connect((short) 3);
        var disconnected = connect((short) 4);
        players().remove(disconnected.info);

        send((short) 99);
        send((short) 4);
        assertTrue(connected.events.isEmpty());
        assertTrue(disconnected.events.isEmpty());

        send(null);
        assertEquals(1, connected.events.size());
        assertTrue(disconnected.events.isEmpty());
    }

    @Test
    void emptyServerAcceptsBroadcastAndTargetedEvents() {
        assertDoesNotThrow(() -> send(null));
        assertDoesNotThrow(() -> send((short) 3));
    }

    private void send(Short playerId) {
        service.sendEvent(GameEventType.MELEE_HIT, GameEvent.ThingFlag.CREATURE, 11,
                GameEvent.ThingFlag.DOOR, 12, entity, position, playerId);
    }

    @SuppressWarnings("unchecked")
    private Map<ClientInfo, Object> players() throws Exception {
        var field = GameHostedService.class.getDeclaredField("players");
        field.setAccessible(true);
        return (Map<ClientInfo, Object>) field.get(service);
    }

    private Client connect(short keeperId) throws Exception {
        var info = new ClientInfo(0, "synthetic", keeperId);
        info.setKeeper(new Keeper(false, keeperId));
        var client = new Client(info, new ArrayList<>());
        var callback = (GameSessionListener) Proxy.newProxyInstance(
                GameSessionListener.class.getClassLoader(), new Class<?>[]{GameSessionListener.class},
                (proxy, method, arguments) -> {
                    assertEquals("onEvent", method.getName());
                    client.events.add(arguments.clone());
                    return null;
                });
        // Exercise the actual hosted-session forwarding without opening sockets
        // or adding a production API just to install test callbacks.
        var sessionClass = Class.forName(GameHostedService.class.getName() + "$GameSessionImpl");
        var constructor = sessionClass.getDeclaredConstructor(
                GameHostedService.class, HostedConnection.class, ClientInfo.class);
        constructor.setAccessible(true);
        var session = constructor.newInstance(service, null, info);
        var callbackField = sessionClass.getDeclaredField("callback");
        callbackField.setAccessible(true);
        callbackField.set(session, callback);
        players().put(info, session);
        return client;
    }

    private record Client(ClientInfo info, List<Object[]> events) {
    }
}
