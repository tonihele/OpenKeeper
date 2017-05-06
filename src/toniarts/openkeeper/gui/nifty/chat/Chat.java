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

import de.lessvoid.nifty.controls.NiftyControl;
import java.util.List;

/**
 * @author ractoc
 */
public interface Chat extends NiftyControl {

    /**
     * This method is called when a chat line is received which should be
     * displayed in the chat control.
     *
     * @param text The text to display.
     * @param playerId The player's unique ID
     * @param keeperId The player's currently assigned keeper ID
     */
    void receivedChatLine(String text, int playerId, short keeperId);

    /**
     * This method returns all the chatlines in the chat.
     *
     * @return The current list of chatlines.
     */
    List<ChatEntry> getLines();

}
