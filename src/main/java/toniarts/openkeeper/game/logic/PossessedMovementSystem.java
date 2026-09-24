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
import com.jme3.math.Vector3f;
import com.simsilica.es.Entity;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntitySet;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.Possessed;
import toniarts.openkeeper.game.component.PossessedMovement;
import toniarts.openkeeper.game.map.IMapInformation;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * Moves possessed creatures according to the movement intent sent by their
 * keeper. Replaces the steering of {@link MovementSystem}, which does not
 * handle possessed creatures since they have no navigation.
 */
public final class PossessedMovementSystem implements IGameLogicUpdatable {

    private final EntityData entityData;
    private final IMapInformation mapInformation;
    private final EntitySet possessedEntities;

    public PossessedMovementSystem(EntityData entityData, IMapInformation mapInformation) {
        this.entityData = entityData;
        this.mapInformation = mapInformation;
        possessedEntities = entityData.getEntities(Possessed.class, PossessedMovement.class, Position.class);
    }

    @Override
    public void processTick(float tpf) {
        possessedEntities.applyChanges();

        for (Entity entity : possessedEntities) {
            PossessedMovement movement = entity.get(PossessedMovement.class);
            Position position = entity.get(Position.class);
            Vector3f newPosition = position.position.clone();
            if (!movement.direction.equals(Vector2f.ZERO)) {
                float x = newPosition.x + movement.direction.x * movement.speed * tpf;
                float z = newPosition.z + movement.direction.y * movement.speed * tpf;

                // Slide along walls by trying the axes separately
                if (isPassable(x, newPosition.z)) {
                    newPosition.x = x;
                }
                if (isPassable(newPosition.x, z)) {
                    newPosition.z = z;
                }
            }

            if (!newPosition.equals(position.position) || movement.rotation != position.rotation) {
                entityData.setComponent(entity.getId(), new Position(movement.rotation, newPosition));
            }
        }
    }

    private boolean isPassable(float x, float z) {
        Point p = WorldUtils.vectorToPoint(x, z);

        // TODO: doors, water & lava restrictions of the creature
        return mapInformation.getMapData().getTile(p) != null && !mapInformation.isSolid(p);
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {
        possessedEntities.release();
    }

}
