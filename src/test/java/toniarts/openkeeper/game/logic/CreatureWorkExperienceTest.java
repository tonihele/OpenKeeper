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
import toniarts.openkeeper.tools.convert.map.PassiveExperienceFixtures;
import toniarts.openkeeper.tools.convert.map.Variable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Workers learn from the jobs they do, not from going somewhere. A hero Dwarf
 * sent to an action point, or marching on a keeper, stays in the work state
 * the whole time, but in DK II it gains nothing for it
 */
class CreatureWorkExperienceTest {

    private static final float TPF = 0.25f;
    private static final int TEN_SECONDS = 40;
    private static final short IMP = 1;
    private static final short DWARF = 14;
    private static final int IMP_EXPERIENCE_GAIN_PER_SECOND = 30;
    private static final int DWARF_EXPERIENCE_PER_SECOND = 200;

    private final EntityData entityData = new DefaultEntityData();
    private final List<int[]> levelUps = new ArrayList<>();
    private CreatureExperienceSystem system;

    @BeforeEach
    void setUp() {
        Map<Variable.MiscVariable.MiscType, Variable.MiscVariable> gameSettings = new EnumMap<>(Variable.MiscVariable.MiscType.class);
        gameSettings.put(Variable.MiscVariable.MiscType.IMP_EXPERIENCE_GAIN_PER_SECOND,
                PassiveExperienceFixtures.miscVariable(Variable.MiscVariable.MiscType.IMP_EXPERIENCE_GAIN_PER_SECOND, IMP_EXPERIENCE_GAIN_PER_SECOND));

        IKwdFile kwdFile = (IKwdFile) Proxy.newProxyInstance(IKwdFile.class.getClassLoader(), new Class<?>[]{IKwdFile.class}, (proxy, method, args) -> switch (method.getName()) {
            case "getImp" -> PassiveExperienceFixtures.creature(IMP);
            default -> throw new UnsupportedOperationException(method.getName());
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
    void dwarfWaitingAtAnActionPointGainsNothing() {
        EntityId dwarf = worker(DWARF, TaskType.GO_TO_LOCATION);

        tenSeconds();

        assertEquals(0, experience(dwarf));
        assertTrue(levelUps.isEmpty());
    }

    @Test
    void dwarfMarchingOnAKeeperGainsNothing() {
        EntityId dwarf = worker(DWARF, TaskType.KILL_PLAYER);

        tenSeconds();

        assertEquals(0, experience(dwarf));
    }

    @Test
    void dwarfDiggingItsWayGainsItsExperiencePerSecond() {
        EntityId dwarf = worker(DWARF, TaskType.DIG_TILE);

        tenSeconds();

        assertEquals(9 * DWARF_EXPERIENCE_PER_SECOND, experience(dwarf));
    }

    @Test
    void impClaimingGainsTheImpExperience() {
        EntityId imp = worker(IMP, TaskType.CLAIM_TILE);

        tenSeconds();

        assertEquals(9 * IMP_EXPERIENCE_GAIN_PER_SECOND, experience(imp));
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

    private EntityId worker(short creatureId, TaskType taskType) {
        EntityId entityId = entityData.createEntity();
        CreatureComponent creatureComponent = new CreatureComponent();
        creatureComponent.creatureId = creatureId;
        creatureComponent.worker = true;
        entityData.setComponents(entityId,
                creatureComponent,
                new CreatureExperience(8, 0, 21600, DWARF_EXPERIENCE_PER_SECOND, 40),
                new CreatureAi(0, CreatureState.WORK, creatureId),
                new TaskComponent(1, null, null, taskType, 0, true));
        return entityId;
    }

    private int experience(EntityId entityId) {
        return entityData.getComponent(entityId, CreatureExperience.class).experience;
    }

}
