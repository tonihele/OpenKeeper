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

import com.simsilica.es.Entity;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.EntitySet;
import java.util.Set;
import toniarts.openkeeper.game.component.CreatureAi;
import toniarts.openkeeper.game.component.Frozen;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.game.controller.creature.CreatureState;
import toniarts.openkeeper.game.controller.creature.ICreatureController;

/**
 * Holds frozen creatures in place. A frozen creature drops its task, stops
 * and stands stunned until the freeze runs out, then goes back to being idle.
 */
public final class FreezeSystem implements IGameLogicUpdatable {

    private final EntityData entityData;
    private final ICreaturesController creaturesController;
    private final IGameTimer gameTimer;
    private final EntitySet frozenEntities;

    public FreezeSystem(EntityData entityData, ICreaturesController creaturesController, IGameTimer gameTimer) {
        this.entityData = entityData;
        this.creaturesController = creaturesController;
        this.gameTimer = gameTimer;

        frozenEntities = entityData.getEntities(Frozen.class, CreatureAi.class);
        processAddedEntities(frozenEntities);
    }

    @Override
    public void processTick(float tpf) {
        if (frozenEntities.applyChanges()) {
            processAddedEntities(frozenEntities.getAddedEntities());
        }

        double gameTime = gameTimer.getGameTime();
        for (Entity entity : frozenEntities) {
            Frozen frozen = entity.get(Frozen.class);
            if (gameTime >= frozen.startTime + frozen.duration) {
                thaw(entity.getId());
            }
        }
    }

    private void processAddedEntities(Set<Entity> entities) {
        for (Entity entity : entities) {
            freeze(entity.getId());
        }
    }

    private void freeze(EntityId entityId) {
        ICreatureController creatureController = creaturesController.createController(entityId);
        creatureController.unassingCurrentTask();
        creatureController.stopCreature();
        creatureController.getStateMachine().changeState(CreatureState.STUNNED);
    }

    private void thaw(EntityId entityId) {
        entityData.removeComponent(entityId, Frozen.class);

        // Something else may have happened to the creature meanwhile
        ICreatureController creatureController = creaturesController.createController(entityId);
        if (creatureController.getStateMachine().isInState(CreatureState.STUNNED)) {
            creatureController.getStateMachine().changeState(CreatureState.IDLE);
        }
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {
        frozenEntities.release();
    }

}
