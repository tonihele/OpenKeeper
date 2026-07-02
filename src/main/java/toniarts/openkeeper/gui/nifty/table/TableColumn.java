/*
 * Copyright (C) 2014-2015 OpenKeeper
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
package toniarts.openkeeper.gui.nifty.table;

import de.lessvoid.nifty.tools.Color;

/**
 * Table column model
 *
 * @param type Either string or boolean
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public record TableColumn(String header, int width, Class type, Color color) {

    protected static TableColumn parse(String property) throws ClassNotFoundException {
        String[] params = property.split(";");
        return new TableColumn(params[0], Integer.parseInt(params[1]), Class.forName(params[2]), new Color(params[3]));
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(header);
        sb.append(";");
        sb.append(width);
        sb.append(";");
        sb.append(type.getName());
        sb.append(";");
        sb.append(color.getColorString());
        return sb.toString();
    }
}
