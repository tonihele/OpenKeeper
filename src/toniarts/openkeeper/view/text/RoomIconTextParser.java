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
import toniarts.openkeeper.utils.Utils;

/**
 * Parses texts for Room icons
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class RoomIconTextParser extends SimpleIconTextParser<Room> {

    @Override
    protected String getReplacement(int index, Room room) {
        switch (index) {
            case 1:
                return Utils.getMainTextResourceBundle().getString(Integer.toString(room.getNameStringId()));
            case 2:
                return Integer.toString(room.getCost());
            case 21: // ?
                return Integer.toString(room.getCost());
            case 22:

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
        }

        return "Parameter " + index + " not implemented!";
    }

}
