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
package toniarts.openkeeper.game.controller.object;

import com.jme3.math.Vector3f;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.Placeable;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.map.FakeMapData;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests which objects the creatures of a player may pick up
 */
class ObjectControllerTest {

    private static final short PLAYER_ID = 3;

    private DefaultEntityData entityData;
    private FakeMapData mapData;
    private ObjectController controller;
    private EntityId object;

    @BeforeEach
    void setUp() {
        entityData = new DefaultEntityData();
        mapData = new FakeMapData(5, 5);
        mapData.getTile(2, 2).ownerId = PLAYER_ID;
        IMapController mapController = (IMapController) Proxy.newProxyInstance(IMapController.class.getClassLoader(),
                new Class<?>[]{IMapController.class}, (proxy, method, args) -> switch (method.getName()) {
            case "getMapData" -> mapData;
            default -> throw new UnsupportedOperationException(method.getName());
        });
        object = entityData.createEntity();
        entityData.setComponent(object, new Placeable());
        controller = new ObjectController(object, entityData, null, null, mapController);
    }

    @AfterEach
    void tearDown() {
        entityData.close();
    }

    @Test
    void objectOnOwnLandIsPickable() {
        entityData.setComponent(object, new Position(0, new Vector3f(2, 0, 2)));

        assertTrue(controller.isPickableByPlayerCreature(PLAYER_ID));
    }

    @Test
    void objectOnOtherLandIsNotPickable() {
        entityData.setComponent(object, new Position(0, new Vector3f(1, 0, 1)));

        assertFalse(controller.isPickableByPlayerCreature(PLAYER_ID));
    }

    @Test
    void objectInTheKeepersHandIsNotPickable() {

        // In the hand the object has no position
        assertFalse(controller.isPickableByPlayerCreature(PLAYER_ID));
    }

}
