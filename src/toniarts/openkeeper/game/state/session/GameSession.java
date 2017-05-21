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
package toniarts.openkeeper.game.state.session;

import com.jme3.network.service.rmi.Asynchronous;

/**
 * Clients view on game service
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public interface GameSession {

    /**
     * Signal that we are ready and loaded up
     */
    @Asynchronous
    public void loadComplete();

    /**
     * Our game loading status update
     *
     * @param progress our current progress
     */
    @Asynchronous
    public void loadStatus(float progress);

}
