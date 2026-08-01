/*
 * Copyright (C) 2014-2022 OpenKeeper
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

import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.utils.TextParameter;
import toniarts.openkeeper.utils.TextUtils;
import toniarts.openkeeper.utils.Utils;

/**
 * Parses texts for Room icons
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class RoomIconTextParser extends SimpleIconTextParser<Room> {

    @Override
    protected String getReplacement(TextParameter parameter, Room room) {
        switch (parameter) {
            case NAME:
                return Utils.getMainTextResourceBundle().getString(Integer.toString(room.getNameStringId()));
            case COST:
                return Integer.toString(room.getCost());
            case ROOM_COST: // ?
                return Integer.toString(room.getCost());
            case ROOM_SIZE_HINT:

                // Size hint
                if (room.getRecommendedSizeX() == 1 && room.getRecommendedSizeY() == 1) {
                    return Utils.getMainTextResourceBundle().getString("2199");
                }
                if (room.getRecommendedSizeX() == 3 && room.getRecommendedSizeY() == 3) {
                    return Utils.getMainTextResourceBundle().getString("2200");
                }
                if (room.getRecommendedSizeX() == 5 && room.getRecommendedSizeY() == 5) {
                    return Utils.getMainTextResourceBundle().getString("2201");
                }


                return "No size hint for " + room.getRecommendedSizeX() + " x " + room.getRecommendedSizeY();
            default:
                return TextUtils.getUnsupportedParameterMessage(parameter, getClass());
        }
    }

}
