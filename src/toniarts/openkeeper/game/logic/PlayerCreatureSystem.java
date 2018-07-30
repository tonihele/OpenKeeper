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
import toniarts.openkeeper.tools.convert.map.KwdFile;

/**
 * Basically just calculates the amount of creatures
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class PlayerCreatureSystem implements IGameLogicUpdatable {

    private final KwdFile kwdFile;
    private final EntitySet creatureEntities;
    private final Map<Short, PlayerCreatureControl> creatureControls = new HashMap<>(4);
    private final Map<EntityId, Short> ownersByEntityId = new HashMap<>();

    public PlayerCreatureSystem(EntityData entityData, KwdFile kwdFile, Collection<IPlayerController> playerControllers) {
        this.kwdFile = kwdFile;
        for (IPlayerController playerController : playerControllers) {
            creatureControls.put(playerController.getKeeper().getId(), playerController.getCreatureControl());
        }

        creatureEntities = entityData.getEntities(CreatureComponent.class, Health.class, Owner.class);
        processAddedEntities(creatureEntities);
    }

    @Override
    public void processTick(float tpf, double gameTime) {
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
            ownersByEntityId.put(entity.getId(), ownerId);
            creatureControls.get(ownerId).onCreatureAdded(entity.getId(), kwdFile.getCreature(creatureId));
        }
    }

    private void processDeletedEntities(Set<Entity> entities) {
        for (Entity entity : entities) {
            short ownerId = entity.get(Owner.class).ownerId;
            short creatureId = entity.get(CreatureComponent.class).creatureId;
            ownersByEntityId.remove(entity.getId());
            creatureControls.get(ownerId).onCreatureRemoved(entity.getId(), kwdFile.getCreature(creatureId));
        }
    }

    private void processChangedEntities(Set<Entity> entities) {
        for (Entity entity : entities) {
            short newOwnerId = entity.get(Owner.class).ownerId;
            short creatureId = entity.get(CreatureComponent.class).creatureId;
            short oldOwnerId = ownersByEntityId.put(entity.getId(), newOwnerId);
            if (newOwnerId != oldOwnerId) {
                creatureControls.get(newOwnerId).onCreatureAdded(entity.getId(), kwdFile.getCreature(creatureId));
                creatureControls.get(oldOwnerId).onCreatureRemoved(entity.getId(), kwdFile.getCreature(creatureId));
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
