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
package toniarts.openkeeper.view.control;

import com.jme3.math.Vector3f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import toniarts.openkeeper.game.component.Position;

/**
 * Provides the render position for an object carried by another entity.
 */
@FunctionalInterface
public interface HauledObjectPositionProvider {

    /**
     * Computes the carried object's local translation for the current frame.
     * Implementations write the result into {@code store} and return
     * {@code true} when a follow position is available.
     *
     * @param entityData the entity data used to resolve the carrier
     * @param haulerId the entity carrying the object
     * @param objectPosition the carried object's current position, read-only
     * @param store reusable vector to receive the local translation; do not retain it
     * @return {@code true} when {@code store} contains a position to apply
     */
    boolean updateFollowPosition(EntityData entityData, EntityId haulerId,
            Position objectPosition, Vector3f store);
}
