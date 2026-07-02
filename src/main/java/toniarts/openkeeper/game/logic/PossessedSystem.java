/*
 * Copyright (C) 2014-2024 OpenKeeper
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
import com.simsilica.es.EntitySet;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Possessed;
import toniarts.openkeeper.game.controller.IGameController;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.player.PlayerManaControl;
import toniarts.openkeeper.utils.GameTimeCounter;

/**
 * Maintains possessed state, sees when we need to stop possessing
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class PossessedSystem extends GameTimeCounter {

    private final EntitySet possessedEntities;
    private final Map<Short, PlayerManaControl> manaControls;
    private final EntityData entityData;
    private final IGameController gameController;

    public PossessedSystem(Collection<IPlayerController> playerControllers, EntityData entityData, IGameController gameController) {
        this.gameController = gameController;
        this.entityData = entityData;
        manaControls = HashMap.newHashMap(playerControllers.size());
        for (IPlayerController playerController : playerControllers) {
            PlayerManaControl manaControl = playerController.getManaControl();
            if (manaControl != null) {
                manaControls.put(playerController.getKeeper().getId(), manaControl);
            }
        }

        // Listen for posessed entities
        possessedEntities = entityData.getEntities(Possessed.class, Owner.class);
        processAddedEntities(possessedEntities);
    }

    @Override
    public void processTick(float tpf) {
        super.processTick(tpf);

        if (possessedEntities.applyChanges()) {
            processAddedEntities(possessedEntities.getAddedEntities());
            processDeletedEntities(possessedEntities.getRemovedEntities());
        }

        for (Entity entity : possessedEntities) {

            // See if the player is running out of mana
            Possessed possessed = entity.get(Possessed.class);
            Owner owner = entity.get(Owner.class);
            if (possessed.manaCheckTime + 1 < timeElapsed) {
                continue;
            }

            if (!manaControls.get(owner.ownerId).hasEnoughMana(possessed.manaDrain)) {
                entityData.removeComponent(entity.getId(), Possessed.class);
            } else {
                entityData.setComponent(entity.getId(), new Possessed(possessed.manaDrain, timeElapsed));
            }
        }
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {
        possessedEntities.release();
    }

    private void processAddedEntities(Set<Entity> entities) {
        for (Entity entity : entities) {
            gameController.setPossession(entity.getId(), entity.get(Owner.class).ownerId);
        }
    }

    private void processDeletedEntities(Set<Entity> entities) {
        for (Entity entity : entities) {
            gameController.setPossession(null, entity.get(Owner.class).ownerId);
        }
    }
}
