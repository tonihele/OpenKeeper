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

import de.lessvoid.nifty.builder.ControlBuilder;

/**
 * Builder for table
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class TableBuilder extends ControlBuilder {

    public TableBuilder(final String id, final TableColumn... columns) {
        super(id, "table");

        // Add the columns
        set("colCount", String.valueOf(columns.length));
        int i = 0;
        for (TableColumn col : columns) {
            set("col" + i, col.toString());
            i++;
        }

        // Some default settings
        set("horizontal", "off");
        set("viewConverterClass", TableRowViewConverter.class.getName());
    }

    public void displayItems(final int displayItems) {
        set("displayItems", String.valueOf(displayItems));
    }

    public void selectionModeSingle() {
        set("selectionMode", "Single");
    }

    public void selectionModeMultiple() {
        set("selectionMode", "Multiple");
    }

    public void selectionModeDisabled() {
        set("selectionMode", "Disabled");
    }

    public void showVerticalScrollbar() {
        set("vertical", "on");
    }

    public void hideVerticalScrollbar() {
        set("vertical", "off");
    }

    public void optionalVerticalScrollbar() {
        set("vertical", "optional");
    }
}
