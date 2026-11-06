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

import com.jme3.math.Vector3f;
import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.builder.ControlBuilder;
import de.lessvoid.nifty.controls.AbstractController;
import de.lessvoid.nifty.controls.Label;
import de.lessvoid.nifty.controls.Parameters;
import de.lessvoid.nifty.elements.Element;
import de.lessvoid.nifty.input.NiftyInputEvent;
import de.lessvoid.nifty.screen.Screen;
import toniarts.openkeeper.game.sound.GlobalCategory;
import toniarts.openkeeper.game.sound.GlobalType;
import toniarts.openkeeper.game.state.IPlayerScreenController;
import toniarts.openkeeper.gui.nifty.NiftyUtils;
import static toniarts.openkeeper.game.state.IPlayerScreenController.SCREEN_HUD_ID;

/**
 *
 * @author ufdada
 */
public final class MessageBoxControl extends AbstractController {

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
        this.hud = nifty.getScreen(SCREEN_HUD_ID);
        this.element = element;
        this.buttonPanel = element.findElementById("#buttonPanel");
    }

    @Override
    public void onStartScreen() {
    }

    @Override
    public boolean inputEvent(NiftyInputEvent inputEvent) {
        return false;
    }

    /**
     * Zoom to the place the shown system message is about. The message stays
     * open, like in the original.
     */
    public void focusElement() {
        IPlayerScreenController screenController = getScreenController();
        Vector3f position = getZoomPosition(systemMessage, screenController);
        if (position != null) {
            screenController.zoomToPosition(position);
        }
    }

    public void dismissMessage() {
        NiftyUtils.playSound(nifty, GlobalCategory.OPTIONS, GlobalType.OPTIONS_CLICK_11.getId());
        if (this.systemMessage != null) {
            this.systemMessage.dismissMessage();
        }
        this.hide();
    }

    public void closeMessage() {
        NiftyUtils.playSound(nifty, GlobalCategory.OPTIONS, GlobalType.OPTIONS_CLICK_10.getId());
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

    /**
     * Shows a message box with text
     *
     * @param text
     */
    public void showMessage(final String text) {
        this.showMessageBox(text);
        this.addButton(ButtonType.TICK, "closeMessage()");
    }

    /**
     * Shows a removeable system message. If the message is about a place that
     * still exists, a zoom to button is offered too.
     *
     * @param systemMessage The control of the system message
     * @param text
     */
    public void showSystemMessage(final SystemMessageControl systemMessage, final String text) {
        this.systemMessage = systemMessage;

        this.showMessageBox(text);
        this.addButton(ButtonType.EXIT, "dismissMessage()");
        this.addButton(ButtonType.TICK, "closeMessage()");
        if (getZoomPosition(systemMessage, getScreenController()) != null) {
            this.addButton(ButtonType.GOTO, "focusElement()");
        }
    }

    private IPlayerScreenController getScreenController() {
        return hud.getScreenController() instanceof IPlayerScreenController controller ? controller : null;
    }

    private static Vector3f getZoomPosition(SystemMessageControl systemMessage, IPlayerScreenController screenController) {
        if (systemMessage == null || systemMessage.getTarget() == null || screenController == null) {
            return null;
        }
        return systemMessage.getTarget().resolve(screenController::getEntityPosition);
    }

    /**
     * Sets the text of the message box
     * @param text
     */
    public void setText(final String text) {
        this.element.findNiftyControl("#messageText", Label.class).setText(text);
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
            }
        }.build(this.buttonPanel);
    }

    private void cleanButtonPanel() {
        this.buttonPanel.getChildren().forEach((child)->{
            child.markForRemoval();
        });
    }
}
