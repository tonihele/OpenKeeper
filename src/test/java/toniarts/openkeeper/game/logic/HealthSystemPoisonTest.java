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
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.Frozen;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.InHand;
import toniarts.openkeeper.game.component.Poisoned;
import toniarts.openkeeper.game.event.GameEventService;
import toniarts.openkeeper.tools.convert.map.CreatureSleepFixtures;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable.MiscType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * A poisoned creature takes the poison's damage every tick until the poison
 * runs out. The poison waits while the creature is frozen, and does no harm
 * while the creature is in the hand (DKII.EXE 0x48E160, 0x4B2CE0)
 */
class HealthSystemPoisonTest {

    private final EntityData entityData = new DefaultEntityData();
    private final HealthSystem healthSystem = new HealthSystem(entityData, null, settings(), null, null, levelInfo(), List.of(), null,
            () -> 0, (GameEventService) Proxy.newProxyInstance(GameEventService.class.getClassLoader(),
                    new Class<?>[]{GameEventService.class}, (proxy, method, args) -> null));

    @Test
    void poisonHurtsEveryTickUntilItRunsOut() {
        EntityId creature = creature(new Poisoned(3, 10));

        tick(4);

        assertEquals(70, entityData.getComponent(creature, Health.class).health);
        assertNull(entityData.getComponent(creature, Poisoned.class));
    }

    @Test
    void poisonWaitsWhileTheCreatureIsFrozen() {
        EntityId creature = creature(new Poisoned(3, 10));
        entityData.setComponent(creature, new Frozen(0, 10));

        tick(2);

        assertEquals(100, entityData.getComponent(creature, Health.class).health);
        assertEquals(3, entityData.getComponent(creature, Poisoned.class).ticksLeft);

        entityData.removeComponent(creature, Frozen.class);
        tick(1);

        assertEquals(90, entityData.getComponent(creature, Health.class).health);
        assertEquals(2, entityData.getComponent(creature, Poisoned.class).ticksLeft);
    }

    @Test
    void poisonDoesNoHarmInTheHand() {
        EntityId creature = creature(new Poisoned(2, 10));
        entityData.setComponent(creature, new InHand());

        tick(2);

        assertEquals(100, entityData.getComponent(creature, Health.class).health);
        assertNull(entityData.getComponent(creature, Poisoned.class));
    }

    private void tick(int ticks) {
        for (int i = 0; i < ticks; i++) {
            healthSystem.processTick(0.25f);
        }
    }

    private EntityId creature(Poisoned poisoned) {
        EntityId entity = entityData.createEntity();
        entityData.setComponents(entity, new CreatureComponent(), new Health(100, 100), poisoned);
        return entity;
    }

    private static ILevelInfo levelInfo() {
        IKwdFile kwdFile = (IKwdFile) Proxy.newProxyInstance(IKwdFile.class.getClassLoader(), new Class<?>[]{IKwdFile.class},
                (proxy, method, args) -> CreatureSleepFixtures.sleeper(0));
        return (ILevelInfo) Proxy.newProxyInstance(ILevelInfo.class.getClassLoader(), new Class<?>[]{ILevelInfo.class},
                (proxy, method, args) -> kwdFile);
    }

    private static Map<MiscType, MiscVariable> settings() {
        Map<MiscType, MiscVariable> settings = new EnumMap<>(MiscType.class);
        for (MiscType type : List.of(MiscType.CREATURE_DYING_STATE_DURATION_SECONDS,
                MiscType.PRISON_MODIFY_CREATURE_HEALTH_PER_SECOND, MiscType.MODIFY_HEALTH_OF_CREATURE_IN_LAIR_PER_SECOND)) {
            settings.put(type, CreatureSleepFixtures.variable(type, 0));
        }
        return settings;
    }
}
