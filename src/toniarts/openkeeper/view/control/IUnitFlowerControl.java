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
package toniarts.openkeeper.view.control;

import com.jme3.scene.control.Control;
import com.simsilica.es.EntityId;

/**
 * A base class for showing unit (creature, object...) flower
 *
 * @param <T> The type of the entity, the data record
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public interface IUnitFlowerControl<T> extends Control {

    T getDataObject();

    /**
     * Get the entity ID
     *
     * @return the entity ID
     */
    EntityId getEntityId();

    /**
     * Hide the flower
     */
    void hide();

    /**
     * Show the flower for a brief time
     */
    void show();

    /**
     * Show the flower for a period of time
     *
     * @param period the time period the flower should be visible
     */
    void show(float period);

}
