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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import toniarts.openkeeper.game.component.CreatureAi;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureExperience;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.creature.CreatureState;
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

    private final KwdFile kwdFile;
    private final EntitySet experienceEntities;
    private final EntityData entityData;
    private final int impExperienceGainPerSecond;
    private final SafeArrayList<EntityId> entityIds;
    private final ICreaturesController creaturesController;
    private final Map<EntityId, Double> timeWorkingByEntityId = new HashMap<>();

    public CreatureExperienceSystem(EntityData entityData, KwdFile kwdFile,
            Map<Variable.MiscVariable.MiscType, Variable.MiscVariable> gameSettings,
            ICreaturesController creaturesController) {
        this.kwdFile = kwdFile;
        this.entityData = entityData;
        this.creaturesController = creaturesController;
        entityIds = new SafeArrayList<>(EntityId.class);

        impExperienceGainPerSecond = (int) gameSettings.get(Variable.MiscVariable.MiscType.IMP_EXPERIENCE_GAIN_PER_SECOND).getValue();

        experienceEntities = entityData.getEntities(CreatureComponent.class, CreatureExperience.class, CreatureAi.class);
        processAddedEntities(experienceEntities);
    }

    @Override
    public void processTick(float tpf, double gameTime) {
        if (experienceEntities.applyChanges()) {

            processAddedEntities(experienceEntities.getAddedEntities());

            processDeletedEntities(experienceEntities.getRemovedEntities());
        }

        // Increase the experience level of those who are worthy
        for (EntityId entityId : entityIds.getArray()) {
            CreatureExperience creatureExperience = entityData.getComponent(entityId, CreatureExperience.class);
            if (creatureExperience.level >= Utils.MAX_CREATURE_LEVEL) {
                continue;
            }

            // Check if we can gain exp
            if (isEntityWorkingOrFighting(entityId)) {
                double timeWorking = timeWorkingByEntityId.compute(entityId, (k, v) -> {
                    if (v == null) {
                        return 0.0;
                    }
                    return v + tpf;
                });

                // Increase the exp
                if (timeWorking >= 1) {
                    timeWorkingByEntityId.merge(entityId, -1.0, Double::sum);

                    CreatureComponent creatureComponent = entityData.getComponent(entityId, CreatureComponent.class);
                    int experience = creatureExperience.experience;
                    if (kwdFile.getImp().getId() == creatureComponent.creatureId) {
                        experience += impExperienceGainPerSecond;
                    } else {
                        experience += creatureExperience.experiencePerSecond;
                    }

                    // See if we gained a level
                    if (experience >= creatureExperience.experienceToNextLevel) {
                        experience -= creatureExperience.experienceToNextLevel;
                        creaturesController.levelUpCreature(entityId, creatureExperience.level + 1, experience);
                    } else {
                        entityData.setComponent(entityId, new CreatureExperience(creatureExperience.level, experience, creatureExperience.experienceToNextLevel, creatureExperience.experiencePerSecond, creatureExperience.experiencePerSecondTraining));
                    }
                }
            }
        }
    }

    private void processAddedEntities(Set<Entity> entities) {
        for (Entity entity : entities) {
            int index = Collections.binarySearch(entityIds, entity.getId());
            entityIds.add(~index, entity.getId());
        }
    }

    private void processDeletedEntities(Set<Entity> entities) {
        for (Entity entity : entities) {
            int index = Collections.binarySearch(entityIds, entity.getId());
            entityIds.remove(index);
            timeWorkingByEntityId.remove(entity.getId());
        }
    }

    private boolean isEntityWorkingOrFighting(EntityId entityId) {
        CreatureAi creatureAi = entityData.getComponent(entityId, CreatureAi.class);
        CreatureState creatureState = creatureAi.getCreatureState();
        return creatureState == CreatureState.MELEE_ATTACK || (creatureState == CreatureState.WORK && isWorker(entityId));
    }

    private boolean isWorker(EntityId entityId) {
        CreatureComponent creatureComponent = entityData.getComponent(entityId, CreatureComponent.class);
        return creatureComponent.worker;
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {
        experienceEntities.release();
        timeWorkingByEntityId.clear();
    }

}
