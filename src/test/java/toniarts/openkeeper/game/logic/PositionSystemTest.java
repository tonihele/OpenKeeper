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
package toniarts.openkeeper.game.logic;

import com.jme3.math.Vector3f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.DoorComponent;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.map.FakeMapData;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PositionSystemTest {

    private static final short PLAYER = 3;
    private static final short ENEMY = 4;
    private static final short WOODEN_DOOR = 1;
    private static final Point TILE = new Point(2, 2);

    private final EntityData entityData = new DefaultEntityData();
    private PositionSystem positionSystem;
    private EntityId door;

    @BeforeEach
    void setUp() {
        FakeMapData mapData = new FakeMapData(5, 5);
        IMapController mapController = (IMapController) Proxy.newProxyInstance(IMapController.class.getClassLoader(),
                new Class<?>[]{IMapController.class}, (proxy, method, args) -> switch (method.getName()) {
            case "getMapData" -> mapData;
            default -> throw new UnsupportedOperationException(method.getName());
        });
        door = entityData.createEntity();
        Vector3f position = WorldUtils.pointToVector3f(TILE);
        entityData.setComponents(door, new DoorComponent(WOODEN_DOOR, false, true), new Owner(PLAYER, PLAYER),
                new Position(0, position));
        positionSystem = new PositionSystem(mapController, entityData, null, null, null);
    }

    @Test
    void blueprintDoesntBlockAnybody() {
        assertFalse(positionSystem.isTileBlocked(TILE, ENEMY));
        assertFalse(positionSystem.isTileBlocked(TILE, PLAYER));
    }

    @Test
    void builtDoorBlocksTheEnemyButNotTheOwner() {
        entityData.setComponent(door, new DoorComponent(WOODEN_DOOR, false, false));
        positionSystem.processTick(0.25f);

        assertTrue(positionSystem.isTileBlocked(TILE, ENEMY));
        assertFalse(positionSystem.isTileBlocked(TILE, PLAYER));
    }

    @Test
    void lockedDoorBlocksTheOwnerToo() {
        entityData.setComponent(door, new DoorComponent(WOODEN_DOOR, true, false));
        positionSystem.processTick(0.25f);

        assertTrue(positionSystem.isTileBlocked(TILE, PLAYER));
    }

}
