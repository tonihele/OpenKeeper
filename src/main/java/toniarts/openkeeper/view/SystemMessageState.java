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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import toniarts.openkeeper.Main;
import toniarts.openkeeper.game.sound.GlobalCategory;
import toniarts.openkeeper.game.sound.GlobalType;
import toniarts.openkeeper.game.state.AbstractPauseAwareState;
import toniarts.openkeeper.gui.nifty.NiftyUtils;
import toniarts.openkeeper.gui.nifty.message.SystemMessageControl;
import toniarts.openkeeper.gui.nifty.message.SystemMessageTarget;
import toniarts.openkeeper.utils.AssetUtils;

/**
 * TODO separate screen
 *
 * @author ufdada
 */
public final class SystemMessageState extends AbstractPauseAwareState {

    private static final float MESSAGE_LIFETIME = 60f;

    /**
     * How many of the keeper's oldest held messages DK II's message row looks
     * at for tabs (0x423BD0)
     */
    private static final int MAX_TABS = 8;

    private Main app;
    private final Element systemMessagesQueue;

    /**
     * The messages held, oldest first
     */
    private final List<HeldMessage> heldMessages = new ArrayList<>();

    public enum MessageType {
        INFO("info"),
        FIGHT("fight"),
        ALLY("ally"),
        ALLY_MSG("allymsg"),
        PLAYER_EXIT("playerexit"),
        CREATURE("creature"),
        OBJECTIVE("objective");

        private MessageType(String icon) {
            this.icon = icon;
        }

        public String getIcon() {
            return icon;
        }

        /**
         * The tab icon of a message in DK II's message table (0x6A0B00). The
         * message row (0x423BD0) draws graphic id 1 as the info icon, 2 the
         * creature, 3 the fight, 4 the objective, 5 the ally message and 6 the
         * player exit icon, and 0 as 1
         *
         * @param graphic the message's graphic id
         * @return the message type with the icon
         */
        public static MessageType ofGraphic(int graphic) {
            return switch (graphic) {
                case 2 -> CREATURE;
                case 3 -> FIGHT;
                case 4 -> OBJECTIVE;
                case 5 -> ALLY_MSG;
                case 6 -> PLAYER_EXIT;
                default -> INFO;
            };
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
     * Adds a message to the message queue of the attached message state, if
     * there is one. The state is gone when the level has ended, or is not yet
     * loaded.
     *
     * @param stateManager the state manager to look the message state up from
     * @param type message type
     * @param text message text
     * @return {@code true} if there was a message state to hand the message to
     */
    public static boolean addMessageIfAttached(AppStateManager stateManager, MessageType type, String text) {
        SystemMessageState systemMessageState = stateManager.getState(SystemMessageState.class);
        if (systemMessageState == null) {
            return false;
        }
        systemMessageState.addMessage(type, text);
        return true;
    }

    /**
     * Adds a message to the message queue
     *
     * @param type message type
     * @param text message text
     */
    public void addMessage(MessageType type, String text) {
        addMessage(type, text, null);
    }

    /**
     * Adds a message about a certain place or thing to the message queue, the
     * message box then offers to zoom to it
     *
     * @param type message type
     * @param text message text
     * @param target what the message is about, or {@code null} if nothing in
     * particular
     */
    public void addMessage(MessageType type, String text, SystemMessageTarget target) {
        addMessage(type, text, target, MESSAGE_LIFETIME, MESSAGE_LIFETIME);
    }

    /**
     * Adds a message to the message queue that is held for its own time, as
     * each message in DK II's message queue is (0x4C4600). Its tab may go
     * sooner
     *
     * @param type message type
     * @param text message text
     * @param target what the message is about, or {@code null} if nothing in
     * particular
     * @param seconds how long the message is held
     * @param tabSeconds how long the message shows its tab at most
     */
    public void addMessage(MessageType type, String text, SystemMessageTarget target, float seconds, float tabSeconds) {
        if (!this.isEnabled() || !isInitialized()) {
            return;
        }
        HeldMessage message = new HeldMessage(type, text, target, seconds, tabSeconds);
        if (this.app != null) {
            this.app.enqueue(() -> {
                heldMessages.add(message);
            });
        } else {
            heldMessages.add(message);
        }
    }

    private void showTab(HeldMessage message) {
        message.tabShown = true;
        if (systemMessagesQueue == null) {
            return;
        }
        final String icon = String.format("Textures/GUI/Tabs/Messages/mt-%s-$index.png", message.type.getIcon());
        final String normalIcon = AssetUtils.getCanonicalAssetKey(icon.replace("$index", "00"));
        final String hoverIcon = AssetUtils.getCanonicalAssetKey(icon.replace("$index", "01"));
        final String activeIcon = AssetUtils.getCanonicalAssetKey(icon.replace("$index", "02"));

        Element systemMessage = new ControlBuilder("sysmessage-" + NiftyIdCreator.generate(), "systemMessage") {
            {
                parameter("image", normalIcon);
                parameter("hoverImage", hoverIcon);
                parameter("activeImage", activeIcon);
                set("text", message.text);
            }
        }.build(systemMessagesQueue);
        SystemMessageControl control = systemMessage.getControl(SystemMessageControl.class);
        if (control != null) {
            control.setTarget(message.target);
        }
        systemMessage.show();
        message.tab = systemMessage;
        if (app != null) {
            SoundHandle soundHandler = NiftyUtils.getSoundHandler(app.getNifty(),
                    GlobalCategory.GUI_MESSAGE_BAR, GlobalType.GUI_MESSAGE_BAR_SLIDE.getId());
            if (soundHandler != null) {
                soundHandler.play();
            }
        }
    }

    private static void removeTab(HeldMessage message) {
        if (message.tab != null) {
            SystemMessageControl control = message.tab.getControl(SystemMessageControl.class);
            if (control == null || !control.isDismissed()) {
                message.tab.markForRemoval();
            }
            message.tab = null;
        }
    }

    @Override
    public void update(float tpf) {

        // DK II's message row (0x423BD0) walks the keeper's held messages oldest
        // first and looks at the first 8 only. A message shows its tab until the
        // tab's time runs out or it is dismissed, but stays one of the 8 for as
        // long as it is held. So a newer message gets its tab once an older one
        // is no longer held, if its own tab time hasn't run out by then
        int index = 0;
        for (Iterator<HeldMessage> iterator = heldMessages.iterator(); iterator.hasNext();) {
            HeldMessage message = iterator.next();
            message.age += tpf;
            if (message.age >= message.seconds) {
                removeTab(message);
                iterator.remove();
                continue;
            }
            if (message.age >= message.tabSeconds) {
                removeTab(message);
            } else if (index < MAX_TABS && !message.tabShown) {
                showTab(message);
            }
            index++;
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
        heldMessages.clear();
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
    }

    /**
     * A message held in the queue, and its tab
     */
    private static final class HeldMessage {

        private final MessageType type;
        private final String text;
        private final SystemMessageTarget target;
        private final float seconds;
        private final float tabSeconds;
        private float age;
        private boolean tabShown;
        private Element tab;

        private HeldMessage(MessageType type, String text, SystemMessageTarget target, float seconds, float tabSeconds) {
            this.type = type;
            this.text = text;
            this.target = target;
            this.seconds = seconds;
            this.tabSeconds = tabSeconds;
        }
    }
}
