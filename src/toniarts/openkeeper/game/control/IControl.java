/*
 * Copyright (C) 2014-2015 OpenKeeper
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
package toniarts.openkeeper.game.control;

import com.jme3.export.Savable;

/**
 *
 * @author ArchDemon
 */

public interface IControl extends Savable {

    /**
     * @param parent the spatial to be controlled. This should not be called from user code.
     */
    public void setParent(IContainer parent);

    /**
     * Updates the control. This should not be called from user code.
     *
     * @param tpf Time per frame.
     */
    public void update(float tpf);
}
