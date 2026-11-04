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
package toniarts.openkeeper.tools.convert.map;

import java.lang.reflect.Proxy;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.view.text.MapTileTextParser;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The tile tooltip texts DK II picks by the terrain and its owner, in this
 * package for the protected setters
 */
class MapTileTooltipTest {

    private static final short PLAYER = 3;
    private static final short ENEMY = 4;
    private static final short NEUTRAL = 0;

    private final MapTileTextParser parser = new MapTileTextParser();

    @Test
    void claimedPathIsYourTerritoryOnlyWhenItIsYours() {
        Terrain claimedPath = terrain(8, 2485);

        assertEquals(2798, parser.getTooltipTextId(tile(PLAYER), claimedPath, PLAYER));
        assertEquals(2487, parser.getTooltipTextId(tile(ENEMY), claimedPath, PLAYER));
    }

    @Test
    void otherPlayersReinforcedWallIsAnEnemyWall() {
        Terrain reinforcedWall = terrain(9, 2479);

        assertEquals(2479, parser.getTooltipTextId(tile(PLAYER), reinforcedWall, PLAYER));
        assertEquals(2481, parser.getTooltipTextId(tile(ENEMY), reinforcedWall, PLAYER));
    }

    @Test
    void claimedManaVaultTellsItsManaValueOnlyToItsOwner() {
        Terrain claimedVault = terrain(32, 2499);

        assertEquals(2799, parser.getTooltipTextId(tile(PLAYER), claimedVault, PLAYER));
        assertEquals(2501, parser.getTooltipTextId(tile(ENEMY), claimedVault, PLAYER));
    }

    @Test
    void dirtPathCannotBeBuiltOn() {
        assertEquals(2797, parser.getTooltipTextId(tile(NEUTRAL), terrain(3, 2483), PLAYER));
    }

    @Test
    void claimScanIsFogOfWar() {
        assertEquals(2473, parser.getTooltipTextId(tile(NEUTRAL), terrain(38, 0), PLAYER));
    }

    @Test
    void otherTerrainsKeepTheirOwnText() {
        assertEquals(2489, parser.getTooltipTextId(tile(NEUTRAL), terrain(4, 2489), PLAYER));
        assertEquals(0, parser.getTooltipTextId(tile(NEUTRAL), terrain(41, 0), PLAYER));
    }

    private static Terrain terrain(int terrainId, int tooltipStringId) {
        Terrain terrain = new Terrain();
        terrain.setTerrainId((short) terrainId);
        terrain.setTooltipStringId(tooltipStringId);
        return terrain;
    }

    private static IMapTileInformation tile(short ownerId) {
        return (IMapTileInformation) Proxy.newProxyInstance(MapTileTooltipTest.class.getClassLoader(),
                new Class<?>[]{IMapTileInformation.class}, (proxy, method, args) -> {
                    if ("getOwnerId".equals(method.getName())) {
                        return ownerId;
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
    }

}
