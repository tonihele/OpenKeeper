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
package toniarts.openkeeper.tools.convert.map;

import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureEfficiency;
import toniarts.openkeeper.game.component.CreatureMood;
import toniarts.openkeeper.game.component.CreatureRecuperating;
import toniarts.openkeeper.game.component.CreatureSleep;
import toniarts.openkeeper.game.component.Damage;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.Slapped;
import toniarts.openkeeper.game.controller.CreaturesController;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.player.PlayerRoomControl;
import toniarts.openkeeper.game.controller.player.PlayerStatsControl;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.logic.CreatureMoodSystem;
import toniarts.openkeeper.game.logic.SlapSystem;
import toniarts.openkeeper.game.trigger.TriggerControl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreatureMoodsActionTest {
    private static final short KEEPER = Player.KEEPER1_ID;
    private double time;
    private final Room room = room();
    private final DefaultEntityData data = new DefaultEntityData();
    private final Creature creature = creature();
    private final IKwdFile kwd = proxy(IKwdFile.class, (method, args) -> switch (method) {
        case "getThings" -> List.of();
        case "getCreature" -> creature;
        case "getRoomById" -> room;
        case "getGameLevel" -> CreatureSleepFixtures.level(4);
        default -> throw new UnsupportedOperationException(method);
    });
    private final CreaturesController creatures = new CreaturesController(
            kwd, data, Map.of(), () -> time, null, null, null);

    @ParameterizedTest
    @ValueSource(ints = {0, 2, 255})
    void onlyAuthoredOneEnablesTheGlobalSwitch(int available) {
        assertTrue(creatures.isCreatureMoodsEnabled());
        action(available);
        assertFalse(creatures.isCreatureMoodsEnabled());
        action(1);
        assertTrue(creatures.isCreatureMoodsEnabled());
    }

    @Test
    void disablingDoesNotChangeStoredMoodAndFreshGameStartsEnabled() {
        EntityId id = entity(KEEPER);
        CreatureMood before = mood(id);
        action(0);
        assertSame(before, mood(id));
        CreaturesController fresh = new CreaturesController(kwd, data, Map.of(), () -> time, null, null, null);
        assertTrue(fresh.isCreatureMoodsEnabled());
        assertFalse(creatures.isCreatureMoodsEnabled());
    }

    @Test
    void happyAndAngrySpecialsFreezeAcrossOwnersAndResume() {
        EntityId own = entity(KEEPER);
        EntityId enemy = entity((short) (KEEPER + 1));
        CreatureMood ownBefore = mood(own);
        CreatureMood enemyBefore = mood(enemy);
        action(0);
        creatures.makeCreaturesHappy(KEEPER);
        creatures.angerEnemyCreatures(KEEPER);
        assertSame(ownBefore, mood(own));
        assertSame(enemyBefore, mood(enemy));
        action(1);
        creatures.makeCreaturesHappy(KEEPER);
        creatures.angerEnemyCreatures(KEEPER);
        assertEquals(0, mood(own).getMaximum());
        for (int reason = 0; reason < 6; reason++) {
            assertEquals(CreatureMood.MAX_ANGER, mood(enemy).get(reason));
        }
    }

    @Test
    void noLairAngerFreezesAndResumesWithoutCatchingUp() {
        EntityId id = entity(KEEPER);
        data.setComponent(id, new CreatureSleep(null, 0, 0, 0));
        Keeper keeper = new Keeper(false, KEEPER);
        PlayerRoomControl rooms = new PlayerRoomControl(keeper, List.of(room));
        rooms.setTypeAvailable(room, true, true);
        IPlayerController player = proxy(IPlayerController.class, (method, args) -> switch (method) {
            case "getKeeper" -> keeper;
            case "getRoomControl" -> rooms;
            default -> throw new UnsupportedOperationException(method);
        });
        CreatureMoodSystem system = new CreatureMoodSystem(data, kwd, List.of(player), () -> time,
                Map.of(Variable.MiscVariable.MiscType.MODIFY_ANGER_OF_CREATURE_IN_LAIR_PER_SECOND,
                        CreatureSleepFixtures.variable(Variable.MiscVariable.MiscType.MODIFY_ANGER_OF_CREATURE_IN_LAIR_PER_SECOND, -10)),
                creatures::isCreatureMoodsEnabled);
        try {
            CreatureMood before = mood(id);
            action(0);
            for (int i = 0; i < 10; i++) system.processTick(0.25f);
            assertSame(before, mood(id));
            action(1);
            system.processTick(0.25f);
            assertEquals(before.noLair + 10, mood(id).noLair);
        } finally {
            system.stop();
        }
    }

    @Test
    void assignedLairClearAndRecoveryFreezeButRecoveryClockKeepsAdvancing() {
        EntityId id = entity(KEEPER);
        EntityId bed = data.createEntity();
        data.setComponent(bed, new Position());
        data.setComponent(id, new CreatureSleep(bed, 0, 0, 0));
        data.setComponent(id, new CreatureRecuperating(0, 0, 0));
        CreatureMood before = mood(id);
        CreatureMoodSystem system = new CreatureMoodSystem(data, kwd, List.of(), () -> time,
                Map.of(Variable.MiscVariable.MiscType.MODIFY_ANGER_OF_CREATURE_IN_LAIR_PER_SECOND,
                        CreatureSleepFixtures.variable(Variable.MiscVariable.MiscType.MODIFY_ANGER_OF_CREATURE_IN_LAIR_PER_SECOND, -10)),
                creatures::isCreatureMoodsEnabled);
        try {
            action(0);
            for (int i = 1; i <= 4; i++) {
                time = i;
                system.processTick(0.25f);
            }
            assertSame(before, mood(id));
            assertEquals(4, data.getComponent(id, CreatureRecuperating.class).moodCheckTime);
            action(1);
            time = 5;
            system.processTick(0.25f);
            assertEquals(0, mood(id).noLair);
            assertEquals(before.other - 10, mood(id).other);
            assertEquals(before.noFood, mood(id).noFood);
        } finally {
            system.stop();
        }
    }

    @Test
    void slapStillDamagesAndBoostsWorkWhileAngerIsFrozenThenResumes() {
        EntityId id = entity(KEEPER);
        data.setComponent(id, new CreatureEfficiency(100));
        PlayerStatsControl stats = new PlayerStatsControl();
        IPlayerController player = proxy(IPlayerController.class, (method, args) -> switch (method) {
            case "getKeeper" -> new Keeper(false, KEEPER);
            case "getStatsControl" -> stats;
            default -> throw new UnsupportedOperationException(method);
        });
        SlapSystem system = new SlapSystem(data, kwd, List.of(player),
                Map.of(Variable.MiscVariable.MiscType.INCREASED_WORK_RATE_DURATION_FROM_SLAPPING_SECONDS,
                        CreatureSleepFixtures.variable(Variable.MiscVariable.MiscType.INCREASED_WORK_RATE_DURATION_FROM_SLAPPING_SECONDS, 10)),
                creatures::isCreatureMoodsEnabled);
        try {
            CreatureMood before = mood(id);
            action(0);
            data.setComponent(id, new Slapped(0));
            system.processTick(0.25f);
            assertSame(before, mood(id));
            assertNotNull(data.getComponent(id, Damage.class));
            assertEquals(110, data.getComponent(id, CreatureEfficiency.class).efficiencyPercentage);
            action(1);
            data.setComponent(id, new Slapped(1));
            system.processTick(0.25f);
            assertEquals(before.other + 100, mood(id).other);
        } finally {
            system.stop();
        }
    }

    private void action(int available) {
        TriggerAction action = new TriggerAction(null);
        action.setId(1);
        action.setType(TriggerAction.ActionType.SET_CREATURE_MOODS);
        action.setUserData("available", (short) available);
        IKwdFile script = proxy(IKwdFile.class, (method, args) -> {
            if (method.equals("getTriggers")) return Map.of(1, action);
            throw new UnsupportedOperationException(method);
        });
        ILevelInfo info = proxy(ILevelInfo.class, (method, args) -> {
            if (method.equals("getLevelData")) return script;
            throw new UnsupportedOperationException(method);
        });
        new TriggerControl(null, info, () -> time, null, creatures, 1).update(0.25f);
    }

    private EntityId entity(short owner) {
        EntityId id = data.createEntity();
        CreatureComponent component = new CreatureComponent();
        component.creatureId = creature.getCreatureId();
        data.setComponents(id, component, new Owner(owner, owner),
                new CreatureMood(10, 20, 30, 40, 50, 60));
        return id;
    }

    private CreatureMood mood(EntityId id) {
        return data.getComponent(id, CreatureMood.class);
    }

    private static Creature creature() {
        Creature result = CreatureSleepFixtures.sleeper(4);
        result.getAttributes().setAngerNoLair((short) 40);
        result.getAttributes().setAngerSlap((short) 100);
        result.getAttributes().setSlapDamage(5);
        return result;
    }

    private static Room room() {
        Room result = new Room();
        result.setRoomId((short) 2);
        result.setFlags(EnumSet.of(Room.RoomFlag.BUILDABLE));
        return result;
    }

    private interface Call {
        Object apply(String method, Object[] args);
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, Call call) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (p, method, args) -> call.apply(method.getName(), args));
    }
}
