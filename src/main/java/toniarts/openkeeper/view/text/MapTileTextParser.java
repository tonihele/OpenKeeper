/*
 * Copyright (C) 2014-2018 OpenKeeper
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
package toniarts.openkeeper.view.text;

import toniarts.openkeeper.utils.TextParameter;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.utils.TextUtils;

/**
 * Parses text and fills the replacements from MapTile data
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class MapTileTextParser {

    // The terrains DK II picks its own tooltip texts for
    private static final short DIRT_PATH = 3;
    private static final short CLAIMED_PATH = 8;
    private static final short REINFORCED_WALL = 9;
    private static final short FOG_OF_WAR = 27;
    private static final short CLAIMED_MANA_VAULT = 32;
    private static final short CLAIM_SCAN = 38;

    public MapTileTextParser() {
    }

    /**
     * Gets the tooltip text of the tile. Like DK II, a few terrains get a text
     * of their own instead of the terrain's tooltip text, and other players'
     * land tells only that it is theirs
     *
     * @param mapTile the tile
     * @param terrain the tile's terrain
     * @param playerId the player looking at the tile
     * @return the text ID, 0 when the tile has no tooltip
     */
    public int getTooltipTextId(IMapTileInformation mapTile, Terrain terrain, short playerId) {
        boolean own = mapTile.getOwnerId() == playerId;
        switch (terrain.getTerrainId()) {
            case DIRT_PATH:
                return 2797; // You cannot build on this unclaimed terrain
            case CLAIMED_PATH:
                return own ? 2798 : 2487;
            case REINFORCED_WALL:
                return own ? terrain.getTooltipStringId() : 2481;
            case CLAIMED_MANA_VAULT:
                return own ? 2799 : 2501;
            case FOG_OF_WAR:
            case CLAIM_SCAN:
                return 2473; // This area has not yet been explored
            default:
                return terrain.getTooltipStringId();
        }
    }

    public String parseText(String text, IMapTileInformation mapTile) {
        return TextUtils.parseText(text, (parameter) -> {
            return getReplacement(parameter, mapTile);
        });
    }
    
    protected String getReplacement(TextParameter parameter, IMapTileInformation mapTile) {
        switch (parameter) {
            case HEALTH_PERCENTAGE:
                return Integer.toString(mapTile.getHealthPercent());
            case MANA_GAIN:
                return Integer.toString(mapTile.getManaGain());
            case GOLD:
                return Integer.toString(mapTile.getGold());
            default:
                return TextUtils.getUnsupportedParameterMessage(parameter, getClass());
        }
    }

}
