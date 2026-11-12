/*
 * Copyright (C) 2014-2016 OpenKeeper
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
package toniarts.openkeeper.game.trigger;

import com.jme3.util.SafeArrayList;
import com.simsilica.es.Entity;
import com.simsilica.es.EntitySet;
import java.util.Map;
import java.util.Set;
import toniarts.openkeeper.game.component.Trigger;
import toniarts.openkeeper.game.controller.IEntityWrapper;
import toniarts.openkeeper.game.controller.entity.IEntityController;
import toniarts.openkeeper.game.logic.IGameLogicUpdatable;

/**
 * A state for handling thing triggers
 *
 * @param <T> the entity controller wrapper
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public abstract class AbstractThingTriggerLogicController<T extends IEntityController> implements IGameLogicUpdatable {

    private final Map<Integer, AbstractThingTriggerControl<T>> thingTriggers;
    private final SafeArrayList<AbstractThingTriggerControl> triggerControls;
    private final EntitySet entities;
    private final IEntityWrapper<T> entityWrapper;

    /**
     * Constructs a new trigger logic controller that handles triggers based on
     * Thing records. Essentially the common nominator is that the thing
     * instances are entities in our system.
     *
     * @param triggers the trigger control instances by trigger ID
     * @param entities this is the entity set that is used to track the actual
     *                 entity instances, they must have a Trigger component set
     * @param entityWrapper the entity wrapper that is going to wrap the entity
     *                      instances to a controller
     */
    public AbstractThingTriggerLogicController(final Map<Integer, AbstractThingTriggerControl<T>> triggers,
            final EntitySet entities, final IEntityWrapper<T> entityWrapper) {
        this.entities = entities;
        this.entityWrapper = entityWrapper;

        // Get all the map thing triggers
        thingTriggers = triggers;
        triggerControls = new SafeArrayList<>(AbstractThingTriggerControl.class, thingTriggers.values());

        // Add existing entities
        processAddedEntities(entities);
    }

    @Override
    public void processTick(float tpf) {

        // Process added entities
        // It is intentional that the old references stay and get replaced possibly by new ones
        if (entities.applyChanges()) {
            processAddedEntities(entities.getAddedEntities());
        }

        // Process triggers
        for (AbstractThingTriggerControl triggerControl : triggerControls.getArray()) {
            triggerControl.update(tpf);
        }
    }

    private void processAddedEntities(Set<Entity> entities) {

        // Each entity we are going to wrap in its controller wrapper
        for (Entity entity : entities) {
            setThing(entity.get(Trigger.class).triggerId, entityWrapper.createController(entity.getId()));
        }
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {

    }

    /**
     * Set a thing instance controller to a thing trigger
     *
     * @param triggerId the trigger ID
     * @param instanceControl the thing instance
     */
    private void setThing(int triggerId, T instanceControl) {
        thingTriggers.get(triggerId).setThing(instanceControl);
    }
}
