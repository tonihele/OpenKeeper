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
package toniarts.openkeeper.game.map;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.tools.convert.map.Terrain.TerrainFlag;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static toniarts.openkeeper.tools.convert.map.RoomFixtures.room;
import static toniarts.openkeeper.tools.convert.map.TerrainFixtures.flags;
import static toniarts.openkeeper.tools.convert.map.TerrainFixtures.terrain;

/**
 * The terrains as in the shipped DK II data. Imps can only claim somebody
 * else's land when the terrain is attackable, so the Hero Lair and the Hero
 * Gates stay with the heroes
 */
class ClaimNotAttackableTest {

    private static final short KEEPER = 3;
    private static final short HEROES = 1;
    private static final short CLAIMED_PATH = 8;
    private static final short PORTAL = 12;
    private static final short HERO_GATE = 37;
    private static final short HERO_LAIR = 35;

    private final Map<Short, Terrain> terrains = Map.of(
            CLAIMED_PATH, terrain(CLAIMED_PATH, flags(TerrainFlag.OWNABLE, TerrainFlag.ATTACKABLE, TerrainFlag.FILL_INABLE), 0, 0, 0, CLAIMED_PATH),
            PORTAL, terrain(PORTAL, flags(TerrainFlag.OWNABLE, TerrainFlag.ROOM, TerrainFlag.ATTACKABLE), 0, 0, 0, PORTAL),
            HERO_GATE, terrain(HERO_GATE, flags(TerrainFlag.OWNABLE, TerrainFlag.ROOM), 0, 0, 0, HERO_GATE),
            HERO_LAIR, terrain(HERO_LAIR, flags(TerrainFlag.OWNABLE, TerrainFlag.FILL_INABLE), 0, 0, 0, HERO_LAIR));

    @Test
    void attackableLandOfAnotherPlayerCanBeClaimed() {
        assertTrue(mapWithHeroTile(CLAIMED_PATH).isClaimableTile(new Point(1, 0), KEEPER));
        assertTrue(mapWithHeroTile(PORTAL).isClaimableRoom(new Point(1, 0), KEEPER));
    }

    @Test
    void heroLairCantBeClaimed() {
        assertFalse(mapWithHeroTile(HERO_LAIR).isClaimable(new Point(1, 0), KEEPER));
    }

    @Test
    void heroGateCantBeClaimed() {
        assertFalse(mapWithHeroTile(HERO_GATE).isClaimable(new Point(1, 0), KEEPER));
    }

    /**
     * A keeper's claimed path at 0,0 with a tile of the heroes next to it
     */
    private MapInformation<FakeMapData, FakeMapTile> mapWithHeroTile(short terrainId) {
        FakeMapData mapData = new FakeMapData(2, 1);
        mapData.getTile(0, 0).terrainId = CLAIMED_PATH;
        mapData.getTile(0, 0).ownerId = KEEPER;
        mapData.getTile(1, 0).terrainId = terrainId;
        mapData.getTile(1, 0).ownerId = HEROES;
        IKwdFile kwdFile = (IKwdFile) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{IKwdFile.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getTerrain" -> terrains.get((Short) args[0]);
                    case "getDungeonHeart" -> room((short) 5, 0);
                    default -> null;
                });

        return new MapInformation<>(mapData, kwdFile, List.of(new Keeper(false, KEEPER), new Keeper(false, HEROES)));
    }

}
