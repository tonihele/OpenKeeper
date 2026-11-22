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

import com.badlogic.gdx.ai.pfa.DefaultGraphPath;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.DoorComponent;
import toniarts.openkeeper.game.component.Mobile;
import toniarts.openkeeper.game.component.Navigation;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.Stored;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.room.AbstractRoomController.ObjectType;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.logic.IEntityPositionLookup;
import toniarts.openkeeper.game.map.FakeMapData;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A chicken moves one tile at a time: a Hatchery chicken to a tile beside it
 * in its Hatchery (DKII.EXE 0x4A1F30), a free-range chicken to a tile beside it
 * on its keeper's land, without a door or trap and only into a room as the
 * last choice (0x4A26D0)
 */
class ChickenStepTest {

    private static final short KEEPER = 3;

    private final EntityData entityData = new DefaultEntityData();
    private final EntityId chickenId = entityData.createEntity();
    private final FakeMapData mapData = new FakeMapData(9, 9);
    private final Map<Point, IRoomController> rooms = new HashMap<>();
    private final Map<Point, List<EntityId>> things = new HashMap<>();
    private final ChickenController chicken = chicken();

    @Test
    void aFreeRangeChickenStepsOntoItsKeepersLandBesideIt() {
        placeChicken(4, 4);
        own(4, 5);

        chicken.navigateToRandomPoint();

        assertEquals(new Point(4, 5), entityData.getComponent(chickenId, Navigation.class).target);
    }

    @Test
    void aFreeRangeChickenKeepsOffDoorsAndTraps() {
        placeChicken(4, 4);
        own(4, 5);
        EntityId door = entityData.createEntity();
        entityData.setComponent(door, new DoorComponent());
        things.put(new Point(4, 5), List.of(door));

        chicken.navigateToRandomPoint();

        assertNull(entityData.getComponent(chickenId, Navigation.class));
    }

    @Test
    void aFreeRangeChickenWalksIntoARoomOnlyAsItsLastChoice() {
        placeChicken(4, 4);
        own(4, 5);
        rooms.put(new Point(4, 5), room(entityData.createEntity()));

        // Whichever tile it starts from, the room tile is refused unless it is the fourth one tried
        int intoTheRoom = 0;
        for (int i = 0; i < 200; i++) {
            entityData.removeComponent(chickenId, Navigation.class);
            chicken.navigateToRandomPoint();
            if (entityData.getComponent(chickenId, Navigation.class) != null) {
                intoTheRoom++;
            }
        }
        assertTrue(intoTheRoom > 20 && intoTheRoom < 90, "about one try in four, was " + intoTheRoom);
    }

    @Test
    void aHatcheryChickenStepsOnlyWithinItsHatchery() {
        placeChicken(4, 4);
        EntityId hatchery = entityData.createEntity();
        entityData.setComponent(chickenId, new Stored(hatchery, ObjectType.FOOD));
        rooms.put(new Point(4, 4), room(hatchery));
        rooms.put(new Point(3, 4), room(hatchery));
        rooms.put(new Point(5, 4), room(entityData.createEntity()));
        own(4, 3);

        chicken.navigateToRandomPoint();

        assertEquals(new Point(3, 4), entityData.getComponent(chickenId, Navigation.class).target);
    }

    private void placeChicken(int x, int y) {
        entityData.setComponents(chickenId, new Position(0, WorldUtils.pointToVector3f(x, y)), new Mobile(false, false, false, 1),
                new Owner(KEEPER, KEEPER));
    }

    private void own(int x, int y) {
        mapData.getTile(x, y).ownerId = KEEPER;
    }

    private static IRoomController room(EntityId id) {
        return proxy(IRoomController.class, (method, args) -> "getEntityId".equals(method) ? id : null);
    }

    private ChickenController chicken() {
        IMapController mapController = proxy(IMapController.class, (method, args) -> switch (method) {
            case "getMapData" -> mapData;
            case "isSolid" -> false;
            case "getRoomControllerByCoordinates" -> rooms.get((Point) args[0]);
            default -> throw new UnsupportedOperationException(method);
        });
        IEntityPositionLookup lookup = proxy(IEntityPositionLookup.class, (method, args) -> switch (method) {
            case "getEntitiesInLocation" -> things.getOrDefault((Point) args[0], List.of());
            default -> throw new UnsupportedOperationException(method);
        });
        INavigationService navigation = proxy(INavigationService.class, (method, args) -> "findPath".equals(method) ? new DefaultGraphPath<>() : null);

        return new ChickenController(chickenId, entityData, null, null, navigation, null, null, mapController, lookup);
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
