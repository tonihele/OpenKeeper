/*
 * Copyright (C) 2014-2024 OpenKeeper
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
package toniarts.openkeeper.gui.nifty.autoscrolltext;

import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.controls.AbstractController;
import de.lessvoid.nifty.controls.Label;
import de.lessvoid.nifty.controls.Parameters;
import de.lessvoid.nifty.effects.Effect;
import de.lessvoid.nifty.effects.EffectEventId;
import de.lessvoid.nifty.elements.Element;
import de.lessvoid.nifty.elements.render.TextRenderer;
import de.lessvoid.nifty.input.NiftyInputEvent;
import de.lessvoid.nifty.layout.align.HorizontalAlign;
import de.lessvoid.nifty.screen.Screen;
import de.lessvoid.nifty.tools.Color;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.Objects;
import toniarts.openkeeper.gui.nifty.effect.HorizontalAutoScroll;

/**
 * Label that autoscrolls if it doesn't fit its constraints
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class AutoScrollTextControl extends AbstractController implements Label {
    
    private static final Logger logger = System.getLogger(AutoScrollTextControl.class.getName());

    private static final String TEXT_FIELD = "#text";

    private Label textControl;
    private Effect autoScroll;
    private Nifty nifty;

    /**
     * Default constructor.
     */
    public AutoScrollTextControl() {
    }

    @Override
    public final void bind(
            final Nifty niftyParam,
            final Screen screenParam,
            final Element newElement,
            final Parameters properties) {
        super.bind(newElement);
        logger.log(Level.DEBUG, "Binding auto scroll text control");
        nifty = niftyParam;

        textControl = newElement.findNiftyControl(TEXT_FIELD, Label.class);
        autoScroll = textControl.getElement().getEffects(EffectEventId.onCustom, HorizontalAutoScroll.class).getFirst();
    }


    @Override
    public final void onStartScreen() {
    }

    @Override
    public boolean inputEvent(NiftyInputEvent inputEvent) {
        return false;
    }

    @Override
    public void setText(String text) {

        // Effects trigger this constantly, check if the text really changed
        if (Objects.equals(getText(), text)) {
            return;
        }

        textControl.setText(text);

        calculateAutoScroll();
    }

    private void calculateAutoScroll() {
        TextRenderer textRenderer = textControl.getElement().getRenderer(TextRenderer.class);
        int textWidth = textRenderer.getTextWidth();
        int elementWidth = getElement().getWidth();
        if (textWidth > elementWidth) {
            textRenderer.setTextHAlign(HorizontalAlign.left);

            autoScroll.getParameters().setProperty("start", Integer.toString(elementWidth - textWidth));
            autoScroll.getParameters().setProperty("end", Integer.toString(0));
            autoScroll.getParameters().setProperty("delay", Integer.toString(2));
            textControl.getElement().startEffect(EffectEventId.onCustom);
        } else {
            textRenderer.setTextHAlign(HorizontalAlign.center);
            textControl.getElement().stopEffect(EffectEventId.onCustom);
        }
    }

    @Override
    public String getText() {
        return textControl.getText();
    }

    @Override
    public void setColor(Color color) {
        textControl.setColor(color);
    }

    @Override
    public Color getColor() {
        return textControl.getColor();
    }

}
