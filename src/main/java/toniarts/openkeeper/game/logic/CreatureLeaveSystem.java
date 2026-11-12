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
import toniarts.openkeeper.game.component.CreatureFall;
import toniarts.openkeeper.game.component.LeavingDungeon;
import toniarts.openkeeper.game.controller.ICreaturesController;

/**
 * Sends the creatures dropped on the middle of their keeper's portal out of the
 * dungeon. Like in DK II, they first fall down and leave when they land.
 */
public final class CreatureLeaveSystem implements IGameLogicUpdatable {

    private final EntityData entityData;
    private final ICreaturesController creaturesController;
    private final EntitySet leavingEntities;

    public CreatureLeaveSystem(EntityData entityData, ICreaturesController creaturesController) {
        this.entityData = entityData;
        this.creaturesController = creaturesController;

        leavingEntities = entityData.getEntities(LeavingDungeon.class);
    }

    @Override
    public void processTick(float tpf) {
        leavingEntities.applyChanges();

        for (Entity entity : leavingEntities) {
            if (entityData.getComponent(entity.getId(), CreatureFall.class) == null) {
                creaturesController.leaveDungeon(entity.getId());
            }
        }
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {
        leavingEntities.release();
    }

}
