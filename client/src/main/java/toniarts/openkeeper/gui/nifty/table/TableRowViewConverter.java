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
import de.lessvoid.nifty.elements.render.ImageRenderer;
import de.lessvoid.nifty.elements.render.TextRenderer;
import de.lessvoid.nifty.render.NiftyImage;
import de.lessvoid.nifty.tools.SizeValue;

/**
 * Converts table row to Nifty listbox item
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 * @param <T> the table row class
 */
public class TableRowViewConverter<T extends TableRow> implements ListBox.ListBoxViewConverter<T> {

    @Override
    public void display(final Element listBoxItem, final T item) {
        int i = 0;
        for (Object obj : item.getData()) {

            // Get the text element for the row
            Element element = listBoxItem.findElementById("#col-" + String.valueOf(i));
            if (obj instanceof String) {
                displayString(element, item, obj.toString());
            } else if (obj instanceof Boolean) {
                displayBoolean(element, item, (boolean) obj);
            }
            i++;
        }
    }

    @Override
    public int getWidth(final Element listBoxItem, final T item) {
        int width = 0;
        int i = 0;

        for (Object obj : item.getData()) {
            Element element = listBoxItem.findElementById("#col-" + String.valueOf(i));
            if (obj instanceof String) {
                TextRenderer renderer = element.getRenderer(TextRenderer.class);
                width += renderer.getFont().getWidth(obj.toString());
            } else if (obj instanceof Boolean) {
                ImageRenderer renderer = element.getRenderer(ImageRenderer.class);
                if (renderer.getImage() != null) {
                    width += renderer.getImage().getWidth();
                }
            }
            i++;
        }
        return width;
    }

    /**
     * Display a String data in cell
     *
     * @param element the cell element
     * @param item the row item
     * @param itemData the cell data
     */
    protected void displayString(Element element, T item, String itemData) {
        element.getRenderer(TextRenderer.class).setText(itemData);
    }

    /**
     * Display a Boolean data in cell
     *
     * @param element the cell element
     * @param item the row item
     * @param itemData the cell data
     */
    protected void displayBoolean(Element element, T item, boolean itemData) {
        ImageRenderer renderer = element.getRenderer(ImageRenderer.class);
        if (itemData) {
            NiftyImage img = element.getNifty().createImage("Textures/Tick-1.png", true);
            renderer.setImage(img);
            element.setConstraintWidth(new SizeValue(img.getWidth() + "px"));
            element.setConstraintHeight(new SizeValue(img.getHeight() + "px"));
            element.getParent().layoutElements();
        } else {
            renderer.setImage(null);
        }
    }
}
