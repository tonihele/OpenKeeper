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

import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.WoodenBridgeDecay;
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.map.IMapDataInformation;
import toniarts.openkeeper.game.map.IMapTileController;
import toniarts.openkeeper.tools.convert.map.BridgeRoomFixtures;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.tools.convert.map.TerrainFixtures;
import toniarts.openkeeper.tools.convert.map.Tile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class WoodenBridgeDecaySystemTest {

    private static final double LIFETIME = 30;
    private static final double GAME_TIME = 100;

    private final EntityData entityData = new DefaultEntityData();

    @Test
    void woodenBridgeOnLavaBurns() {
        EntityId tile = entityData.createEntity();

        startSystem(BridgeRoomFixtures.WOODEN_BRIDGE_TERRAIN_ID, Tile.BridgeTerrainType.LAVA, tile);

        WoodenBridgeDecay decay = entityData.getComponent(tile, WoodenBridgeDecay.class);
        assertNotNull(decay);
        assertEquals(GAME_TIME + LIFETIME, decay.endTime);
    }

    @Test
    void woodenBridgeOnWaterDoesNotBurn() {
        EntityId tile = entityData.createEntity();

        startSystem(BridgeRoomFixtures.WOODEN_BRIDGE_TERRAIN_ID, Tile.BridgeTerrainType.WATER, tile);

        assertNull(entityData.getComponent(tile, WoodenBridgeDecay.class));
    }

    @Test
    void stoneBridgeOnLavaDoesNotBurn() {
        EntityId tile = entityData.createEntity();

        startSystem(BridgeRoomFixtures.STONE_BRIDGE_TERRAIN_ID, Tile.BridgeTerrainType.LAVA, tile);

        assertNull(entityData.getComponent(tile, WoodenBridgeDecay.class));
    }

    @Test
    void heroStoneBridgeOnLavaDoesNotBurn() {
        EntityId tile = entityData.createEntity();

        startSystem(BridgeRoomFixtures.HERO_STONE_BRIDGE_TERRAIN_ID, Tile.BridgeTerrainType.LAVA, tile);

        assertNull(entityData.getComponent(tile, WoodenBridgeDecay.class));
    }

    private void startSystem(short terrainId, Tile.BridgeTerrainType bridgeTerrainType, EntityId tileEntity) {
        IGameTimer gameTimer = () -> GAME_TIME;
        new WoodenBridgeDecaySystem(entityData, gameTimer, null, kwdFile(),
                mapController(oneTileMap(tile(terrainId, bridgeTerrainType, tileEntity))), LIFETIME);
    }

    private static IKwdFile kwdFile() {
        Map<Short, Room> roomsByTerrain = Map.of(
                BridgeRoomFixtures.WOODEN_BRIDGE_TERRAIN_ID, BridgeRoomFixtures.woodenBridge(),
                BridgeRoomFixtures.STONE_BRIDGE_TERRAIN_ID, BridgeRoomFixtures.stoneBridge(),
                BridgeRoomFixtures.HERO_STONE_BRIDGE_TERRAIN_ID, BridgeRoomFixtures.heroStoneBridge());
        List<Terrain> terrains = List.of(
                bridgeTerrain(BridgeRoomFixtures.WOODEN_BRIDGE_TERRAIN_ID),
                bridgeTerrain(BridgeRoomFixtures.STONE_BRIDGE_TERRAIN_ID),
                bridgeTerrain(BridgeRoomFixtures.HERO_STONE_BRIDGE_TERRAIN_ID));
        return proxy(IKwdFile.class, (method, args) -> switch (method) {
            case "getTerrainList" -> terrains;
            case "getRoomByTerrain" -> roomsByTerrain.get((Short) args[0]);
            default -> throw new UnsupportedOperationException(method);
        });
    }

    private static Terrain bridgeTerrain(short terrainId) {
        return TerrainFixtures.terrain(terrainId, TerrainFixtures.flags(Terrain.TerrainFlag.OWNABLE,
                Terrain.TerrainFlag.ROOM, Terrain.TerrainFlag.ATTACKABLE), 0, 2000, 2000, (short) 0);
    }

    private static IMapTileController tile(short terrainId, Tile.BridgeTerrainType bridgeTerrainType,
            EntityId entityId) {
        return proxy(IMapTileController.class, (method, args) -> switch (method) {
            case "getTerrainId" -> terrainId;
            case "getBridgeTerrainType" -> bridgeTerrainType;
            case "getEntityId" -> entityId;
            default -> throw new UnsupportedOperationException(method);
        });
    }

    private static IMapDataInformation<IMapTileController> oneTileMap(IMapTileController tile) {
        return proxy(IMapDataInformation.class, (method, args) -> switch (method) {
            case "getWidth", "getHeight" -> 1;
            case "getTile" -> tile;
            default -> throw new UnsupportedOperationException(method);
        });
    }

    private static IMapController mapController(IMapDataInformation<IMapTileController> mapData) {
        return proxy(IMapController.class, (method, args) -> switch (method) {
            case "getMapData" -> mapData;
            default -> throw new UnsupportedOperationException(method);
        });
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<?> type, Handler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (proxy, method, args) -> handler.invoke(method.getName(), args));
    }

    @FunctionalInterface
    private interface Handler {

        Object invoke(String method, Object[] args);
    }
}
