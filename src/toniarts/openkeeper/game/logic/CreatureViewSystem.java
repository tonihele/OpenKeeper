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
import java.util.Set;
import toniarts.openkeeper.game.component.CreatureAi;
import toniarts.openkeeper.game.component.CreatureViewState;
import toniarts.openkeeper.game.component.Navigation;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.tools.convert.map.Creature;

/**
 * Handles creature animation states. These are based on the creature states and
 * such. I'm not entirely sure is this the way this would work but hey...
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class CreatureViewSystem implements IGameLogicUpdatable {

    private final EntityData entityData;
    private final EntitySet creatureViewEntities;

    // TODO: The creature shouldn't be able to access the world like this, needs breaking up to pieces
    private final SafeArrayList<EntityId> creatureEntities = new SafeArrayList<>(EntityId.class);

    public CreatureViewSystem(EntityData entityData) {
        this.entityData = entityData;

        creatureViewEntities = entityData.getEntities(CreatureViewState.class, Position.class);
        processAddedEntities(creatureViewEntities);
    }

    @Override
    public void processTick(float tpf, double gameTime) {

        // Add new & remove old
        if (creatureViewEntities.applyChanges()) {

            processAddedEntities(creatureViewEntities.getAddedEntities());

            processDeletedEntities(creatureViewEntities.getRemovedEntities());
        }

        // Process ticks
        for (EntityId entityId : creatureEntities.getArray()) {

            // Determine what animation to show
            CreatureViewState state = entityData.getComponent(entityId, CreatureViewState.class);
            Creature.AnimationType currentState = state.state;
            Creature.AnimationType targetState = currentState;
            if (entityData.getComponent(entityId, Navigation.class) != null) {
                targetState = Creature.AnimationType.WALK;
            } else {
                CreatureAi aiState = entityData.getComponent(entityId, CreatureAi.class);
                if (aiState != null) {
                    switch (aiState.creatureState) {
                        case IDLE:
                            targetState = Creature.AnimationType.IDLE_1;
                            break;
                        case STUNNED:
                            targetState = Creature.AnimationType.STUNNED;
                            break;
                        case FALLEN:
                            targetState = Creature.AnimationType.FALLBACK;
                            break;
                        case GETTING_UP:
                            targetState = Creature.AnimationType.GET_UP;
                            break;
                        case ENTERING_DUNGEON:
                            targetState = Creature.AnimationType.ENTRANCE;
                            break;
                    }
                }
            }

            // Change!
            if (currentState != targetState) {
                entityData.setComponent(entityId, new CreatureViewState(state.creatureId, gameTime, targetState));
            }
        }
    }

    private void processAddedEntities(Set<Entity> entities) {
        for (Entity entity : entities) {
            int index = Collections.binarySearch(creatureEntities, entity.getId());
            creatureEntities.add(~index, entity.getId());
        }
    }

    private void processDeletedEntities(Set<Entity> entities) {
        for (Entity entity : entities) {
            int index = Collections.binarySearch(creatureEntities, entity.getId());
            creatureEntities.remove(index);
        }
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {
        creatureViewEntities.release();
        creatureEntities.clear();
    }

}
