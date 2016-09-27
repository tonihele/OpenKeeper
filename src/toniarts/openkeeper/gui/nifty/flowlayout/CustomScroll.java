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
import de.lessvoid.nifty.tools.SizeValue;

/**
 *
 * @author ArchDemon
 */
public class CustomScroll implements Controller {
    private Nifty nifty;
    private Screen screen;
    private Parameters parameters;

    private Element element;
    private Element content;
    private Element back;
    private Element forward;

    private boolean enable = true;
    private boolean visible = true;

    @Override
    public void bind(Nifty nifty, Screen screen, Element element, Parameters parameter) {
        this.nifty = nifty;
        this.screen = screen;
        this.parameters = parameter;
        this.element = element;

        if (this.element.getId() == null) {
            this.element.setId("FlowLayout-" + NiftyIdCreator.generate());
        }
        // Get the elements
        content = this.element.findElementById("#content");
        back = this.element.findElementById("#back");
        forward = this.element.findElementById("#forward");

        setEnable(false);
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

    public void back() {
        // FIXME not worked
        content.setConstraintX(SizeValue.px(content.getConstraintX().getValueAsInt(1.f) - 64));
    }

    public void forward() {
        // FIXME not worked
        content.setConstraintX(SizeValue.px(content.getConstraintX().getValueAsInt(1.f) + 64));
    }

    public Element addElement(ControlBuilder controlBuilder) {
        Element el = controlBuilder.build(nifty, screen, content);
        if (content.getConstraintWidth().getValueAsInt(1.f) > content.getWidth()) {
            setEnable(true);
        }
        content.layoutElements();
        return el;
    }

    public void removeAll() {
        for (Element child : content.getChildren()) {
            child.markForRemoval();
        }
    }

    public void setEnable(boolean enable) {
        if (this.enable != enable) {

            if (enable) {
                back.enable();
                forward.enable();
            } else {
                back.disable();
                forward.disable();
            }

            this.enable = enable;
        }
    }

    public void setVisible(boolean visible) {
        if (this.visible != visible) {

            if (visible) {
                back.show();
                forward.show();
            } else {
                back.hide();
                forward.hide();
            }

            this.visible = visible;
        }
    }
}
