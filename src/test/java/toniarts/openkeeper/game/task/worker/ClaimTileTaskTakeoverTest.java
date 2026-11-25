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
import java.lang.reflect.Proxy;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.map.IMapDataInformation;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.game.task.Task;
import toniarts.openkeeper.utils.Point;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A worker nearer to a tile takes its claim over from the one assigned to it
 * (0x46AB50). The floor that digging has left is claimed by a worker walking
 * over it, once no tile around it is tagged for digging anymore (0x469600)
 */
class ClaimTileTaskTakeoverTest {

    private final Set<Point> tagged = new HashSet<>();
    private final Set<Point> gold = new HashSet<>();
    private final ClaimTileTask task = new ClaimTileTask(navigationService(), mapController(), new Point(10, 10), (short) 3);

    @Test
    void nearerWorkerTakesTheClaimOver() {
        Imp far = new Imp(new Vector3f(16, 0, 10), false);
        task.assign(far.creature, true);

        Imp near = new Imp(new Vector3f(12, 0, 10), false);

        assertTrue(task.isFull());
        assertTrue(task.makeRoomFor(near.creature));
        assertEquals(0, task.getAssigneeCount());
        assertNull(far.task.get(), "The one that lost the claim looks for work again");
    }

    @Test
    void workerKeepsTheClaimFromOneFartherAway() {
        Imp near = new Imp(new Vector3f(12, 0, 10), false);
        task.assign(near.creature, true);

        assertFalse(task.makeRoomFor(new Imp(new Vector3f(16, 0, 10), false).creature));
        assertEquals(1, task.getAssigneeCount());
        assertEquals(task, near.task.get());
    }

    /**
     * A worker in the keeper's hand has no position, it is not on its way to
     * the tile
     */
    @Test
    void workerInTheHandLosesTheClaim() {
        Imp held = new Imp(null, false);
        task.assign(held.creature, true);

        assertTrue(task.makeRoomFor(new Imp(new Vector3f(16, 0, 10), false).creature));
        assertNull(held.task.get());
    }

    @Test
    void workerClaimingTheTileKeepsIt() {
        Imp claiming = new Imp(new Vector3f(16, 0, 10), true);
        task.assign(claiming.creature, true);

        assertFalse(task.makeRoomFor(new Imp(new Vector3f(12, 0, 10), false).creature));
        assertEquals(task, claiming.task.get());
    }

    @Test
    void onlyFloorLeftByDiggingIsClaimedOnTheWay() {
        assertTrue(dugFloor().isDugFloorToClaim());
        assertFalse(task.isDugFloorToClaim(), "Other floor is claimed in its turn");
    }

    @Test
    void floorLeftByDiggingWaitsWhileATileAroundItIsTaggedForDigging() {
        tagged.add(new Point(11, 11));

        assertFalse(dugFloor().isDugFloorToClaim());

        // The digging around it is done
        tagged.clear();

        assertTrue(dugFloor().isDugFloorToClaim());
    }

    @Test
    void tileTaggedForMiningGoldDoesNotHoldTheFloorUp() {
        tagged.add(new Point(9, 10));
        gold.add(new Point(9, 10));

        assertTrue(dugFloor().isDugFloorToClaim());
    }

    private ClaimTileTask dugFloor() {
        return new ClaimTileTask(navigationService(), mapController(), new Point(10, 10), (short) 3, true);
    }

    /**
     * A worker that remembers the task it is given
     */
    private static final class Imp {

        private final AtomicReference<Task> task = new AtomicReference<>();
        private final ICreatureController creature;

        private Imp(Vector3f position, boolean claiming) {
            creature = stub(ICreatureController.class, (proxy, name, args) -> switch (name) {
                case "getPosition" -> position;
                case "getOwnerId" -> (short) 3;
                case "isAtAssignedTaskTarget" -> claiming;
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

    private IMapController mapController() {
        IMapDataInformation<?> mapData = stub(IMapDataInformation.class, (proxy, name, args) -> switch (name) {
            case "getTile" -> tile(args.length == 1 ? (Point) args[0] : new Point((int) args[0], (int) args[1]));
            case "getWidth", "getHeight" -> 20;
            default -> null;
        });
        return stub(IMapController.class, (proxy, name, args) -> switch (name) {
            case "getMapData" -> mapData;
            case "isClaimableTile" -> true;
            default -> null;
        });
    }

    private static INavigationService navigationService() {
        GraphPath<?> path = stub(GraphPath.class, (proxy, name, args) -> null);
        return stub(INavigationService.class, (proxy, name, args) -> switch (name) {
            case "isAccessible" -> true;
            case "findPath" -> path;
            default -> null;
        });
    }

    private IMapTileInformation tile(Point location) {
        return stub(IMapTileInformation.class, (proxy, name, args) -> switch (name) {
            case "getLocation" -> location;
            case "isSelected" -> tagged.contains(location);
            case "getGold" -> gold.contains(location) ? 1 : 0;
            case "getX" -> location.x;
            case "getY" -> location.y;
            default -> null;
        });
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
