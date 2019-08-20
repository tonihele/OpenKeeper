/*
 * Copyright (C) 2014-2019 OpenKeeper
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
import com.simsilica.es.filter.FieldFilter;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.RoomStorage;
import toniarts.openkeeper.game.component.Spellbook;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.player.PlayerSpellControl;
import toniarts.openkeeper.game.controller.room.AbstractRoomController;
import toniarts.openkeeper.tools.convert.map.KwdFile;

/**
 * Basically just reacts to stored spellbooks
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class PlayerSpellSystem implements IGameLogicUpdatable {

    private final KwdFile kwdFile;
    private final EntitySet spellbookEntities;
    private final Map<Short, PlayerSpellControl> spellControls = new HashMap<>(4);
    private final Map<EntityId, Short> ownersByEntityId = new HashMap<>();

    public PlayerSpellSystem(EntityData entityData, KwdFile kwdFile, Collection<IPlayerController> playerControllers) {
        this.kwdFile = kwdFile;
        for (IPlayerController playerController : playerControllers) {
            spellControls.put(playerController.getKeeper().getId(), playerController.getSpellControl());
        }

        spellbookEntities = entityData.getEntities(new FieldFilter(RoomStorage.class, "objectType", AbstractRoomController.ObjectType.SPELL_BOOK), Spellbook.class, RoomStorage.class, Owner.class);
        processAddedEntities(spellbookEntities);
    }

    @Override
    public void processTick(float tpf, double gameTime) {
        if (spellbookEntities.applyChanges()) {

            processDeletedEntities(spellbookEntities.getRemovedEntities());

            processAddedEntities(spellbookEntities.getAddedEntities());

            processChangedEntities(spellbookEntities.getChangedEntities());
        }
    }

    private void processAddedEntities(Set<Entity> entities) {
        for (Entity entity : entities) {
            short ownerId = entity.get(Owner.class).ownerId;
            int keeperSpellId = entity.get(Spellbook.class).keeperSpellId;
            ownersByEntityId.put(entity.getId(), ownerId);
            if (spellControls.containsKey(ownerId)) {
                spellControls.get(ownerId).onSpellbookAdded(kwdFile.getKeeperSpellById(keeperSpellId));
            }
        }
    }

    private void processDeletedEntities(Set<Entity> entities) {
        for (Entity entity : entities) {
            short ownerId = entity.get(Owner.class).ownerId;
            int keeperSpellId = entity.get(Spellbook.class).keeperSpellId;
            ownersByEntityId.remove(entity.getId());
            if (spellControls.containsKey(ownerId)) {
                spellControls.get(ownerId).onSpellbookRemoved(kwdFile.getKeeperSpellById(keeperSpellId));
            }
        }
    }

    private void processChangedEntities(Set<Entity> entities) {
        for (Entity entity : entities) {
            short newOwnerId = entity.get(Owner.class).ownerId;
            int keeperSpellId = entity.get(Spellbook.class).keeperSpellId;
            short oldOwnerId = ownersByEntityId.put(entity.getId(), newOwnerId);
            if (newOwnerId != oldOwnerId) {
                if (spellControls.containsKey(newOwnerId)) {
                    spellControls.get(newOwnerId).onSpellbookAdded(kwdFile.getKeeperSpellById(keeperSpellId));
                }
                if (spellControls.containsKey(oldOwnerId)) {
                    spellControls.get(oldOwnerId).onSpellbookRemoved(kwdFile.getKeeperSpellById(keeperSpellId));
                }
            }
        }
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {
        spellbookEntities.release();
    }

}
