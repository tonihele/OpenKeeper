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
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.map.IMapDataInformation;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.utils.Point;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Finding the spot to dig a tile from (#671)
 */
class DigTileTargetTest {

    @Test
    void eachSpotNextToAnUnreachableTileIsSearchedOnce() {
        AtomicInteger pathSearches = new AtomicInteger();
        IMapDataInformation<?> mapData = stub(IMapDataInformation.class, (name, args) -> switch (name) {
            case "getTile" -> tile(args.length == 1 ? (Point) args[0] : new Point((int) args[0], (int) args[1]));
            case "getWidth", "getHeight" -> 20;
            default -> null;
        });
        IMapController mapController = stub(IMapController.class, (name, args) -> switch (name) {
            case "getMapData" -> mapData;
            default -> null;
        });
        INavigationService navigationService = stub(INavigationService.class, (name, args) -> switch (name) {
            case "isAccessible" -> true;
            case "findPath" -> {
                pathSearches.incrementAndGet();
                yield null; // Walled off
            }
            default -> null;
        });
        ICreatureController imp = stub(ICreatureController.class, (name, args) -> switch (name) {
            case "getPosition" -> new Vector3f(1, 0, 1);
            case "getOwnerId" -> (short) 3;
            default -> null;
        });

        DigTileTask task = new DigTileTask(navigationService, mapController, new Point(10, 10), (short) 3);

        assertNull(task.getTarget(imp));
        assertEquals(4, pathSearches.get(), "One search for each of the four sides");
    }

    private static IMapTileInformation tile(Point location) {
        return stub(IMapTileInformation.class, (name, args) -> switch (name) {
            case "getLocation" -> location;
            case "getX" -> location.x;
            case "getY" -> location.y;
            default -> null;
        });
    }

    private interface Answer {

        Object answer(String methodName, Object[] args);
    }

    @SuppressWarnings("unchecked")
    private static <T> T stub(Class<T> type, Answer answer) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (proxy, method, args) -> answer.answer(method.getName(), args));
    }

}
