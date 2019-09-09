/*
 * Copyright (C) 2014-2016 OpenKeeper
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

import com.jme3.network.service.rmi.Asynchronous;
import toniarts.openkeeper.game.data.PlayerSpell;

/**
 * Listen for changes in keeper spell statuses
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public interface PlayerSpellListener {

    @Asynchronous
    void onAdded(short keeperId, PlayerSpell spell);

    @Asynchronous
    void onRemoved(short keeperId, PlayerSpell spell);

    @Asynchronous
    void onResearchStatusChanged(short keeperId, PlayerSpell spell);

}
