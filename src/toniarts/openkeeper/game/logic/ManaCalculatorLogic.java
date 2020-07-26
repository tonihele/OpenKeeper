/*
 * Copyright (C) 2014-2017 OpenKeeper
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
import toniarts.openkeeper.game.component.Mana;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.player.PlayerManaControl;

/**
 * Calculates mana for all players
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class ManaCalculatorLogic implements IGameLogicUpdatable {

    private float tick = 0;
    private final EntitySet manaEntities;
    private final Map<EntityId, Short> ownerIdsByEntityId = new HashMap<>();
    private final Map<EntityId, Integer> manaGenerationByEntityId = new HashMap<>();
    private final Map<Short, PlayerManaControl> manaControls = new HashMap<>(4);
    private final Map<Short, Integer> manaGains;
    private final Map<Short, Integer> manaLosses;

    public ManaCalculatorLogic(Collection<IPlayerController> playerControllers, EntityData entityData) {
        for (IPlayerController playerController : playerControllers) {
            PlayerManaControl manaControl = playerController.getManaControl();
            if (manaControl != null) {
                manaControls.put(playerController.getKeeper().getId(), manaControl);
            }
        }
        manaGains = new HashMap<>(manaControls.size());
        manaLosses = new HashMap<>(manaControls.size());

        // Listen for mana entities
        manaEntities = entityData.getEntities(Mana.class, Owner.class);
        processAddedEntities(manaEntities);
    }

    @Override
    public void processTick(float tpf, double gameTime) {
        tick += tpf;
        if (tick >= 1) {
            updateManaSources();
            updateManaControls();
            tick -= 1;
        }
    }

    private void updateManaControls() {
        for (Map.Entry<Short, PlayerManaControl> entry : manaControls.entrySet()) {
            entry.getValue().updateMana(manaGains.getOrDefault(entry.getKey(), 0), manaLosses.getOrDefault(entry.getKey(), 0));
        }
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {
        manaEntities.release();
    }

    private void updateManaSources() {
        if (manaEntities.applyChanges()) {

            processAddedEntities(manaEntities.getAddedEntities());

            processDeletedEntities(manaEntities.getRemovedEntities());

            processChangedEntities(manaEntities.getChangedEntities());
        }
    }

    private void processAddedEntities(Set<Entity> entities) {
        for (Entity entity : entities) {
            short ownerId = entity.get(Owner.class).ownerId;
            int manaGeneration = entity.get(Mana.class).manaGeneration;
            ownerIdsByEntityId.put(entity.getId(), ownerId);
            manaGenerationByEntityId.put(entity.getId(), manaGeneration);

            addManaGenerationToPlayer(ownerId, manaGeneration);
        }
    }

    private void processDeletedEntities(Set<Entity> entities) {
        for (Entity entity : entities) {
            short ownerId = ownerIdsByEntityId.remove(entity.getId());
            int manaGeneration = manaGenerationByEntityId.remove(entity.getId());

            removeManaGenerationFromPlayer(ownerId, manaGeneration);
        }
    }

    private void processChangedEntities(Set<Entity> entities) {
        for (Entity entity : entities) {
            short newOwnerId = entity.get(Owner.class).ownerId;
            int newManaGeneration = entity.get(Mana.class).manaGeneration;
            short oldOwnerId = ownerIdsByEntityId.put(entity.getId(), newOwnerId);
            int oldManaGeneration = manaGenerationByEntityId.put(entity.getId(), newManaGeneration);

            if (newOwnerId != oldOwnerId || newManaGeneration != oldManaGeneration) {
                addManaGenerationToPlayer(newOwnerId, newManaGeneration);
                removeManaGenerationFromPlayer(oldOwnerId, oldManaGeneration);
            }
        }
    }

    private void removeManaGenerationFromPlayer(short playerId, int manaGeneration) {
        if (manaGeneration == 0) {
            return;
        }

        // Substract the mana generation from appropriate slot
        if (manaControls.containsKey(playerId)) {
            if (manaGeneration < 0) {
                substractMana(manaLosses, playerId, -manaGeneration);
            } else {
                substractMana(manaGains, playerId, manaGeneration);
            }
        }
    }

    private void addManaGenerationToPlayer(short playerId, int manaGeneration) {
        if (manaGeneration == 0) {
            return;
        }

        // Add the mana generation from appropriate slot
        if (manaControls.containsKey(playerId)) {
            if (manaGeneration < 0) {
                addMana(manaLosses, playerId, -manaGeneration);
            } else {
                addMana(manaGains, playerId, manaGeneration);
            }
        }
    }

    private static void substractMana(Map<Short, Integer> manaValues, short playerId, int manaGeneration) {
        manaValues.merge(playerId, manaGeneration, (t, u) -> {
            return t - u;
        });
    }

    private static void addMana(Map<Short, Integer> manaValues, short playerId, int manaGeneration) {
        manaValues.merge(playerId, manaGeneration, (t, u) -> {
            return t + u;
        });
    }
}
