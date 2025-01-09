/*
 * Copyright (C) 2014-2020 OpenKeeper
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
package toniarts.openkeeper.game.map;

import com.simsilica.es.EntityComponent;
import com.simsilica.es.EntityId;
import java.util.Objects;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.RoomComponent;

/**
 * A presentation of a single map tile. Gets data from specified entity
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public abstract class AbstractRoomInformation implements IRoomInformation {

    protected final EntityId entityId;

    public AbstractRoomInformation(EntityId entityId) {
        this.entityId = entityId;
    }

    protected abstract <T extends EntityComponent> T getEntityComponent(Class<T> type);

    @Override
    public short getOwnerId() {
        return getEntityComponent(Owner.class).ownerId;
    }

    @Override
    public int getHealth() {
        return getEntityComponent(Health.class).health;
    }

    @Override
    public int getMaxHealth() {
        return getEntityComponent(Health.class).maxHealth;
    }

    @Override
    public Integer getHealthPercent() {
        Health health = getEntityComponent(Health.class);
        return Math.round((float) health.health / health.maxHealth * 100);
    }

    /**
     * Is tile at full health
     *
     * @return true if full health
     */
    @Override
    public boolean isAtFullHealth() {
        Health health = getEntityComponent(Health.class);
        return (health.health == health.maxHealth);
    }

    @Override
    public EntityId getEntityId() {
        return entityId;
    }

    @Override
    public short getRoomId() {
        return getEntityComponent(RoomComponent.class).roomId;
    }

    @Override
    public boolean isDestroyed() {
        return getEntityComponent(RoomComponent.class).destroyed;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 41 * hash + Objects.hashCode(this.entityId);
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
        final AbstractRoomInformation other = (AbstractRoomInformation) obj;
        if (!Objects.equals(this.entityId, other.entityId)) {
            return false;
        }
        return true;
    }

}
