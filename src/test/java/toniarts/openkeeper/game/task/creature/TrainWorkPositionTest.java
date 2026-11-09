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
import java.util.Map;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.map.FakeMapData;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.tools.convert.map.TrainingDummyFixtures;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable.MiscType;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Where a trainee stands at the training dummy
 */
class TrainWorkPositionTest {

    private static final Point DUMMY = new Point(2, 2);

    @Test
    void traineeStandsWithinReachOfTheDummy() {
        ICreatureController goblin = creatureAt(new Vector3f(2, 0, 0));
        Train task = train();

        // DK II puts the trainee 0x7FF / 4096 of a tile from the dummy in a random direction, a whole tile is out
        // of a goblin's reach. The direction stays once picked
        assertEquals(new Vector2f(2, 1), task.getTarget(goblin));
        assertEquals(0x7FF / 4096f, task.getWorkPosition(goblin).distance(new Vector2f(DUMMY.x, DUMMY.y)), 0.0001f);
        assertEquals(task.getWorkPosition(goblin), task.getWorkPosition(goblin));
        assertTrue(task.isFaceTarget());
    }

    private static Train train() {
        Map<MiscType, MiscVariable> settings = Map.of(
                MiscType.TRAINING_ROOM_MAX_EXPERIENCE_LEVEL, TrainingDummyFixtures.trainingVariable(MiscType.TRAINING_ROOM_MAX_EXPERIENCE_LEVEL, 4),
                MiscType.MODIFY_PLAYER_GOLD_WHILE_TRAINING_PER_SECOND, TrainingDummyFixtures.trainingVariable(MiscType.MODIFY_PLAYER_GOLD_WHILE_TRAINING_PER_SECOND, -10));
        return new Train(navigation(), map(), DUMMY, (short) 3, null, null, null, settings, null, null, null);
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
        return (INavigationService) Proxy.newProxyInstance(
                INavigationService.class.getClassLoader(), new Class<?>[]{INavigationService.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "isAccessible" -> true;
                    case "findPath" -> new DefaultGraphPath<>();
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

}
