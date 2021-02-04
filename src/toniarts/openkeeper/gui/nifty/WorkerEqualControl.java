/*
 * Copyright (C) 2014-2017 OpenKeeper
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
package toniarts.openkeeper.gui.nifty;

import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.controls.AbstractController;
import de.lessvoid.nifty.controls.Parameters;
import de.lessvoid.nifty.elements.Element;
import de.lessvoid.nifty.elements.render.ImageRenderer;
import de.lessvoid.nifty.input.NiftyInputEvent;
import de.lessvoid.nifty.screen.Screen;
import toniarts.openkeeper.gui.nifty.WorkerAmountControl.State;

/**
 *
 * @author archdemon
 */
public class WorkerEqualControl extends AbstractController {

    private Nifty nifty;
    private Element element;
    private Screen screen;
    private State state = State.TOTAL;

    @Override
    public void bind(Nifty nifty, Screen screen, Element element, Parameters parameter) {
        this.nifty = nifty;
        this.screen = screen;
        this.element = element;
    }

    @Override
    public void onStartScreen() {
        initState();
    }

    private void initState() {
        Element e = element.findElementById("#equal");
        String fileName = String.format("Textures/GUI/Tabs/t-cp-%s.png", state.toString().toLowerCase());

        ImageRenderer imageRenderer = e.getRenderer(ImageRenderer.class);
        imageRenderer.setImage(nifty.createImage(screen, fileName, true));
    }

    @Override
    public boolean inputEvent(NiftyInputEvent inputEvent) {
        return false;
    }

    public void setState(WorkerAmountControl.State state) {
        this.state = state;

        initState();
    }
}
