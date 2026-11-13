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
package toniarts.openkeeper.gui.nifty.message;

import com.jme3.math.Vector3f;
import com.simsilica.es.EntityId;
import java.util.function.Function;

/**
 * What a system message is about, so that the message box can offer a zoom to
 * button for it. Like in the original, the target is either a thing (followed
 * to wherever it is now, as long as it still exists) or a fixed map position.
 *
 * @param entityId the entity the message is about, or {@code null}
 * @param position the fixed position the message is about, or {@code null}
 */
public record SystemMessageTarget(EntityId entityId, Vector3f position) {

    /**
     * A message about an entity, e.g. an enemy creature that was sighted
     *
     * @param entityId the entity
     * @return the target
     */
    public static SystemMessageTarget ofEntity(EntityId entityId) {
        return new SystemMessageTarget(entityId, null);
    }

    /**
     * A message about a place on the map
     *
     * @param position the position
     * @return the target
     */
    public static SystemMessageTarget ofPosition(Vector3f position) {
        return new SystemMessageTarget(null, position.clone());
    }

    /**
     * Where the camera should zoom to for this message. An entity target
     * resolves to where the entity is now, and to nothing once it is gone,
     * in which case there is nothing to zoom to.
     *
     * @param entityPositions gives the current position of an entity, or
     * {@code null} if the entity no longer exists
     * @return the position to zoom to, or {@code null} if there is none
     */
    public Vector3f resolve(Function<EntityId, Vector3f> entityPositions) {
        if (entityId != null) {
            return entityPositions.apply(entityId);
        }
        return position;
    }
}
