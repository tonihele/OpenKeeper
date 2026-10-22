/*
 * Copyright (C) 2014-2026 OpenKeeper
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
package toniarts.openkeeper.game.listener;

import toniarts.openkeeper.game.data.PlayerMessageType;

/**
 * Receives the messages (advisor notifications) posted for a player
 *
 * @author ufdada
 */
@FunctionalInterface
public interface PlayerMessageListener {

    /**
     * A message was posted to the player
     *
     * @param type the message type
     * @param key variant of the type, 0 if the type has none
     * @param data1 type specific data
     * @param data2 type specific data
     */
    void onPlayerMessage(PlayerMessageType type, int key, long data1, long data2);
}
