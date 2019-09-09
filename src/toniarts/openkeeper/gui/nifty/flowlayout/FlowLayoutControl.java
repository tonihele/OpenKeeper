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
import de.lessvoid.nifty.controls.Controller;
import de.lessvoid.nifty.controls.Parameters;
import de.lessvoid.nifty.elements.Element;
import de.lessvoid.nifty.input.NiftyInputEvent;
import de.lessvoid.nifty.screen.Screen;

/**
 *
 * @author ArchDemon
 */
public class FlowLayoutControl implements Controller {

    private Nifty nifty;
    private Screen screen;
    private Parameters parameters;

    private Element element;
    private CustomScroll line;
    private int rowId = 0;
    private int rows = 1;

    @Override
    public void bind(Nifty nifty, Screen screen, Element element, Parameters parameter) {
        this.nifty = nifty;
        this.screen = screen;
        this.parameters = parameter;
        this.element = element;

        if (this.element.getId() == null) {
            this.element.setId("FlowLayout-" + NiftyIdCreator.generate());
        }

        rows = parameter.getAsInteger("rows", rows);
        for (int i = 0; i < rows; i++) {
            new ControlBuilder("#row-" + i, "customScroll").build(element);
        }

        // Get the elements
        line = getLineElement(rowId);
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

    public void addElement(ControlBuilder controlBuilder) {

        line.addElement(controlBuilder);

        rowId++;
        if (rowId == rows) {
            rowId = 0;
        }
        line = getLineElement(rowId);
    }

    private CustomScroll getLineElement(int id) {
        return element.findElementById("#row-" + id).getControl(CustomScroll.class);
    }

    public void removeAll() {
        for (int i = 0; i < rows; i++) {
            getLineElement(i).removeAll();
        }
        rowId = 0;
        line = getLineElement(rowId);
    }

    @Override
    public void onEndScreen() {

    }
}
