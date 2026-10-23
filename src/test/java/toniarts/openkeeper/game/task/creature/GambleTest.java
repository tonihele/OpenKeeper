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
package toniarts.openkeeper.game.task.creature;

import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.random.RandomGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.component.CreatureMood;
import toniarts.openkeeper.game.component.Gold;
import toniarts.openkeeper.game.controller.IGameWorldController;
import toniarts.openkeeper.game.controller.IObjectsController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.room.CasinoController.PayOut;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.controller.room.storage.IRoomObjectControl;
import toniarts.openkeeper.game.task.TaskType;
import toniarts.openkeeper.tools.convert.map.CasinoVariableFixtures;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable.MiscType;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Gambling in the Casino follows the rules of the original: once a second the
 * creature's mood and gold change by the pay out setting of the casino, every
 * four seconds the keeper pays or takes the casino's share, and the creature
 * may hit the jackpot
 */
class GambleTest {

    private static final short KEEPER = 3;
    private static final int NO_JACKPOT = 0;
    private static final int JACKPOT = Gamble.JACKPOT_NUMBER;

    private final EntityData entityData = new DefaultEntityData();
    private final Map<MiscType, MiscVariable> gameSettings = new EnumMap<>(MiscType.class);
    private final List<Integer> keeperPayments = new ArrayList<>();
    private final List<int[]> goldPiles = new ArrayList<>();
    private int keeperGold = 5000;
    private EntityId creatureId;
    private ICreatureController creature;

    @BeforeEach
    void setUp() {
        setting(MiscType.GAMBLING_MODIFY_CREATURE_MONEY_SMILES_PER_SECOND, 1);
        setting(MiscType.GAMBLING_MODIFY_CREATURE_MONEY_MONEY_PER_SECOND, -8);
        setting(MiscType.GAMBLING_MODIFY_CREATURE_ANGER_SMILES_PER_SECOND, -2000);
        setting(MiscType.GAMBLING_MODIFY_CREATURE_ANGER_MONEY_PER_SECOND, 100);
        setting(MiscType.GAMBLING_CREATURE_JACKPOT_CHANCE_SMILES_PER_SECOND, 5000);
        setting(MiscType.GAMBLING_CREATURE_JACKPOT_CHANCE_MONEY_PER_SECOND, 20000);
        setting(MiscType.GAMBLING_JACKPOT_BASIC_AMOUNT, 1000);
        setting(MiscType.MAX_GOLD_PILE_OUTSIDE_TREASURY, 2000);

        creatureId = entityData.createEntity();
        entityData.setComponents(creatureId, new Gold(100, 400), new CreatureMood(10000, 0, 0, 10000, 0, 3000));
        creature = creature();
    }

    @Test
    void creatureWithMoreGoldThanTheMoneyModeStakeMayGamble() {
        Gamble gamble = gamble(PayOut.SMILES, NO_JACKPOT);
        assertTrue(gamble.isValid(creature));

        entityData.setComponent(creatureId, new Gold(8, 400));
        assertFalse(gamble.isValid(creature));
        assertEquals(TaskType.GAMBLE, gamble.getTaskType());
    }

    @Test
    void creaturesWinGoldAndCheerUpOnceASecondWhenTheCasinoPaysInSmiles() {
        Gamble gamble = gamble(PayOut.SMILES, NO_JACKPOT);

        gamble.executeTask(creature, 0.75f);
        assertEquals(100, gold());

        gamble.executeTask(creature, 1f);
        assertEquals(101, gold());
        CreatureMood mood = entityData.getComponent(creatureId, CreatureMood.class);
        assertEquals(8000, mood.general);
        assertEquals(8000, mood.noWork);
        assertEquals(1000, mood.other);

        gamble.executeTask(creature, 2.25f);
        assertEquals(102, gold());
        mood = entityData.getComponent(creatureId, CreatureMood.class);
        assertEquals(6000, mood.general);
        assertEquals(0, mood.other);
    }

    @Test
    void theKeeperPaysTheCasinoWinningsEveryFourSecondsInSmilesMode() {
        Gamble gamble = gamble(PayOut.SMILES, NO_JACKPOT);

        gamble.executeTask(creature, 3f);
        assertTrue(keeperPayments.isEmpty());

        gamble.executeTask(creature, 4f);
        assertEquals(List.of(4), keeperPayments);
    }

    @Test
    void creatureWinningsStopAtTheGoldItCanCarry() {
        entityData.setComponent(creatureId, new Gold(400, 400));
        Gamble gamble = gamble(PayOut.SMILES, NO_JACKPOT);

        gamble.executeTask(creature, 1f);

        assertEquals(400, gold());
    }

    @Test
    void creaturesLoseGoldAndGetAngrierWhenTheCasinoPaysInMoney() {
        Gamble gamble = gamble(PayOut.MONEY, NO_JACKPOT);

        gamble.executeTask(creature, 2f);

        assertEquals(84, gold());
        CreatureMood mood = entityData.getComponent(creatureId, CreatureMood.class);
        assertEquals(10200, mood.general);
        assertEquals(10200, mood.noWork);
        assertEquals(3200, mood.other);
        assertEquals(0, mood.noFood);
    }

    @Test
    void theCasinoTakingsAreLeftOnItsCentreTileInMoneyMode() {
        Gamble gamble = gamble(PayOut.MONEY, NO_JACKPOT);

        gamble.executeTask(creature, 4f);

        assertEquals(1, goldPiles.size());
        assertEquals(6, goldPiles.get(0)[0]);
        assertEquals(6, goldPiles.get(0)[1]);
        assertEquals(32, goldPiles.get(0)[2]);
        assertEquals(2000, goldPiles.get(0)[3]);
        assertTrue(keeperPayments.isEmpty());
    }

    @Test
    void creatureStopsGamblingWhenItCanNoLongerCoverTheStake() {
        entityData.setComponent(creatureId, new Gold(20, 400));
        Gamble gamble = gamble(PayOut.MONEY, NO_JACKPOT);

        gamble.executeTask(creature, 5f);

        assertEquals(4, gold());
        assertFalse(gamble.isValid(creature));
    }

    @Test
    void theKeeperPaysTheJackpotFromTheTreasury() {
        entityData.setComponent(creatureId, new Gold(100, 2000));
        Gamble gamble = gamble(PayOut.SMILES, JACKPOT);

        gamble.executeTask(creature, 1f);

        assertEquals(List.of(1000), keeperPayments);
        assertEquals(1101, gold());
    }

    @Test
    void theJackpotIsWhatTheKeeperHasWhenThatIsLess() {
        keeperGold = 300;
        Gamble gamble = gamble(PayOut.SMILES, JACKPOT);

        gamble.executeTask(creature, 1f);

        assertEquals(List.of(1000), keeperPayments);
        assertEquals(0, keeperGold);
        assertEquals(400, gold());
    }

    private Gamble gamble(PayOut payOut, int jackpotRoll) {
        RoomInstance roomInstance = new RoomInstance(new Room());
        for (int x = 5; x <= 7; x++) {
            for (int y = 5; y <= 7; y++) {
                roomInstance.addCoordinate(new Point(x, y));
            }
        }
        IRoomObjectControl control = (IRoomObjectControl) Proxy.newProxyInstance(IRoomObjectControl.class.getClassLoader(), new Class<?>[]{IRoomObjectControl.class}, (proxy, method, args) -> {
            if ("isFullCapacity".equals(method.getName())) {
                return false;
            }
            throw new UnsupportedOperationException(method.getName());
        });
        IRoomController room = (IRoomController) Proxy.newProxyInstance(IRoomController.class.getClassLoader(), new Class<?>[]{IRoomController.class}, (proxy, method, args) -> {
            return switch (method.getName()) {
                case "getRoomInstance" ->
                    roomInstance;
                case "isRemoved", "isDestroyed" ->
                    false;
                case "getOwnerId" ->
                    KEEPER;
                case "getObjectControl" ->
                    control;
                default ->
                    throw new UnsupportedOperationException(method.getName());
            };
        });
        IGameWorldController gameWorldController = (IGameWorldController) Proxy.newProxyInstance(IGameWorldController.class.getClassLoader(), new Class<?>[]{IGameWorldController.class}, (proxy, method, args) -> {
            if ("substractGold".equals(method.getName())) {
                int amount = (int) args[0];
                keeperPayments.add(amount);
                int paid = Math.min(amount, keeperGold);
                keeperGold -= paid;
                return amount - paid;
            }
            throw new UnsupportedOperationException(method.getName());
        });
        IObjectsController objectsController = (IObjectsController) Proxy.newProxyInstance(IObjectsController.class.getClassLoader(), new Class<?>[]{IObjectsController.class}, (proxy, method, args) -> {
            if ("addLooseGold".equals(method.getName())) {
                goldPiles.add(new int[]{(int) args[1], (int) args[2], (int) args[3], (int) args[4]});
                return null;
            }
            throw new UnsupportedOperationException(method.getName());
        });
        return new Gamble(null, null, new Point(5, 5), KEEPER, room, null, entityData, gameWorldController, objectsController,
                gameSettings, () -> payOut, fixedRandom(jackpotRoll));
    }

    private ICreatureController creature() {
        return (ICreatureController) Proxy.newProxyInstance(ICreatureController.class.getClassLoader(), new Class<?>[]{ICreatureController.class}, (proxy, method, args) -> {
            Gold gold = entityData.getComponent(creatureId, Gold.class);
            return switch (method.getName()) {
                case "getEntityId" ->
                    creatureId;
                case "getOwnerId" ->
                    KEEPER;
                case "getGold" ->
                    gold.gold;
                case "getMaxGold" ->
                    gold.maxGold;
                case "addGold" -> {
                    entityData.setComponent(creatureId, new Gold(gold.gold + (int) args[0], gold.maxGold));
                    yield null;
                }
                case "substractGold" -> {
                    entityData.setComponent(creatureId, new Gold(gold.gold - (int) args[0], gold.maxGold));
                    yield null;
                }
                default ->
                    throw new UnsupportedOperationException(method.getName());
            };
        });
    }

    private int gold() {
        return entityData.getComponent(creatureId, Gold.class).gold;
    }

    private void setting(MiscType type, int value) {
        gameSettings.put(type, CasinoVariableFixtures.miscVariable(type, value));
    }

    private static RandomGenerator fixedRandom(int value) {
        return new RandomGenerator() {

            @Override
            public long nextLong() {
                return value;
            }

            @Override
            public int nextInt(int bound) {
                return value;
            }
        };
    }

}
