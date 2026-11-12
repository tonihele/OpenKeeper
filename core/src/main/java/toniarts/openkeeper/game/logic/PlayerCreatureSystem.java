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

import com.simsilica.es.Entity;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.EntitySet;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.player.PlayerCreatureControl;
import toniarts.openkeeper.tools.convert.map.IKwdFile;

/**
 * Basically just calculates the amount of creatures
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class PlayerCreatureSystem implements IGameLogicUpdatable {

    private final IKwdFile kwdFile;
    private final EntitySet creatureEntities;
    private final Map<Short, PlayerCreatureControl> creatureControls;
    private final Map<EntityId, Short> ownerIdsByEntityId = new HashMap<>();
    private final Map<EntityId, Short> creatureIdsByEntityId = new HashMap<>();

    public PlayerCreatureSystem(EntityData entityData, IKwdFile kwdFile, Collection<IPlayerController> playerControllers) {
        this.kwdFile = kwdFile;
        creatureControls = HashMap.newHashMap(playerControllers.size());
        for (IPlayerController playerController : playerControllers) {
            creatureControls.put(playerController.getKeeper().getId(), playerController.getCreatureControl());
        }

        creatureEntities = entityData.getEntities(CreatureComponent.class, Health.class, Owner.class);
        processAddedEntities(creatureEntities);
    }

    @Override
    public void processTick(float tpf) {
        if (creatureEntities.applyChanges()) {

            processAddedEntities(creatureEntities.getAddedEntities());

            processDeletedEntities(creatureEntities.getRemovedEntities());

            processChangedEntities(creatureEntities.getChangedEntities());
        }
    }

    private void processAddedEntities(Set<Entity> entities) {
        for (Entity entity : entities) {
            short ownerId = entity.get(Owner.class).ownerId;
            short creatureId = entity.get(CreatureComponent.class).creatureId;
            ownerIdsByEntityId.put(entity.getId(), ownerId);
            creatureIdsByEntityId.put(entity.getId(), creatureId);
            if (creatureControls.containsKey(ownerId)) {
                creatureControls.get(ownerId).onCreatureAdded(entity.getId(), kwdFile.getCreature(creatureId));
            }
        }
    }

    private void processDeletedEntities(Set<Entity> entities) {
        for (Entity entity : entities) {
            short ownerId = ownerIdsByEntityId.remove(entity.getId());
            short creatureId = creatureIdsByEntityId.remove(entity.getId());
            if (creatureControls.containsKey(ownerId)) {
                creatureControls.get(ownerId).onCreatureRemoved(entity.getId(), kwdFile.getCreature(creatureId));
            }
        }
    }

    private void processChangedEntities(Set<Entity> entities) {
        for (Entity entity : entities) {
            short newOwnerId = entity.get(Owner.class).ownerId;
            short newCreatureId = entity.get(CreatureComponent.class).creatureId;
            short oldOwnerId = ownerIdsByEntityId.put(entity.getId(), newOwnerId);
            short oldCreatureId = creatureIdsByEntityId.put(entity.getId(), newCreatureId);
            if (newOwnerId != oldOwnerId || newCreatureId != oldCreatureId) {
                if (creatureControls.containsKey(newOwnerId)) {
                    creatureControls.get(newOwnerId).onCreatureAdded(entity.getId(), kwdFile.getCreature(newCreatureId));
                }
                if (creatureControls.containsKey(oldOwnerId)) {
                    creatureControls.get(oldOwnerId).onCreatureRemoved(entity.getId(), kwdFile.getCreature(oldCreatureId));
                }
            }
        }
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {
        creatureEntities.release();
    }

}
