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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.CasinoBigWinner;
import toniarts.openkeeper.game.component.Gold;
import toniarts.openkeeper.game.controller.IGameWorldController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.task.TaskType;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The big winner of the Casino collects its jackpot from the keeper's gold
 */
class CollectWinningsTest {

    private static final short KEEPER = 3;

    private final EntityData entityData = new DefaultEntityData();
    private int keeperGold = 5000;
    private EntityId creatureId;
    private ICreatureController creature;
    private CollectWinnings collectWinnings;

    @BeforeEach
    void setUp() {
        creatureId = entityData.createEntity();
        entityData.setComponents(creatureId, new Gold(100, 2000), new CasinoBigWinner(100, 1000, 1));
        creature = creature();
        collectWinnings = new CollectWinnings(null, null, new Point(2, 2), KEEPER, room(), entityData, gameWorldController());
    }

    @Test
    void theBigWinnerCollectsItsWinningsFromTheKeeper() {
        assertTrue(collectWinnings.isValid(creature));
        assertEquals(TaskType.COLLECT_WINNINGS, collectWinnings.getTaskType());

        collectWinnings.executeTask(creature, 0);

        assertEquals(1100, gold());
        assertEquals(4000, keeperGold);
        assertNull(entityData.getComponent(creatureId, CasinoBigWinner.class));
        assertFalse(collectWinnings.isValid(creature));
    }

    @Test
    void theTaskIsRemovedOnceTheWinningsAreCollected() {
        assertFalse(collectWinnings.isRemovable());

        collectWinnings.executeTask(creature, 0);

        assertTrue(collectWinnings.isRemovable());
    }

    @Test
    void theBigWinnerComesBackForWhatTheKeeperCouldNotPay() {
        keeperGold = 300;

        collectWinnings.executeTask(creature, 0);

        assertEquals(400, gold());
        CasinoBigWinner bigWinner = entityData.getComponent(creatureId, CasinoBigWinner.class);
        assertEquals(700, bigWinner.winnings);
        assertEquals(100, bigWinner.timeLeft);
        assertEquals(1, bigWinner.slapCount);
    }

    @Test
    void theBigWinnerKeepsWhatItCanCarry() {
        entityData.setComponent(creatureId, new Gold(1800, 2000));

        collectWinnings.executeTask(creature, 0);

        assertEquals(2000, gold());
        assertEquals(4000, keeperGold);
        assertNull(entityData.getComponent(creatureId, CasinoBigWinner.class));
    }

    private IRoomController room() {
        return (IRoomController) Proxy.newProxyInstance(IRoomController.class.getClassLoader(), new Class<?>[]{IRoomController.class}, (proxy, method, args) -> {
            return switch (method.getName()) {
                case "isRemoved", "isDestroyed" ->
                    false;
                case "getOwnerId" ->
                    KEEPER;
                default ->
                    throw new UnsupportedOperationException(method.getName());
            };
        });
    }

    private IGameWorldController gameWorldController() {
        return (IGameWorldController) Proxy.newProxyInstance(IGameWorldController.class.getClassLoader(), new Class<?>[]{IGameWorldController.class}, (proxy, method, args) -> {
            if ("substractGold".equals(method.getName())) {
                int amount = (int) args[0];
                int paid = Math.min(amount, keeperGold);
                keeperGold -= paid;
                return amount - paid;
            }
            throw new UnsupportedOperationException(method.getName());
        });
    }

    private ICreatureController creature() {
        return (ICreatureController) Proxy.newProxyInstance(ICreatureController.class.getClassLoader(), new Class<?>[]{ICreatureController.class}, (proxy, method, args) -> {
            Gold gold = entityData.getComponent(creatureId, Gold.class);
            return switch (method.getName()) {
                case "getEntityId" ->
                    creatureId;
                case "getGold" ->
                    gold.gold;
                case "getMaxGold" ->
                    gold.maxGold;
                case "addGold" -> {
                    entityData.setComponent(creatureId, new Gold(gold.gold + (int) args[0], gold.maxGold));
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

}
