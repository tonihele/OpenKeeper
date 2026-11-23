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

import com.jme3.math.Vector3f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.Death;
import toniarts.openkeeper.game.component.Frozen;
import toniarts.openkeeper.game.component.HauledBy;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.Unconscious;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.entity.IEntityController;
import toniarts.openkeeper.game.event.GameEventService;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.CreatureSleepFixtures;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.ShotFixtures;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable.MiscType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * A knocked out creature dies when its dying time runs out, but like in DK II
 * the time doesn't run while an Imp carries it (DKII.EXE 0x4599D0)
 */
class HealthSystemDyingTest {

    private static final short CREATURE_ID = 14;
    private static final int DYING_SECONDS = 60;
    private static final float TICK = 0.25f;

    private final EntityData entityData = new DefaultEntityData();
    private final List<String> calls = new ArrayList<>();
    private final HealthSystem healthSystem = createHealthSystem();
    private final EntityId creature = createKnockedOutCreature();

    @Test
    void knockedOutCreatureDiesWhenItsDyingTimeRunsOut() {
        runSeconds(DYING_SECONDS - 1);
        assertNull(entityData.getComponent(creature, Death.class));

        runSeconds(1);
        assertNotNull(entityData.getComponent(creature, Death.class));
    }

    @Test
    void carriedCreatureDoesNotLoseDyingTime() {
        runSeconds(30);
        entityData.setComponent(creature, new HauledBy(entityData.createEntity()));
        runSeconds(45);
        entityData.removeComponent(creature, HauledBy.class);

        runSeconds(DYING_SECONDS - 30 - 1);
        assertNull(entityData.getComponent(creature, Death.class));

        runSeconds(1);
        assertNotNull(entityData.getComponent(creature, Death.class));
    }

    @Test
    void creatureThatDiesFrozenShattersAndLeavesNoBody() {
        entityData.setComponents(creature, new Frozen(0, DYING_SECONDS + 1), new Position(0, new Vector3f(3, 0, 4)));

        runSeconds(DYING_SECONDS);
        assertNull(entityData.getComponent(creature, Death.class));
        assertEquals(List.of("emitEffect", "remove"), calls);
    }

    @Test
    void seededHealthBudgetTicksDownAndExpiresAtItsOwnDeadline() {
        entityData.setComponents(creature, new Health(8, 1000), new Unconscious(-58));
        healthSystem.processTick(TICK);
        assertEquals(7, entityData.getComponent(creature, Health.class).health);
        for (int i = 0; i < 6; i++) healthSystem.processTick(TICK);
        assertNull(entityData.getComponent(creature, Death.class));
        assertEquals(1, entityData.getComponent(creature, Health.class).health);
        healthSystem.processTick(TICK);
        assertNotNull(entityData.getComponent(creature, Death.class));
    }

    @Test
    void carryingPreservesSeededHealthAndRemainingLifetime() {
        entityData.setComponents(creature, new Health(8, 1000), new Unconscious(-58));
        runSeconds(1);
        assertEquals(4, entityData.getComponent(creature, Health.class).health);
        entityData.setComponent(creature, new HauledBy(entityData.createEntity()));
        runSeconds(70);
        assertEquals(4, entityData.getComponent(creature, Health.class).health);
        assertNull(entityData.getComponent(creature, Death.class));
        entityData.removeComponent(creature, HauledBy.class);
        runSeconds(1);
        assertNotNull(entityData.getComponent(creature, Death.class));
    }

    @Test
    void budgetLongerThanTheOrdinaryDyingDurationIsNotShortened() {
        entityData.setComponents(creature, new Health(300, 1800), new Unconscious(15));
        runSeconds(60);
        assertNull(entityData.getComponent(creature, Death.class));
        assertEquals(60, entityData.getComponent(creature, Health.class).health);
        runSeconds(15);
        assertNotNull(entityData.getComponent(creature, Death.class));
    }

    private void runSeconds(int seconds) {
        for (int tick = 0; tick < seconds / TICK; tick++) {
            healthSystem.processTick(TICK);
        }
    }

    private EntityId createKnockedOutCreature() {
        EntityId entityId = entityData.createEntity();
        CreatureComponent creatureComponent = new CreatureComponent();
        creatureComponent.creatureId = CREATURE_ID;
        entityData.setComponents(entityId, creatureComponent, new Owner((short) 1, (short) 1), new Health(0, 100),
                new Unconscious(0));
        return entityId;
    }

    private HealthSystem createHealthSystem() {
        Creature dwarf = ShotFixtures.creature(CREATURE_ID, EnumSet.of(Creature.CreatureFlag.GENERATE_DEAD_BODY),
                EnumSet.noneOf(Creature.CreatureFlag3.class));
        IKwdFile kwdFile = proxy(IKwdFile.class, method -> switch (method) { case "getCreature" -> dwarf; case "getGameLevel" -> CreatureSleepFixtures.level(4); default -> null; });
        ILevelInfo levelInfo = proxy(ILevelInfo.class, method -> method.equals("getLevelData") ? kwdFile : null);

        IEntityController controller = proxy(IEntityController.class, calls::add);
        GameEventService gameEventService = proxy(GameEventService.class,
                method -> method.equals("emitEffect") ? calls.add(method) : null);

        return new HealthSystem(entityData, proxy(IEntityPositionLookup.class, method -> method.equals("getEntityController") ? controller : null),
                Map.of(MiscType.CREATURE_DYING_STATE_DURATION_SECONDS, CreatureSleepFixtures.variable(MiscType.CREATURE_DYING_STATE_DURATION_SECONDS, DYING_SECONDS),
                        MiscType.PRISON_MODIFY_CREATURE_HEALTH_PER_SECOND, CreatureSleepFixtures.variable(MiscType.PRISON_MODIFY_CREATURE_HEALTH_PER_SECOND, -3),
                        MiscType.MODIFY_HEALTH_OF_CREATURE_IN_LAIR_PER_SECOND, CreatureSleepFixtures.variable(MiscType.MODIFY_HEALTH_OF_CREATURE_IN_LAIR_PER_SECOND, 10)),
                null, levelInfo, List.of(), null, () -> 0.0, gameEventService);
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, Answer answer) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (proxy, method, args) -> answer.answer(method.getName()));
    }

    private interface Answer {

        Object answer(String method);
    }
}
