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

    PayDaySystemTest() {
        creatures.put(GOBLIN, PayDayFixtures.creature(GOBLIN, ANGER_NO_PAY));
        creatures.put(IMP, PayDayFixtures.creature(IMP, 0));
        payDaySystem = new PayDaySystem(entityData, kwdFile(), () -> gameTime,
                List.of(playerController(Player.GOOD_PLAYER_ID, new PlayerStatsControl()), playerController(KEEPER, keeperStats)),
                gameSettings());
    }

    @Test
    void nobodyIsOwedBeforeTheFirstPayDay() {
        EntityId goblin = addCreature(KEEPER, GOBLIN, WAGE, false);

        tick(0);
        tick(FIRST_PAY_DAY - 0.25);

        assertEquals(0, owed(goblin));
        assertEquals(0, keeperStats.getPayDays());
    }

    @Test
    void creaturesAreOwedTheirWageOnPayDay() {
        EntityId goblin = addCreature(KEEPER, GOBLIN, WAGE, false);

        tick(0);
        tick(FIRST_PAY_DAY);

        assertEquals(WAGE, owed(goblin));
        assertEquals(1, keeperStats.getPayDays());
    }

    @Test
    void workersDoNotStartThePayDayClock() {
        addCreature(KEEPER, IMP, 0, true);
        tick(0);
        EntityId goblin = addCreature(KEEPER, GOBLIN, WAGE, false);
        tick(50);

        tick(FIRST_PAY_DAY);
        assertEquals(0, owed(goblin));

        tick(50 + FIRST_PAY_DAY);
        assertEquals(WAGE, owed(goblin));
    }

    @Test
    void payDaysFollowAtTheFrequencyAndTheWageAddsUp() {
        EntityId goblin = addCreature(KEEPER, GOBLIN, WAGE, false);

        tick(0);
        tick(FIRST_PAY_DAY);
        tick(FIRST_PAY_DAY + FREQUENCY - 0.25);
        assertEquals(WAGE, owed(goblin));

        tick(FIRST_PAY_DAY + FREQUENCY);
        assertEquals(2 * WAGE, owed(goblin));
        assertEquals(2, keeperStats.getPayDays());
    }

    @Test
    void aCreatureThatIsNotPaidGetsAngryUntilItIs() {
        EntityId goblin = addCreature(KEEPER, GOBLIN, WAGE, false);

        tick(0);
        tick(FIRST_PAY_DAY);
        assertEquals(ANGER_NO_PAY / 4, mood(goblin).noPay);

        tick(FIRST_PAY_DAY + 0.25);
        assertEquals(2 * ANGER_NO_PAY / 4, mood(goblin).noPay);

        // The creature collects its pay
        entityData.removeComponent(goblin, CreaturePay.class);
        tick(FIRST_PAY_DAY + 0.5);
        assertEquals(0, mood(goblin).noPay);
    }

    @Test
    void aNewPayDayLetsACreatureThatGaveUpTryAgain() {
        EntityId goblin = addCreature(KEEPER, GOBLIN, WAGE, false);

        tick(0);
        tick(FIRST_PAY_DAY);
        entityData.setComponent(goblin, new CreaturePay(WAGE, CreaturePay.MAX_COLLECTION_ATTEMPTS, FIRST_PAY_DAY + REEVALUATION, false));

        tick(FIRST_PAY_DAY + FREQUENCY);
        assertEquals(0, entityData.getComponent(goblin, CreaturePay.class).attempts);
    }

    @Test
    void creaturesThatJoinJustBeforePayDayWaitForTheNextOne() {
        addCreature(KEEPER, GOBLIN, WAGE, false);
        tick(0);
        EntityId newcomer = addCreature(KEEPER, GOBLIN, WAGE, false);
        tick(FIRST_PAY_DAY - CUT_OFF + 1);

        tick(FIRST_PAY_DAY);
        assertEquals(0, owed(newcomer));

        tick(FIRST_PAY_DAY + FREQUENCY);
        assertEquals(WAGE, owed(newcomer));
    }

    @Test
    void cutOffTimeIsInTicksNotSeconds() {
        addCreature(KEEPER, GOBLIN, WAGE, false);
        tick(0);

        // 40 seconds before the pay day, outside the 30 seconds (120 ticks) of the cut off
        EntityId newcomer = addCreature(KEEPER, GOBLIN, WAGE, false);
        tick(FIRST_PAY_DAY - 40);

        tick(FIRST_PAY_DAY);
        assertEquals(WAGE, owed(newcomer));
    }

    @Test
    void prisonersAreNotPaid() {
        EntityId goblin = addCreature(KEEPER, GOBLIN, WAGE, false);
        entityData.setComponent(goblin, new CreatureImprisoned(0, 0));

        tick(0);
        tick(FIRST_PAY_DAY);

        assertNull(entityData.getComponent(goblin, CreaturePay.class));
    }

    @Test
    void heroesAreNotPaid() {
        EntityId knight = addCreature(Player.GOOD_PLAYER_ID, GOBLIN, WAGE, false);

        tick(0);
        tick(FIRST_PAY_DAY);

        assertNull(entityData.getComponent(knight, CreaturePay.class));
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

    private int owed(EntityId entityId) {
        CreaturePay pay = entityData.getComponent(entityId, CreaturePay.class);
        return pay != null ? pay.unpaid : 0;
    }

    private CreatureMood mood(EntityId entityId) {
        return entityData.getComponent(entityId, CreatureMood.class);
    }

    private Map<MiscType, MiscVariable> gameSettings() {
        Map<MiscType, MiscVariable> settings = new EnumMap<>(MiscType.class);
        settings.put(MiscType.TIME_BEFORE_FIRST_PAY_DAY_SECONDS, PayDayFixtures.payDayVariable(MiscType.TIME_BEFORE_FIRST_PAY_DAY_SECONDS, FIRST_PAY_DAY));
        settings.put(MiscType.PAY_DAY_FREQUENCY_SECONDS, PayDayFixtures.payDayVariable(MiscType.PAY_DAY_FREQUENCY_SECONDS, FREQUENCY));
        settings.put(MiscType.PAY_DAY_CUT_OFF_TIME_SECONDS, PayDayFixtures.payDayVariable(MiscType.PAY_DAY_CUT_OFF_TIME_SECONDS, CUT_OFF_TICKS));
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
