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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.CasinoBigWinner;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureMood;
import toniarts.openkeeper.game.component.CreatureSleep;
import toniarts.openkeeper.game.component.Gold;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.Slapped;
import toniarts.openkeeper.game.controller.IObjectsController;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.creature.CreatureState;
import toniarts.openkeeper.game.controller.player.PlayerStatsControl;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.tools.convert.map.CasinoVariableFixtures;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.CreatureSleepFixtures;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable.MiscType;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Slapping the Casino's big winner makes it drop a quarter of its winnings and
 * angers it, and slapping it again angers all of the keeper's creatures
 */
class SlapSystemTest {

    private static final short KEEPER = 3;
    private static final short ENEMY = 4;

    private final EntityData entityData = new DefaultEntityData();
    private final Map<MiscType, MiscVariable> gameSettings = new EnumMap<>(MiscType.class);
    private final List<int[]> goldPiles = new ArrayList<>();
    private SlapSystem slapSystem;

    @BeforeEach
    void setUp() {
        setting(MiscType.INCREASED_WORK_RATE_DURATION_FROM_SLAPPING_SECONDS, 25);
        setting(MiscType.MAX_GOLD_PILE_OUTSIDE_TREASURY, 2000);
        slapSystem = new SlapSystem(entityData, kwdFile(), List.of(player()), gameSettings, objectsController());
    }

    @Test
    void theBigWinnerDropsAQuarterOfItsWinningsWhenSlapped() {
        EntityId bigWinner = creature(KEEPER, 1500);
        entityData.setComponent(bigWinner, new CasinoBigWinner(200, 4000, 0));
        EntityId otherCreature = creature(KEEPER, 0);

        slap(bigWinner);

        assertEquals(500, entityData.getComponent(bigWinner, Gold.class).gold);
        assertEquals(2, goldPiles.size());
        for (int[] pile : goldPiles) {
            assertEquals(KEEPER, pile[0]);
            assertEquals(6, pile[1]);
            assertEquals(6, pile[2]);
            assertEquals(500, pile[3]);
        }
        assertEquals(1250, entityData.getComponent(bigWinner, CreatureMood.class).other);
        assertEquals(1000, entityData.getComponent(otherCreature, CreatureMood.class).other);
        CasinoBigWinner casinoBigWinner = entityData.getComponent(bigWinner, CasinoBigWinner.class);
        assertEquals(4000, casinoBigWinner.winnings);
        assertEquals(1, casinoBigWinner.slapCount);
    }

    @Test
    void slappingTheBigWinnerAgainAngersAllOfTheKeepersCreatures() {
        EntityId bigWinner = creature(KEEPER, 100);
        entityData.setComponent(bigWinner, new CasinoBigWinner(200, 1000, 1));
        EntityId otherCreature = creature(KEEPER, 0);
        EntityId enemyCreature = creature(ENEMY, 0);

        slap(bigWinner);

        // It can only drop the gold it has
        assertEquals(0, entityData.getComponent(bigWinner, Gold.class).gold);
        assertEquals(1, goldPiles.size());
        assertEquals(100, goldPiles.get(0)[3]);
        assertEquals(1250, entityData.getComponent(bigWinner, CreatureMood.class).other);
        assertEquals(6000, entityData.getComponent(otherCreature, CreatureMood.class).other);
        assertEquals(1000, entityData.getComponent(enemyCreature, CreatureMood.class).other);
        assertEquals(2, entityData.getComponent(bigWinner, CasinoBigWinner.class).slapCount);
    }

    private void slap(EntityId creature) {
        entityData.setComponent(creature, new Slapped(0));
        slapSystem.processTick(0.25f);
    }

    private EntityId creature(short ownerId, int gold) {
        EntityId entityId = entityData.createEntity();
        entityData.setComponents(entityId, new CreatureComponent(), new Owner(ownerId, ownerId), new Gold(gold, 2000),
                new CreatureMood(0, 0, 0, 0, 0, 1000), new Position(0, WorldUtils.pointToVector3f(new Point(6, 6))));
        return entityId;
    }

    private void setting(MiscType type, int value) {
        gameSettings.put(type, CasinoVariableFixtures.miscVariable(type, value));
    }

    private static IKwdFile kwdFile() {
        Creature creature = new Creature();
        return (IKwdFile) Proxy.newProxyInstance(IKwdFile.class.getClassLoader(), new Class<?>[]{IKwdFile.class}, (proxy, method, args) -> {
            if ("getCreature".equals(method.getName())) {
                return creature;
            }
            if ("getGameLevel".equals(method.getName())) {
                return CreatureSleepFixtures.level(4);
            }
            throw new UnsupportedOperationException(method.getName());
        });
    }

    private static IPlayerController player() {
        Keeper keeper = new Keeper(false, KEEPER);
        PlayerStatsControl statsControl = new PlayerStatsControl();
        return (IPlayerController) Proxy.newProxyInstance(IPlayerController.class.getClassLoader(), new Class<?>[]{IPlayerController.class}, (proxy, method, args) -> {
            return switch (method.getName()) {
                case "getKeeper" ->
                    keeper;
                case "getStatsControl" ->
                    statsControl;
                default ->
                    throw new UnsupportedOperationException(method.getName());
            };
        });
    }

    private IObjectsController objectsController() {
        return (IObjectsController) Proxy.newProxyInstance(IObjectsController.class.getClassLoader(), new Class<?>[]{IObjectsController.class}, (proxy, method, args) -> {
            if ("addLooseGold".equals(method.getName())) {
                goldPiles.add(new int[]{(short) args[0], (int) args[1], (int) args[2], (int) args[3]});
                return null;
            }
            throw new UnsupportedOperationException(method.getName());
        });
    }

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
}
