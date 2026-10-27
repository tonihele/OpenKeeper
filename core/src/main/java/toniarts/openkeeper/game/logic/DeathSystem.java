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

import com.jme3.util.SafeArrayList;
import com.simsilica.es.Entity;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.EntitySet;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import toniarts.openkeeper.game.component.Death;
import toniarts.openkeeper.tools.convert.map.Variable;
import toniarts.openkeeper.utils.GameTimeCounter;

/**
 * The waste disposal class, removes entities after reasonable amount of time has passed (i.e. death
 * animations or corpse decays have passed). Also handles passing other posessions related to the entity,
 * maybe some other universal remove listener would be better in the long run...
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class DeathSystem extends GameTimeCounter {

    private final IEntityPositionLookup entityPositionLookup;
    private final EntitySet deathEntities;
    private final EntityData entityData;
    private final SafeArrayList<EntityId> entityIds;
    private final int timeToDecay;

    public DeathSystem(EntityData entityData, Map<Variable.MiscVariable.MiscType, Variable.MiscVariable> gameSettings,
            IEntityPositionLookup entityPositionLookup) {
        this.entityPositionLookup = entityPositionLookup;
        this.entityData = entityData;
        entityIds = new SafeArrayList<>(EntityId.class);

        timeToDecay = (int) gameSettings.get(Variable.MiscVariable.MiscType.DEAD_BODY_DIES_AFTER_SECONDS).getValue();

        deathEntities = entityData.getEntities(Death.class);
        processAddedEntities(deathEntities);
    }

    @Override
    public void processTick(float tpf) {
        super.processTick(tpf);

        if (deathEntities.applyChanges()) {
            processDeletedEntities(deathEntities.getRemovedEntities());
            processAddedEntities(deathEntities.getAddedEntities());
        }

        // Decay stuff
        for (EntityId entityId : entityIds.getArray()) {
            Death death = deathEntities.getEntity(entityId).get(Death.class);
            if (timeElapsed - death.startTime >= timeToDecay) {
                entityData.removeEntity(entityId);
            }
        }
    }

    private void processAddedEntities(Set<Entity> entities) {
        for (Entity entity : entities) {
            int index = Collections.binarySearch(entityIds, entity.getId());
            entityIds.add(~index, entity.getId());
            entityPositionLookup.getEntityController(entity.getId()).removePossession();
        }
    }

    private void processDeletedEntities(Set<Entity> entities) {
        for (Entity entity : entities) {
            int index = Collections.binarySearch(entityIds, entity.getId());
            entityIds.remove(index);
        }
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {
        deathEntities.release();
        entityIds.clear();
    }

}
