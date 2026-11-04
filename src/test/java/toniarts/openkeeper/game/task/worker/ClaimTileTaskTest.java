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
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.CreatureSleepFixtures;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.tools.convert.map.TerrainFixtures;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * DK II's claim action on ground nobody owns (0x496AA0, 0x4974C0), measured
 * on a Dirt Path in the original: a level 1 imp adds 164 health a tick from
 * the tick after it starts, the tile turns on the fifth, and the imp leaves
 * when the loop of Imp_ClaimFloor (11 frames at 20 fps) that is playing has
 * ended, 7 ticks after it started.
 */
class ClaimTileTaskTest {

    private static final int TICKS_PER_SECOND = 4;
    private static final int FRESH_IMP = 80;
    private static final short PLAYER = 3;
    private static final Terrain DIRT_PATH = TerrainFixtures.terrain((short) 3, TerrainFixtures.flags(), 0, 1, 660, (short) 8);
    private static final Terrain CLAIMED_PATH = TerrainFixtures.terrain((short) 8, TerrainFixtures.flags(Terrain.TerrainFlag.OWNABLE), 0, 1, 660, (short) 2);

    @Test
    void impClaimsDirtPathInFiveTicks() {
        Claiming claiming = new Claiming(1, TileWork.perTick(825 / TICKS_PER_SECOND, FRESH_IMP));
        ClaimTileTask task = new ClaimTileTask(null, claiming.mapController(), new Point(1, 1), PLAYER);
        ICreatureController imp = creature(CreatureSleepFixtures.claimer(11, 20));
        task.assign(imp, false);

        int leaves = -1;
        for (int tick = 0; tick < 20 && leaves < 0; tick++) {
            claiming.tick = tick;
            task.executeTask(imp, (float) tick / TICKS_PER_SECOND);
            if (!task.isValid(imp)) {
                leaves = tick;
            }
        }

        assertEquals(List.of(1, 2, 3, 4, 5), claiming.claims);
        assertEquals(7, leaves);
    }

    @Test
    void othersCantTakeTheClaimedTileWhileTheImpFinishes() {
        Claiming claiming = new Claiming(657, 164);
        ClaimTileTask task = new ClaimTileTask(null, claiming.mapController(), new Point(1, 1), PLAYER);
        ICreatureController imp = creature(CreatureSleepFixtures.claimer(11, 20));
        task.assign(imp, false);

        task.executeTask(imp, 0);
        task.executeTask(imp, 0.25f);

        assertEquals(true, task.isValid(imp));
        assertFalse(task.isValid(creature(CreatureSleepFixtures.claimer(11, 20))));
        assertFalse(task.isValid(null));
    }

    private static ICreatureController creature(Creature creature) {
        return (ICreatureController) Proxy.newProxyInstance(ICreatureController.class.getClassLoader(),
                new Class<?>[]{ICreatureController.class}, (proxy, method, args) -> switch (method.getName()) {
            case "getCreature" ->
                creature;
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
     * One Dirt Path being claimed, a claimed path once its health is full
     */
    private static final class Claiming {

        private final int perTick;
        private final List<Integer> claims = new ArrayList<>();
        private int health;
        private int tick = 0;

        private Claiming(int health, int perTick) {
            this.health = health;
            this.perTick = perTick;
        }

        private IMapController mapController() {
            IMapDataInformation<?> mapData = (IMapDataInformation<?>) Proxy.newProxyInstance(IMapDataInformation.class.getClassLoader(),
                    new Class<?>[]{IMapDataInformation.class}, (proxy, method, args) -> switch (method.getName()) {
                case "getTile" ->
                    null;
                default ->
                    throw new UnsupportedOperationException(method.getName());
            });
            return (IMapController) Proxy.newProxyInstance(IMapController.class.getClassLoader(),
                    new Class<?>[]{IMapController.class}, (proxy, method, args) -> switch (method.getName()) {
                case "getMapData" ->
                    mapData;
                case "getTerrain" ->
                    health < DIRT_PATH.getMaxHealth() ? DIRT_PATH : CLAIMED_PATH;
                case "isClaimableTile" ->
                    health < DIRT_PATH.getMaxHealth();
                case "applyClaimTile" -> {
                    claims.add(tick);
                    health = Math.min(DIRT_PATH.getMaxHealth(), health + perTick);
                    yield null;
                }
                default ->
                    throw new UnsupportedOperationException(method.getName());
            });
        }

    }

}
