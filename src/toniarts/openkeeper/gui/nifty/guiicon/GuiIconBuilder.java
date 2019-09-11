/*
 * Copyright (C) 2014-2019 OpenKeeper
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
package toniarts.openkeeper.gui.nifty.guiicon;

import de.lessvoid.nifty.builder.ControlBuilder;

/**
 * Builder for the GuiIcon
 *
 * @see GuiIconControl
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class GuiIconBuilder extends ControlBuilder {

    /**
     * Build a new GUI icon
     *
     * @param id id
     * @param image
     * @param hoverImage
     * @param activeImage
     * @param hint
     * @param tooltip
     *
     * @param onClick onClick callback method
     */
    public GuiIconBuilder(final String id, final String image, final String hoverImage, final String activeImage, final String hint, final String tooltip, final String onClick) {
        super(id, "guiIcon");

        parameter("image", image);
        parameter("hoverImage", hoverImage);
        parameter("activeImage", activeImage);
        parameter("hint", hint);
        parameter("tooltip", tooltip);
        parameter("click", onClick);
    }
}
