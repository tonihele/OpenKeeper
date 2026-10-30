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
package toniarts.openkeeper.view.text;

import com.simsilica.es.EntityId;
import java.lang.reflect.Proxy;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.map.IRoomInformation;
import toniarts.openkeeper.game.map.IRoomsInformation;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A destroyed Dungeon Heart has no tooltip text, instead of "Health: 100 %".
 */
class DestroyedHeartTooltipTest {

    private static final String HEART_TEXT = "Dungeon Heart.  Health: %37%%.";

    @Test
    void destroyedHeartHasNoTooltip() {
        assertEquals("", tooltip(true));
    }

    @Test
    void workingHeartShowsItsHealth() {
        assertEquals("Dungeon Heart.  Health: 40%.", tooltip(false));
    }

    @SuppressWarnings("unchecked")
    private static String tooltip(boolean destroyed) {
        IRoomInformation room = (IRoomInformation) Proxy.newProxyInstance(DestroyedHeartTooltipTest.class.getClassLoader(),
                new Class<?>[]{IRoomInformation.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "isDungeonHeart" -> true;
                    case "isDestroyed" -> destroyed;
                    case "getHealthPercent" -> 40;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        IRoomsInformation<IRoomInformation> rooms = (IRoomsInformation<IRoomInformation>) Proxy.newProxyInstance(
                DestroyedHeartTooltipTest.class.getClassLoader(), new Class<?>[]{IRoomsInformation.class},
                (proxy, method, args) -> room);
        return new RoomTextParser(rooms).parseText(HEART_TEXT, new EntityId(1), new Keeper(false, (short) 4));
    }

}
