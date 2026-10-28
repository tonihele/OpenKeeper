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
import com.simsilica.es.EntityId;
import com.simsilica.es.EntitySet;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Possessed;
import toniarts.openkeeper.game.component.PossessedAttack;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.IGameController;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.player.PlayerManaControl;
import toniarts.openkeeper.utils.GameTimeCounter;

/**
 * Maintains possessed state, sees when we need to stop possessing. Also swings
 * the attacks the keepers give to their possessed creatures, so that they hit
 * on the game logic thread.
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class PossessedSystem extends GameTimeCounter {

    private final EntitySet possessedEntities;
    private final EntitySet attackingEntities;
    private final Map<Short, PlayerManaControl> manaControls;
    private final Map<EntityId, Short> possessorsByEntity = new HashMap<>();
    private final EntityData entityData;
    private final IGameController gameController;
    private final ICreaturesController creaturesController;

    public PossessedSystem(Collection<IPlayerController> playerControllers, EntityData entityData, IGameController gameController,
            ICreaturesController creaturesController) {
        this.gameController = gameController;
        this.creaturesController = creaturesController;
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
        attackingEntities = entityData.getEntities(Possessed.class, PossessedAttack.class);
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
            if (timeElapsed - possessed.manaCheckTime < 1) {
                continue;
            }

            PlayerManaControl manaControl = manaControls.get(owner.ownerId);
            if (manaControl == null || !manaControl.hasEnoughMana(possessed.manaDrain)) {
                creaturesController.createController(entity.getId()).setPossession(false);
            } else {
                entityData.setComponent(entity.getId(), new Possessed(possessed.manaDrain, timeElapsed));
            }
        }

        processAttacks();
    }

    private void processAttacks() {
        attackingEntities.applyChanges();
        for (Entity entity : attackingEntities) {
            creaturesController.createController(entity.getId()).executePossessedAttack();

            // A released attack only had this one swing
            // The session thread may have written a new press meanwhile, so only remove the very same released press
            PossessedAttack attack = entity.get(PossessedAttack.class);
            PossessedAttack live = entityData.getComponent(entity.getId(), PossessedAttack.class);
            if (!attack.held && live == attack) {
                entityData.removeComponent(entity.getId(), PossessedAttack.class);
            }
        }
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {
        possessedEntities.release();
        attackingEntities.release();
    }

    private void processAddedEntities(Set<Entity> entities) {
        for (Entity entity : entities) {
            short ownerId = entity.get(Owner.class).ownerId;
            possessorsByEntity.put(entity.getId(), ownerId);
            gameController.setPossession(entity.getId(), ownerId);
        }
    }

    private void processDeletedEntities(Set<Entity> entities) {
        for (Entity entity : entities) {
            // A removed entity has lost its components (e.g. the creature died
            // while possessed), so the owner is remembered from when it was added
            Short ownerId = possessorsByEntity.remove(entity.getId());
            if (ownerId != null) {
                gameController.setPossession(null, ownerId);
            }
        }
    }
}
