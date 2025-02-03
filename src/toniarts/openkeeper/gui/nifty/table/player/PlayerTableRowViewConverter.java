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
import toniarts.openkeeper.utils.MapThumbnailGenerator;

/**
 * Gives the player the coloring it deserves
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class PlayerTableRowViewConverter extends TableRowViewConverter<PlayerTableRow> {

    @Override
    protected void displayString(Element element, PlayerTableRow item, String itemData) {
        TextRenderer renderer = element.getRenderer(TextRenderer.class);
        renderer.setText(itemData);
        java.awt.Color c = MapThumbnailGenerator.getPlayerColor(item.getClientInfo().getKeeper().getId());
        renderer.setColor(new Color(c.getRed() / 255.0f, c.getGreen() / 255.0f, c.getBlue() / 255.0f, 1f));
    }

}
