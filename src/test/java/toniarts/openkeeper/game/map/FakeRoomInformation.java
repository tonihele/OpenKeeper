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
package toniarts.openkeeper.game.map;

import com.simsilica.es.EntityId;
import toniarts.openkeeper.game.controller.room.AbstractRoomController;

/**
 * An {@link IRoomInformation} test double. Public: also reused by
 * {@code view.minimap}'s rendering tests.
 */
public final class FakeRoomInformation implements IRoomInformation {

    private final EntityId entityId;
    public short ownerId;
    public short roomId;
    public boolean dungeonHeart;
    public boolean removed;

    public FakeRoomInformation(EntityId entityId) {
        this.entityId = entityId;
    }

    @Override
    public EntityId getEntityId() {
        return entityId;
    }

    @Override
    public int getHealth() {
        return 0;
    }

    @Override
    public int getMaxHealth() {
        return 0;
    }

    @Override
    public Integer getHealthPercent() {
        return null;
    }

    @Override
    public boolean isAtFullHealth() {
        return true;
    }

    @Override
    public short getOwnerId() {
        return ownerId;
    }

    @Override
    public short getRoomId() {
        return roomId;
    }

    @Override
    public boolean isDungeonHeart() {
        return dungeonHeart;
    }

    @Override
    public boolean isDestroyed() {
        return false;
    }

    @Override
    public int getMaxCapacity() {
        return 0;
    }

    @Override
    public int getUsedCapacity() {
        return 0;
    }

    @Override
    public int getMaxCapacity(AbstractRoomController.ObjectType objectType) {
        return 0;
    }

    @Override
    public int getUsedCapacity(AbstractRoomController.ObjectType objectType) {
        return 0;
    }

    @Override
    public AbstractRoomController.ObjectType getDefaultStorageType() {
        return null;
    }

    @Override
    public boolean isRemoved() {
        return removed;
    }

}
