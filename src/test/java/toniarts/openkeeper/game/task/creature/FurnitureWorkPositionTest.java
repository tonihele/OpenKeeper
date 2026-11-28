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

import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.simsilica.es.EntityId;
import java.lang.reflect.Proxy;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.Casino.PayOut;
import toniarts.openkeeper.game.controller.IObjectsController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.object.IObjectController;
import toniarts.openkeeper.game.controller.room.CasinoController;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.tools.convert.map.CasinoVariableFixtures;
import toniarts.openkeeper.tools.convert.map.CreatureFixtures;
import toniarts.openkeeper.tools.convert.map.GameObjectFixtures;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable.MiscType;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Where a creature stands to use a piece of room furniture: like DK II, half
 * the furniture's width plus half the creature's width from it
 */
class FurnitureWorkPositionTest {

    private static final float DELTA = 0.0001f;
    private static final float WARLOCK = 0.185f;
    private static final float BILE_DEMON = 0.425f;
    private static final float GOBLIN = 0.16f;
    private static final float BOOK_CASE = 0.65f;
    private static final float WORK_BENCH = 0.8f;
    private static final float ANVIL = 0.55f;
    private static final float ROULETTE_TABLE = 0.8f;
    private static final float BAR_TABLE = 0.6f;
    private static final float ALCOVE_BARRELS = 0.6f;
    private static final float WALL_DISTANCE = 0x700 / 4096f;

    private final Map<EntityId, IObjectController> objects = new HashMap<>();
    private final Set<EntityId> floor = new HashSet<>();
    private final Set<EntityId> wall = new HashSet<>();

    @Test
    void researcherStandsInFrontOfTheBookcase() {

        // A bookcase on an even row faces north, on an odd row south
        addFloor(64, BOOK_CASE, 2, 2);
        addFloor(64, BOOK_CASE, 2, 3);

        assertAt(2, 2 - (BOOK_CASE + WARLOCK) / 2, research(2, 2).getWorkPosition(creature(WARLOCK)));
        assertAt(2, 3 + (BOOK_CASE + WARLOCK) / 2, research(2, 3).getWorkPosition(creature(WARLOCK)));
        assertAt(2, 2 - (BOOK_CASE + BILE_DEMON) / 2, research(2, 2).getWorkPosition(creature(BILE_DEMON)));
    }

    @Test
    void researcherWalksToTheTileItStandsOn() {
        addFloor(64, BOOK_CASE, 2, 2);

        // The warlock stands on the bookcase's tile, the bigger bile demon on the tile in front of it
        assertEquals(new Vector2f(2, 2), research(2, 2).getTarget(creature(WARLOCK)));
        assertEquals(new Vector2f(2, 1), research(2, 2).getTarget(creature(BILE_DEMON)));
    }

    @Test
    void researcherReadsAWallBookFromAQuarterTileOffTheMiddleOfTheTile() {
        addWall(58, 0.1f, new Vector3f(4, 0, 1 - WALL_DISTANCE));
        Research research = research(4, 1);

        assertAt(4, 0.75f, research.getWorkPosition(creature(WARLOCK)));
        assertEquals(new Point(4, 0), research.getFaceLocation());
    }

    @Test
    void workerStandsAtTheBenchOnTheSideSetByTheTile() {

        // The tile's random number puts the worker north-east of this bench
        addFloor(46, WORK_BENCH, 3, 4);
        float north = FastMath.cos(FastMath.QUARTER_PI);

        for (float width : new float[]{WARLOCK, BILE_DEMON}) {
            float distance = (WORK_BENCH + width) / 2;
            assertAt(3 + north * distance, 4 - north * distance, manufacture(3, 4).getWorkPosition(creature(width)));
        }
    }

    @Test
    void workerStandsInFrontOfAWallAnvil() {

        // Against the west wall the anvil faces east, the worker stands a sixteenth of a turn north of that
        addWall(15, ANVIL, new Vector3f(1 - WALL_DISTANCE, 0, 3));
        float distance = (ANVIL + BILE_DEMON) / 2;
        Manufacture manufacture = manufacture(1, 3);
        assertAt(1 - WALL_DISTANCE + FastMath.cos(FastMath.PI / 8) * distance, 3 - FastMath.sin(FastMath.PI / 8) * distance,
                manufacture.getWorkPosition(creature(BILE_DEMON)));
        assertEquals(new Point(0, 3), manufacture.getFaceLocation());
    }

    @Test
    void gamblerStandsOnACircleRoundTheTable() {
        addFloor(67, BAR_TABLE, 5, 5);

        for (int seed = 0; seed < 20; seed++) {
            Vector2f position = gamble(5, 5, seed).getWorkPosition(creature(GOBLIN));
            assertEquals(1.1f * (BAR_TABLE + GOBLIN) / 2, position.distance(new Vector2f(5, 5)), DELTA);
        }
    }

    @Test
    void gamblerStandsAtASideOfTheRouletteTableWithoutAWheel() {
        addFloor(CasinoController.ROULETTE_TABLE_OBJECT_ID, ROULETTE_TABLE, 5, 5);

        for (int seed = 0; seed < 20; seed++) {
            Vector2f position = gamble(5, 5, seed).getWorkPosition(creature(BILE_DEMON));
            assertEquals(1.1f * (ROULETTE_TABLE + BILE_DEMON) / 2, Math.abs(position.x - 5), DELTA);
            assertEquals(5, position.y, DELTA);
        }
    }

    @Test
    void gamblerDrinksInFrontOfTheAlcoveBarrels() {
        Vector3f barrels = new Vector3f(1 - WALL_DISTANCE, 0, 5);
        addWall(68, ALCOVE_BARRELS, barrels);

        for (int seed = 0; seed < 20; seed++) {
            Vector2f position = gamble(1, 5, seed).getWorkPosition(creature(GOBLIN));
            float east = position.x - barrels.x;
            assertTrue(east >= Math.abs(position.y - barrels.z) - DELTA, "Within an eighth of a turn of east");
            assertEquals(1.1f * (ALCOVE_BARRELS + GOBLIN) / 2, position.distance(new Vector2f(barrels.x, barrels.z)), DELTA);
        }
    }

    @Test
    void gamblersAtNeighbouringTablesDoNotShareASpot() {

        // The Casino puts its tables diagonally next to each other
        addFloor(67, BAR_TABLE, 5, 5);
        addFloor(59, 0.46f, 6, 6);

        for (int seed = 0; seed < 20; seed++) {
            Vector2f first = gamble(5, 5, seed).getWorkPosition(creature(GOBLIN));
            Vector2f second = gamble(6, 6, seed + 100).getWorkPosition(creature(GOBLIN));
            assertTrue(first.distance(second) > GOBLIN, "The gamblers stand apart");
        }
    }

    private static void assertAt(float x, float y, Vector2f position) {
        assertEquals(x, position.x, DELTA);
        assertEquals(y, position.y, DELTA);
    }

    private Research research(int x, int y) {
        return new Research(null, null, new Point(x, y), (short) 3, room(), null, null, objectsController());
    }

    private Manufacture manufacture(int x, int y) {
        return new Manufacture(null, null, new Point(x, y), (short) 3, room(), null, null, objectsController(), null);
    }

    private Gamble gamble(int x, int y, int seed) {
        Map<MiscType, MiscVariable> gameSettings = new EnumMap<>(MiscType.class);
        for (MiscType type : new MiscType[]{MiscType.GAMBLING_MODIFY_CREATURE_MONEY_SMILES_PER_SECOND,
            MiscType.GAMBLING_MODIFY_CREATURE_MONEY_MONEY_PER_SECOND, MiscType.GAMBLING_MODIFY_CREATURE_ANGER_SMILES_PER_SECOND,
            MiscType.GAMBLING_MODIFY_CREATURE_ANGER_MONEY_PER_SECOND, MiscType.GAMBLING_CREATURE_JACKPOT_CHANCE_SMILES_PER_SECOND,
            MiscType.GAMBLING_CREATURE_JACKPOT_CHANCE_MONEY_PER_SECOND, MiscType.GAMBLING_JACKPOT_BASIC_AMOUNT,
            MiscType.GAMBLING_JACKPOT_PAY_TIME_LIMIT, MiscType.MAX_GOLD_PILE_OUTSIDE_TREASURY}) {
            gameSettings.put(type, CasinoVariableFixtures.miscVariable(type, 1));
        }
        return new Gamble(null, null, null, new Point(x, y), (short) 3, room(), null, null, null, objectsController(),
                gameSettings, () -> PayOut.SMILES, new Random(seed));
    }

    private void addFloor(int objectId, float width, int x, int y) {
        floor.add(add(objectId, width, new Vector3f(x, 0, y)));
    }

    private void addWall(int objectId, float width, Vector3f position) {
        wall.add(add(objectId, width, position));
    }

    private EntityId add(int objectId, float width, Vector3f position) {
        EntityId entityId = new EntityId(objects.size() + 1);
        objects.put(entityId, proxy(IObjectController.class, (proxy, method, args) -> switch (method.getName()) {
            case "getEntityId" ->
                entityId;
            case "getPosition" ->
                position;
            case "getObject" ->
                GameObjectFixtures.furniture((short) objectId, width);
            default ->
                throw new UnsupportedOperationException(method.getName());
        }));
        return entityId;
    }

    private IRoomController room() {
        return proxy(IRoomController.class, (proxy, method, args) -> switch (method.getName()) {
            case "getFloorFurniture" ->
                floor;
            case "getWallFurniture" ->
                wall;
            default ->
                throw new UnsupportedOperationException(method.getName());
        });
    }

    private IObjectsController objectsController() {
        return proxy(IObjectsController.class, (proxy, method, args) -> switch (method.getName()) {
            case "createController" ->
                objects.get((EntityId) args[0]);
            default ->
                throw new UnsupportedOperationException(method.getName());
        });
    }

    private static ICreatureController creature(float width) {
        return proxy(ICreatureController.class, (proxy, method, args) -> switch (method.getName()) {
            case "getCreature" ->
                CreatureFixtures.creatureWithWidth(width);
            default ->
                throw new UnsupportedOperationException(method.getName());
        });
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, java.lang.reflect.InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler);
    }

}
