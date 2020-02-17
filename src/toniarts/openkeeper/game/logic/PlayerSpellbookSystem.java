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
import toniarts.openkeeper.game.controller.player.AbstractResearchablePlayerControl;
import toniarts.openkeeper.game.controller.room.AbstractRoomController;
import toniarts.openkeeper.game.data.IIndexable;
import toniarts.openkeeper.tools.convert.map.KwdFile;

/**
 * Basically just reacts to stored spellbooks
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class PlayerSpellbookSystem implements IGameLogicUpdatable {

    private final KwdFile kwdFile;
    private final EntitySet spellbookEntities;
    private final Map<Short, IPlayerController> playerControllersByPlayerId = new HashMap<>(4);
    private final Map<EntityId, Short> ownersByEntityId = new HashMap<>();

    public PlayerSpellbookSystem(EntityData entityData, KwdFile kwdFile, Collection<IPlayerController> playerControllers) {
        this.kwdFile = kwdFile;
        for (IPlayerController playerController : playerControllers) {
            if (playerController.getResearchControl() != null) {
                playerControllersByPlayerId.put(playerController.getKeeper().getId(), playerController);
            }
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
            Spellbook spellbook = entity.get(Spellbook.class);
            short ownerId = entity.get(Owner.class).ownerId;
            ownersByEntityId.put(entity.getId(), ownerId);
            AbstractResearchablePlayerControl researchablePlayerControl = getResearchablePlayerControl(playerControllersByPlayerId, ownerId, spellbook);
            if (researchablePlayerControl != null) {
                researchablePlayerControl.onResearchResultsAdded(getResearchableType(kwdFile, spellbook));
            }
        }
    }

    private void processDeletedEntities(Set<Entity> entities) {
        for (Entity entity : entities) {
            Spellbook spellbook = entity.get(Spellbook.class);
            short ownerId = entity.get(Owner.class).ownerId;
            ownersByEntityId.remove(entity.getId());
            AbstractResearchablePlayerControl researchablePlayerControl = getResearchablePlayerControl(playerControllersByPlayerId, ownerId, spellbook);
            if (researchablePlayerControl != null) {
                researchablePlayerControl.onResearchResultsRemoved(getResearchableType(kwdFile, spellbook));
            }
        }
    }

    private void processChangedEntities(Set<Entity> entities) {
        for (Entity entity : entities) {
            Spellbook spellbook = entity.get(Spellbook.class);
            short newOwnerId = entity.get(Owner.class).ownerId;
            short oldOwnerId = ownersByEntityId.put(entity.getId(), newOwnerId);
            if (newOwnerId != oldOwnerId) {
                AbstractResearchablePlayerControl newResearchablePlayerControl = getResearchablePlayerControl(playerControllersByPlayerId, newOwnerId, spellbook);
                AbstractResearchablePlayerControl oldResearchablePlayerControl = getResearchablePlayerControl(playerControllersByPlayerId, oldOwnerId, spellbook);
                if (newResearchablePlayerControl != null) {
                    newResearchablePlayerControl.onResearchResultsAdded(getResearchableType(kwdFile, spellbook));
                }
                if (oldResearchablePlayerControl != null) {
                    oldResearchablePlayerControl.onResearchResultsRemoved(getResearchableType(kwdFile, spellbook));
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

    private static AbstractResearchablePlayerControl getResearchablePlayerControl(Map<Short, IPlayerController> playerControllersByPlayerId, short ownerId, Spellbook spellbook) {
        IPlayerController playerController = playerControllersByPlayerId.get(ownerId);
        if (playerController != null) {
            switch (spellbook.type) {
                case DOOR: {
                    return playerController.getDoorControl();
                }
                case ROOM: {
                    return playerController.getRoomControl();
                }
                case SPELL: {
                    return playerController.getSpellControl();
                }
                case TRAP: {
                    return playerController.getTrapControl();
                }
            }
        }

        return null;
    }

    private static IIndexable getResearchableType(KwdFile kwdFile, Spellbook spellbook) {
        switch (spellbook.type) {
            case DOOR: {
                return kwdFile.getDoorById(spellbook.typeId);
            }
            case ROOM: {
                return kwdFile.getRoomById(spellbook.typeId);
            }
            case SPELL: {
                return kwdFile.getKeeperSpellById(spellbook.typeId);
            }
            case TRAP: {
                return kwdFile.getTrapById(spellbook.typeId);
            }
        }

        return null;
    }
}
