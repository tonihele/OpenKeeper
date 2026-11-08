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
package toniarts.openkeeper.game.controller;

import com.simsilica.es.EntityData;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.component.RoomEffect;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.tools.convert.map.HeroGateFixtures;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The in-game hero gate and the 3D front end hero gate are built differently
 * (issue #675)
 */
class HeroGateControllerTest {

    private static final short OBJECT_GEM_HOLDER_ID = 131;
    private static final short OBJECT_CHAIN_ID = 132;

    private final EntityData entityData = new DefaultEntityData();
    private final List<Short> loadedObjects = new ArrayList<>();

    @Test
    void inGameHeroGateHasNoFrontEndFurniture() {
        IRoomController room = constructRoom(HeroGateFixtures.heroGateRoom((short) 18, "Hero Gate [ Final ]", Room.TileConstruction.HERO_GATE));

        assertTrue(loadedObjects.isEmpty(), "Loaded objects " + loadedObjects);
        assertTrue(room.getFloorFurniture().isEmpty());
        assertTrue(room.getWallFurniture().isEmpty());
        assertNull(entityData.getComponent(room.getEntityId(), RoomEffect.class));
    }

    @Test
    void frontEndHeroGateKeepsItsFurniture() {
        IRoomController room = constructRoom(HeroGateFixtures.heroGateRoom((short) 21, "Hero Gate [ FrontEnd ]", Room.TileConstruction.HERO_GATE_FRONT_END));

        assertTrue(loadedObjects.contains(OBJECT_GEM_HOLDER_ID));
        assertTrue(loadedObjects.contains(OBJECT_CHAIN_ID));
        assertFalse(room.getFloorFurniture().isEmpty());
        assertEquals(loadedObjects.size(), room.getFloorFurniture().size());
    }

    private IRoomController constructRoom(Room room) {

        // The layout of the hero gates in Level20 and FrontEnd3DLevel: 3 x 5 tiles and one in front
        RoomInstance roomInstance = new RoomInstance(room);
        roomInstance.setOwnerId((short) 1);
        for (int y = 4; y <= 8; y++) {
            for (int x = 48; x <= 50; x++) {
                roomInstance.addCoordinate(new Point(x, y));
            }
        }
        roomInstance.addCoordinate(new Point(49, 9));

        IRoomController controller = RoomControllerFactory.constructRoom(entityData, null, roomInstance,
                objectsController(), Collections.emptyMap(), null);
        controller.construct();

        return controller;
    }

    private IObjectsController objectsController() {
        return (IObjectsController) Proxy.newProxyInstance(IObjectsController.class.getClassLoader(),
                new Class<?>[]{IObjectsController.class}, (proxy, method, args) -> {
                    if (method.getName().equals("loadObject")) {
                        loadedObjects.add((Short) args[0]);
                        return entityData.createEntity();
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
    }

}
