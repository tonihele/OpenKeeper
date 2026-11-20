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
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.Damage;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.event.GameEventService;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.SummonReaperFixtures;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable.MiscType;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReaperDamageTest {

    private static final short REAPER_ID = 27;
    private static final short IMP_ID = 1;

    private final EntityData entityData = new DefaultEntityData();
    private final Map<Short, Creature> creatures = Map.of(
            IMP_ID, SummonReaperFixtures.creature(IMP_ID, Creature.CreatureFlag.IS_WORKER),
            REAPER_ID, SummonReaperFixtures.creature(REAPER_ID, Creature.CreatureFlag.IS_HORNY));

    @Test
    void reaperCanNotBeHurt() {
        EntityId reaper = addCreature(REAPER_ID, 800, 1000);
        entityData.setComponent(reaper, new Damage(300));

        healthSystem().processTick(0.25f);

        assertEquals(800, entityData.getComponent(reaper, Health.class).health);
    }

    @Test
    void reaperCanBeHealed() {
        EntityId reaper = addCreature(REAPER_ID, 800, 1000);
        entityData.setComponent(reaper, new Damage(-100));

        healthSystem().processTick(0.25f);

        assertEquals(900, entityData.getComponent(reaper, Health.class).health);
    }

    @Test
    void otherCreaturesGetHurt() {
        EntityId imp = addCreature(IMP_ID, 800, 1000);
        entityData.setComponent(imp, new Damage(300));

        healthSystem().processTick(0.25f);

        assertEquals(500, entityData.getComponent(imp, Health.class).health);
    }

    private EntityId addCreature(short creatureId, int health, int maxHealth) {
        EntityId entityId = entityData.createEntity();
        CreatureComponent creatureComponent = new CreatureComponent();
        creatureComponent.creatureId = creatureId;
        entityData.setComponent(entityId, creatureComponent);
        entityData.setComponent(entityId, new Health(health, maxHealth));
        return entityId;
    }

    private HealthSystem healthSystem() {
        Map<MiscType, MiscVariable> gameSettings = new EnumMap<>(MiscType.class);
        for (MiscType type : List.of(MiscType.CREATURE_DYING_STATE_DURATION_SECONDS, MiscType.PRISON_MODIFY_CREATURE_HEALTH_PER_SECOND,
                MiscType.MODIFY_HEALTH_OF_CREATURE_IN_LAIR_PER_SECOND)) {
            gameSettings.put(type, SummonReaperFixtures.miscVariable(type, 0));
        }
        IKwdFile kwdFile = stub(IKwdFile.class, (name, args) -> name.equals("getCreature") ? creatures.get((short) args[0]) : unsupported(name));
        ILevelInfo levelInfo = stub(ILevelInfo.class, (name, args) -> name.equals("getLevelData") ? kwdFile : unsupported(name));
        return new HealthSystem(entityData, stub(IEntityPositionLookup.class, (name, args) -> unsupported(name)), gameSettings,
                stub(ICreaturesController.class, (name, args) -> unsupported(name)), levelInfo, List.of(),
                stub(IMapController.class, (name, args) -> unsupported(name)), () -> 0.0,
                stub(GameEventService.class, (name, args) -> null));
    }

    private interface Answer {

        Object answer(String name, Object[] args);
    }

    private static <T> T stub(Class<T> type, Answer answer) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (proxy, method, args) -> answer.answer(method.getName(), args)));
    }

    private static Object unsupported(String name) {
        throw new UnsupportedOperationException(name);
    }

}
