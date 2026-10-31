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

import com.jme3.math.Vector3f;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.IObjectsController;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.player.PlayerRoomControl;
import toniarts.openkeeper.game.component.ChickenGenerator;
import toniarts.openkeeper.game.component.Decay;
import toniarts.openkeeper.game.component.Food;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.controller.room.IChickenGenerator;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.listener.RoomListener;
import toniarts.openkeeper.tools.convert.map.ChickenSpawnFixtures;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable.MiscType;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A new hatchery lays its first chicken after the level variable's time (16
 * seconds), then the bigger the hatchery the faster the chickens come, from 15
 * seconds down to 3 seconds
 */
class ChickenSpawnSystemTest {

    private static final short PLAYER = 3;
    private static final int TICKS_PER_SECOND = 4;

    @Test
    void cooldownFollowsTheHatcherySize() {
        int[][] expected = {{1, 15}, {3, 15}, {4, 13}, {6, 13}, {7, 12}, {9, 12}, {10, 11}, {11, 10}, {12, 9},
            {13, 8}, {14, 7}, {15, 6}, {16, 5}, {17, 4}, {19, 4}, {20, 3}, {45, 3}};

        for (int[] sizeAndCooldown : expected) {
            assertEquals(sizeAndCooldown[1], ChickenSpawnSystem.getCooldownByHatcherySize(sizeAndCooldown[0]),
                    "Tiles " + sizeAndCooldown[0]);
        }
    }

    @Test
    void firstChickenTakesTheVariableTimeThenTheSizeTime() {
        assertEquals(List.of(16, 29, 42, 55), spawnSeconds(5, 60));
    }

    @Test
    void bigHatcheryBreedsFast() {
        assertEquals(List.of(16, 19, 22, 25), spawnSeconds(25, 26));
    }

    @Test
    void fullHatcheryLaysFreeRangeChickensAtTheCoopsThatWereThereFromTheStart() {
        assertEquals(List.of(16), spawnSeconds(5, 20, 0));
    }

    @Test
    void fullHatcheryLaysNoMoreFreeRangeChickensThanTheLimitCountingThoseThatWereThereFromTheStart() {
        assertEquals(List.of(), spawnSeconds(5, 20, 5));
    }

    private static List<Integer> spawnSeconds(int tiles, int seconds) {
        return spawnSeconds(tiles, seconds, -1);
    }

    /**
     * The seconds at which a hatchery of the given size lays a chicken
     *
     * @param freeRangeChickens negative for a hatchery with room for chickens,
     * otherwise a full hatchery with a coop and this many free range chickens
     * already around
     */
    private static List<Integer> spawnSeconds(int tiles, int seconds, int freeRangeChickens) {
        List<Integer> spawns = new ArrayList<>();
        int[] second = new int[1];
        double[] lastSpawn = {0.0};
        RoomInstance roomInstance = new RoomInstance(ChickenSpawnFixtures.hatchery());
        for (int i = 0; i < tiles; i++) {
            roomInstance.addCoordinate(new Point(i, 0));
        }
        IChickenGenerator hatchery = (IChickenGenerator) Proxy.newProxyInstance(IChickenGenerator.class.getClassLoader(),
                new Class<?>[]{IChickenGenerator.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    case "getLastSpawnTime" -> lastSpawn[0];
                    case "onSpawn" -> {
                        lastSpawn[0] = (Double) args[0];
                        yield null;
                    }
                    case "isFullCapacity" -> freeRangeChickens >= 0;
                    case "getOwnerId" -> PLAYER;
                    case "getEntranceCoordinate" -> new Point(0, 0);
                    case "getRoomInstance" -> roomInstance;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        IObjectsController objectsController = (IObjectsController) Proxy.newProxyInstance(IObjectsController.class.getClassLoader(),
                new Class<?>[]{IObjectsController.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "spawnChicken" -> {
                        spawns.add(second[0]);
                        yield null;
                    }
                    case "spawnFreerangeChicken" -> {
                        spawns.add(second[0]);
                        yield null;
                    }
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        RoomListener[] listener = new RoomListener[1];
        IMapController mapController = (IMapController) Proxy.newProxyInstance(IMapController.class.getClassLoader(),
                new Class<?>[]{IMapController.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "addListener" -> {
                        listener[0] = (RoomListener) args[1];
                        yield null;
                    }
                    case "getRoomControllerByCoordinates" -> hatchery;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        Keeper keeper = new Keeper(false, PLAYER);
        IPlayerController player = (IPlayerController) Proxy.newProxyInstance(IPlayerController.class.getClassLoader(),
                new Class<?>[]{IPlayerController.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getKeeper" -> keeper;
                    case "getRoomControl" -> new PlayerRoomControl(keeper, List.of());
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        Map<MiscType, MiscVariable> settings = new EnumMap<>(MiscType.class);
        settings.put(MiscType.CHICKEN_GENERATION_TIME_PER_HATCHERY,
                ChickenSpawnFixtures.miscVariable(MiscType.CHICKEN_GENERATION_TIME_PER_HATCHERY, 16));
        settings.put(MiscType.MAX_FREE_RANGE_CHICKENS_PER_PLAYER,
                ChickenSpawnFixtures.miscVariable(MiscType.MAX_FREE_RANGE_CHICKENS_PER_PLAYER, 5));
        DefaultEntityData entityData = new DefaultEntityData();
        if (freeRangeChickens >= 0) {
            entityData.setComponents(entityData.createEntity(), new ChickenGenerator(), new Position(0, new Vector3f(0.5f, 0, 0.5f)));
        }
        for (int i = 0; i < freeRangeChickens; i++) {
            entityData.setComponents(entityData.createEntity(), new Food(), new Decay(), new Owner(PLAYER, PLAYER));
        }
        ChickenSpawnSystem system = new ChickenSpawnSystem(entityData, objectsController,
                List.of(player), settings, mapController);
        listener[0].onBuild(hatchery);

        for (int tick = 1; tick <= seconds * TICKS_PER_SECOND; tick++) {
            second[0] = tick / TICKS_PER_SECOND;
            system.processTick(1.0f / TICKS_PER_SECOND);
        }
        return spawns;
    }

}
