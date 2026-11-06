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
import toniarts.openkeeper.game.component.Morphed;
import toniarts.openkeeper.game.controller.IGameTimer;

/**
 * Ends the Chicken spell. The creature's AI notices it is no longer a chicken
 * and goes back to its business
 */
public final class ChickenSpellSystem implements IGameLogicUpdatable {

    private final EntityData entityData;
    private final IGameTimer gameTimer;
    private final EntitySet morphedEntities;

    public ChickenSpellSystem(EntityData entityData, IGameTimer gameTimer) {
        this.entityData = entityData;
        this.gameTimer = gameTimer;

        morphedEntities = entityData.getEntities(Morphed.class);
    }

    @Override
    public void processTick(float tpf) {
        morphedEntities.applyChanges();

        for (Entity entity : morphedEntities) {
            if (entity.get(Morphed.class).endTime <= gameTimer.getGameTime()) {
                entityData.removeComponent(entity.getId(), Morphed.class);
            }
        }
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {
        morphedEntities.release();
    }

}
