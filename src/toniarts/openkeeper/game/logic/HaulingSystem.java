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

import com.jme3.math.Vector3f;
import com.simsilica.es.Entity;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntitySet;
import toniarts.openkeeper.game.component.HauledBy;
import toniarts.openkeeper.game.component.Position;

/**
 * Manages hauled entities. Basically moves them as their hauler moves.
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class HaulingSystem implements IGameLogicUpdatable {

    private final EntityData entityData;
    private final EntitySet hauledEntities;

    public HaulingSystem(EntityData entityData) {
        this.entityData = entityData;

        hauledEntities = entityData.getEntities(HauledBy.class, Position.class);
    }

    @Override
    public void processTick(float tpf, double gameTime) {

        // Add new & remove old
        hauledEntities.applyChanges();

        // Process ticks
        for (Entity entity : hauledEntities) {
            HauledBy hauledBy = entity.get(HauledBy.class);
            Position position = entity.get(Position.class);
            Position haulerPosition = entityData.getComponent(hauledBy.entityId, Position.class);
            Vector3f newPos = haulerPosition.position.clone();
            newPos.y = position.position.y;
            entityData.setComponent(entity.getId(), new Position(position.rotation, newPos));
        }
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {
        hauledEntities.release();
    }

}
