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
package toniarts.openkeeper.world.control;

import com.jme3.math.Vector3f;

/**
 * Simple interface to tell us that the object / creature can be hauled by a
 * worker
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public interface IHaulable {

    /**
     * Signal that the hauling has started
     */
    public void haulingStarted();

    /**
     * Signal that the hauling has ended
     */
    public void haulingEnded();

    /**
     * Update the position of the object as it is being hauled
     *
     * @param position the new position
     */
    public void updatePosition(Vector3f position);

}
