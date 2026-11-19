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

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.simsilica.es.Entity;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntitySet;
import toniarts.openkeeper.game.component.CreatureFall;
import toniarts.openkeeper.game.component.HauledBy;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.PossessedAbility;
import toniarts.openkeeper.game.component.TaskComponent;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * Manages hauled entities. Basically moves them as their hauler moves. Also
 * holds the worker a possessed creature carries.
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class HaulingSystem implements IGameLogicUpdatable {

    /**
     * A carried worker is held half a tile in front of the creature (0x4831D0)
     */
    private static final float CARRY_DISTANCE = WorldUtils.TILE_WIDTH / 2;

    /**
     * A thrown or dropped worker takes this much damage as it lands (0x4833C0)
     */
    private static final int THROWN_WORKER_DAMAGE = 50;

    private final EntityData entityData;
    private final ICreaturesController creaturesController;
    private final EntitySet hauledEntities;
    private final EntitySet carryingEntities;

    public HaulingSystem(EntityData entityData, ICreaturesController creaturesController) {
        this.entityData = entityData;
        this.creaturesController = creaturesController;

        hauledEntities = entityData.getEntities(HauledBy.class, Position.class);
        carryingEntities = entityData.getEntities(PossessedAbility.class, Position.class);
    }

    @Override
    public void processTick(float tpf) {

        // Add new & remove old
        hauledEntities.applyChanges();

        // Process ticks
        for (Entity entity : hauledEntities) {
            HauledBy hauledBy = entity.get(HauledBy.class);
            Position position = entity.get(Position.class);
            Position haulerPosition = entityData.getComponent(hauledBy.entityId, Position.class);

            // A hauler that has left its job, like an Imp that runs away, puts it down. Held on to, a knocked out
            // creature would never bleed out and nobody would pick it up again
            if (haulerPosition == null || entityData.getComponent(hauledBy.entityId, TaskComponent.class) == null) {
                entityData.removeComponent(entity.getId(), HauledBy.class);
                continue;
            }

            Vector3f newPos = haulerPosition.position.clone();
            newPos.y = position.position.y;
            entityData.setComponent(entity.getId(), new Position(position.rotation, newPos));
        }

        carryingEntities.applyChanges();
        for (Entity entity : carryingEntities) {
            holdCarriedWorker(entity);
        }
    }

    /**
     * Like DK II, the carried worker is put in front of the creature, at the
     * creature's eye height, on every turn (0x4831D0). A worker the keeper
     * has thrown, or one whose carrier is no longer possessed, is let go
     * instead, from where it was last held (0x4831D0)
     */
    private void holdCarriedWorker(Entity entity) {

        // The keeper's throw comes from the session thread, read it as it is now
        PossessedAbility possessedAbility = entityData.getComponent(entity.getId(), PossessedAbility.class);
        if (possessedAbility == null || possessedAbility.entity == null) {
            return;
        }
        boolean workerOnMap = entityData.getComponent(possessedAbility.entity, Position.class) != null;
        if (possessedAbility.releaseVelocity != null) {
            entityData.removeComponent(entity.getId(), PossessedAbility.class);

            // The keeper's hand may have taken it, or it has died
            if (workerOnMap) {
                entityData.setComponent(possessedAbility.entity, new CreatureFall(possessedAbility.releaseVelocity, THROWN_WORKER_DAMAGE));
            }
            return;
        }
        if (!workerOnMap) {
            return;
        }

        Position position = entity.get(Position.class);
        float eyeHeight = creaturesController.createController(entity.getId()).getCreature().getAttributes().getEyeHeight();
        Vector3f carryPosition = position.position.add(FastMath.sin(position.rotation) * CARRY_DISTANCE, eyeHeight,
                FastMath.cos(position.rotation) * CARRY_DISTANCE);
        entityData.setComponent(possessedAbility.entity, new Position(position.rotation, carryPosition));
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {
        hauledEntities.release();
        carryingEntities.release();
    }

}
