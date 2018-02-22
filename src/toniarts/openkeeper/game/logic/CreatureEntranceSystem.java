/*
 * Copyright (C) 2014-2018 OpenKeeper
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
import toniarts.openkeeper.game.component.CreatureAi;
import toniarts.openkeeper.game.component.CreatureEntrance;

/**
 * Handles creature entrances. Basically calculates when they end and the assign
 * creature AIs to the poor creatures.
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class CreatureEntranceSystem implements IGameLogicUpdatable {

    private final EntityData entityData;
    private final EntitySet entranceEntities;

    public CreatureEntranceSystem(EntityData entityData) {
        this.entityData = entityData;

        entranceEntities = entityData.getEntities(CreatureEntrance.class);
    }

    @Override
    public void processTick(float tpf, double gameTime) {

        // Add new & remove old
        entranceEntities.applyChanges();

        // Process ticks
        for (Entity entity : entranceEntities) {
            CreatureEntrance creatureEntrance = entity.get(CreatureEntrance.class);
            if (gameTime >= creatureEntrance.started + creatureEntrance.duration) {

                // We'll just remove this and add the AI
                entityData.removeComponent(entity.getId(), CreatureEntrance.class);
                entityData.setComponent(entity.getId(), new CreatureAi());
            }
        }
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {
        entranceEntities.release();
    }

}
