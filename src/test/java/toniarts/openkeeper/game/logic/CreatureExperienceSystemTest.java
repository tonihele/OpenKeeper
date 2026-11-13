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
import java.util.EnumSet;
import java.util.Map;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.CreatureAi;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureExperience;
import toniarts.openkeeper.game.component.CreatureExperienceTimer;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.creature.CreatureState;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.CreatureSleepFixtures;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.ShotFixtures;
import toniarts.openkeeper.tools.convert.map.Variable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CreatureExperienceSystemTest {

    private static final Map<Variable.MiscVariable.MiscType, Variable.MiscVariable> SETTINGS = Map.of(
            Variable.MiscVariable.MiscType.IMP_EXPERIENCE_GAIN_PER_SECOND,
            CreatureSleepFixtures.variable(Variable.MiscVariable.MiscType.IMP_EXPERIENCE_GAIN_PER_SECOND, 1));

    @Test
    void timeWorkingSurvivesANewSystem() {
        EntityData entityData = new DefaultEntityData();
        EntityId fighter = entityData.createEntity();
        CreatureComponent creature = new CreatureComponent();
        creature.creatureId = 6;
        entityData.setComponents(fighter, creature,
                new CreatureExperience(1, 0, 100, 10, 0),
                new CreatureAi(0, CreatureState.MELEE_ATTACK, (short) 0, null));

        // The first tick only starts the timer, then three quarters of a second
        CreatureExperienceSystem system = createSystem(entityData);
        for (int i = 0; i < 4; i++) {
            system.processTick(0.25f);
        }
        system.stop();
        assertEquals(0.75, entityData.getComponent(fighter, CreatureExperienceTimer.class).timeWorking, 0.0001);

        // Like after loading a save, a new system carries on where the old one was
        system = createSystem(entityData);
        system.processTick(0.25f);
        assertEquals(10, entityData.getComponent(fighter, CreatureExperience.class).experience);
        assertEquals(0, entityData.getComponent(fighter, CreatureExperienceTimer.class).timeWorking, 0.0001);

        // Gone when the creature stops being a creature with experience
        entityData.removeComponent(fighter, CreatureAi.class);
        system.processTick(0.25f);
        assertNull(entityData.getComponent(fighter, CreatureExperienceTimer.class));
    }

    private static CreatureExperienceSystem createSystem(EntityData entityData) {
        Creature imp = ShotFixtures.creature((short) 1, EnumSet.noneOf(Creature.CreatureFlag.class), EnumSet.noneOf(Creature.CreatureFlag3.class));
        IKwdFile kwdFile = stub(IKwdFile.class, (name, args) -> "getImp".equals(name) ? imp : null);
        return new CreatureExperienceSystem(entityData, kwdFile, SETTINGS, stub(ICreaturesController.class, (name, args) -> null));
    }

    @SuppressWarnings("unchecked")
    private static <T> T stub(Class<T> type, Answer answer) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (proxy, method, args) -> answer.answer(method.getName(), args));
    }

    private interface Answer {

        Object answer(String name, Object[] args);
    }

}
