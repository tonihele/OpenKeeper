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

/**
 * Chat line entry, derived from the Nifty GUI example
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class ChatEntry {

    private final String label;
    private final int playerId;
    private final short keeperId;

    public ChatEntry(String label, int playerId, short keeperId) {
        this.label = label;
        this.playerId = playerId;
        this.keeperId = keeperId;
    }

    public String getLabel() {
        return label;
    }

    public int getPlayerId() {
        return playerId;
    }

    public short getKeeperId() {
        return keeperId;
    }

}
