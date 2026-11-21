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
package toniarts.openkeeper.game.controller.chicken;

import com.badlogic.gdx.math.Vector2;
import com.jme3.math.Vector3f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.Navigation;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.Stored;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.room.AbstractRoomController.ObjectType;
import toniarts.openkeeper.game.logic.IEntityPositionLookup;
import toniarts.openkeeper.game.map.FakeMapData;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A free-range chicken runs half a tile straight away from a creature on its
 * own tile or one of the eight around it, unless the creature is a worker or
 * in the air (DKII.EXE 0x49CEC0)
 */
class ChickenDodgeTest {

    private final EntityData entityData = new DefaultEntityData();
    private final EntityId chickenId = entityData.createEntity();
    private final Map<Point, List<ICreatureController>> creatures = new HashMap<>();
    private final Set<Point> solidTiles = new HashSet<>();
    private final ChickenController chicken = chicken();

    @Test
    void runsHalfATileStraightAwayFromACreatureNextToIt() {
        placeChicken(4, 4);
        creatures.put(new Point(5, 4), List.of(creature(5, 4, false, false)));

        assertTrue(chicken.dodgeCreature());

        Navigation navigation = entityData.getComponent(chickenId, Navigation.class);
        assertEquals(List.of(new Vector2(4, 4), new Vector2(3.5f, 4)), navigation.navigationPath);
        assertEquals(WorldUtils.vectorToPoint(3.5f, 4), navigation.target);
    }

    @Test
    void runsNorthFromACreatureStandingRightOnIt() {
        placeChicken(4, 4);
        creatures.put(new Point(4, 4), List.of(creature(4, 4, false, false)));

        assertTrue(chicken.dodgeCreature());

        assertEquals(new Vector2(4, 3.5f), entityData.getComponent(chickenId, Navigation.class).navigationPath.get(1));
    }

    @Test
    void workersAndCreaturesInTheAirDoNotScareIt() {
        placeChicken(4, 4);
        creatures.put(new Point(5, 4), List.of(creature(5, 4, true, false)));
        creatures.put(new Point(4, 5), List.of(creature(4, 5, false, true)));

        assertFalse(chicken.dodgeCreature());
        assertNull(entityData.getComponent(chickenId, Navigation.class));
    }

    @Test
    void creaturesFurtherAwayDoNotScareIt() {
        placeChicken(4, 4);
        creatures.put(new Point(6, 4), List.of(creature(6, 4, false, false)));

        assertFalse(chicken.dodgeCreature());
    }

    @Test
    void doesNotRunIntoRock() {
        placeChicken(4, 4);
        creatures.put(new Point(5, 5), List.of(creature(5, 5, false, false)));
        solidTiles.add(new Point(3, 3));

        // Up against the rock the half tile ends on the rock's tile
        entityData.setComponent(chickenId, new Position(0, new Vector3f(3.6f, 0, 3.6f)));

        assertFalse(chicken.dodgeCreature());
    }

    @Test
    void aCreatureThatLeftTheMapThisTickIsNotThere() {
        placeChicken(4, 4);

        // Picked up or removed earlier in the tick: the position lookup lists it until it updates
        ICreatureController gone = proxy(ICreatureController.class, (method, args) -> switch (method) {
            case "getPosition" -> null;
            default -> false;
        });
        creatures.put(new Point(4, 3), List.of(gone));
        creatures.put(new Point(5, 4), List.of(gone, creature(5, 4, false, false)));

        assertTrue(chicken.dodgeCreature());
        assertEquals(new Vector2(3.5f, 4), entityData.getComponent(chickenId, Navigation.class).navigationPath.get(1));
    }

    @Test
    void aChickenOfTheHatcheryStaysWhereItIs() {
        placeChicken(4, 4);
        creatures.put(new Point(5, 4), List.of(creature(5, 4, false, false)));
        entityData.setComponent(chickenId, new Stored(entityData.createEntity(), ObjectType.FOOD));

        assertFalse(chicken.dodgeCreature());
    }

    private void placeChicken(int x, int y) {
        entityData.setComponent(chickenId, new Position(0, WorldUtils.pointToVector3f(x, y)));
    }

    private static ICreatureController creature(int x, int y, boolean worker, boolean flying) {
        return proxy(ICreatureController.class, (method, args) -> switch (method) {
            case "isWorker" -> worker;
            case "canFly" -> flying;
            case "getPosition" -> WorldUtils.pointToVector3f(x, y);
            default -> throw new UnsupportedOperationException(method);
        });
    }

    private ChickenController chicken() {
        FakeMapData mapData = new FakeMapData(9, 9);
        IMapController mapController = proxy(IMapController.class, (method, args) -> switch (method) {
            case "getMapData" -> mapData;
            case "isSolid" -> solidTiles.contains((Point) args[0]);
            default -> throw new UnsupportedOperationException(method);
        });
        IEntityPositionLookup lookup = proxy(IEntityPositionLookup.class, (method, args) -> switch (method) {
            case "getEntityTypesInLocation" -> creatures.getOrDefault((Point) args[0], List.of());
            default -> throw new UnsupportedOperationException(method);
        });

        return new ChickenController(chickenId, entityData, null, null, null, null, null, mapController, lookup);
    }

    private interface Answer {

        Object get(String method, Object[] args);
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, Answer answer) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (proxy, method, args) -> answer.get(method.getName(), args));
    }

}
