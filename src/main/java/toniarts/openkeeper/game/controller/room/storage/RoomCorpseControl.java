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
package toniarts.openkeeper.game.controller.room.storage;

import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import toniarts.openkeeper.game.component.Stored;
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.game.controller.room.AbstractRoomController.ObjectType;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.utils.Point;

/**
 * Holds the dead bodies resting in a graveyard. Like in DK II, one body takes
 * one tile and a stored body belongs to the owner of the graveyard.
 */
public abstract class RoomCorpseControl extends AbstractRoomObjectControl<EntityId> {

    public RoomCorpseControl(IKwdFile kwdFile, IRoomController parent, EntityData entityData, IGameTimer gameTimer) {
        super(kwdFile, parent, entityData, gameTimer, ObjectType.CORPSE);
    }

    @Override
    protected int getObjectsPerTile() {
        return 1;
    }

    /**
     * Lays a corpse to rest on the given tile
     *
     * @param corpse the corpse
     * @param p the tile
     * @return {@code null} if the corpse was stored, otherwise the corpse that
     * didn't fit
     */
    @Override
    public EntityId addItem(EntityId corpse, Point p) {
        Collection<EntityId> corpses = objectsByCoordinate.get(p);
        if (isFullCapacity() || (corpses != null && corpses.size() >= getObjectsPerTile())) {
            return corpse;
        }

        setRoomStorageToItem(corpse, true);
        objectsByCoordinate.computeIfAbsent(p, (point) -> new ArrayList<>(getObjectsPerTile())).add(corpse);
        addCurrentCapacity(1);

        return null;
    }

    @Override
    public void removeItem(EntityId corpse) {
        if (!isStored(corpse)) {
            return;
        }

        super.removeItem(corpse);
        addCurrentCapacity(-1);
    }

    private boolean isStored(EntityId corpse) {
        for (Collection<EntityId> corpses : objectsByCoordinate.values()) {
            if (corpses.contains(corpse)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void destroy() {

        // The bodies are left lying on the ground
        List<EntityId> corpses = new ArrayList<>();
        for (Collection<EntityId> objects : objectsByCoordinate.values()) {
            corpses.addAll(objects);
        }
        for (EntityId corpse : corpses) {
            entityData.removeComponent(corpse, Stored.class);
        }
        objectsByCoordinate.clear();

        super.destroy();
    }

}
