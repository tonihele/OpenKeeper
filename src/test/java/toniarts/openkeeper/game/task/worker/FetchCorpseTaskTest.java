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

import com.badlogic.gdx.ai.pfa.GraphPath;
import com.jme3.math.Vector3f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.Death;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.map.IMapDataInformation;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.game.task.Task;
import toniarts.openkeeper.utils.WorldUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A worker nearer to a dead body than the one on its way to it takes the job
 * over (DKII.EXE 0x46D810)
 */
class FetchCorpseTaskTest {

    private static final short KEEPER = 3;

    private final EntityData entityData = new DefaultEntityData();
    private final FetchCorpseTask task = task();

    @Test
    void nearerWorkerTakesTheBodyOver() {
        Imp far = new Imp(new Vector3f(16, 0, 10));
        task.assign(far.creature, true);

        assertTrue(task.makeRoomFor(new Imp(new Vector3f(12, 0, 10)).creature));
        assertEquals(0, task.getAssigneeCount());
        assertNull(far.task.get(), "The one that lost the job looks for work again");
    }

    @Test
    void workerKeepsTheBodyFromOneFartherAway() {
        Imp near = new Imp(new Vector3f(12, 0, 10));
        task.assign(near.creature, true);

        assertFalse(task.makeRoomFor(new Imp(new Vector3f(16, 0, 10)).creature));
        assertEquals(task, near.task.get());
    }

    private FetchCorpseTask task() {
        EntityId corpse = entityData.createEntity();
        entityData.setComponents(corpse, new Death(), new Position(0, WorldUtils.pointToVector3f(10, 10)));
        IMapTileInformation tile = stub(IMapTileInformation.class, (proxy, name, args) -> "getOwnerId".equals(name) ? KEEPER : null);
        IMapDataInformation<?> mapData = stub(IMapDataInformation.class, (proxy, name, args) -> "getTile".equals(name) ? tile : null);
        IRoomController graveyard = stub(IRoomController.class, (proxy, name, args) -> "isFullCapacity".equals(name) ? false : null);
        IMapController mapController = stub(IMapController.class, (proxy, name, args) -> switch (name) {
            case "getMapData" -> mapData;
            case "getRoomsByFunction" -> List.of(graveyard);
            default -> null;
        });
        GraphPath<?> path = stub(GraphPath.class, (proxy, name, args) -> null);
        INavigationService navigationService = stub(INavigationService.class, (proxy, name, args) -> switch (name) {
            case "isAccessible" -> true;
            case "findPath" -> path;
            default -> null;
        });
        return new FetchCorpseTask(navigationService, mapController, entityData, null, corpse, KEEPER);
    }

    /**
     * A worker that remembers the task it is given
     */
    private static final class Imp {

        private final AtomicReference<Task> task = new AtomicReference<>();
        private final ICreatureController creature;

        private Imp(Vector3f position) {
            creature = stub(ICreatureController.class, (proxy, name, args) -> switch (name) {
                case "getPosition" -> position;
                case "getOwnerId" -> KEEPER;
                case "isAtAssignedTaskTarget" -> false;
                case "setAssignedTask" -> {
                    task.set((Task) args[0]);
                    yield null;
                }
                case "getAssignedTask" -> task.get();
                case "unassingCurrentTask" -> {
                    task.set(null);
                    yield null;
                }
                case "equals" -> proxy == args[0];
                case "hashCode" -> System.identityHashCode(proxy);
                default -> null;
            });
        }
    }

    private interface Answer {

        Object answer(Object proxy, String methodName, Object[] args);
    }

    @SuppressWarnings("unchecked")
    private static <T> T stub(Class<T> type, Answer answer) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (proxy, method, args) -> answer.answer(proxy, method.getName(), args));
    }

}
