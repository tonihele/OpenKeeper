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

import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.CreatureAi;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureExperience;
import toniarts.openkeeper.game.component.TaskComponent;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.creature.CreatureState;
import toniarts.openkeeper.game.task.TaskType;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.TrainingVariableFixtures;
import toniarts.openkeeper.tools.convert.map.Variable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Training in the Training Room gives experience on every logic tick, as the
 * original does, and never more than the creature needs for its next level
 */
class CreatureTrainingExperienceTest {

    private static final float TPF = 0.25f;
    private static final int MAX_TRAINING_LEVEL = 4;

    private final EntityData entityData = new DefaultEntityData();
    private final List<int[]> levelUps = new ArrayList<>();
    private CreatureExperienceSystem system;

    @BeforeEach
    void setUp() {
        Map<Variable.MiscVariable.MiscType, Variable.MiscVariable> gameSettings = new EnumMap<>(Variable.MiscVariable.MiscType.class);
        gameSettings.put(Variable.MiscVariable.MiscType.IMP_EXPERIENCE_GAIN_PER_SECOND,
                TrainingVariableFixtures.miscVariable(Variable.MiscVariable.MiscType.IMP_EXPERIENCE_GAIN_PER_SECOND, 2));
        gameSettings.put(Variable.MiscVariable.MiscType.TRAINING_ROOM_MAX_EXPERIENCE_LEVEL,
                TrainingVariableFixtures.miscVariable(Variable.MiscVariable.MiscType.TRAINING_ROOM_MAX_EXPERIENCE_LEVEL, MAX_TRAINING_LEVEL));

        // The training gain comes from the creature's own experience component
        IKwdFile kwdFile = (IKwdFile) Proxy.newProxyInstance(IKwdFile.class.getClassLoader(), new Class<?>[]{IKwdFile.class}, (proxy, method, args) -> {
            throw new UnsupportedOperationException(method.getName());
        });
        ICreaturesController creaturesController = (ICreaturesController) Proxy.newProxyInstance(ICreaturesController.class.getClassLoader(), new Class<?>[]{ICreaturesController.class}, (proxy, method, args) -> {
            if ("levelUpCreature".equals(method.getName())) {
                levelUps.add(new int[]{(int) ((EntityId) args[0]).getId(), (int) args[1], (int) args[2]});
                return null;
            }
            throw new UnsupportedOperationException(method.getName());
        });
        system = new CreatureExperienceSystem(entityData, kwdFile, gameSettings, creaturesController);
    }

    @AfterEach
    void tearDown() {
        system.stop();
    }

    @Test
    void trainingGivesTheTrainingExperienceOnEveryTick() {
        EntityId warlock = trainee(1, 0, 1000, 30);

        system.processTick(TPF);
        assertEquals(30, experience(warlock));

        for (int i = 0; i < 3; i++) {
            system.processTick(TPF);
        }
        assertEquals(120, experience(warlock));
        assertTrue(levelUps.isEmpty());
    }

    @Test
    void trainingUsesTheLevelScaledTrainingExperience() {
        EntityId warlock = trainee(3, 100, 2000, 45);

        system.processTick(TPF);

        assertEquals(145, experience(warlock));
    }

    @Test
    void trainingGivesNoMoreThanTheNextLevelNeeds() {
        EntityId warlock = trainee(2, 990, 1000, 30);

        system.processTick(TPF);

        assertEquals(1, levelUps.size());
        assertEquals(warlock.getId(), levelUps.get(0)[0]);
        assertEquals(3, levelUps.get(0)[1]);
        assertEquals(0, levelUps.get(0)[2]);
    }

    @Test
    void trainingGivesNothingAtTheTrainingRoomMaximumLevel() {
        EntityId warlock = trainee(MAX_TRAINING_LEVEL, 100, 2000, 45);

        for (int i = 0; i < 8; i++) {
            system.processTick(TPF);
        }

        assertEquals(100, experience(warlock));
        assertTrue(levelUps.isEmpty());
    }

    @Test
    void creatureThatHasNotStartedTrainingGetsNothing() {
        EntityId warlock = trainee(1, 0, 1000, 30);
        TaskComponent task = entityData.getComponent(warlock, TaskComponent.class);
        entityData.setComponent(warlock, new TaskComponent(task.taskId, task.targetEntity, task.targetLocation, task.taskType, task.taskDuration, false));

        system.processTick(TPF);
        system.processTick(TPF);

        assertEquals(0, experience(warlock));
    }

    private EntityId trainee(int level, int experience, int experienceToNextLevel, int experiencePerSecondTraining) {
        EntityId entityId = entityData.createEntity();
        entityData.setComponents(entityId,
                new CreatureComponent(),
                new CreatureExperience(level, experience, experienceToNextLevel, 5, experiencePerSecondTraining),
                new CreatureAi(0, CreatureState.WORK, (short) 1),
                new TaskComponent(1, null, null, TaskType.TRAIN, 0, true));
        return entityId;
    }

    private int experience(EntityId entityId) {
        return entityData.getComponent(entityId, CreatureExperience.class).experience;
    }

}
