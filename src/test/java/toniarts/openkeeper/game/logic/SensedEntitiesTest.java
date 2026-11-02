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

import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.Set;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.DoorComponent;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.Senses;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.map.FakeMapData;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.tools.convert.map.TerrainFixtures;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SensedEntitiesTest {

    private static final short KEEPER = 3;
    private static final short WOODEN_DOOR = 1;

    private final EntityData entityData = new DefaultEntityData();

    @Test
    void doorBlocksTheEnemysSensesButNotTheOwners() {

        // A corridor: hero, floor, the keeper's door, keeper's creature
        FakeMapData mapData = new FakeMapData(4, 1);
        Terrain floor = TerrainFixtures.terrain((short) 1, TerrainFixtures.flags(), 0, 1, 1, (short) 1);
        IMapController mapController = (IMapController) Proxy.newProxyInstance(IMapController.class.getClassLoader(),
                new Class<?>[]{IMapController.class}, (proxy, method, args) -> switch (method.getName()) {
            case "getMapData" -> mapData;
            case "getTerrain" -> floor;
            default -> throw new UnsupportedOperationException(method.getName());
        });
        EntityId hero = add(Player.GOOD_PLAYER_ID, new Point(0, 0));
        entityData.setComponent(hero, new Senses(4, 4));
        EntityId door = add(KEEPER, new Point(2, 0));
        entityData.setComponent(door, new DoorComponent(WOODEN_DOOR, false, false));
        EntityId creature = add(KEEPER, new Point(3, 0));
        entityData.setComponent(creature, new Senses(4, 4));
        PositionSystem positionSystem = new PositionSystem(mapController, entityData, null, null, null);

        assertEquals(Set.of(door), positionSystem.getSensedEntities(hero));
        assertEquals(Set.of(door, hero), positionSystem.getSensedEntities(creature));
    }

    private EntityId add(short owner, Point tile) {
        EntityId entityId = entityData.createEntity();
        entityData.setComponents(entityId, new Owner(owner, owner), new Position(0, WorldUtils.pointToVector3f(tile)));
        return entityId;
    }
}
