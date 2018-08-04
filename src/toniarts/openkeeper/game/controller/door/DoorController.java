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
package toniarts.openkeeper.game.controller.door;

import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import java.util.Objects;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.tools.convert.map.Door;

/**
 * Controller for door type entities
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class DoorController implements IDoorController {

    private final EntityId entityId;
    private final EntityData entityData;
    private final Door door;

    public DoorController(EntityId entityId, EntityData entityData, Door door) {
        this.entityId = entityId;
        this.entityData = entityData;
        this.door = door;
    }

    @Override
    public boolean isDestroyed() {
        Health health = entityData.getComponent(entityId, Health.class);
        return (health == null || health.health == 0);
    }


    @Override
    public int hashCode() {
        int hash = 3;
        hash = 29 * hash + Objects.hashCode(this.entityId);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final DoorController other = (DoorController) obj;
        if (!Objects.equals(this.entityId, other.entityId)) {
            return false;
        }
        return true;
    }
}
