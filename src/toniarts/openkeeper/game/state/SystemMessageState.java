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
package toniarts.openkeeper.game.state;

import com.jme3.app.Application;
import com.jme3.app.state.AppStateManager;
import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.NiftyIdCreator;
import de.lessvoid.nifty.builder.ControlBuilder;
import de.lessvoid.nifty.elements.Element;
import de.lessvoid.nifty.screen.Screen;
import toniarts.openkeeper.Main;
import static toniarts.openkeeper.game.state.PlayerScreenController.HUD_SCREEN_ID;
import toniarts.openkeeper.gui.nifty.message.SystemMessageControl;
import toniarts.openkeeper.tools.convert.ConversionUtils;

/**
 * TODO separate screen
 * @author ufdada
 */
public class SystemMessageState extends AbstractPauseAwareState {
    private final float lifeTime = 60000f;
    private Main app;
    private final Screen hud;
    private final Element systemMessagesQueue;

    public enum MessageType {
        INFO,
        FIGHT,
        ALLY,
        ALLYMSG,
        PLAYEREXIT,
        CREATURE
    };

    public SystemMessageState(Element systemMessages, boolean enabled) {
        this.systemMessagesQueue = systemMessages;
        this.hud = systemMessagesQueue.getNifty().getScreen(HUD_SCREEN_ID);

        // can be removed if system messages are correctly detached after quiting the game (like going back to main menu)
        this.removeAllMessages();

        super.setEnabled(enabled);
    }

    public SystemMessageState(Element systemMessages) {
        this(systemMessages, true);
    }

    @Override
    public void initialize(final AppStateManager stateManager, final Application app) {
        super.initialize(stateManager, app);
        this.app = (Main) app;
    }

    /**
     * Adds a message to the message queue
     *
     * @param type
     * @param text
     */
    public void addMessage(MessageType type, String text) {
        if (!this.isEnabled() || !isInitialized()) {
            return;
        }
        if (this.app != null) {
            this.app.enqueue(()-> {
                this.addMessageIcon(type, text);
                return null;
            });
        } else {
            this.addMessageIcon(type, text);
        }
    }

    public void addMessageIcon(MessageType type, String text) {
        final String icon = String.format("Textures/GUI/Tabs/Messages/mt-%s-$index.png", type.toString().toLowerCase());
        final String normalIcon = ConversionUtils.getCanonicalAssetKey(icon.replace("$index", "00"));
        final String hoverIcon = ConversionUtils.getCanonicalAssetKey(icon.replace("$index", "01"));
        final String activeIcon = ConversionUtils.getCanonicalAssetKey(icon.replace("$index", "02"));

        Element systemMessage = new ControlBuilder("sysmessage-" + NiftyIdCreator.generate(), "systemMessage"){{
            parameter("image", normalIcon);
            parameter("hoverImage", hoverIcon);
            parameter("activeImage", activeIcon);
            set("text", text);
        }}.build(systemMessagesQueue.getNifty(), this.hud, systemMessagesQueue);
        systemMessage.show();
    }

    @Override
    public void update(float tpf) {
        if (systemMessagesQueue != null) {
            for(Element child : systemMessagesQueue.getChildren()) {
                SystemMessageControl control = child.getControl(SystemMessageControl.class);
                if (control != null && System.currentTimeMillis() - control.getCreatedAt() > lifeTime) {
                    child.markForRemoval();
                }
            }
        }
    }

    @Override
    public boolean isPauseable() {
        return true;
    }

    /**
     * Removes all existing messages from the queue
     */
    private void removeAllMessages() {
        if (systemMessagesQueue != null && systemMessagesQueue.getChildrenCount() > 0) {
            systemMessagesQueue.getChildren().forEach((child)->{
                child.markForRemoval();
            });
        }
    }

    @Override
    public void cleanup() {
        super.cleanup();
        this.removeAllMessages();
    }
}
