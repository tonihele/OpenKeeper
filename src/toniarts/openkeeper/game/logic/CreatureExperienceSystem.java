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

import com.jme3.util.SafeArrayList;
import com.simsilica.es.Entity;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.EntitySet;
import com.simsilica.es.filter.AndFilter;
import com.simsilica.es.filter.FieldFilter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import toniarts.openkeeper.game.component.CreatureAi;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureExperience;
import toniarts.openkeeper.game.component.TaskComponent;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.creature.CreatureState;
import toniarts.openkeeper.game.task.TaskType;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.KwdFile;
import toniarts.openkeeper.tools.convert.map.Variable;
import toniarts.openkeeper.utils.Utils;

/**
 * Levels up creatures and adds experience points to them when they are doing
 * meaningful tasks<br>
 * This creates some counter etc. that basically needs to be saved then, but I
 * felt wrong to add them to the components, idk
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class CreatureExperienceSystem implements IGameLogicUpdatable {

    private enum ExperienceGainReason {
        NONE,
        WORKING_OR_FIGHTING,
        TRAINING
    }

    private final KwdFile kwdFile;
    private final EntitySet experienceEntities;
    private final EntitySet trainingEntities;
    private final EntityData entityData;
    private final int impExperienceGainPerSecond;
    private final SafeArrayList<EntityId> experienceEntityIds;
    private final SafeArrayList<EntityId> trainingEntityIds;
    private final ICreaturesController creaturesController;
    private final Map<EntityId, Double> timeWorkingByEntityId = new HashMap<>();

    public CreatureExperienceSystem(EntityData entityData, KwdFile kwdFile,
            Map<Variable.MiscVariable.MiscType, Variable.MiscVariable> gameSettings,
            ICreaturesController creaturesController) {
        this.kwdFile = kwdFile;
        this.entityData = entityData;
        this.creaturesController = creaturesController;
        experienceEntityIds = new SafeArrayList<>(EntityId.class);
        trainingEntityIds = new SafeArrayList<>(EntityId.class);

        impExperienceGainPerSecond = (int) gameSettings.get(Variable.MiscVariable.MiscType.IMP_EXPERIENCE_GAIN_PER_SECOND).getValue();

        experienceEntities = entityData.getEntities(CreatureComponent.class, CreatureExperience.class, CreatureAi.class);
        trainingEntities = entityData.getEntities(new AndFilter(TaskComponent.class, new FieldFilter(TaskComponent.class, "taskStarted", true), new FieldFilter(TaskComponent.class, "taskType", TaskType.TRAIN)), CreatureComponent.class, CreatureExperience.class, CreatureAi.class, TaskComponent.class);
        processAddedEntities(experienceEntities, experienceEntityIds);
        processAddedEntities(trainingEntities, trainingEntityIds);
    }

    @Override
    public void processTick(float tpf, double gameTime) {
        if (experienceEntities.applyChanges()) {

            processAddedEntities(experienceEntities.getAddedEntities(), experienceEntityIds);

            processDeletedEntities(experienceEntities.getRemovedEntities(), experienceEntityIds);
        }

        if (trainingEntities.applyChanges()) {

            processAddedEntities(trainingEntities.getAddedEntities(), trainingEntityIds);

            processDeletedEntities(trainingEntities.getRemovedEntities(), trainingEntityIds);
        }

        // Increase the experience level of those who are worthy
        for (EntityId entityId : experienceEntityIds.getArray()) {
            Entity entity = experienceEntities.getEntity(entityId);
            handleEntity(entity, entityId, tpf, () -> {
                return isEntityWorkingOrFighting(entity) ? ExperienceGainReason.WORKING_OR_FIGHTING : ExperienceGainReason.NONE;
            });
        }
        for (EntityId entityId : trainingEntityIds.getArray()) {
            Entity entity = trainingEntities.getEntity(entityId);
            handleEntity(entity, entityId, tpf, () -> {
                return ExperienceGainReason.TRAINING;
            });
        }
    }

    private void handleEntity(Entity entity, EntityId entityId, float tpf, Supplier<ExperienceGainReason> reasonSupplier) {
        CreatureExperience creatureExperience = entity.get(CreatureExperience.class);
        if (creatureExperience.level >= Utils.MAX_CREATURE_LEVEL) {
            return;
        }

        // Check if we can gain exp
        ExperienceGainReason reason = reasonSupplier.get();
        if (reason == ExperienceGainReason.NONE) {
            return;
        }

        double timeWorking = timeWorkingByEntityId.compute(entityId, (k, v) -> {
            if (v == null) {
                return 0.0;
            }
            return v + tpf;
        });

        // Increase the exp
        if (timeWorking < 1) {
            return;
        }

        timeWorkingByEntityId.merge(entityId, -1.0, Double::sum);
        int experience = creatureExperience.experience + getCreatureExperienceGain(entity, reason);

        // See if we gained a level
        if (experience >= creatureExperience.experienceToNextLevel) {
            experience -= creatureExperience.experienceToNextLevel;
            creaturesController.levelUpCreature(entityId, creatureExperience.level + 1, experience);
        } else {
            entityData.setComponent(entityId, new CreatureExperience(creatureExperience.level, experience, creatureExperience.experienceToNextLevel, creatureExperience.experiencePerSecond, creatureExperience.experiencePerSecondTraining));
        }
    }

    private void processAddedEntities(Set<Entity> entities, SafeArrayList<EntityId> entityIds) {
        for (Entity entity : entities) {
            int index = Collections.binarySearch(entityIds, entity.getId());
            entityIds.add(~index, entity.getId());
        }
    }

    private void processDeletedEntities(Set<Entity> entities, SafeArrayList<EntityId> entityIds) {
        for (Entity entity : entities) {
            int index = Collections.binarySearch(entityIds, entity.getId());
            entityIds.remove(index);

            // Logically it can't be on the two lists at the same time
            timeWorkingByEntityId.remove(entity.getId());
        }
    }

    private boolean isEntityWorkingOrFighting(Entity entity) {
        CreatureAi creatureAi = entity.get(CreatureAi.class);
        CreatureState creatureState = creatureAi.getCreatureState();
        return creatureState == CreatureState.MELEE_ATTACK || creatureState == CreatureState.CAST_SPELL || (creatureState == CreatureState.WORK && isWorker(entity));
    }

    private boolean isWorker(Entity entity) {
        CreatureComponent creatureComponent = entity.get(CreatureComponent.class);
        return creatureComponent.worker;
    }

    private int getCreatureExperienceGain(Entity entity, ExperienceGainReason reason) {
        switch (reason) {
            case WORKING_OR_FIGHTING -> {
                CreatureComponent creatureComponent = entity.get(CreatureComponent.class);
                if (kwdFile.getImp().getId() == creatureComponent.creatureId) {
                    return impExperienceGainPerSecond;
                } else {
                    return entity.get(CreatureExperience.class).experiencePerSecond;
                }
            }
            case TRAINING -> {
                Creature creature = kwdFile.getCreature(entity.get(CreatureComponent.class).creatureId);
                Map<Variable.CreatureStats.StatType, Variable.CreatureStats> stats = kwdFile.getCreatureStats(entity.get(CreatureExperience.class).level);

                return stats != null
                        ? stats.get(Variable.CreatureStats.StatType.EXPERIENCE_POINTS_FROM_TRAINING_PER_SECOND).getValue()
                        : creature.getAttributes().getExpPerSecondTraining();
            }
            default -> {
                return 0;
            }
        }
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {
        experienceEntities.release();
        trainingEntities.release();
        timeWorkingByEntityId.clear();
        experienceEntityIds.clear();
        trainingEntityIds.clear();
    }

}
