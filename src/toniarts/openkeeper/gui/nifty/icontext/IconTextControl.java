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
package toniarts.openkeeper.gui.nifty.icontext;

import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.NiftyIdCreator;
import de.lessvoid.nifty.controls.Controller;
import de.lessvoid.nifty.effects.EffectEventId;
import de.lessvoid.nifty.elements.Element;
import de.lessvoid.nifty.input.NiftyInputEvent;
import de.lessvoid.nifty.screen.Screen;
import de.lessvoid.xml.xpp3.Attributes;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * A small class combining an image and a text, with on hover effect
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class IconTextControl implements Controller {

    private Nifty nifty;
    private Screen screen;
    private Properties properties;
    private Element element;
    private Attributes controlDefinitionAttributes;
    private Element image;
    private Element text;
    
    /**
     * The logger that takes care for the output of log messages in this class.
     */
    private static final Logger log = Logger.getLogger(IconTextControl.class.getName());

    @Override
    public void bind(Nifty nifty, Screen screen, Element element, Properties parameter, Attributes controlDefinitionAttributes) {
        this.nifty = nifty;
        this.screen = screen;
        this.properties = parameter;
        this.element = element;
        this.controlDefinitionAttributes = controlDefinitionAttributes;
        
        if (this.element.getId() == null) {
            this.element.setId("IconText-" + NiftyIdCreator.generate());
            //log.log(Level.INFO, "element {0} have no id", element.toString());
        }
        // Get the elements
        image = this.element.findElementByName("#image");
        //image.setId(this.element.getId() + "#image");
        text = this.element.findElementByName("#text");
        //text.setId(this.element.getId() + "#image");
    }

    @Override
    public void init(Properties parameter, Attributes controlDefinitionAttributes) {        
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

    public void startHover() {
        image.startEffect(EffectEventId.onCustom, null, "hover");
        text.startEffect(EffectEventId.onCustom, null, "hover");
    }

    public void endHover() {
        image.stopEffect(EffectEventId.onCustom);
        text.stopEffect(EffectEventId.onCustom);
    }
}
