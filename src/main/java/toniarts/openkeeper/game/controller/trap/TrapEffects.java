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
package toniarts.openkeeper.game.controller.trap;

import com.jme3.math.Vector3f;
import com.simsilica.es.Entity;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.EntitySet;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.Predicate;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.controller.entity.EntityController;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Shot;
import toniarts.openkeeper.tools.convert.map.Trap;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * Applies the shots of the traps. A trap hands its own shot data to its shot,
 * so the trap decides how hard the shot hits.<br>
 * The shots hit at once, there are no flying shots or lingering clouds yet:
 * <ul>
 * <li>A shot that hits a thing (Sentry Trap) deals shot data 1 as damage to
 * the trap's target.</li>
 * <li>A shot with constant damage (Spike Trap) deals shot data 1 to the
 * creatures on the trap's tile.</li>
 * <li>A shot with damage from its origin (Fireburst Trap) or a gas shot (Gas
 * Trap) deals shot data 1 to the creatures within shot data 2 tiles.</li>
 * </ul>
 * Area damage spares only the trap owner's creatures.
 */
public final class TrapEffects implements ITrapEffects {

    private static final Logger logger = System.getLogger(TrapEffects.class.getName());

    private final EntityData entityData;
    private final IKwdFile kwdFile;
    private final EntitySet creatureEntities;
    private final Set<Shot.ProcessType> reportedProcessTypes = EnumSet.noneOf(Shot.ProcessType.class);

    public TrapEffects(EntityData entityData, IKwdFile kwdFile) {
        this.entityData = entityData;
        this.kwdFile = kwdFile;

        creatureEntities = entityData.getEntities(CreatureComponent.class, Owner.class, Position.class, Health.class);
    }

    @Override
    public void fire(Trap trap, short ownerId, Vector3f origin, EntityId target) {
        Shot shot = kwdFile.getShotById(trap.getShotTypeId());
        if (shot == null) {
            return;
        }

        switch (shot.getProcessType()) {
            case NONE, GAS ->
                damage(shot, trap, ownerId, origin, target);
            default ->
                reportNotImplemented(shot.getProcessType());
        }
    }

    private void damage(Shot shot, Trap trap, short ownerId, Vector3f origin, EntityId target) {
        switch (shot.getDamageType()) {
            case HIT_THING -> {
                if (target != null) {
                    EntityController.setDamage(entityData, target, trap.getShotData1());
                }
            }
            case CONSTANT -> {
                Point tile = WorldUtils.vectorToPoint(origin);
                damageCreatures(trap.getShotData1(), ownerId,
                        position -> tile.equals(WorldUtils.vectorToPoint(position)));
            }
            case RANGE_FROM_ORIGIN -> {
                float radius = trap.getShotData2();
                damageCreatures(trap.getShotData1(), ownerId,
                        position -> distanceSquared(origin, position) <= radius * radius);
            }
            case NONE -> {

                // Nothing to hurt, the Trigger Trap's shot for one
            }
            default ->
                logger.log(Level.WARNING, "Trap shot damage type {0} not implemented", shot.getDamageType());
        }
    }

    private void damageCreatures(int damage, short ownerId, Predicate<Vector3f> inArea) {
        creatureEntities.applyChanges();
        for (Entity creatureEntity : creatureEntities) {
            if (creatureEntity.get(Owner.class).ownerId != ownerId && inArea.test(creatureEntity.get(Position.class).position)) {
                EntityController.setDamage(entityData, creatureEntity.getId(), damage);
            }
        }
    }

    private static float distanceSquared(Vector3f from, Vector3f to) {
        float dx = to.x - from.x;
        float dz = to.z - from.z;
        return dx * dx + dz * dz;
    }

    private void reportNotImplemented(Shot.ProcessType processType) {

        // Traps fire often, say it once
        if (reportedProcessTypes.add(processType)) {
            logger.log(Level.WARNING, "Trap shot type {0} not implemented", processType);
        }
    }

}
