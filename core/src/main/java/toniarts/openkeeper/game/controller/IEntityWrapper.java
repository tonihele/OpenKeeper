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
package toniarts.openkeeper.game.controller;

import com.simsilica.es.EntityId;
import toniarts.openkeeper.game.controller.entity.IEntityController;

/**
 * Wraps entities in controllers
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 * @param <T> The entity wrapper
 */
public interface IEntityWrapper<T extends IEntityController> {

    T createController(EntityId entityId);

    /**
     * Test whether the entity can the wrapped as T
     *
     * @param entityId the entity to check
     * @return true if you can wrap
     */
    boolean isValidEntity(EntityId entityId);

}
