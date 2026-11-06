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
package toniarts.openkeeper.game.task.worker;

import com.jme3.math.Vector3f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.Death;
import toniarts.openkeeper.game.component.HauledBy;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.Stored;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.room.AbstractRoomController.ObjectType;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.controller.room.storage.RoomCorpseControl;
import toniarts.openkeeper.game.map.IMapDataInformation;
import toniarts.openkeeper.game.map.IMapTileController;
import toniarts.openkeeper.game.task.ITaskManager;
import toniarts.openkeeper.game.task.TaskType;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CorpseTasksTest {

    private static final short PLAYER = 3;
    private static final short ENEMY = 4;
    private static final Point CORPSE_LOCATION = new Point(5, 6);
    private static final Point GRAVEYARD_LOCATION = new Point(10, 10);

    private final EntityData entityData = new DefaultEntityData();
    private final EntityId imp = entityData.createEntity();
    private final EntityId graveyardEntity = entityData.createEntity();
    private final List<IRoomController> graveyards = new ArrayList<>();
    private final List<Object[]> roomTaskRequests = new ArrayList<>();
    private RoomCorpseControl graveyardStorage;
    private short tileOwner = PLAYER;
    private boolean graveyardFull = false;

    @Test
    void impsFetchAnyCorpseLyingOnTheirOwnLand() {
        addGraveyard();
        EntityId corpse = createCorpse(ENEMY);
        FetchCorpseTask task = createFetchTask(corpse);

        assertTrue(task.isValid(creature()));
        assertEquals(CORPSE_LOCATION, task.getTaskLocation());
        assertEquals(TaskType.FETCH_CORPSE, task.getTaskType());
        assertEquals(corpse, task.getTaskTarget());

        tileOwner = ENEMY;
        assertFalse(task.isValid(creature()));
    }

    @Test
    void noFetchingWithoutAGraveyardWithRoom() {
        EntityId corpse = createCorpse(PLAYER);
        FetchCorpseTask task = createFetchTask(corpse);

        assertFalse(task.isValid(creature()));

        addGraveyard();
        graveyardFull = true;
        assertFalse(task.isValid(creature()));
    }

    @Test
    void storedOrCarriedCorpsesAreNotFetched() {
        addGraveyard();
        EntityId corpse = createCorpse(PLAYER);
        FetchCorpseTask task = createFetchTask(corpse);

        entityData.setComponent(corpse, new HauledBy(imp));
        assertFalse(task.isValid(creature()));
        entityData.removeComponent(corpse, HauledBy.class);

        entityData.setComponent(corpse, new Stored(graveyardEntity, ObjectType.CORPSE));
        assertFalse(task.isValid(creature()));
        assertFalse(task.isRemovable());

        entityData.removeEntity(corpse);
        assertTrue(task.isRemovable());
    }

    @Test
    void reachingTheCorpseStartsCarryingItToTheGraveyard() {
        addGraveyard();
        EntityId corpse = createCorpse(ENEMY);
        FetchCorpseTask task = createFetchTask(corpse);

        task.executeTask(creature(), 0);

        assertEquals(1, roomTaskRequests.size());
        assertEquals(ObjectType.CORPSE, roomTaskRequests.get(0)[0]);
        assertEquals(corpse, roomTaskRequests.get(0)[1]);
        assertEquals(imp, entityData.getComponent(corpse, HauledBy.class).entityId);
    }

    @Test
    void corpsesOfDifferentPlayersOrLocationsAreDifferentTasks() {
        EntityId first = createCorpse(PLAYER);
        EntityId second = createCorpse(PLAYER);

        assertNotEquals(createFetchTask(first), createFetchTask(second));
        assertEquals(createFetchTask(first), createFetchTask(first));
    }

    @Test
    void droppingTheCorpseStoresItInTheGraveyard() {
        addGraveyard();
        EntityId corpse = createCorpse(ENEMY);
        entityData.setComponent(corpse, new HauledBy(imp));
        CarryCorpseToGraveyardTask task = new CarryCorpseToGraveyardTask(null, null, GRAVEYARD_LOCATION, PLAYER, graveyards.get(0), null, entityData, corpse);

        assertTrue(task.isValid(creature()));
        assertEquals(TaskType.CARRY_CORPSE_TO_GRAVEYARD, task.getTaskType());

        task.executeTask(creature(), 0);

        assertNull(entityData.getComponent(corpse, HauledBy.class));
        assertEquals(graveyardEntity, entityData.getComponent(corpse, Stored.class).room);
        assertEquals(PLAYER, entityData.getComponent(corpse, Owner.class).ownerId);
        assertEquals(1, graveyardStorage.getCurrentCapacity());
        assertFalse(task.isValid(creature()));
    }

    @Test
    void decayedCorpseCanNotBeStored() {
        addGraveyard();
        EntityId corpse = createCorpse(ENEMY);
        CarryCorpseToGraveyardTask task = new CarryCorpseToGraveyardTask(null, null, GRAVEYARD_LOCATION, PLAYER, graveyards.get(0), null, entityData, corpse);

        entityData.removeEntity(corpse);

        assertFalse(task.isValid(creature()));
        task.executeTask(creature(), 0);
        assertEquals(0, graveyardStorage.getCurrentCapacity());
    }

    private EntityId createCorpse(short ownerId) {
        EntityId corpse = entityData.createEntity();
        entityData.setComponents(corpse,
                new CreatureComponent(),
                new Death(0),
                new Owner(ownerId, ownerId),
                new Position(0, WorldUtils.pointToVector3f(CORPSE_LOCATION)));
        return corpse;
    }

    private FetchCorpseTask createFetchTask(EntityId corpse) {
        return new FetchCorpseTask(null, mapController(), entityData, taskManager(), corpse, PLAYER);
    }

    private void addGraveyard() {
        RoomInstance roomInstance = new RoomInstance(new Room());
        roomInstance.addCoordinate(GRAVEYARD_LOCATION);
        IRoomController room = (IRoomController) Proxy.newProxyInstance(
                IRoomController.class.getClassLoader(), new Class<?>[]{IRoomController.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getEntityId" -> graveyardEntity;
                    case "getOwnerId" -> PLAYER;
                    case "getRoomInstance" -> roomInstance;
                    case "isTileAccessible" -> true;
                    case "isRemoved", "isDestroyed" -> false;
                    case "isFullCapacity" -> graveyardFull || graveyardStorage.isFullCapacity();
                    case "getObjectControl" -> graveyardStorage;
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        graveyardStorage = new RoomCorpseControl(null, room, entityData, () -> 0) {
            @Override
            protected int getNumberOfAccessibleTiles() {
                return roomInstance.getCoordinates().size();
            }
        };
        graveyards.add(room);
    }

    private ICreatureController creature() {
        return (ICreatureController) Proxy.newProxyInstance(
                ICreatureController.class.getClassLoader(), new Class<?>[]{ICreatureController.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getEntityId" -> imp;
                    case "getOwnerId" -> PLAYER;
                    case "getPosition" -> new Vector3f();
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    private ITaskManager taskManager() {
        return (ITaskManager) Proxy.newProxyInstance(
                ITaskManager.class.getClassLoader(), new Class<?>[]{ITaskManager.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "assignClosestRoomTask" -> {
                        roomTaskRequests.add(new Object[]{args[1], args[2]});
                        yield true;
                    }
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    private IMapController mapController() {
        IMapTileController tile = (IMapTileController) Proxy.newProxyInstance(
                IMapTileController.class.getClassLoader(), new Class<?>[]{IMapTileController.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getOwnerId" -> tileOwner;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        IMapDataInformation<?> mapData = (IMapDataInformation<?>) Proxy.newProxyInstance(
                IMapDataInformation.class.getClassLoader(), new Class<?>[]{IMapDataInformation.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getTile" -> tile;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        return (IMapController) Proxy.newProxyInstance(
                IMapController.class.getClassLoader(), new Class<?>[]{IMapController.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getMapData" -> mapData;
                    case "getRoomsByFunction" -> new ArrayList<>(graveyards);
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }
}
