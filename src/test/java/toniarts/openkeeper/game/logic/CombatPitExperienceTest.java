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
import java.util.Map;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.CombatPitFighter;
import toniarts.openkeeper.game.component.CreatureAi;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureExperience;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.creature.CreatureState;
import toniarts.openkeeper.tools.convert.map.CombatPitFixtures;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable.MiscType;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CombatPitExperienceTest {

    private static final short FIGHTER = 1;
    private static final short IMP = 2;
    private static final int MAX_PIT_LEVEL = 8;

    @Test
    void pitFighterBelowTheTopPitLevelLearns() {
        assertEquals(10, getExperienceAfterASecondOfFighting(MAX_PIT_LEVEL - 1, true));
    }

    @Test
    void pitFighterAtTheTopPitLevelLearnsNoMore() {
        assertEquals(0, getExperienceAfterASecondOfFighting(MAX_PIT_LEVEL, true));
    }

    @Test
    void fighterOutsideThePitStillLearnsAtThatLevel() {
        assertEquals(10, getExperienceAfterASecondOfFighting(MAX_PIT_LEVEL, false));
    }

    private static int getExperienceAfterASecondOfFighting(int level, boolean inPit) {
        EntityData entityData = new DefaultEntityData();
        EntityId entity = entityData.createEntity();
        CreatureComponent creatureComponent = new CreatureComponent();
        creatureComponent.creatureId = FIGHTER;
        entityData.setComponents(entity, creatureComponent, new CreatureExperience(level, 0, 1000, 10, 0),
                new CreatureAi(0, CreatureState.MELEE_ATTACK, FIGHTER));
        if (inPit) {
            entityData.setComponent(entity, new CombatPitFighter(entityData.createEntity()));
        }

        Creature imp = CombatPitFixtures.pitCreature(IMP);
        IKwdFile kwdFile = (IKwdFile) Proxy.newProxyInstance(IKwdFile.class.getClassLoader(), new Class<?>[]{IKwdFile.class},
                (proxy, method, args) -> "getImp".equals(method.getName()) ? imp : null);
        ICreaturesController creaturesController = (ICreaturesController) Proxy.newProxyInstance(ICreaturesController.class.getClassLoader(),
                new Class<?>[]{ICreaturesController.class}, (proxy, method, args) -> null);
        Map<MiscType, MiscVariable> gameSettings = Map.of(
                MiscType.IMP_EXPERIENCE_GAIN_PER_SECOND, CombatPitFixtures.pitVariable(MiscType.IMP_EXPERIENCE_GAIN_PER_SECOND, 1),
                MiscType.COMBAT_PIT_MAX_EXPERIENCE_LEVEL, CombatPitFixtures.pitVariable(MiscType.COMBAT_PIT_MAX_EXPERIENCE_LEVEL, MAX_PIT_LEVEL));

        CreatureExperienceSystem system = new CreatureExperienceSystem(entityData, kwdFile, gameSettings, creaturesController);
        system.processTick(0.5f);
        system.processTick(1f);
        system.stop();

        return entityData.getComponent(entity, CreatureExperience.class).experience;
    }

}
