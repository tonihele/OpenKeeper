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
package toniarts.openkeeper.view.effect;

import java.lang.reflect.Proxy;
import java.util.Map;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.event.GameEvent;
import toniarts.openkeeper.game.event.GameEventType;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.RoomFixtures;
import toniarts.openkeeper.tools.convert.map.Variable;
import toniarts.openkeeper.utils.Point;
import static org.junit.jupiter.api.Assertions.*;

class RoomActionEffectsTest {
    @Test
    void everyBuiltTileRequestsTheNativeBlockCreationEffect() {
        for (int x = 0; x < 3; x++) {
            var request = RoomActionEffects.resolve(event(GameEventType.ROOM_TILE_BUILT, new Point(x, 2)), null);
            assertEquals(81, request.effectId());
            assertEquals(1.5f, request.height());
            assertNull(request.goldValue());
        }
    }

    @Test
    void soldTileUsesTheEventRoomAndRefundValueAfterTerrainHasReverted() throws Exception {
        Variable.MiscVariable percentage = new Variable.MiscVariable();
        var value = Variable.MiscVariable.class.getDeclaredField("value");
        value.setAccessible(true);
        value.setFloat(percentage, 50);
        var room = RoomFixtures.room((short) 7, 501);
        IKwdFile level = (IKwdFile) Proxy.newProxyInstance(IKwdFile.class.getClassLoader(), new Class<?>[]{IKwdFile.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getRoomById" -> ((Number) args[0]).intValue() == 7 ? room : null;
                    case "getVariables" -> Map.of(Variable.MiscVariable.MiscType.ROOM_SELL_VALUE_PERCENTAGE_OF_COST, percentage);
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        var request = RoomActionEffects.resolve(event(GameEventType.ROOM_SOLD, new Point(2, 2)), level);
        assertEquals(22, request.effectId());
        assertEquals(1f, request.height());
        assertEquals(250, request.goldValue());
    }

    @Test
    void placementSoundAndUnpositionedEventsDoNotCreateExtraTileEffects() {
        assertNull(RoomActionEffects.resolve(event(GameEventType.ROOM_TILES_LAID, new Point(1, 1)), null));
        assertNull(RoomActionEffects.resolve(event(GameEventType.ROOM_TILE_BUILT, null), null));
    }

    private static GameEvent event(GameEventType type, Point position) {
        return new GameEvent(type, GameEvent.ThingFlag.ROOM, 7, null, 0, null, position);
    }
}
