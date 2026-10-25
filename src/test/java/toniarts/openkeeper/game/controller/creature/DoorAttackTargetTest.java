/*
 * Copyright (C) 2014-2016 OpenKeeper
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

import com.badlogic.gdx.ai.pfa.DefaultGraphPath;
import com.badlogic.gdx.ai.pfa.GraphPath;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.DoorComponent;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DoorAttackTargetTest {

    private static final short WOODEN_DOOR = 1;

    private final EntityData entityData = new DefaultEntityData();
    private EntityId door;

    @BeforeEach
    void setUp() {
        door = entityData.createEntity();
        entityData.setComponents(door, new DoorComponent(WOODEN_DOOR, false, false), new Health(2000, 2000));
    }

    @Test
    void aStandingDoorCanBeAttacked() {
        assertTrue(DoorAttackTarget.isBreakable(entityData, door));
    }

    @Test
    void aBlueprintIsNotADoorYet() {
        entityData.setComponent(door, new DoorComponent(WOODEN_DOOR, false, true));

        assertFalse(DoorAttackTarget.isBreakable(entityData, door));
    }

    @Test
    void aDoorWithNoHealthLeftIsGone() {
        entityData.setComponent(door, new Health(0, 2000));

        assertFalse(DoorAttackTarget.isBreakable(entityData, door));
    }

    @Test
    void somethingThatIsNotADoorIsNotAttacked() {
        EntityId other = entityData.createEntity();
        entityData.setComponent(other, new Health(100, 100));

        assertFalse(DoorAttackTarget.isBreakable(entityData, other));
    }

    @Test
    void fightersBreakEnemyDoorsOnly() {
        assertTrue(DoorAttackTarget.canBreak(entityData, door, Creature.FightStyle.BLITZER, true));
        assertTrue(DoorAttackTarget.canBreak(entityData, door, Creature.FightStyle.BLOCKER, true));
        assertFalse(DoorAttackTarget.canBreak(entityData, door, Creature.FightStyle.BLITZER, false));
    }

    @Test
    void workersLeaveDoorsAlone() {
        assertFalse(DoorAttackTarget.canBreak(entityData, door, Creature.FightStyle.NON_FIGHTER, true));
    }

    @Test
    void theFirstDoorOnThePathIsTheOneToBreak() {
        List<Point> path = List.of(new Point(1, 1), new Point(1, 2), new Point(1, 3), new Point(1, 4), new Point(1, 5));

        int index = DoorAttackTarget.getDoorIndex(path, p -> p.y == 3 || p.y == 5);

        assertEquals(2, index);
    }

    @Test
    void aDoorNextToUsIsTheAdjacentDoor() {
        List<Point> path = List.of(new Point(1, 1), new Point(1, 2), new Point(1, 3));

        assertEquals(DoorAttackTarget.ADJACENT_DOOR_INDEX, DoorAttackTarget.getDoorIndex(path, p -> p.y == 2));
    }

    @Test
    void theTileWeStandOnIsNotLookedAt() {
        List<Point> path = List.of(new Point(1, 1), new Point(1, 2));

        assertEquals(-1, DoorAttackTarget.getDoorIndex(path, p -> p.y == 1));
    }

    @Test
    void aPathWithoutDoorsHasNoDoorToBreak() {
        List<Point> path = List.of(new Point(1, 1), new Point(1, 2), new Point(1, 3));

        assertEquals(-1, DoorAttackTarget.getDoorIndex(path, p -> false));
    }

    @Test
    void theWalkStopsBeforeTheDoor() {
        GraphPath<IMapTileInformation> path = new DefaultGraphPath<>();
        for (int y = 1; y <= 5; y++) {
            path.add(tile(1, y));
        }

        GraphPath<IMapTileInformation> shortPath = DoorAttackTarget.stopBefore(path, 3);

        assertEquals(3, shortPath.getCount());
        assertEquals(new Point(1, 3), shortPath.get(2).getLocation());
    }

    @Test
    void anIdleFighterFindsTheDoorOnTheTileNextToIt() {
        EntityId found = DoorAttackTarget.findAdjacent(new Point(5, 5),
                p -> p.equals(new Point(5, 6)) ? door : null, d -> true);

        assertEquals(door, found);
    }

    @Test
    void aDoorOnTheDiagonalIsNotNextToUs() {
        EntityId found = DoorAttackTarget.findAdjacent(new Point(5, 5),
                p -> p.equals(new Point(6, 6)) ? door : null, d -> true);

        assertNull(found);
    }

    @Test
    void aDoorThatCantBeBrokenIsLeftAlone() {
        EntityId found = DoorAttackTarget.findAdjacent(new Point(5, 5),
                p -> p.equals(new Point(4, 5)) ? door : null, d -> false);

        assertNull(found);
    }

    @Test
    void noDoorWithoutATile() {

        // A creature in the keeper's hand has no position
        EntityId found = DoorAttackTarget.findAdjacent(null, p -> door, d -> true);

        assertNull(found);
    }

    @Test
    void onlyTheKeepersCreaturesGoForDoorsOnTheirOwn() {
        assertTrue(DoorAttackTarget.attacksAdjacentWhenIdle((short) 3));
        assertFalse(DoorAttackTarget.attacksAdjacentWhenIdle(Player.GOOD_PLAYER_ID));
        assertFalse(DoorAttackTarget.attacksAdjacentWhenIdle(Player.NEUTRAL_PLAYER_ID));
    }

    private static IMapTileInformation tile(int x, int y) {
        Point location = new Point(x, y);
        return (IMapTileInformation) Proxy.newProxyInstance(IMapTileInformation.class.getClassLoader(),
                new Class<?>[]{IMapTileInformation.class}, (proxy, method, args) -> switch (method.getName()) {
            case "getLocation" -> location;
            case "getX" -> x;
            case "getY" -> y;
            default -> throw new UnsupportedOperationException(method.getName());
        });
    }

}
