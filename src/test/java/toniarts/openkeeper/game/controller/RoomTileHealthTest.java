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
package toniarts.openkeeper.game.controller;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.map.IMapTileController;
import toniarts.openkeeper.tools.convert.map.DestroyRoomsFixtures;
import toniarts.openkeeper.tools.convert.map.Terrain;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A room tile has the health of its room terrain (10000 for a Lair), not the
 * health of the claimed path it was built on (660)
 */
class RoomTileHealthTest {

    @Test
    void builtRoomTileGetsTheHealthOfTheRoomTerrain() {
        Map<String, Integer> values = new HashMap<>(Map.of("health", 660, "maxHealth", 660));
        IMapTileController tile = (IMapTileController) Proxy.newProxyInstance(IMapTileController.class.getClassLoader(),
                new Class<?>[]{IMapTileController.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "setHealth" ->
                        values.put("health", (Integer) args[0]);
                    case "setMaxHealth" ->
                        values.put("maxHealth", (Integer) args[0]);
                    default ->
                        null;
                });
        Terrain lair = DestroyRoomsFixtures.destroyRoomsTerrain((short) 11, (short) 8, 10000, Terrain.TerrainFlag.ROOM);

        GameWorldController.setHealthFromTerrain(tile, lair);

        assertEquals(Map.of("health", 10000, "maxHealth", 10000), values);
    }

}
