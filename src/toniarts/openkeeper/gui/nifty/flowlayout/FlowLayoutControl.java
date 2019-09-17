/*
 * Copyright (C) 2014-2016 OpenKeeper
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
package toniarts.openkeeper.gui.nifty.flowlayout;

import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.NiftyIdCreator;
import de.lessvoid.nifty.builder.ControlBuilder;
import de.lessvoid.nifty.controls.AbstractController;
import de.lessvoid.nifty.controls.Parameters;
import de.lessvoid.nifty.elements.Element;
import de.lessvoid.nifty.input.NiftyInputEvent;
import de.lessvoid.nifty.screen.Screen;

/**
 * Wraps a custom scroll panel container
 *
 * @see CustomScroll
 * @author ArchDemon
 */
public class FlowLayoutControl extends AbstractController {

    private Nifty nifty;
    private Screen screen;
    private Parameters parameters;

    private Element element;
    private CustomScroll content;

    @Override
    public void bind(Nifty nifty, Screen screen, Element element, Parameters parameter) {
        this.nifty = nifty;
        this.screen = screen;
        this.parameters = parameter;
        this.element = element;

        if (this.element.getId() == null) {
            this.element.setId(getClass().getSimpleName() + "-" + NiftyIdCreator.generate());
        }

        content = this.element.findControl("#scroll-area", CustomScroll.class);
    }

    @Override
    public void init(Parameters parameter) {
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

    public Element addElement(ControlBuilder controlBuilder) {
        return content.addElement(controlBuilder);
    }

    public void removeAll() {
        content.removeAll();
    }

    @Override
    public void onEndScreen() {

    }
}
