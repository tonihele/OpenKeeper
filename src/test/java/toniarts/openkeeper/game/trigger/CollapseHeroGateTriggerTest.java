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
package toniarts.openkeeper.game.trigger;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.tools.convert.map.CollapseHeroGateFixtures;
import toniarts.openkeeper.tools.convert.map.HeroGateFixtures;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.tools.convert.map.Trigger;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The COLLAPSE_HERO_GATE script action collapses the hero gate on the tile
 */
class CollapseHeroGateTriggerTest {

    private static final int ACTION_ID = 65393;

    private final Map<Integer, Trigger> triggers = new HashMap<>();
    private final Map<Point, IRoomController> rooms = new HashMap<>();
    private final List<Room> collapsed = new ArrayList<>();

    @Test
    void collapsesTheTwoByTwoHeroGateOnTheTile() {
        Room gate = HeroGateFixtures.heroGateRoom((short) 20, "Hero Gate [ 2x2 ]", Room.TileConstruction.HERO_GATE_2_BY_2);
        rooms.put(new Point(45, 14), room(gate));

        // level3 names the tile 1-based
        runAction(46, 15);

        assertEquals(List.of(gate), collapsed);
    }

    @Test
    void collapsesTheThreeByOneHeroGate() {
        Room gate = HeroGateFixtures.heroGateRoom((short) 23, "Hero Gate [ 3x1 ]", Room.TileConstruction.HERO_GATE_3_BY_1);
        rooms.put(new Point(33, 35), room(gate));

        runAction(34, 36);

        assertEquals(List.of(gate), collapsed);
    }

    @Test
    void collapsesTheFinalHeroGate() {
        Room gate = HeroGateFixtures.heroGateRoom((short) 18, "Hero Gate [ Final ]", Room.TileConstruction.HERO_GATE);
        rooms.put(new Point(49, 6), room(gate));

        runAction(50, 7);

        assertEquals(List.of(gate), collapsed);
    }

    @Test
    void leavesOtherRoomsAlone() {
        Room treasury = HeroGateFixtures.heroGateRoom((short) 1, "Treasury", Room.TileConstruction.NORMAL);
        rooms.put(new Point(45, 14), room(treasury));

        runAction(46, 15);

        assertTrue(collapsed.isEmpty());
    }

    @Test
    void aTileWithoutARoomCollapsesNothing() {

        // One of level7's is on a wall
        runAction(36, 42);

        assertTrue(collapsed.isEmpty());
    }

    private void runAction(int posX, int posY) {
        triggers.put(ACTION_ID, CollapseHeroGateFixtures.collapseHeroGate(ACTION_ID, posX, posY));
        IKwdFile kwdFile = (IKwdFile) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{IKwdFile.class},
                (proxy, method, args) -> method.getName().equals("getTriggers") ? triggers : null);
        ILevelInfo levelInfo = (ILevelInfo) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{ILevelInfo.class},
                (proxy, method, args) -> method.getName().equals("getLevelData") ? kwdFile : null);
        IMapController mapController = (IMapController) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{IMapController.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getRoomControllerByCoordinates")) {
                        return rooms.get((Point) args[0]);
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
        IGameTimer gameTimer = () -> 0;

        new TriggerControl(null, levelInfo, gameTimer, mapController, null, null, ACTION_ID).update(0.25f);
    }

    private IRoomController room(Room room) {
        return (IRoomController) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{IRoomController.class},
                (proxy, method, args) -> switch (method.getName()) {
            case "getRoom" ->
                room;
            case "destroy" -> {
                collapsed.add(room);
                yield null;
            }
            default ->
                throw new UnsupportedOperationException(method.getName());
        });
    }

}
