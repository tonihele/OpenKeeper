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
package toniarts.openkeeper.game.controller.player;

import java.lang.reflect.Proxy;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.data.Keeper;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * When a gold room changes hands, its capacity and the gold stored in it move
 * from the old owner's totals to the new owner's
 */
class CapturedGoldRoomTest {

    private static final short KEEPER_ID = 3;
    private static final short ENEMY_ID = 4;

    private final Keeper keeper = new Keeper(false, KEEPER_ID);
    private final Keeper enemy = new Keeper(false, ENEMY_ID);
    private final PlayerGoldControl keeperGold = new PlayerGoldControl(keeper);
    private final PlayerGoldControl enemyGold = new PlayerGoldControl(enemy);

    CapturedGoldRoomTest() {
        keeperGold.setGoldMax(16000);
        keeperGold.addGold(4270);
        enemyGold.setGoldMax(43000);
        enemyGold.addGold(5000);
    }

    @Test
    void capturedTreasuryBringsItsGoldAndCapacity() {
        IRoomController treasury = room(true, 27000, 3000);

        enemyGold.onCapturedByEnemy(treasury);
        keeperGold.onCaptured(treasury);

        assertEquals(2000, enemy.getGold());
        assertEquals(16000, enemyGold.getGoldMax());
        assertEquals(7270, keeper.getGold());
        assertEquals(43000, keeperGold.getGoldMax());
    }

    @Test
    void capturedGoldIsNotMined() {
        keeperGold.onCaptured(room(true, 27000, 3000));

        assertEquals(4270, keeper.getGoldMined());
    }

    @Test
    void otherRoomsLeaveTheGoldAlone() {
        IRoomController lair = room(false, 0, 0);

        enemyGold.onCapturedByEnemy(lair);
        keeperGold.onCaptured(lair);

        assertEquals(5000, enemy.getGold());
        assertEquals(43000, enemyGold.getGoldMax());
        assertEquals(4270, keeper.getGold());
        assertEquals(16000, keeperGold.getGoldMax());
    }

    private static IRoomController room(boolean storesGold, int capacity, int storedGold) {
        return (IRoomController) Proxy.newProxyInstance(IRoomController.class.getClassLoader(), new Class<?>[]{IRoomController.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "canStoreGold" ->
                        storesGold;
                    case "getMaxCapacity" ->
                        capacity;
                    case "getUsedCapacity" ->
                        storedGold;
                    default ->
                        null;
                });
    }
}
