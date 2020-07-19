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

import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.utils.TextUtils;

/**
 * Parses text and fills the replacements from MapTile data
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class MapTileTextParser {

    public MapTileTextParser() {
    }

    public String parseText(String text, IMapTileInformation mapTile) {
        return TextUtils.parseText(text, (index) -> {
            return getReplacement(index, mapTile);
        });
    }
    
    protected String getReplacement(int index, IMapTileInformation mapTile) {
        switch (index) {
            case 37:
                return Integer.toString(mapTile.getHealthPercent());
            case 66:
                return Integer.toString(mapTile.getManaGain());
            case 67:
                return Integer.toString(mapTile.getGold());
        }

        return "Parameter " + index + " not implemented!";
    }

}
