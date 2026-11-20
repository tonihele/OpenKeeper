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
package toniarts.openkeeper.game.logic;

import com.jme3.util.SafeArrayList;
import com.simsilica.es.Entity;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.EntitySet;
import com.simsilica.es.filter.AndFilter;
import com.simsilica.es.filter.FieldFilter;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.DoorComponent;
import toniarts.openkeeper.game.component.DoorViewState;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * Open sesame! Manages door opening and closing, on view level. We don't really
 * care in logic that is the door open or not. It is kinda always open if it is
 * otherwise valid to pass though one.
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class DoorViewSystem implements IGameLogicUpdatable {

    private final EntitySet doorEntities;

    private final EntityData entityData;
    private final SafeArrayList<EntityId> doorEntityIds;
    private final IEntityPositionLookup entityPositionLookup;

    public DoorViewSystem(EntityData entityData, IEntityPositionLookup entityPositionLookup) {
        this.entityData = entityData;
        this.entityPositionLookup = entityPositionLookup;

        doorEntities = entityData.getEntities(new AndFilter(DoorComponent.class,
                new FieldFilter(DoorComponent.class, "locked", false), new FieldFilter(DoorComponent.class, "blueprint", false)),
                DoorViewState.class, DoorComponent.class, Position.class, Owner.class);
        doorEntityIds = new SafeArrayList<>(EntityId.class);
        processAddedEntities(doorEntities);
    }

    @Override
    public void processTick(float tpf) {

        // Add new & remove old
        if (doorEntities.applyChanges()) {

            processAddedEntities(doorEntities.getAddedEntities());

            processDeletedEntities(doorEntities.getRemovedEntities());
        }

        // Basically we could also monitor movements, but I guess this is ok.
        // And if somebody is just left standing on the door, the door will stay open
        for (EntityId doorEntityId : doorEntityIds.getArray()) {
            Entity doorEntity = doorEntities.getEntity(doorEntityId);
            boolean shouldBeOpen = false;
            Position position = doorEntity.get(Position.class);
            List<EntityId> entitiesInSameTile = entityPositionLookup.getEntitiesInLocation(WorldUtils.vectorToPoint(position.position));
            if (entitiesInSameTile.size() > 1) {
                Owner owner = doorEntity.get(Owner.class);
                for (EntityId entityId : entitiesInSameTile) {
                    if (doorEntityId != entityId) {

                        // Should we open? Only to a creature of ours
                        if (entityData.getComponent(entityId, CreatureComponent.class) != null) {
                            Owner creatureOwner = entityData.getComponent(entityId, Owner.class);
                            if (creatureOwner.ownerId == owner.ownerId) {
                                shouldBeOpen = true;
                                break;
                            }
                        }
                    }
                }
            }

            // Close if nobody there or open if somebody is
            DoorViewState doorViewState = doorEntity.get(DoorViewState.class);
            if (doorViewState.open != shouldBeOpen) {
                entityData.setComponent(doorEntityId, new DoorViewState(doorViewState.doorId, doorViewState.locked,
                        doorViewState.blueprint, shouldBeOpen));
            }
        }
    }

    private void processAddedEntities(Set<Entity> entities) {
        for (Entity entity : entities) {
            int index = Collections.binarySearch(doorEntityIds, entity.getId());
            doorEntityIds.add(~index, entity.getId());
        }
    }

    private void processDeletedEntities(Set<Entity> entities) {
        for (Entity entity : entities) {
            int index = Collections.binarySearch(doorEntityIds, entity.getId());
            doorEntityIds.remove(index);
        }
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {
        doorEntities.release();
        doorEntityIds.clear();
    }

}
