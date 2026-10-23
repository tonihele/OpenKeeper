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
package toniarts.openkeeper.game.logic;

import com.jme3.math.Vector2f;
import com.simsilica.es.Entity;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.EntitySet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.Death;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.ObjectComponent;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.Stored;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.room.AbstractRoomController.ObjectType;
import toniarts.openkeeper.game.controller.room.GraveyardController;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.controller.room.storage.IRoomObjectControl;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Variable;
import toniarts.openkeeper.utils.Utils;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * Raises vampires from the graveyards, as DK II does. When a body has rotted
 * away in a graveyard, its decompose value goes to that graveyard. Once the
 * graveyard has gathered at least DECOMPOSE_VALUE_NEEDED_FOR_VAMPIRE, its
 * count goes back to zero and the creature the room creates (the Vampire)
 * rises at level one for the graveyard owner, at a random crypt of the room or
 * at the room centre if it has no crypts.<br>
 * The owner can only have as many vampires (creatures that can resurrect) as
 * all his graveyards have tiles for bodies. While that limit is reached no
 * vampire rises and the graveyard keeps gathering.
 */
public final class GraveyardSystem implements IGameLogicUpdatable {

    private final EntityData entityData;
    private final IMapController mapController;
    private final ICreaturesController creaturesController;
    private final int decomposeValueNeededForVampire;
    private final Set<Short> vampireTypes = new HashSet<>();
    private final EntitySet storedCorpses;
    private final EntitySet livingCreatures;
    private final Map<EntityId, StoredCorpse> corpses = new HashMap<>();
    private final Map<EntityId, Integer> decomposeValues = new HashMap<>();

    public GraveyardSystem(EntityData entityData, IKwdFile kwdFile,
            Map<Variable.MiscVariable.MiscType, Variable.MiscVariable> gameSettings,
            IMapController mapController, ICreaturesController creaturesController) {
        this.entityData = entityData;
        this.mapController = mapController;
        this.creaturesController = creaturesController;
        decomposeValueNeededForVampire = (int) gameSettings.get(Variable.MiscVariable.MiscType.DECOMPOSE_VALUE_NEEDED_FOR_VAMPIRE).getValue();
        for (Creature creature : kwdFile.getCreatureList()) {
            if (creature.getFlags().contains(Creature.CreatureFlag.CAN_BE_RESURECTED)) {
                vampireTypes.add(creature.getCreatureId());
            }
        }

        storedCorpses = entityData.getEntities(CreatureComponent.class, Death.class, Stored.class);
        processAddedCorpses(storedCorpses);
        livingCreatures = entityData.getEntities(CreatureComponent.class, Owner.class, Health.class);
    }

    @Override
    public void processTick(float tpf) {
        livingCreatures.applyChanges();
        if (storedCorpses.applyChanges()) {
            processRemovedCorpses(storedCorpses.getRemovedEntities());
            processAddedCorpses(storedCorpses.getAddedEntities());
            processAddedCorpses(storedCorpses.getChangedEntities());
        }
    }

    private void processAddedCorpses(Set<Entity> entities) {
        for (Entity entity : entities) {
            Stored stored = entity.get(Stored.class);
            if (stored.objectType == ObjectType.CORPSE) {
                corpses.put(entity.getId(), new StoredCorpse(stored.room, entity.get(CreatureComponent.class).decomposeValue));
            } else {
                corpses.remove(entity.getId());
            }
        }
    }

    private void processRemovedCorpses(Set<Entity> entities) {
        for (Entity entity : entities) {
            StoredCorpse corpse = corpses.remove(entity.getId());

            // Only a body that rotted away counts, not one taken out of the graveyard
            if (corpse == null || entityData.getComponent(entity.getId(), CreatureComponent.class) != null) {
                continue;
            }

            IRoomController room = mapController.getRoomController(corpse.room());
            if (room == null || room.isRemoved() || room.isDestroyed()) {
                continue;
            }
            IRoomObjectControl<?> control = room.getObjectControl(ObjectType.CORPSE);
            if (control != null) {
                control.removeItem(entity.getId());
            }
            corpseDecomposed(room, corpse.decomposeValue());
        }
    }

    private void corpseDecomposed(IRoomController room, int decomposeValue) {
        int value = decomposeValues.merge(room.getEntityId(), Math.max(0, decomposeValue), Integer::sum);
        if (value >= decomposeValueNeededForVampire && !isVampireLimitReached(room.getOwnerId())) {
            decomposeValues.put(room.getEntityId(), 0);
            raiseVampire(room);
        }
    }

    private boolean isVampireLimitReached(short playerId) {
        int maxVampires = 0;
        for (IRoomController room : mapController.getRoomsByFunction(ObjectType.CORPSE, playerId)) {
            maxVampires += room.getObjectControl(ObjectType.CORPSE).getMaxCapacity();
        }

        int vampires = 0;
        for (Entity entity : livingCreatures) {
            if (entity.get(Owner.class).ownerId == playerId && vampireTypes.contains(entity.get(CreatureComponent.class).creatureId)) {
                vampires++;
            }
        }

        return vampires >= maxVampires;
    }

    private void raiseVampire(IRoomController room) {
        short creatureId = room.getRoom().getCreatedCreatureId();
        if (creatureId == 0) {
            return;
        }

        List<Vector2f> crypts = new ArrayList<>();
        for (EntityId furniture : room.getFloorFurniture()) {
            ObjectComponent objectComponent = entityData.getComponent(furniture, ObjectComponent.class);
            Position position = entityData.getComponent(furniture, Position.class);
            if (objectComponent != null && objectComponent.objectId == GraveyardController.OBJECT_CRYPT_ID && position != null) {
                crypts.add(new Vector2f(position.position.x, position.position.z));
            }
        }
        Vector2f location = crypts.isEmpty() ? WorldUtils.pointToVector2f(room.getRoomInstance().getCenter()) : Utils.getRandomItem(crypts);

        creaturesController.spawnCreature(creatureId, room.getOwnerId(), 1, location, ICreaturesController.SpawnType.PLACE);
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {
        storedCorpses.release();
        livingCreatures.release();
        corpses.clear();
        decomposeValues.clear();
    }

    private record StoredCorpse(EntityId room, int decomposeValue) {

    }

}
