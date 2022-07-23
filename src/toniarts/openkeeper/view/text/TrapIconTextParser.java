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

import toniarts.openkeeper.tools.convert.map.Trap;
import toniarts.openkeeper.utils.Utils;

/**
 * Parses texts for Trap icons
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class TrapIconTextParser extends SimpleIconTextParser<Trap> {

    @Override
    protected String getReplacement(int index, Trap trap) {
        switch (index) {
            case 1:
                return Utils.getMainTextResourceBundle().getString(Integer.toString(trap.getNameStringId()));
            case 2:
                return Integer.toString(trap.getManaCost());
            case 17: // ?
                return Integer.toString(trap.getManaCost());
            case 25:
                return Integer.toString(trap.getManaUsage());
            case 26:
                return Integer.toString(trap.getManaCostToFire());
        }

        return "Parameter " + index + " not implemented!";
    }

}
