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
package toniarts.openkeeper.game.controller.room;

import com.simsilica.es.EntityData;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.controller.IObjectsController;
import toniarts.openkeeper.game.controller.room.AbstractRoomController.ObjectType;
import toniarts.openkeeper.tools.convert.map.TempleFixtures;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TempleControllerTest {

    private TempleController temple;
    private final List<Object[]> loadedObjects = new ArrayList<>();

    @BeforeEach
    void setUp() {
        EntityData entityData = new DefaultEntityData();
        RoomInstance roomInstance = new RoomInstance(TempleFixtures.temple());
        for (int x = 10; x < 15; x++) {
            for (int y = 20; y < 25; y++) {
                roomInstance.addCoordinate(new Point(x, y));
            }
        }
        IObjectsController objectsController = (IObjectsController) Proxy.newProxyInstance(
                IObjectsController.class.getClassLoader(), new Class<?>[]{IObjectsController.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "loadObject" -> {
                        loadedObjects.add(args);
                        yield entityData.createEntity();
                    }
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        temple = new TempleController(entityData.createEntity(), entityData, null, roomInstance, objectsController, () -> 0);
        temple.construct();
    }

    @Test
    void thePoolIsTheTilesWithTheTempleAllAround() {
        assertTrue(temple.isSacrificePool(new Point(12, 22)));
        assertTrue(temple.isSacrificePool(new Point(11, 21)));
        assertFalse(temple.isSacrificePool(new Point(10, 22)));
        assertFalse(temple.isSacrificePool(new Point(14, 24)));
    }

    @Test
    void theTempleHandIsPlacedInTheCenterOfTheFirstFiveByFiveSquare() {
        List<Object[]> hands = loadedObjects.stream()
                .filter(args -> (short) args[0] == TempleController.OBJECT_TEMPLE_HAND_ID).toList();

        assertEquals(1, hands.size());
        assertEquals(12, (int) hands.get(0)[2]);
        assertEquals(22, (int) hands.get(0)[3]);
    }

    @Test
    void creaturesPrayOnEveryFloorTileAroundThePool() {
        Collection<Point> places = temple.getObjectControl(ObjectType.WORSHIPPER).getAvailableCoordinates();

        assertEquals(16, temple.getMaxCapacity());
        assertEquals(16, places.size());
        assertFalse(places.contains(new Point(12, 22)));
        assertTrue(places.contains(new Point(10, 20)));
    }

}
