/*
 * Copyright (C) 2014-2019 OpenKeeper
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

import com.google.common.base.Objects;
import com.jme3.util.SafeArrayList;
import com.simsilica.es.Entity;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.EntitySet;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import toniarts.openkeeper.game.component.ChickenAi;
import toniarts.openkeeper.game.component.ObjectViewState;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.controller.IObjectsController;
import toniarts.openkeeper.game.controller.chicken.ChickenState;
import toniarts.openkeeper.game.controller.chicken.IChickenController;

/**
 * Handles chicken logic updates, the chicken AI updates that is. The AI is
 * implemented elsewhere for clarity. This class just attaches the AI to the
 * entity having this component and updates it periodically.
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class ChickenAiSystem implements IGameLogicUpdatable {

    private final EntitySet chickenEntities;
    private final EntitySet chickenViewEntities;

    private final SafeArrayList<IChickenController> chickenControllers;
    private final Map<EntityId, IChickenController> chickenControllersByEntityId;
    private final IObjectsController objectsController;

    public ChickenAiSystem(EntityData entityData, IObjectsController objectsController) {
        this.objectsController = objectsController;

        chickenEntities = entityData.getEntities(ChickenAi.class, Position.class);
        chickenViewEntities = entityData.getEntities(ChickenAi.class, ObjectViewState.class);
        chickenControllers = new SafeArrayList<>(IChickenController.class);
        chickenControllersByEntityId = new HashMap<>();
        processAddedEntities(chickenEntities);
    }

    @Override
    public void processTick(float tpf, double gameTime) {

        // Add new & remove old
        if (chickenEntities.applyChanges()) {
            processDeletedEntities(chickenEntities.getRemovedEntities());

            processAddedEntities(chickenEntities.getAddedEntities());
        }

        // Process ticks
        for (IChickenController creatureController : chickenControllers.getArray()) {
            creatureController.processTick(tpf, gameTime);
        }

        // This is shorthand for managing also the view state.... Not sure if smart or not
        if (chickenViewEntities.applyChanges()) {
            processViewEntities(chickenViewEntities.getAddedEntities());
            processViewEntities(chickenViewEntities.getChangedEntities());
        }
    }

    private void processViewEntities(Set<Entity> entities) {
        for (Entity entity : entities) {
            ChickenAi chickenAi = entity.get(ChickenAi.class);
            ObjectViewState objectViewState = entity.get(ObjectViewState.class);
            ObjectViewState.GameObjectAnimState animState = getAnimStateFromAiState(chickenAi.getChickenState());
            if (!Objects.equal(animState, objectViewState.animState)) {
                entity.set(new ObjectViewState(objectViewState.objectId, objectViewState.state, animState, objectViewState.visible));
            }
        }
    }

    private ObjectViewState.GameObjectAnimState getAnimStateFromAiState(ChickenState chickenState) {
        switch (chickenState) {
            case HATCHING_START: {
                return ObjectViewState.GameObjectAnimState.MESH_RESOURCE;
            }
            case HATCHING_END: {
                return ObjectViewState.GameObjectAnimState.ADDITIONAL_RESOURCE_1;
            }
            case PECKING: {
                return ObjectViewState.GameObjectAnimState.ADDITIONAL_RESOURCE_1;
            }
            case WANDERING: {
                return ObjectViewState.GameObjectAnimState.MESH_RESOURCE;
            }
        }

        return null;
    }

    private void processAddedEntities(Set<Entity> entities) {
        for (Entity entity : entities) {
            IChickenController chickenController = objectsController.createChickenController(entity.getId());
            int index = Collections.binarySearch(chickenControllers, chickenController);
            chickenControllers.add(~index, chickenController);
            chickenControllersByEntityId.put(entity.getId(), chickenController);
        }
    }

    private void processDeletedEntities(Set<Entity> entities) {
        for (Entity entity : entities) {
            IChickenController chickenController = chickenControllersByEntityId.remove(entity.getId());
            if (chickenController != null) {
                int index = Collections.binarySearch(chickenControllers, chickenController);
                chickenControllers.remove(index);
                chickenController.getStateMachine().changeState(null);
            }
        }
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {
        chickenEntities.release();
        chickenControllers.clear();
        chickenControllersByEntityId.clear();
        chickenViewEntities.release();
    }

}
