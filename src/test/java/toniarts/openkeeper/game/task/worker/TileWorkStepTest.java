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

import com.simsilica.es.EntityId;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.map.IMapData;
import toniarts.openkeeper.game.map.IMapTileController;
import toniarts.openkeeper.game.task.Task;
import toniarts.openkeeper.tools.convert.map.TileWorkFixtures;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Imps work on a tile once a second, however long the job takes. The tile work
 * rates (<code>TileWork</code>) are the per second rates of the level
 * variables, so a work step more often than that makes the job too fast
 */
class TileWorkStepTest {

    private static final int TICKS_PER_SECOND = 4;
    private static final int SECONDS = 60;
    private static final Point TILE = new Point(3, 4);

    @Test
    void digsOncePerSecond() {
        AtomicInteger steps = new AtomicInteger();

        run(new DigTileTask(null, mapController(steps), TILE, (short) 3));

        assertEquals(SECONDS, steps.get());
    }

    @Test
    void claimsTileOncePerSecond() {
        AtomicInteger steps = new AtomicInteger();

        run(new ClaimTileTask(null, mapController(steps), TILE, (short) 3));

        assertEquals(SECONDS, steps.get());
    }

    @Test
    void claimsWallOncePerSecond() {
        AtomicInteger steps = new AtomicInteger();

        run(new ClaimWallTileTask(null, mapController(steps), TILE, (short) 3));

        assertEquals(SECONDS, steps.get());
    }

    @Test
    void repairsWallOncePerSecond() {
        AtomicInteger steps = new AtomicInteger();

        run(new RepairWallTileTask(null, mapController(steps), TILE, (short) 3));

        assertEquals(SECONDS, steps.get());
    }

    private static void run(Task task) {
        ICreatureController creature = creature();
        task.assign(creature, false);
        float tpf = 1.0f / TICKS_PER_SECOND;
        for (int tick = 1; tick <= TICKS_PER_SECOND * SECONDS; tick++) {
            task.executeTask(creature, tick * tpf);
        }
    }

    private static IMapController mapController(AtomicInteger steps) {
        return (IMapController) Proxy.newProxyInstance(IMapController.class.getClassLoader(),
                new Class<?>[]{IMapController.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "damageTile" -> {
                        steps.incrementAndGet();
                        yield 0;
                    }
                    case "applyClaimTile" -> {
                        steps.incrementAndGet();
                        yield null;
                    }
                    case "isClaimableTile", "isClaimableWall", "isRepairableWall" -> false;
                    case "getMapData" -> mapData();
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    private static IMapData mapData() {
        IMapTileController tile = (IMapTileController) Proxy.newProxyInstance(IMapTileController.class.getClassLoader(),
                new Class<?>[]{IMapTileController.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "isSelected" -> false;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        return (IMapData) Proxy.newProxyInstance(IMapData.class.getClassLoader(),
                new Class<?>[]{IMapData.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getTile" -> tile;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    private static ICreatureController creature() {
        return (ICreatureController) Proxy.newProxyInstance(ICreatureController.class.getClassLoader(),
                new Class<?>[]{ICreatureController.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    case "addGold" -> null;
                    case "fireEvent" -> null;
                    case "getEntityId" -> new EntityId(1);
                    case "getCreature" -> TileWorkFixtures.creature();
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

}
