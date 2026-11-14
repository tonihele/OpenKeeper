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
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureSleep;
import toniarts.openkeeper.game.component.Mobile;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Slapped;
import toniarts.openkeeper.game.component.SpeedChange;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.creature.CreatureState;
import toniarts.openkeeper.game.controller.player.PlayerStatsControl;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.CreatureSleepFixtures;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Variable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class SlapSystemTest {

    private static final short KEEPER = 3;

    @Test
    void slapWhileSleepingRemovesOneAuthoredUnitAndPreservesBed() {
        EntityId bed = new EntityId(7);
        CreatureSleep sleep = new CreatureSleep(bed, 10, 12, 36);

        CreatureSleep updated = SlapSystem.applySleepingSlap(
                sleep, CreatureState.SLEEPING, 9, 4);

        assertSame(bed, updated.lairObjectId);
        assertEquals(10, updated.lastSleepTime);
        assertEquals(12, updated.sleepStartTime);
        assertEquals(27, updated.sleepNeed);
    }

    @Test
    void sleepingSlapUsesConfiguredRateAndPreservesRetryClock() {
        CreatureSleep sleep = new CreatureSleep(null, 0, 0, 72);
        sleep.lastSleepAttemptTime = 12;
        CreatureSleep updated = SlapSystem.applySleepingSlap(sleep, CreatureState.SLEEPING, 9, 8);
        assertEquals(54, updated.sleepNeed);
        assertEquals(12, updated.lastSleepAttemptTime);
    }

    @Test
    void slapClampsRemainingDebtAtZero() {
        CreatureSleep sleep = new CreatureSleep(null, 0, 0, 4);

        CreatureSleep updated = SlapSystem.applySleepingSlap(
                sleep, CreatureState.SLEEPING, 9, 4);

        assertEquals(0, updated.sleepNeed);
    }

    @Test
    void slapOutsideSleepLeavesDebtUntouched() {
        CreatureSleep sleep = new CreatureSleep(null, 0, 0, 36);

        assertSame(sleep, SlapSystem.applySleepingSlap(
                sleep, CreatureState.IDLE, 9, 4));
    }

    @Test
    void slapSpeedsTheCreatureUpByHalfForAHundredSeconds() {
        EntityData entityData = new DefaultEntityData();
        EntityId creature = slappedCreature(entityData, 2f);
        SlapSystem slapSystem = slapSystem(entityData);

        assertEquals(3f, entityData.getComponent(creature, Mobile.class).maxSpeed, 0.0001f);

        slapSystem.processTick(99f);
        assertEquals(3f, entityData.getComponent(creature, Mobile.class).maxSpeed, 0.0001f);

        slapSystem.processTick(1f);
        assertEquals(2f, entityData.getComponent(creature, Mobile.class).maxSpeed, 0.0001f);
        assertNull(entityData.getComponent(creature, SpeedChange.class));
    }

    @Test
    void slapDuringTheSpeedUpNeitherAddsToItNorExtendsIt() {
        EntityData entityData = new DefaultEntityData();
        EntityId creature = slappedCreature(entityData, 2f);
        SlapSystem slapSystem = slapSystem(entityData);

        slapSystem.processTick(10f);
        entityData.setComponent(creature, new Slapped(10));
        slapSystem.processTick(0.25f);
        assertEquals(3f, entityData.getComponent(creature, Mobile.class).maxSpeed, 0.0001f);

        slapSystem.processTick(89.75f);
        assertEquals(2f, entityData.getComponent(creature, Mobile.class).maxSpeed, 0.0001f);
    }

    private static EntityId slappedCreature(EntityData entityData, float speed) {
        EntityId entity = entityData.createEntity();
        CreatureComponent creatureComponent = new CreatureComponent();
        creatureComponent.creatureId = 7;
        creatureComponent.speed = speed;
        entityData.setComponents(entity, creatureComponent, new Owner(KEEPER, KEEPER),
                new Mobile(false, false, false, speed), new Slapped(0));
        return entity;
    }

    private static SlapSystem slapSystem(EntityData entityData) {
        IKwdFile kwdFile = proxy(IKwdFile.class, name -> switch (name) {
            case "getCreature" -> new Creature();
            case "getGameLevel" -> CreatureSleepFixtures.level(4);
            default -> null;
        });
        IPlayerController player = proxy(IPlayerController.class, name -> switch (name) {
            case "getKeeper" -> new Keeper(false, KEEPER);
            case "getStatsControl" -> new PlayerStatsControl();
            default -> null;
        });
        Map<Variable.MiscVariable.MiscType, Variable.MiscVariable> settings = Map.of(
                Variable.MiscVariable.MiscType.INCREASED_WORK_RATE_DURATION_FROM_SLAPPING_SECONDS,
                CreatureSleepFixtures.variable(Variable.MiscVariable.MiscType.INCREASED_WORK_RATE_DURATION_FROM_SLAPPING_SECONDS, 60));
        return new SlapSystem(entityData, kwdFile, List.of(player), settings);
    }

    private static <T> T proxy(Class<T> type, Function<String, Object> handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (proxy, method, args) -> handler.apply(method.getName())));
    }
}
