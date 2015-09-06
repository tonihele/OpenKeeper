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
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class TableColumn {

    private final String header;
    private final int width;
    private final Class type; // Either string or boolean
    private final Color color;

    public TableColumn(String header, int width, Class type, Color color) {
        this.header = header;
        this.width = width;
        this.type = type;
        this.color = color;
    }

    public String getHeader() {
        return header;
    }

    public Color getColor() {
        return color;
    }

    public Class getType() {
        return type;
    }

    public int getWidth() {
        return width;
    }

    protected static TableColumn parse(String property) throws ClassNotFoundException {
        String[] params = property.split(";");
        return new TableColumn(params[0], Integer.valueOf(params[1]), Class.forName(params[2]), new Color(params[3]));
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(header);
        sb.append(";");
        sb.append(width);
        sb.append(";");
        sb.append(type);
        sb.append(";");
        sb.append(color);
        return sb.toString();
    }
}
