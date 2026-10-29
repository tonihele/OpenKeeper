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
package toniarts.openkeeper.game.controller.creature;

import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.Gold;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.IObjectsController;
import toniarts.openkeeper.tools.convert.map.Variable;
import toniarts.openkeeper.tools.convert.map.VariableFixtures;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A worker that can't take its gold to the treasury drops it on the floor, so
 * that a full imp at a gold seam doesn't keep mining nothing forever
 */
class CreatureDropGoldTest {

    private static final short PLAYER_ID = 3;
    private static final Point TILE = new Point(5, 7);
    private static final int MAX_PILE = 2000;

    private final EntityData entityData = new DefaultEntityData();
    private final List<int[]> droppedGold = new ArrayList<>();

    @Test
    void fullWorkerDropsAllItsGoldWhereItStands() {
        CreatureController imp = worker(1400, 1400);

        imp.dropGold();

        assertEquals(0, imp.getGold());
        assertEquals(1, droppedGold.size());
        assertEquals(TILE.x, droppedGold.get(0)[0]);
        assertEquals(TILE.y, droppedGold.get(0)[1]);
        assertEquals(1400, droppedGold.get(0)[2]);
        assertEquals(MAX_PILE, droppedGold.get(0)[3]);
    }

    @Test
    void workerCanMineAgainAfterDroppingItsGold() {
        CreatureController imp = worker(1400, 1400);

        imp.dropGold();
        imp.addGold(300);

        assertEquals(300, imp.getGold());
        assertEquals(1, droppedGold.size());
    }

    @Test
    void emptyWorkerDropsNothing() {
        CreatureController imp = worker(0, 1400);

        imp.dropGold();

        assertEquals(0, imp.getGold());
        assertTrue(droppedGold.isEmpty());
    }

    private CreatureController worker(int gold, int maxGold) {
        EntityId entityId = entityData.createEntity();
        entityData.setComponent(entityId, new Gold(gold, maxGold));
        entityData.setComponent(entityId, new Owner(PLAYER_ID, PLAYER_ID));
        entityData.setComponent(entityId, new Position(0, WorldUtils.pointToVector3f(TILE)));

        Map<Variable.MiscVariable.MiscType, Variable.MiscVariable> gameSettings = new EnumMap<>(Variable.MiscVariable.MiscType.class);
        gameSettings.put(Variable.MiscVariable.MiscType.MAX_GOLD_PILE_OUTSIDE_TREASURY,
                VariableFixtures.miscVariable(Variable.MiscVariable.MiscType.MAX_GOLD_PILE_OUTSIDE_TREASURY, MAX_PILE));

        return new CreatureController(entityId, entityData, null, null, null, null, gameSettings, null, null, null, level(),
                looseGoldRecorder(), null, null);
    }

    /**
     * Stands in for the level, dropping gold doesn't need anything from it
     */
    private static ILevelInfo level() {
        return (ILevelInfo) Proxy.newProxyInstance(ILevelInfo.class.getClassLoader(),
                new Class<?>[]{ILevelInfo.class}, (proxy, method, args) -> null);
    }

    /**
     * Stands in for the objects controller: records the loose gold piles
     */
    private IObjectsController looseGoldRecorder() {
        return (IObjectsController) Proxy.newProxyInstance(IObjectsController.class.getClassLoader(),
                new Class<?>[]{IObjectsController.class}, (proxy, method, args) -> {
                    if (!method.getName().equals("addLooseGold")) {
                        throw new UnsupportedOperationException(method.getName());
                    }
                    droppedGold.add(new int[]{(int) args[1], (int) args[2], (int) args[3], (int) args[4]});
                    return entityData.createEntity();
                });
    }

}
