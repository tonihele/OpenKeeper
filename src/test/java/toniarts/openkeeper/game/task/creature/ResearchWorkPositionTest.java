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

import com.badlogic.gdx.ai.pfa.DefaultGraphPath;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import java.lang.reflect.Proxy;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.map.FakeMapData;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Where a researcher stands at the library's bookcase
 */
class ResearchWorkPositionTest {

    private static final Point BOOKCASE = new Point(2, 2);

    @Test
    void researcherStandsInFrontOfTheBookcaseNotInIt() {
        ICreatureController warlock = creatureAt(new Vector3f(0, 0, 2));
        Research task = new Research(navigation(), map(), BOOKCASE, (short) 3, null, null, null, null);

        // The middle of the tile next to the bookcase, closest to the warlock, and from there halfway to the bookcase
        assertEquals(new Vector2f(1, 2), task.getTarget(warlock));
        assertEquals(new Vector2f(1.5f, 2), task.getWorkPosition(warlock));
        assertTrue(task.isFaceTarget());
    }

    @Test
    void researcherKeepsItsPlaceWhileWalkingThere() {
        Vector3f position = new Vector3f(0, 0, 2);
        ICreatureController warlock = creatureAt(position);
        Research task = new Research(navigation(), map(), BOOKCASE, (short) 3, null, null, null, null);
        Vector2f workPosition = task.getWorkPosition(warlock);

        // On its way round the bookcase another free tile is closer, the place stays
        position.set(2, 0, 4);
        assertEquals(workPosition, task.getWorkPosition(warlock));
    }

    private static ICreatureController creatureAt(Vector3f position) {
        return (ICreatureController) Proxy.newProxyInstance(
                ICreatureController.class.getClassLoader(), new Class<?>[]{ICreatureController.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getPosition" -> position;
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    private static IMapController map() {
        FakeMapData mapData = new FakeMapData(5, 5);
        return (IMapController) Proxy.newProxyInstance(
                IMapController.class.getClassLoader(), new Class<?>[]{IMapController.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getMapData" -> mapData;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    private static INavigationService navigation() {

        // An open library floor, the furniture doesn't block the way
        return (INavigationService) Proxy.newProxyInstance(
                INavigationService.class.getClassLoader(), new Class<?>[]{INavigationService.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "isAccessible" -> true;
                    case "findPath" -> new DefaultGraphPath<>();
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

}
