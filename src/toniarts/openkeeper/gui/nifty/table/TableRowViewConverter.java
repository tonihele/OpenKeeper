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

import de.lessvoid.nifty.controls.ListBox;
import de.lessvoid.nifty.elements.Element;
import de.lessvoid.nifty.elements.render.TextRenderer;

/**
 * Converts table row to Nifty listbox item
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class TableRowViewConverter implements ListBox.ListBoxViewConverter<TableRow> {

    @Override
    public void display(final Element listBoxItem, final TableRow item) {
        for (int i = 0; i < 5; i++) {

            // Get the text element for the row
            Element textElement = listBoxItem.findElementByName("#col-" + String.valueOf(i));
            textElement.getRenderer(TextRenderer.class).setText(item.data[i]);
        }
    }

    @Override
    public int getWidth(final Element listBoxItem, final TableRow item) {
        int width = 0;
        for (int i = 0; i < 5; i++) {
            TextRenderer renderer = listBoxItem.findElementByName("#col-" + String.valueOf(i)).getRenderer(TextRenderer.class);
            width += renderer.getFont().getWidth(item.data[i]);
        }
        return width;
    }
}
