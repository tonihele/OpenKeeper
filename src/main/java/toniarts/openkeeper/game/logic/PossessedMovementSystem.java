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
import java.util.Objects;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.Possessed;
import toniarts.openkeeper.game.component.PossessedMovement;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.IMapController;

/**
 * Applies possessed creature input on the authoritative simulation tick.
 * Possessed movement shares the map sweep used by AI path following so both
 * control modes stop and slide at the same tile boundaries.
 */
public final class PossessedMovementSystem implements IGameLogicUpdatable {

    private final EntityData entityData;
    private final ICreaturesController creaturesController;
    private final GridMovementCollision collision;
    private final EntitySet possessedEntities;

    public PossessedMovementSystem(EntityData entityData, IMapController mapController,
            IEntityPositionLookup entityPositionLookup, ICreaturesController creaturesController) {
        this(entityData, creaturesController,
                new GridMovementCollision(mapController, entityPositionLookup, MovementSettings.DEFAULT));
    }

    public PossessedMovementSystem(EntityData entityData, ICreaturesController creaturesController,
            GridMovementCollision collision) {
        this.entityData = Objects.requireNonNull(entityData, "entityData");
        this.creaturesController = creaturesController;
        this.collision = Objects.requireNonNull(collision, "collision");
        possessedEntities = entityData.getEntities(Possessed.class, PossessedMovement.class, Position.class);
    }

    @Override
    public void processTick(float tpf) {
        if (!Float.isFinite(tpf) || tpf <= 0f) {
            return;
        }
        possessedEntities.applyChanges();

        for (Entity entity : possessedEntities) {
            PossessedMovement movement = entity.get(PossessedMovement.class);
            Position position = entity.get(Position.class);
            Vector3f next = position.position.clone();
            Vector2f direction = movement.direction;
            float directionLengthSquared = direction != null
                    ? direction.x * direction.x + direction.y * direction.y : 0f;
            if (direction != null && Float.isFinite(directionLengthSquared) && directionLengthSquared > 0f
                    && Float.isFinite(movement.speed) && movement.speed > 0f) {
                float directionScale = directionLengthSquared > 1f
                        ? (float) (1d / Math.sqrt(directionLengthSquared)) : 1f;
                float deltaX = direction.x * directionScale * movement.speed * tpf;
                float deltaZ = direction.y * directionScale * movement.speed * tpf;
                collision.move(creaturesController.createController(entity.getId()),
                        position.position.x, position.position.z, deltaX, deltaZ, next);
            }

            float rotation = Float.isFinite(movement.rotation) ? movement.rotation : position.rotation;
            if (!next.equals(position.position) || rotation != position.rotation) {
                entityData.setComponent(entity.getId(), new Position(rotation, next));
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
}
