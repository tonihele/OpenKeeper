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
package toniarts.openkeeper.gui.nifty.jme;

import com.jme3.app.SimpleApplication;
import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.NiftyIdCreator;
import de.lessvoid.nifty.controls.AbstractController;
import de.lessvoid.nifty.controls.Parameters;
import de.lessvoid.nifty.elements.Element;
import de.lessvoid.nifty.input.NiftyInputEvent;
import de.lessvoid.nifty.screen.Screen;
import toniarts.openkeeper.Main;

/**
 * Nifty lacks some advanced scene graph etc. manipulation options. This is
 * clumsy and non-intrusive way to marry Nifty GUI elements with jME
 * capabilities. Needs manual initializing to provide the jME stuff.<br>
 * This is the Nifty control part you add to your element. Extend this class to
 * create the control / effect of yor dreams!
 *
 * @see AbstractJmeControl
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public abstract class AbstractNiftyJmeControl extends AbstractController {

    private SimpleApplication app;
    private Nifty nifty;
    private Screen screen;

    @Override
    public void bind(Nifty nifty, Screen screen, Element element, Parameters parameter) {
        super.bind(element);
        this.nifty = nifty;
        this.screen = screen;

        if (element.getId() == null) {
            element.setId(getClass().getSimpleName() + NiftyIdCreator.generate());
        }
    }

    @Override
    public void onStartScreen() {

    }

    @Override
    public boolean inputEvent(NiftyInputEvent inputEvent) {
        return false;
    }


    public void initJme(SimpleApplication app) {
        this.app = app;
    }

    public SimpleApplication getApp() {
        return app;
    }

    @Override
    public void init(Parameters parameter) {

    }

    @Override
    public void onEndScreen() {
        cleanup();
    }

    /**
     * Called when the element is removed from the screen
     */
    protected abstract void cleanup();

    protected boolean isControlVisible() {
        return getElement().isVisibleWithParent();
    }

    protected int getControlX() {
        Element element = getElement().getParent();
        return element.getX();
    }

    protected int getControlY() {
        Element element = getElement().getParent();
        int height = Main.getUserSettings().getAppSettings().getHeight();
        return height - element.getY() - element.getHeight();
    }

    protected int getControlWidth() {
        Element element = getElement().getParent();
        return element.getWidth();
    }

    protected int getControlHeight() {
        Element element = getElement().getParent();
        return element.getHeight();
    }

}
