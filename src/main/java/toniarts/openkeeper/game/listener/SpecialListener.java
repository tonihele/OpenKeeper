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

/**
 * Get notified when a player activates a special. Server side only, the
 * listeners are not sent over the network.
 *
 * @author OpenKeeper
 */
public interface SpecialListener {

    /**
     * A special was activated
     *
     * @param playerId the player who activated it
     * @param objectId the object ID of the special
     */
    void onSpecialActivated(short playerId, short objectId);

}
