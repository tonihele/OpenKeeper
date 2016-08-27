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
package toniarts.openkeeper.gui.nifty.message;

import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.builder.ControlBuilder;
import de.lessvoid.nifty.controls.AbstractController;
import de.lessvoid.nifty.controls.Label;
import de.lessvoid.nifty.controls.Parameters;
import de.lessvoid.nifty.elements.Element;
import de.lessvoid.nifty.input.NiftyInputEvent;
import de.lessvoid.nifty.screen.Screen;

/**
 *
 * @author ufdada
 */
public class MessageBoxControl extends AbstractController {
    private Nifty nifty;
    private Screen hud;
    private Element element;
    private Element buttonPanel;
    private SystemMessageControl systemMessage;

    private enum ButtonType {
        EXIT,
        TICK,
        GOTO
    }

    @Override
    public void bind(Nifty nifty, Screen screen, Element element, Parameters parameter) {
        this.nifty = nifty;
        this.hud = nifty.getScreen("hud");
        this.element = element;
        this.buttonPanel = element.findElementById("buttonPanel");
    }

    @Override
    public void onStartScreen() {
    }

    @Override
    public boolean inputEvent(NiftyInputEvent inputEvent) {
        return false;
    }

    public void focusElement() {
        // TODO
    }

    public void dismissMessage() {
        this.systemMessage.dismissMessage();
        this.hide();
    }
    
    public void closeMessage() {
        this.hide();
    }

    public void show() {
        this.element.show();
    }

    public void hide() {
        this.systemMessage = null;
        this.element.hide();
    }

    private void showMessageBox(final String text) {
        this.setText(text);

        this.cleanButtonPanel();

        this.show();
    }
    
    public void showMessage(final String text) {
        this.showMessageBox(text);
        this.addButton(ButtonType.TICK, "closeMessage()");
    }

    public void showSystemMessage(final SystemMessageControl systemMessage, final String text) {
        this.systemMessage = systemMessage;
        
        this.showMessageBox(text);
        this.addButton(ButtonType.EXIT, "dismissMessage()");
        this.addButton(ButtonType.TICK, "closeMessage()");
    }

    public void showFocusMessage(final SystemMessageControl systemMessage, final String text) {
        this.showSystemMessage(systemMessage, text);

        this.addButton(ButtonType.GOTO, "focusElement()");
    }

    public void setText(final String text) {
        this.element.findNiftyControl("messageText", Label.class).setText(text);
        // update layout otherwise the scrollbar isn't correct
        this.element.layoutElements();
    }

    private void addButton(final ButtonType button, final String onClick) {
        String image = String.format("Textures/GUI/Tabs/Messages/mt-%s-$index.png", button.toString().toLowerCase());
        new ControlBuilder("messageButton"){{
            parameter("image", image.replace("$index", "00"));
            parameter("hoverImage", image.replace("$index", "01"));
            parameter("activeImage", image.replace("$index", "02"));
            parameter("click", onClick);
        }}.build(this.nifty, this.hud, this.buttonPanel);
    }

    private void cleanButtonPanel() {
        this.buttonPanel.getChildren().forEach((child)->{
            child.markForRemoval();
        });
    }
}
