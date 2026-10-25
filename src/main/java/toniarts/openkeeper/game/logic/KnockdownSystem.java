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
import com.simsilica.es.EntitySet;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.KnockedDown;
import toniarts.openkeeper.game.controller.ICreaturesController;

/**
 * Knocks down creatures tagged with {@link KnockedDown}. Spells and such tag
 * the creature from whatever thread they run on, the creature's state machine
 * is only touched here, on the game logic thread, next to the creature AI.
 */
public final class KnockdownSystem implements IGameLogicUpdatable {

    private final EntityData entityData;
    private final ICreaturesController creaturesController;
    private final EntitySet knockedDownEntities;

    public KnockdownSystem(EntityData entityData, ICreaturesController creaturesController) {
        this.entityData = entityData;
        this.creaturesController = creaturesController;

        knockedDownEntities = entityData.getEntities(KnockedDown.class, CreatureComponent.class);
    }

    @Override
    public void processTick(float tpf) {
        knockedDownEntities.applyChanges();

        // Several hits within the same tick are one knock down
        for (Entity entity : knockedDownEntities) {
            entityData.removeComponent(entity.getId(), KnockedDown.class);
            creaturesController.createController(entity.getId()).knockDown();
        }
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {
        knockedDownEntities.release();
    }

}
