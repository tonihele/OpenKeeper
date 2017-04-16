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
package toniarts.openkeeper.gui.nifty.table.player;

import de.lessvoid.nifty.elements.Element;
import de.lessvoid.nifty.elements.render.TextRenderer;
import de.lessvoid.nifty.tools.Color;
import toniarts.openkeeper.gui.nifty.table.*;
import toniarts.openkeeper.world.MapThumbnailGenerator;

/**
 * Gives the player the coloring it deserves
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class PlayerTableRowViewConverter extends TableRowViewConverter<PlayerTableRow> {

    @Override
    public void display(final Element listBoxItem, final PlayerTableRow item) {
        int i = 0;
        for (String s : item.getData()) {

            // Get the text element for the row
            Element textElement = listBoxItem.findElementById("#col-" + String.valueOf(i));
            TextRenderer renderer = textElement.getRenderer(TextRenderer.class);
            renderer.setText(s);
            java.awt.Color c = MapThumbnailGenerator.getPlayerColor(item.getClientInfo().getKeeper().getId());
            renderer.setColor(new Color(c.getRed() / 255.0f, c.getGreen() / 255.0f, c.getBlue() / 255.0f, 1f));
            i++;
        }
    }

}
