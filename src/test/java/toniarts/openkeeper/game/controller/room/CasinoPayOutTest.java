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
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.component.ObjectComponent;
import toniarts.openkeeper.game.component.ObjectViewState;
import toniarts.openkeeper.game.component.ObjectViewState.GameObjectAnimState;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.controller.IObjectsController;
import toniarts.openkeeper.game.controller.room.AbstractRoomController.ObjectType;
import toniarts.openkeeper.game.controller.room.CasinoController.PayOut;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static toniarts.openkeeper.tools.convert.map.RoomFixtures.roomWithObjects;

class CasinoPayOutTest {

    private static final short PAY_OUT_ADJUSTER = 95;
    private static final short ROULETTE_TABLE = 87;

    private final EntityData entityData = new DefaultEntityData();

    @Test
    void newCasinoPaysOutInSmiles() {
        assertEquals(PayOut.SMILES, buildCasino(5).getPayOut());
    }

    @Test
    void adjusterTakesTheFirstFurnitureTileWithNoFurnitureAbove() {
        CasinoController casino = buildCasino(5);

        // The furniture stands on (2, 2), (2, 4), (3, 3), (4, 2) and (4, 4)
        assertEquals(new Point(2, 2), getTile(findObject(PAY_OUT_ADJUSTER)));
        assertEquals(4, casino.getFloorFurnitureCount());
        assertEquals(4, casino.getMaxCapacity(ObjectType.GAMBLER));
    }

    @Test
    void adjusterSwitchesThePayOut() {
        CasinoController casino = buildCasino(5);
        EntityId adjuster = findObject(PAY_OUT_ADJUSTER);

        assertTrue(casino.switchFurniture(adjuster));
        assertEquals(PayOut.MONEY, casino.getPayOut());
        assertTrue(casino.switchFurniture(adjuster));
        assertEquals(PayOut.SMILES, casino.getPayOut());
    }

    @Test
    void adjusterShowsTheIndicatorOfThePayOut() {
        CasinoController casino = buildCasino(5);
        EntityId adjuster = findObject(PAY_OUT_ADJUSTER);

        assertEquals(GameObjectAnimState.ADDITIONAL_RESOURCE_1, entityData.getComponent(adjuster, ObjectViewState.class).animState);
        casino.switchFurniture(adjuster);
        assertEquals(GameObjectAnimState.ADDITIONAL_RESOURCE_2, entityData.getComponent(adjuster, ObjectViewState.class).animState);
    }

    @Test
    void adjusterOfAControllerBuiltAgainShowsTheIndicatorOfThePayOut() {
        CasinoController casino = buildCasino(5);
        EntityId adjuster = findObject(PAY_OUT_ADJUSTER);

        // A controller built again on the room's entities doesn't place the furniture
        CasinoController rebuilt = new CasinoController(casino.getEntityId(), entityData, null, casino.getRoomInstance(), null, null);
        rebuilt.switchFurniture(adjuster);
        assertEquals(GameObjectAnimState.ADDITIONAL_RESOURCE_2, entityData.getComponent(adjuster, ObjectViewState.class).animState);
    }

    @Test
    void tableIsNoSwitch() {
        CasinoController casino = buildCasino(5);

        assertFalse(casino.switchFurniture(findObject(ROULETTE_TABLE)));
        assertEquals(PayOut.SMILES, casino.getPayOut());
    }

    @Test
    void payOutAndOneAdjusterStayWhenTheCasinoGrows() {
        CasinoController casino = buildCasino(5);
        casino.setPayOut(PayOut.MONEY);

        casino.construct();

        assertEquals(PayOut.MONEY, casino.getPayOut());
        assertEquals(1, entityData.getEntities(ObjectComponent.class).stream()
                .filter(entity -> entity.get(ObjectComponent.class).objectId == PAY_OUT_ADJUSTER).count());
    }

    @Test
    void soldCasinoTakesItsAdjusterAlong() {
        CasinoController casino = buildCasino(5);

        casino.remove();

        assertNull(findObject(PAY_OUT_ADJUSTER));
    }

    private EntityId findObject(short objectId) {
        return entityData.getEntities(ObjectComponent.class).stream()
                .filter(entity -> entity.get(ObjectComponent.class).objectId == objectId)
                .map(entity -> entity.getId())
                .findFirst()
                .orElse(null);
    }

    private Point getTile(EntityId entityId) {
        assertNotNull(entityId);
        return WorldUtils.vectorToPoint(entityData.getComponent(entityId, Position.class).position);
    }

    private CasinoController buildCasino(int size) {
        IObjectsController objectsController = (IObjectsController) Proxy.newProxyInstance(
                IObjectsController.class.getClassLoader(),
                new Class<?>[]{IObjectsController.class},
                (proxy, method, args) -> {
                    if (!method.getName().equals("loadObject")) {
                        return null;
                    }
                    EntityId entityId = entityData.createEntity();
                    entityData.setComponents(entityId,
                            new ObjectComponent((short) args[0], null),
                            new ObjectViewState((short) args[0], null, GameObjectAnimState.MESH_RESOURCE, true),
                            new Position(0, WorldUtils.pointToVector3f((int) args[2], (int) args[3])));
                    return entityId;
                });

        // Only Roulette Tables, to tell the furniture apart from the adjuster
        RoomInstance roomInstance = new RoomInstance(roomWithObjects((short) 15, ROULETTE_TABLE, (short) 0, (short) 0, (short) 0, (short) 0, (short) 0));
        for (int x = 1; x <= size; x++) {
            for (int y = 1; y <= size; y++) {
                roomInstance.addCoordinate(new Point(x, y));
            }
        }

        CasinoController casino = new CasinoController(entityData.createEntity(), entityData, null, roomInstance, objectsController, null);
        casino.construct();
        return casino;
    }
}
