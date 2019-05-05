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

import de.lessvoid.nifty.controls.ListBox.ListBoxViewConverter;
import de.lessvoid.nifty.elements.Element;
import de.lessvoid.nifty.elements.render.TextRenderer;
import de.lessvoid.nifty.tools.Color;
import java.util.logging.Logger;
import toniarts.openkeeper.utils.MapThumbnailGenerator;

/**
 * Handles the displaying of the items in the ChatBox.
 *
 * @author Mark
 * @version 0.1
 */
public class ChatBoxViewConverter implements ListBoxViewConverter<ChatEntry> {

    private static final Logger log = Logger.getLogger(ChatBoxViewConverter.class.getName());
    private static final String CHAT_LINE_TEXT = "#chat-line-text";

    /**
     * Default constructor.
     */
    public ChatBoxViewConverter() {
    }

    @Override
    public final void display(final Element listBoxItem, final ChatEntry item) {
        final Element text = listBoxItem.findElementById(CHAT_LINE_TEXT);
        if (text == null) {
            log.severe("Failed to locate text part of chat line! Can't display entry.");
        }
        final TextRenderer textRenderer = text.getRenderer(TextRenderer.class);
        if (textRenderer == null) {
            log.severe("Text entry of the chat line does not contain the required text renderer.");
            return;
        }
        textRenderer.setText(item.getLabel());

        // If keeper ID is set, color the line
        if (item.getKeeperId() != 0) {
            java.awt.Color c = MapThumbnailGenerator.getPlayerColor(item.getKeeperId());
            textRenderer.setColor(new Color(c.getRed() / 255.0f, c.getGreen() / 255.0f, c.getBlue() / 255.0f, 1f));
        } else {
            textRenderer.setColor(new Color("#bbbcbb"));
        }
    }

    @Override
    public final int getWidth(final Element listBoxItem, final ChatEntry item) {
        final Element text = listBoxItem.findElementById(CHAT_LINE_TEXT);
        if (text == null) {
            log.severe("Failed to locate text part of chat line! Can't display entry.");
            return 0;
        }
        final TextRenderer textRenderer = text.getRenderer(TextRenderer.class);
        if (textRenderer == null) {
            log.severe("Text entry of the chat line does not contain the required text renderer.");
            return 0;
        }
        return ((textRenderer.getFont() == null) ? 0 : textRenderer.getFont().getWidth(item.getLabel()));
    }

}
