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
import de.lessvoid.nifty.controls.AbstractController;
import de.lessvoid.nifty.controls.Parameters;
import de.lessvoid.nifty.effects.EffectEventId;
import de.lessvoid.nifty.elements.Element;
import de.lessvoid.nifty.input.NiftyInputEvent;
import de.lessvoid.nifty.screen.Screen;
import static toniarts.openkeeper.game.state.IPlayerScreenController.SCREEN_HUD_ID;

/**
 *
 * @author ufdada
 */
public class SystemMessageControl extends AbstractController {

    private Nifty nifty;
    private Element element;
    private boolean unread = true;
    private String text = "";
    private final Long createdAt = System.currentTimeMillis();
    private Screen hud;

    @Override
    public void bind(Nifty nifty, Screen screen, Element element, Parameters parameter) {
        this.text = parameter.get("text");
        this.element = element;
        this.nifty = nifty;
        this.hud = this.nifty.getScreen(SCREEN_HUD_ID);
        // sync effects on adding messages
        syncActiveEffects();
    }

    @Override
    public void onStartScreen() {
    }

    @Override
    public boolean inputEvent(NiftyInputEvent inputEvent) {
        return false;
    }

    /**
     * Set the message as read
     * @param read
     */
    public void setRead(boolean read) {
        this.unread = !read;
        if (read) {
            this.element.stopEffect(EffectEventId.onActive);
        } else {
            syncActiveEffects();
        }
    }

    /**
     * Show a message in an info box
     */
    public void showMessage() {
        this.setRead(true);
        hud.findControl("messageBox", MessageBoxControl.class).showSystemMessage(this, text);
    }

    /**
     * Remove a message from the message queue
     */
    public void dismissMessage() {
        this.element.markForRemoval();
    }

    /**
     * Used to sync the effect on active messages.
     * Otherwise all messages pulsate differently, which looks odd.
     */
    public void syncActiveEffects() {
        for(Element child : element.getParent().getChildren()) {
            SystemMessageControl control = child.getControl(SystemMessageControl.class);
            if (control != null && control.unread) {
                child.startEffect(EffectEventId.onActive);
            }
        }
    }

    /**
     * @return the time this message was created
     */
    public long getCreatedAt() {
        return this.createdAt;
    }

    /**
     * @return true if system message unread by the user
     */
    public boolean isUnread() {
        return this.unread;
    }

    /**
     * @return text of the system message
     */
    public String getText() {
        return this.text;
    }
}
