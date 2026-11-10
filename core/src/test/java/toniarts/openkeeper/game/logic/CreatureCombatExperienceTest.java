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
import java.util.EnumMap;
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
import toniarts.openkeeper.tools.convert.map.CombatExperienceFixtures;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Variable;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Creatures learn from fighting, except a keeper's workers. In DK II an Imp
 * gains nothing in battle, while a hero Dwarf, also a worker, does
 */
class CreatureCombatExperienceTest {

    private static final float TPF = 0.25f;
    private static final int TEN_SECONDS = 40;
    private static final short IMP = 1;
    private static final short GOBLIN = 4;
    private static final short DWARF = 14;
    private static final int IMP_EXPERIENCE_GAIN_PER_SECOND = 30;
    private static final int EXPERIENCE_PER_SECOND = 200;

    private final EntityData entityData = new DefaultEntityData();
    private final Map<Short, Creature> creatures = Map.of(
            IMP, CombatExperienceFixtures.creature(IMP, Creature.CreatureFlag.IS_WORKER),
            GOBLIN, CombatExperienceFixtures.creature(GOBLIN),
            DWARF, CombatExperienceFixtures.creature(DWARF, Creature.CreatureFlag.IS_WORKER, Creature.CreatureFlag.IS_GOOD));
    private CreatureExperienceSystem system;

    @BeforeEach
    void setUp() {
        Map<Variable.MiscVariable.MiscType, Variable.MiscVariable> gameSettings = new EnumMap<>(Variable.MiscVariable.MiscType.class);
        gameSettings.put(Variable.MiscVariable.MiscType.IMP_EXPERIENCE_GAIN_PER_SECOND,
                CombatExperienceFixtures.miscVariable(Variable.MiscVariable.MiscType.IMP_EXPERIENCE_GAIN_PER_SECOND, IMP_EXPERIENCE_GAIN_PER_SECOND));

        IKwdFile kwdFile = (IKwdFile) Proxy.newProxyInstance(IKwdFile.class.getClassLoader(), new Class<?>[]{IKwdFile.class}, (proxy, method, args) -> switch (method.getName()) {
            case "getImp" -> creatures.get(IMP);
            case "getCreature" -> creatures.get((Short) args[0]);
            default -> throw new UnsupportedOperationException(method.getName());
        });
        ICreaturesController creaturesController = (ICreaturesController) Proxy.newProxyInstance(ICreaturesController.class.getClassLoader(), new Class<?>[]{ICreaturesController.class}, (proxy, method, args) -> {
            throw new UnsupportedOperationException(method.getName());
        });
        system = new CreatureExperienceSystem(entityData, kwdFile, gameSettings, creaturesController);
    }

    @AfterEach
    void tearDown() {
        system.stop();
    }

    @Test
    void impFightingGainsNothing() {
        EntityId imp = creature(IMP, true, CreatureState.MELEE_ATTACK);

        tenSeconds();

        assertEquals(0, experience(imp));
    }

    @Test
    void impWorkingGainsTheImpExperience() {
        EntityId imp = creature(IMP, true, CreatureState.WORK);

        tenSeconds();

        assertEquals(9 * IMP_EXPERIENCE_GAIN_PER_SECOND, experience(imp));
    }

    @Test
    void dwarfFightingGainsItsExperiencePerSecond() {
        EntityId dwarf = creature(DWARF, true, CreatureState.MELEE_ATTACK);

        tenSeconds();

        assertEquals(9 * EXPERIENCE_PER_SECOND, experience(dwarf));
    }

    @Test
    void goblinFightingGainsItsExperiencePerSecond() {
        EntityId goblin = creature(GOBLIN, false, CreatureState.MELEE_ATTACK);

        tenSeconds();

        assertEquals(9 * EXPERIENCE_PER_SECOND, experience(goblin));
    }

    /**
     * The first tick only starts the clock, so ten seconds give experience
     * nine times
     */
    private void tenSeconds() {
        for (int i = 0; i < TEN_SECONDS; i++) {
            system.processTick(TPF);
        }
    }

    private EntityId creature(short creatureId, boolean worker, CreatureState state) {
        EntityId entityId = entityData.createEntity();
        CreatureComponent creatureComponent = new CreatureComponent();
        creatureComponent.creatureId = creatureId;
        creatureComponent.worker = worker;
        entityData.setComponents(entityId,
                creatureComponent,
                new CreatureExperience(1, 0, 21600, EXPERIENCE_PER_SECOND, 40),
                new CreatureAi(0, state, creatureId));
        if (state == CreatureState.WORK) {
            entityData.setComponent(entityId, new TaskComponent(1, null, null, TaskType.CLAIM_TILE, 0, true));
        }
        return entityId;
    }

    private int experience(EntityId entityId) {
        return entityData.getComponent(entityId, CreatureExperience.class).experience;
    }

}
