/*
 * Copyright (C) 2014-2018 OpenKeeper
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
package toniarts.openkeeper.gui.nifty;

import toniarts.openkeeper.gui.nifty.AbstractCreatureCardControl.CreatureUIState;


/**
 * Exposes the {@link CreatureCardControl} user actions for listening
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public interface CreatureCardEventListener {

    /**
     * User requested to zoom to a creature
     *
     * @param creatureId the creature ID
     * @param uiState UI state, a.k.a. filter that was selected
     */
    public void zoomTo(short creatureId, CreatureUIState uiState);

    /**
     * User requested to pick up a creature
     *
     * @param creatureId the creature ID
     * @param uiState UI state, a.k.a. filter that was selected
     */
    public void pickUp(short creatureId, CreatureUIState uiState);

}
