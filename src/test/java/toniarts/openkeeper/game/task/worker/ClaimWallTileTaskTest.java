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

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.TileWork;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.map.IMapDataInformation;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.CreatureSleepFixtures;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * DK II's reinforce action (0x466390, 0x497960): a blow of a tick's worth of
 * the reinforce rate on the tick after the imp starts, the next once the
 * first loop of Imp_Claim1(Land) (26 frames at 40 fps) is over, and then one
 * every tick. Rock starts at 165 of its 660 health. In DK II a lone imp spends
 * 58 ticks in the reinforcing state on a wall. Repairing a wall goes the same
 * way (0x466660, 0x497AC0).
 */
class ClaimWallTileTaskTest {

    private static final int TICKS_PER_SECOND = 4;
    private static final int FRESH_IMP = 80;
    private static final int ROCK_START = 165;
    private static final int ROCK_MAX = 660;

    @Test
    void impReinforcesRockEveryTick() {
        Reinforcing reinforcing = new Reinforcing(ROCK_START, ROCK_MAX, TileWork.perTick(50 / TICKS_PER_SECOND, FRESH_IMP));
        ClaimWallTileTask task = new ClaimWallTileTask(null, reinforcing.mapController(), new Point(1, 1), (short) 3);

        // 9 a blow, 55 blows
        assertEquals(57, work(task, reinforcing));
        assertEquals(55, reinforcing.blows.size());
        assertEquals(List.of(1, 4, 5, 6), reinforcing.blows.subList(0, 4));
        assertEquals(57, reinforcing.blows.get(54));
    }

    @Test
    void impRepairsWallEveryTick() {

        // A Reinforced Wall of 900 health hit for 30, the repair rate of 22 is 3 a blow for a fresh imp
        Reinforcing repairing = new Reinforcing(870, 900, TileWork.perTick(22 / TICKS_PER_SECOND, FRESH_IMP));
        RepairWallTileTask task = new RepairWallTileTask(null, repairing.mapController(), new Point(1, 1), (short) 3);

        assertEquals(12, work(task, repairing));
        assertEquals(List.of(1, 4, 5, 6, 7, 8, 9, 10, 11, 12), repairing.blows);
    }

    private static int work(DigTileTask task, Reinforcing reinforcing) {
        ICreatureController imp = creature();
        task.assign(imp, false);
        for (int tick = 0; tick < 400; tick++) {
            reinforcing.tick = tick;
            task.executeTask(imp, (float) tick / TICKS_PER_SECOND);
            if (!task.isValid(imp)) {
                return tick;
            }
        }
        throw new AssertionError("Still working after 400 ticks");
    }

    private static ICreatureController creature() {
        Creature imp = CreatureSleepFixtures.claimer(Creature.AnimationType.SLEEPING, 26, 40);
        return (ICreatureController) Proxy.newProxyInstance(ICreatureController.class.getClassLoader(),
                new Class<?>[]{ICreatureController.class}, (proxy, method, args) -> switch (method.getName()) {
            case "getCreature" ->
                imp;
            case "fireEvent" ->
                null;
            case "hashCode" ->
                System.identityHashCode(proxy);
            case "equals" ->
                proxy == args[0];
            default ->
                throw new UnsupportedOperationException(method.getName());
        });
    }

    /**
     * One rock being reinforced or wall being repaired, done once its health
     * is full
     */
    private static final class Reinforcing {

        private final int maxHealth;
        private final int perBlow;
        private final List<Integer> blows = new ArrayList<>();
        private int health;
        private int tick = 0;

        private Reinforcing(int health, int maxHealth, int perBlow) {
            this.health = health;
            this.maxHealth = maxHealth;
            this.perBlow = perBlow;
        }

        private IMapController mapController() {
            IMapTileInformation tile = (IMapTileInformation) Proxy.newProxyInstance(IMapTileInformation.class.getClassLoader(),
                    new Class<?>[]{IMapTileInformation.class}, (proxy, method, args) -> switch (method.getName()) {
                case "isSelected" ->
                    false;
                default ->
                    throw new UnsupportedOperationException(method.getName());
            });
            IMapDataInformation<?> mapData = (IMapDataInformation<?>) Proxy.newProxyInstance(IMapDataInformation.class.getClassLoader(),
                    new Class<?>[]{IMapDataInformation.class}, (proxy, method, args) -> switch (method.getName()) {
                case "getTile" ->
                    tile;
                default ->
                    throw new UnsupportedOperationException(method.getName());
            });
            return (IMapController) Proxy.newProxyInstance(IMapController.class.getClassLoader(),
                    new Class<?>[]{IMapController.class}, (proxy, method, args) -> switch (method.getName()) {
                case "getMapData" ->
                    mapData;
                case "isClaimableWall", "isRepairableWall" ->
                    health < maxHealth;
                case "applyClaimTile" -> {
                    blows.add(tick);
                    health = Math.min(maxHealth, health + perBlow);
                    yield null;
                }
                default ->
                    throw new UnsupportedOperationException(method.getName());
            });
        }

    }

}
