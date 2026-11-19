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

import com.jme3.math.FastMath;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.DoorComponent;
import toniarts.openkeeper.game.component.DoorViewState;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.tools.convert.map.Door;
import toniarts.openkeeper.tools.convert.map.DoorFixtures;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Who opens a door, as in DK II (0x4A55F0)
 */
class DoorViewSystemTest {

    private static final short KEEPER = 3;
    private static final short ALLY = 4;
    private static final short ENEMY = 5;
    private static final short WOODEN_DOOR = 2;
    private static final short BARRICADE = 7;
    private static final Point DOOR_TILE = new Point(5, 5);

    private final EntityData entityData = new DefaultEntityData();
    private final Map<Point, List<EntityId>> entitiesByTile = new HashMap<>();

    @Test
    void creatureOfTheKeeperInTheDoorwayOpensIt() {
        assertTrue(isOpen(WOODEN_DOOR, 0, KEEPER, DOOR_TILE));
    }

    @Test
    void creatureOfAnAllyOpensIt() {
        assertTrue(isOpen(WOODEN_DOOR, 0, ALLY, DOOR_TILE));
    }

    @Test
    void enemyAndNeutralCreaturesDoNotOpenIt() {
        assertFalse(isOpen(WOODEN_DOOR, 0, ENEMY, DOOR_TILE));
        assertFalse(isOpen(WOODEN_DOOR, 0, Player.NEUTRAL_PLAYER_ID, DOOR_TILE));
    }

    @Test
    void creatureInThePassageBesideTheDoorOpensIt() {

        // A door between walls east and west has its passage north to south
        assertTrue(isOpen(WOODEN_DOOR, 0, KEEPER, new Point(5, 4)));
        assertTrue(isOpen(WOODEN_DOOR, 0, KEEPER, new Point(5, 6)));
        assertFalse(isOpen(WOODEN_DOOR, 0, KEEPER, new Point(4, 5)));

        // A door turned to stand between walls north and south has its passage east to west
        assertTrue(isOpen(WOODEN_DOOR, -FastMath.HALF_PI, KEEPER, new Point(4, 5)));
        assertTrue(isOpen(WOODEN_DOOR, -FastMath.HALF_PI, KEEPER, new Point(6, 5)));
        assertFalse(isOpen(WOODEN_DOOR, -FastMath.HALF_PI, KEEPER, new Point(5, 6)));
    }

    @Test
    void barricadeNeverOpens() {
        assertFalse(isOpen(BARRICADE, 0, KEEPER, DOOR_TILE));
    }

    private boolean isOpen(short doorId, float rotation, short creatureOwner, Point creatureTile) {
        entitiesByTile.clear();
        EntityId door = entityData.createEntity();
        entityData.setComponents(door, new DoorComponent(doorId, false, false), new DoorViewState(doorId, false, false, false),
                new Position(rotation, WorldUtils.pointToVector3f(DOOR_TILE)), new Owner(KEEPER, KEEPER));
        entitiesByTile.computeIfAbsent(DOOR_TILE, p -> new ArrayList<>()).add(door);
        EntityId creature = entityData.createEntity();
        entityData.setComponents(creature, new CreatureComponent(), new Owner(creatureOwner, creatureOwner));
        entitiesByTile.computeIfAbsent(creatureTile, p -> new ArrayList<>()).add(creature);

        DoorViewSystem system = new DoorViewSystem(entityData, positionLookup(), levelInfo());
        system.processTick(0.25f);
        boolean open = entityData.getComponent(door, DoorViewState.class).open;
        system.stop();
        entityData.removeEntity(door);
        entityData.removeEntity(creature);
        return open;
    }

    private IEntityPositionLookup positionLookup() {
        return (IEntityPositionLookup) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{IEntityPositionLookup.class},
                (proxy, method, args) -> method.getName().equals("getEntitiesInLocation") && args.length == 1 && args[0] instanceof Point p
                ? entitiesByTile.getOrDefault(p, List.of()) : null);
    }

    private ILevelInfo levelInfo() {
        Keeper keeper = new Keeper(false, KEEPER);
        keeper.createAlliance(ALLY);
        Map<Short, Keeper> keepers = Map.of(KEEPER, keeper);
        Map<Short, Door> doors = Map.of(WOODEN_DOOR, DoorFixtures.door(WOODEN_DOOR, 0),
                BARRICADE, DoorFixtures.door(BARRICADE, 0, Door.DoorFlag.IS_BARRICADE));
        IKwdFile kwdFile = (IKwdFile) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{IKwdFile.class},
                (proxy, method, args) -> method.getName().equals("getDoorById") ? doors.get(((Number) args[0]).shortValue()) : null);
        return (ILevelInfo) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{ILevelInfo.class},
                (proxy, method, args) -> switch (method.getName()) {
            case "getPlayer" ->
                keepers.get((short) args[0]);
            case "getLevelData" ->
                kwdFile;
            default ->
                null;
        });
    }
}
