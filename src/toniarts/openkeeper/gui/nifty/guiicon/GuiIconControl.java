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

import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.NiftyIdCreator;
import de.lessvoid.nifty.controls.AbstractController;
import de.lessvoid.nifty.controls.Parameters;
import de.lessvoid.nifty.elements.Element;
import de.lessvoid.nifty.input.NiftyInputEvent;
import de.lessvoid.nifty.screen.Screen;

/**
 * A clickable GUI icon with on hover effect and active states
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class GuiIconControl extends AbstractController {

    private Nifty nifty;
    private Screen screen;
    private Parameters parameters;
    private Element element;

    @Override
    public void bind(Nifty nifty, Screen screen, Element elmnt, Parameters prmtrs) {
        this.nifty = nifty;
        this.screen = screen;
        this.parameters = prmtrs;
        this.element = elmnt;

        if (this.element.getId() == null) {
            this.element.setId(getClass().getSimpleName() + NiftyIdCreator.generate());
        }

        if (!parameters.getAsBoolean("enabled", true)) {
            element.setVisibleToMouseEvents(false);
        }
    }

    @Override
    public void onStartScreen() {
    }

    @Override
    public void onFocus(boolean getFocus) {
    }

    @Override
    public boolean inputEvent(NiftyInputEvent inputEvent) {
        return false;
    }

    @Override
    public void init(Parameters prmtrs) {

    }

    @Override
    public void onEndScreen() {

    }
}
