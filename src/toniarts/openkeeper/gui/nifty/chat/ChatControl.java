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
package toniarts.openkeeper.gui.nifty.chat;

import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.controls.AbstractController;
import de.lessvoid.nifty.controls.ListBox;
import de.lessvoid.nifty.controls.Parameters;
import de.lessvoid.nifty.controls.TextField;
import de.lessvoid.nifty.elements.Element;
import de.lessvoid.nifty.input.NiftyInputEvent;
import de.lessvoid.nifty.input.NiftyStandardInputEvent;
import de.lessvoid.nifty.screen.KeyInputHandler;
import de.lessvoid.nifty.screen.Screen;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;
import toniarts.openkeeper.gui.nifty.chat.event.ChatTextSendEvent;

/**
 * This is the main controller for the chat control.
 *
 * @author Mark
 * @version 0.1
 */
public class ChatControl extends AbstractController implements KeyInputHandler, Chat {
    
    private static final Logger LOGGER = Logger.getLogger(ChatControl.class.getName());

    private static final String CHAT_BOX = "#chatBox";
    private static final String CHAT_TEXT_INPUT = "#chat-text-input";
    
    private TextField textControl;
    private Nifty nifty;
    private final Queue<ChatEntry> linesBuffer = new ArrayDeque<>();

    /**
     * Default constructor.
     */
    public ChatControl() {
    }

    @Override
    public final void bind(
            final Nifty niftyParam,
            final Screen screenParam,
            final Element newElement,
            final Parameters properties) {
        super.bind(newElement);
        LOGGER.fine("binding chat control");
        nifty = niftyParam;

        // this buffer is needed because in some cases the entry is added to either list before the element is bound.
        final ListBox<ChatEntry> chatBox = getListBox(CHAT_BOX);
        if (chatBox == null) {
            LOGGER.severe("Element for chat box \"" + CHAT_BOX + "\" not found. ChatControl will not work.");
        } else {
            while (!linesBuffer.isEmpty()) {
                ChatEntry line = linesBuffer.poll();
                LOGGER.log(Level.FINE, "adding message {0}", (chatBox.itemCount() + 1));
                chatBox.addItem(line);
                chatBox.showItemByIndex(chatBox.itemCount() - 1);
            }
        }
    }

    @Override
    public void onFocus(final boolean arg0) {
        if (textControl != null) {
            textControl.setFocus();
        }
    }

    @Override
    public final void onStartScreen() {
        Element element = getElement();
        if (element != null) {
            textControl = element.findNiftyControl(CHAT_TEXT_INPUT, TextField.class);
            if (textControl == null) {
                LOGGER.severe("Text input field for chat box was not found!");
            } else {
                Element textControlElement = textControl.getElement();
                if (textControlElement != null) {
                    textControlElement.addInputHandler(this);
                }
            }
        }
    }

    @Override
    public List<ChatEntry> getLines() {
        final ListBox<ChatEntry> chatBox = getListBox(CHAT_BOX);
        if (chatBox == null) {
            return Collections.emptyList();
        }
        return new ArrayList<>(chatBox.getItems());
    }

    /**
     * This method is called when the player either presses the send button or
     * the Return key.
     */
    public final void sendText() {
        final String text;
        if (textControl == null) {
            text = "";
        } else {
            text = textControl.getRealText();
            textControl.setText("");
        }
        final String id = getId();
        if (id != null) {
            nifty.publishEvent(id, new ChatTextSendEvent(this, text));
        }
    }

    private ListBox<ChatEntry> getListBox(final String name) {
        Element element = getElement();
        if (element == null) {
            return null;
        }
        return element.findNiftyControl(name, ListBox.class);
    }

    @Override
    public boolean keyEvent(final NiftyInputEvent inputEvent) {
        if (inputEvent == NiftyStandardInputEvent.SubmitText) {
            sendText();
            return true;
        }
        return false;
    }

    @Override
    public boolean inputEvent(final NiftyInputEvent inputEvent) {
        return keyEvent(inputEvent);
    }

    @Override
    public void receivedChatLine(String text, int playerId, short keeperId) {
        if (linesBuffer.isEmpty()) {
            final ListBox<ChatEntry> chatBox = getListBox(CHAT_BOX);
            if (chatBox != null) {
                LOGGER.log(Level.FINE, "adding message {0}", (chatBox.itemCount() + 1));
                chatBox.addItem(new ChatEntry(text, playerId, keeperId));
                chatBox.showItemByIndex(chatBox.itemCount() - 1);
            } else {
                linesBuffer.add(new ChatEntry(text, playerId, keeperId));
            }
        } else {
            linesBuffer.add(new ChatEntry(text, playerId, keeperId));
        }
    }

    @Override
    public void clear() {
        getListBox(CHAT_BOX).clear();
    }

}
