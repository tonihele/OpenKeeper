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
package toniarts.openkeeper.game.controller;

import java.lang.reflect.Proxy;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.map.IMapTileController;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Values from the shipped GlobalVariables.kwd: an imp carries 1400 gold, a
 * swing mines 300 and a seam holds 3000
 */
class MiningGoldTest {

    private static final int IMP_CAPACITY = 1400;
    private static final int SWING = 300;
    private static final int SEAM_GOLD = 3000;

    @Test
    void impTakesOnlyWhatItCanCarry() {
        Seam seam = new Seam(SEAM_GOLD);

        // Four swings in, the fifth used to drop 100 on the floor
        int taken = MapController.mineGold(seam.tile(), SWING, MapController.getFreeGoldCapacity(imp(1200)));

        assertEquals(200, taken);
        assertEquals(SEAM_GOLD - 200, seam.gold);
    }

    @Test
    void fullImpTakesNothing() {
        Seam seam = new Seam(SEAM_GOLD);

        assertEquals(0, MapController.mineGold(seam.tile(), SWING, MapController.getFreeGoldCapacity(imp(IMP_CAPACITY))));
        assertEquals(SEAM_GOLD, seam.gold);
    }

    @Test
    void lastSwingTakesWhatIsLeft() {
        Seam seam = new Seam(100);

        assertEquals(100, MapController.mineGold(seam.tile(), SWING, MapController.getFreeGoldCapacity(imp(0))));
        assertEquals(0, seam.gold);
    }

    @Test
    void miningWithoutAMinerIsNotLimited() {
        assertEquals(Integer.MAX_VALUE, MapController.getFreeGoldCapacity(null));
    }

    @Test
    void wholeSeamGoesToTheTreasuryWithoutLeftovers() {
        Seam seam = new Seam(SEAM_GOLD);
        int carried = 0;
        int delivered = 0;
        int swings = 0;
        while (seam.gold > 0) {
            carried += MapController.mineGold(seam.tile(), SWING, MapController.getFreeGoldCapacity(imp(carried)));
            assertTrue(carried <= IMP_CAPACITY, "Carries " + carried);

            // The imp only leaves for the treasury when full
            if (carried == IMP_CAPACITY) {
                delivered += carried;
                carried = 0;
            }
            assertTrue(++swings < 100, "Stuck mining");
        }
        delivered += carried;

        assertEquals(SEAM_GOLD, delivered);
    }

    private static ICreatureController imp(int gold) {
        return (ICreatureController) Proxy.newProxyInstance(ICreatureController.class.getClassLoader(),
                new Class<?>[]{ICreatureController.class}, (proxy, method, args) -> switch (method.getName()) {
            case "getGold" ->
                gold;
            case "getMaxGold" ->
                IMP_CAPACITY;
            default ->
                throw new UnsupportedOperationException(method.getName());
        });
    }

    private static final class Seam {

        private int gold;

        private Seam(int gold) {
            this.gold = gold;
        }

        private IMapTileController tile() {
            return (IMapTileController) Proxy.newProxyInstance(IMapTileController.class.getClassLoader(),
                    new Class<?>[]{IMapTileController.class}, (proxy, method, args) -> switch (method.getName()) {
                case "getGold" ->
                    gold;
                case "setGold" -> {
                    gold = (int) args[0];
                    yield null;
                }
                default ->
                    throw new UnsupportedOperationException(method.getName());
            });
        }
    }

}
