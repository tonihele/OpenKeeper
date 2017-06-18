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

import com.jme3.math.Vector2f;
import com.jme3.network.service.rmi.Asynchronous;
import com.simsilica.es.EntityData;

/**
 * Clients view on game service
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public interface GameSession {

    /**
     * Get the game entity data
     *
     * @return entity data
     */
    public EntityData getEntityData();

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

    /**
     * Mark us ready to start receiving game updates
     */
    @Asynchronous
    public void markReady();

    /**
     * Set some tiles selected/undelected
     *
     * @param start start coordinates
     * @param end end coordinates
     * @param select select or unselect
     */
    @Asynchronous
    void selectTiles(Vector2f start, Vector2f end, boolean select);

}
