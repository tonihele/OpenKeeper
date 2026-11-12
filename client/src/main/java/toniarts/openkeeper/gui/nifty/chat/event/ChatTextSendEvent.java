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
package toniarts.openkeeper.gui.nifty.chat.event;

import de.lessvoid.nifty.NiftyEvent;
import toniarts.openkeeper.gui.nifty.chat.Chat;

/**
 * @author ractoc
 */
public final class ChatTextSendEvent implements NiftyEvent {

    private final Chat chatControl;
    private final String text;

    public ChatTextSendEvent(final Chat chatControl, final String textParam) {
        this.chatControl = chatControl;
        this.text = textParam;
    }

    public Chat getChatControl() {
        return chatControl;
    }

    public String getText() {
        return text;
    }

}
