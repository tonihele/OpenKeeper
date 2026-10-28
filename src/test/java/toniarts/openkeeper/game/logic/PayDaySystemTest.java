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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureImprisoned;
import toniarts.openkeeper.game.component.CreatureMood;
import toniarts.openkeeper.game.component.CreaturePay;
import toniarts.openkeeper.game.component.Gold;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.PayDayClock;
import toniarts.openkeeper.game.controller.IGameWorldController;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.player.PlayerStatsControl;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.PayDayFixtures;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable.MiscType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PayDaySystemTest {

    private static final short KEEPER = Player.KEEPER1_ID;
    private static final short GOBLIN = 7;
    private static final short IMP = 1;
    private static final int WAGE = 500;
    private static final int ANGER_NO_PAY = 60;
    private static final int FIRST_PAY_DAY = 100;
    private static final int FREQUENCY = 200;
    private static final int TICKS_PER_SECOND = 4;
    private static final int CUT_OFF_TICKS = 120;
    private static final int CUT_OFF = CUT_OFF_TICKS / TICKS_PER_SECOND;
    private static final int REEVALUATION = 4;

    private final EntityData entityData = new DefaultEntityData();
    private final Map<Short, Creature> creatures = new HashMap<>();
    private final PlayerStatsControl keeperStats = new PlayerStatsControl();
    private final PayDaySystem payDaySystem;
    private double gameTime;
    private int treasury = 1000;

    PayDaySystemTest() {
        creatures.put(GOBLIN, PayDayFixtures.creature(GOBLIN, ANGER_NO_PAY));
        creatures.put(IMP, PayDayFixtures.creature(IMP, 0));
        payDaySystem = new PayDaySystem(entityData, kwdFile(), () -> gameTime,
                List.of(playerController(Player.GOOD_PLAYER_ID, new PlayerStatsControl()), playerController(KEEPER, keeperStats)),
                gameWorldController(), gameSettings());
    }

    @Test
    void nobodyIsPaidBeforeTheFirstPayDay() {
        EntityId goblin = addCreature(KEEPER, GOBLIN, WAGE, false);

        tick(0);
        tick(FIRST_PAY_DAY - 0.25);

        assertEquals(1000, treasury);
        assertEquals(0, gold(goblin));
        assertEquals(0, keeperStats.getPayDays());
    }

    @Test
    void creaturesArePaidTheirWageFromTheTreasuryOnPayDay() {
        EntityId goblin = addCreature(KEEPER, GOBLIN, WAGE, false);

        tick(0);
        tick(FIRST_PAY_DAY);

        assertEquals(500, treasury);
        assertEquals(WAGE / 2, gold(goblin));
        assertNull(entityData.getComponent(goblin, CreaturePay.class));
        assertEquals(1, keeperStats.getPayDays());
    }

    @Test
    void workersDoNotStartThePayDayClock() {
        addCreature(KEEPER, IMP, 0, true);
        tick(0);
        EntityId goblin = addCreature(KEEPER, GOBLIN, WAGE, false);
        tick(50);

        tick(FIRST_PAY_DAY);
        assertEquals(1000, treasury);

        tick(50 + FIRST_PAY_DAY);
        assertEquals(500, treasury);
        assertEquals(WAGE / 2, gold(goblin));
    }

    @Test
    void payDaysFollowAtTheFrequency() {
        addCreature(KEEPER, GOBLIN, WAGE, false);
        treasury = 2000;

        tick(0);
        tick(FIRST_PAY_DAY);
        tick(FIRST_PAY_DAY + FREQUENCY - 0.25);
        assertEquals(1500, treasury);

        tick(FIRST_PAY_DAY + FREQUENCY);
        assertEquals(1000, treasury);
        assertEquals(2, keeperStats.getPayDays());
    }

    @Test
    void aCreatureThatIsNotPaidGetsAngryUntilItIs() {
        EntityId goblin = addCreature(KEEPER, GOBLIN, WAGE, false);
        treasury = 0;

        tick(0);
        tick(FIRST_PAY_DAY);
        assertEquals(WAGE, entityData.getComponent(goblin, CreaturePay.class).unpaid);
        assertEquals(ANGER_NO_PAY / 4, mood(goblin).noPay);

        tick(FIRST_PAY_DAY + 0.25);
        assertEquals(2 * ANGER_NO_PAY / 4, mood(goblin).noPay);

        treasury = 1000;
        tick(FIRST_PAY_DAY + REEVALUATION);
        assertEquals(0, mood(goblin).noPay);
        assertNull(entityData.getComponent(goblin, CreaturePay.class));
    }

    @Test
    void anUnpaidCreatureTriesTwiceThenWaitsForTheNextPayDay() {
        EntityId goblin = addCreature(KEEPER, GOBLIN, WAGE, false);
        treasury = 0;

        tick(0);
        tick(FIRST_PAY_DAY);
        tick(FIRST_PAY_DAY + REEVALUATION);
        treasury = 1500;
        tick(FIRST_PAY_DAY + 2 * REEVALUATION);
        assertEquals(1500, treasury);

        // The wage adds up
        tick(FIRST_PAY_DAY + FREQUENCY);
        assertEquals(500, treasury);
        assertEquals(WAGE, gold(goblin));
    }

    @Test
    void aCreatureTakesWhatThereIsAndCollectsTheRestLater() {
        EntityId goblin = addCreature(KEEPER, GOBLIN, WAGE, false);
        treasury = 200;

        tick(0);
        tick(FIRST_PAY_DAY);
        assertEquals(0, treasury);
        assertEquals(300, entityData.getComponent(goblin, CreaturePay.class).unpaid);
        assertEquals(100, gold(goblin));

        treasury = 1000;
        tick(FIRST_PAY_DAY + REEVALUATION);
        assertEquals(700, treasury);
        assertEquals(250, gold(goblin));
    }

    @Test
    void creaturesThatJoinJustBeforePayDayWaitForTheNextOne() {
        addCreature(KEEPER, GOBLIN, WAGE, false);
        tick(0);
        EntityId newcomer = addCreature(KEEPER, GOBLIN, WAGE, false);
        tick(FIRST_PAY_DAY - CUT_OFF + 1);
        treasury = 2000;

        tick(FIRST_PAY_DAY);
        assertEquals(1500, treasury);
        assertEquals(0, gold(newcomer));

        tick(FIRST_PAY_DAY + FREQUENCY);
        assertEquals(500, treasury);
        assertEquals(WAGE / 2, gold(newcomer));
    }

    @Test
    void cutOffTimeIsInTicksNotSeconds() {
        addCreature(KEEPER, GOBLIN, WAGE, false);
        tick(0);

        // 40 seconds before the pay day, outside the 30 seconds (120 ticks) of the cut off
        EntityId newcomer = addCreature(KEEPER, GOBLIN, WAGE, false);
        tick(FIRST_PAY_DAY - 40);

        tick(FIRST_PAY_DAY);
        assertEquals(WAGE / 2, gold(newcomer));
    }

    @Test
    void prisonersAreNotPaid() {
        EntityId goblin = addCreature(KEEPER, GOBLIN, WAGE, false);
        entityData.setComponent(goblin, new CreatureImprisoned(0, 0));

        tick(0);
        tick(FIRST_PAY_DAY);

        assertEquals(1000, treasury);
        assertNull(entityData.getComponent(goblin, CreaturePay.class));
    }

    @Test
    void heroesAreNotPaid() {
        EntityId knight = addCreature(Player.GOOD_PLAYER_ID, GOBLIN, WAGE, false);

        tick(0);
        tick(FIRST_PAY_DAY);

        assertNull(entityData.getComponent(knight, CreaturePay.class));
        assertEquals(0, gold(knight));
    }

    @Test
    void theClockIsEmptyBeforeItStarts() {
        payDaySystem.start();
        addCreature(KEEPER, IMP, 0, true);

        tick(0);

        assertEquals(new PayDayClock(0, 0, 0), payDayClock(KEEPER));
        assertEquals(0f, payDayClock(KEEPER).getTimeLeftFraction());
    }

    @Test
    void theClockCountsDownToThePayDayAndShowsItsCost() {
        payDaySystem.start();
        addCreature(KEEPER, GOBLIN, WAGE, false);
        addCreature(KEEPER, GOBLIN, 300, false);

        tick(0);
        assertEquals(new PayDayClock(FIRST_PAY_DAY, FIRST_PAY_DAY, WAGE + 300), payDayClock(KEEPER));
        assertEquals(1f, payDayClock(KEEPER).getTimeLeftFraction());

        tick(FIRST_PAY_DAY / 4.0);
        assertEquals(0.75f, payDayClock(KEEPER).getTimeLeftFraction());
    }

    @Test
    void theClockStartsAgainOnPayDay() {
        payDaySystem.start();
        addCreature(KEEPER, GOBLIN, WAGE, false);

        tick(0);
        tick(FIRST_PAY_DAY - 1);
        assertEquals(1, payDayClock(KEEPER).timeLeft);

        tick(FIRST_PAY_DAY);
        assertEquals(new PayDayClock(FREQUENCY, FREQUENCY, WAGE), payDayClock(KEEPER));
    }

    @Test
    void theCostLeavesOutWhoIsNotPaidOnTheNextPayDay() {
        payDaySystem.start();
        addCreature(KEEPER, GOBLIN, WAGE, false);
        EntityId prisoner = addCreature(KEEPER, GOBLIN, 300, false);
        entityData.setComponent(prisoner, new CreatureImprisoned());
        addCreature(Player.GOOD_PLAYER_ID, GOBLIN, 200, false);

        tick(0);

        // Joins too close to the pay day, it is paid on the one after
        tick(FIRST_PAY_DAY - CUT_OFF + 1);
        addCreature(KEEPER, GOBLIN, 100, false);
        tick(FIRST_PAY_DAY - CUT_OFF + 2);

        assertEquals(WAGE, payDayClock(KEEPER).cost);
    }

    @Test
    void onlyTheKeepersHaveAClock() {
        payDaySystem.start();
        assertEquals(1, entityData.getEntities(PayDayClock.class).size());

        payDaySystem.stop();
        assertEquals(0, entityData.getEntities(PayDayClock.class).size());
    }

    private PayDayClock payDayClock(short keeperId) {
        for (var entity : entityData.getEntities(PayDayClock.class, Owner.class)) {
            if (entity.get(Owner.class).ownerId == keeperId) {
                return entity.get(PayDayClock.class);
            }
        }
        return null;
    }

    private void tick(double time) {
        gameTime = time;
        payDaySystem.processTick(0.25f);
    }

    private EntityId addCreature(short ownerId, short creatureId, int pay, boolean worker) {
        EntityId entityId = entityData.createEntity();
        CreatureComponent creatureComponent = new CreatureComponent();
        creatureComponent.creatureId = creatureId;
        creatureComponent.pay = pay;
        creatureComponent.worker = worker;
        entityData.setComponent(entityId, creatureComponent);
        entityData.setComponent(entityId, new Owner(ownerId, ownerId));
        entityData.setComponent(entityId, new Gold(0, 1000));
        entityData.setComponent(entityId, new CreatureMood());
        return entityId;
    }

    private int gold(EntityId entityId) {
        return entityData.getComponent(entityId, Gold.class).gold;
    }

    private CreatureMood mood(EntityId entityId) {
        return entityData.getComponent(entityId, CreatureMood.class);
    }

    private Map<MiscType, MiscVariable> gameSettings() {
        Map<MiscType, MiscVariable> settings = new EnumMap<>(MiscType.class);
        settings.put(MiscType.TIME_BEFORE_FIRST_PAY_DAY_SECONDS, PayDayFixtures.payDayVariable(MiscType.TIME_BEFORE_FIRST_PAY_DAY_SECONDS, FIRST_PAY_DAY));
        settings.put(MiscType.PAY_DAY_FREQUENCY_SECONDS, PayDayFixtures.payDayVariable(MiscType.PAY_DAY_FREQUENCY_SECONDS, FREQUENCY));
        settings.put(MiscType.PAY_DAY_CUT_OFF_TIME_SECONDS, PayDayFixtures.payDayVariable(MiscType.PAY_DAY_CUT_OFF_TIME_SECONDS, CUT_OFF_TICKS));
        settings.put(MiscType.DELAY_BEFORE_PAY_REEVALUATION_SECONDS, PayDayFixtures.payDayVariable(MiscType.DELAY_BEFORE_PAY_REEVALUATION_SECONDS, REEVALUATION));
        return settings;
    }

    private IKwdFile kwdFile() {
        return (IKwdFile) Proxy.newProxyInstance(IKwdFile.class.getClassLoader(), new Class<?>[]{IKwdFile.class},
                (proxy, method, args) -> {
                    if ("getCreature".equals(method.getName())) {
                        return creatures.get((Short) args[0]);
                    }
                    if ("getGameLevel".equals(method.getName())) {
                        return PayDayFixtures.gameLevel(TICKS_PER_SECOND);
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
    }

    private IGameWorldController gameWorldController() {
        return (IGameWorldController) Proxy.newProxyInstance(IGameWorldController.class.getClassLoader(), new Class<?>[]{IGameWorldController.class},
                (proxy, method, args) -> {
                    if ("substractGold".equals(method.getName())) {
                        int amount = (Integer) args[0];
                        int taken = Math.min(amount, treasury);
                        treasury -= taken;
                        return amount - taken;
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
    }

    private static IPlayerController playerController(short playerId, PlayerStatsControl statsControl) {
        Keeper keeper = new Keeper(false, playerId);
        return (IPlayerController) Proxy.newProxyInstance(IPlayerController.class.getClassLoader(), new Class<?>[]{IPlayerController.class},
                (proxy, method, args) -> switch (method.getName()) {
            case "getKeeper" ->
                keeper;
            case "getStatsControl" ->
                statsControl;
            default ->
                throw new UnsupportedOperationException(method.getName());
        });
    }

}
