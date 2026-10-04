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
package toniarts.openkeeper.view;

import com.jme3.app.Application;
import com.jme3.app.state.AppStateManager;
import de.lessvoid.nifty.NiftyIdCreator;
import de.lessvoid.nifty.builder.ControlBuilder;
import de.lessvoid.nifty.elements.Element;
import de.lessvoid.nifty.spi.sound.SoundHandle;
import java.util.HashMap;
import java.util.Map;
import toniarts.openkeeper.Main;
import toniarts.openkeeper.game.data.PlayerMessageType;
import toniarts.openkeeper.game.sound.GlobalCategory;
import toniarts.openkeeper.game.sound.GlobalType;
import toniarts.openkeeper.game.state.AbstractPauseAwareState;
import toniarts.openkeeper.gui.nifty.NiftyUtils;
import toniarts.openkeeper.gui.nifty.message.SystemMessageControl;
import toniarts.openkeeper.utils.AssetUtils;

/**
 * TODO separate screen
 * TODO:
 * - Events the game doesn't have yet: payday, heart damage and destruction, combat, torture and prison, casino, creature moods, and the single-player save/load messages.
 * - Imprisonment on/off (0x3E/0x3F): I didn't look for a hook, so I can't say whether one exists.
 * @author ufdada
 */
public final class SystemMessageState extends AbstractPauseAwareState {

    /**
     * A tab is shown at most this long, whatever the type's timeout is
     */
    private static final int MAX_TAB_LIFETIME_SECONDS = 50;

    private Main app;
    private final Element systemMessagesQueue;
    private float time = 0;
    private boolean systemMessagesEnabled = true;
    private final Map<Long, Float> liveMessages = new HashMap<>();

    public enum MessageType {
        INFO("info"),
        FIGHT("fight"),
        ALLY("ally"),
        ALLY_MSG("allymsg"),
        PLAYER_EXIT("playerexit"),
        CREATURE("creature");

        private MessageType(String icon) {
            this.icon = icon;
        }

        public String getIcon() {
            return icon;
        }

        private final String icon;
    };

    public SystemMessageState(Element systemMessages, boolean enabled) {
        this.systemMessagesQueue = systemMessages;

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
     * @param type message category (tab icon)
     * @param text message text
     */
    public void addMessage(MessageType type, String text) {
        addMessage(PlayerMessageType.CUSTOM, type, 0, text);
    }

    /**
     * Adds a message of a known type to the message queue
     *
     * @param type message type
     * @param text message text
     */
    public void addMessage(PlayerMessageType type, String text) {
        addMessage(type, MessageType.valueOf(type.getCategory().name()), 0, text);
    }

    /**
     * Adds a message of a known type to the message queue
     *
     * @param type message type
     * @param key variant of the type (e.g. the room or spell), a live message
     * is only a duplicate if the key matches too
     * @param text message text
     */
    public void addMessage(PlayerMessageType type, int key, String text) {
        addMessage(type, MessageType.valueOf(type.getCategory().name()), key, text);
    }

    private void addMessage(PlayerMessageType type, MessageType category, int key, String text) {
        if (!this.isEnabled() || !isInitialized()) {
            return;
        }
        if (!systemMessagesEnabled && !type.isAlwaysAllowed()) {
            return;
        }

        // The timeout is also the repeat interval
        if (type.isDeduplicated()) {
            Long id = ((long) type.getId() << 32) | (key & 0xFFFFFFFFL);
            Float expires = liveMessages.get(id);
            if (expires != null && expires > time) {
                return;
            }
            liveMessages.put(id, time + type.getTimeoutSeconds());
        }

        final long lifetimeMs = Math.min(type.getTimeoutSeconds(), MAX_TAB_LIFETIME_SECONDS) * 1000L;
        if (this.app != null) {
            SoundHandle soundHandler = NiftyUtils.getSoundHandler(app.getNifty(),
                        GlobalCategory.GUI_MESSAGE_BAR, GlobalType.GUI_MESSAGE_BAR_SLIDE.getId());
            this.app.enqueue(() -> {
                this.addMessageIcon(category, text, lifetimeMs);
                if (soundHandler != null) {
                    soundHandler.play();
                }
            });
        } else {
            this.addMessageIcon(category, text, lifetimeMs);
        }
    }

    /**
     * Switches the advisor messages on or off, see the trigger action
     * SET_SYSTEM_MESSAGES. When off, only the level's own speeches and the
     * magic item and special notices get through.
     *
     * @param enabled true to show all messages
     */
    public void setSystemMessagesEnabled(boolean enabled) {
        this.systemMessagesEnabled = enabled;
    }

    public void addMessageIcon(MessageType type, String text, long lifetimeMs) {
        final String icon = String.format("Textures/GUI/Tabs/Messages/mt-%s-$index.png", type.getIcon());
        final String normalIcon = AssetUtils.getCanonicalAssetKey(icon.replace("$index", "00"));
        final String hoverIcon = AssetUtils.getCanonicalAssetKey(icon.replace("$index", "01"));
        final String activeIcon = AssetUtils.getCanonicalAssetKey(icon.replace("$index", "02"));

        Element systemMessage = new ControlBuilder("sysmessage-" + NiftyIdCreator.generate(), "systemMessage") {
            {
                parameter("image", normalIcon);
                parameter("hoverImage", hoverIcon);
                parameter("activeImage", activeIcon);
                set("text", text);
            }
        }.build(systemMessagesQueue);
        SystemMessageControl control = systemMessage.getControl(SystemMessageControl.class);
        if (control != null) {
            control.setLifetime(lifetimeMs);
        }
        systemMessage.show();
    }

    @Override
    public void update(float tpf) {
        time += tpf;
        if (systemMessagesQueue == null) {
            return;
        }

        for (Element child : systemMessagesQueue.getChildren()) {
            SystemMessageControl control = child.getControl(SystemMessageControl.class);
            if (control != null && System.currentTimeMillis() - control.getCreatedAt() > control.getLifetime()) {
                child.markForRemoval();
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
            systemMessagesQueue.getChildren().forEach((child) -> {
                child.markForRemoval();
            });
        }
    }

    @Override
    public void cleanup() {
        super.cleanup();
        this.removeAllMessages();
        liveMessages.clear();
    }
}
