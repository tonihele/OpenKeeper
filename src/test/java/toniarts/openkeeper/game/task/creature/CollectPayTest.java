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

import com.badlogic.gdx.ai.steer.SteeringAcceleration;
import com.badlogic.gdx.math.Vector2;
import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.CreaturePay;
import toniarts.openkeeper.game.component.Gold;
import toniarts.openkeeper.game.controller.IGameWorldController;
import toniarts.openkeeper.game.controller.creature.CreatureController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.navigation.steering.EntitySteeringFactory;
import toniarts.openkeeper.game.navigation.steering.SteerableEntity;
import toniarts.openkeeper.game.navigation.steering.SteeringUtils;
import toniarts.openkeeper.game.task.ITaskManager;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.PayDayFixtures;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable.MiscType;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Creatures walk to a gold room to collect their pay
 */
class CollectPayTest {

    private static final short KEEPER = Player.KEEPER1_ID;
    private static final int WAGE = 500;
    private static final int REEVALUATION = 4;
    private static final float DELTA = 0.0001f;

    private final EntityData entityData = new DefaultEntityData();
    private final EntityId goblin = entityData.createEntity();
    private int goldInRoom;
    private boolean goldRoomFound;
    private double gameTime;

    CollectPayTest() {
        entityData.setComponents(goblin, new Gold(0, 1000), new CreaturePay(WAGE, 0, 0, false));
    }

    @Test
    void creatureTakesItsPayFromTheRoomAndKeepsHalf() {
        goldInRoom = 1000;
        CollectPay task = task();
        assertTrue(task.isValid(null));

        task.executeTask(creature(), 0);

        assertEquals(500, goldInRoom);
        assertEquals(WAGE / 2, entityData.getComponent(goblin, Gold.class).gold);
        assertNull(entityData.getComponent(goblin, CreaturePay.class));
        assertFalse(task.isValid(null));
    }

    @Test
    void creatureTakesWhatTheRoomHasAndIsOwedTheRest() {
        goldInRoom = 200;

        task().executeTask(creature(), 0);

        assertEquals(0, goldInRoom);
        assertEquals(100, entityData.getComponent(goblin, Gold.class).gold);
        assertEquals(300, pay().unpaid);
        assertEquals(0, pay().attempts);
    }

    @Test
    void comingAwayWithNothingCountsAsAFailedTry() {
        goldInRoom = 0;

        task().executeTask(creature(), 0);

        assertEquals(WAGE, pay().unpaid);
        assertEquals(1, pay().attempts);
    }

    @Test
    void creatureThatIsOwedGoesToAGoldRoom() {
        goldRoomFound = true;

        assertTrue(controller().goToCollectPay());
        assertEquals(0, pay().attempts);
    }

    @Test
    void creatureThatIsOwedNothingStays() {
        goldRoomFound = true;
        entityData.removeComponent(goblin, CreaturePay.class);

        assertFalse(controller().goToCollectPay());
    }

    @Test
    void creatureThatFindsNoGoldTriesTwiceThenGivesUp() {
        CreatureController controller = controller();

        assertFalse(controller.goToCollectPay());
        assertEquals(1, pay().attempts);

        // Not before the reevaluation delay has passed
        gameTime = REEVALUATION - 1;
        assertFalse(controller.goToCollectPay());
        assertEquals(1, pay().attempts);

        gameTime = REEVALUATION;
        assertFalse(controller.goToCollectPay());
        assertEquals(2, pay().attempts);

        // It has given up, even when there is gold again
        goldRoomFound = true;
        gameTime = 2 * REEVALUATION;
        assertFalse(controller.goToCollectPay());
    }

    @Test
    void creatureStandsOnASpotRoundTheGoldAndKeepsIt() {

        // The 8 spots of a 3 by 3 grid on the tile, a third of a tile apart, without the middle (DKII.EXE 0x698C20)
        Set<Vector2f> expected = new HashSet<>();
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                if (x != 0 || y != 0) {
                    expected.add(new Vector2f(x, y));
                }
            }
        }

        Set<Vector2f> spots = new HashSet<>();
        for (int i = 0; i < 200; i++) {
            CollectPay task = task();
            Vector2f position = task.getWorkPosition(null);
            assertEquals(position, task.getWorkPosition(null));

            Vector2f thirds = position.subtract(3, 3).multLocal(3);
            Vector2f spot = new Vector2f(Math.round(thirds.x), Math.round(thirds.y));
            assertEquals(0, thirds.distance(spot), DELTA);
            spots.add(spot);
        }
        assertEquals(expected, spots);
    }

    @Test
    void creatureTurnsToFaceTheGold() {
        for (int i = 0; i < 50; i++) {
            CollectPay task = task();
            Vector2f position = task.getWorkPosition(null);
            assertTrue(task.isFaceTarget());

            // On its spot it turns until it faces the middle of the tile (DKII.EXE 0x472690)
            float towardsGold = SteeringUtils.calculateVectorToAngle(new Vector2(3 - position.x, 3 - position.y));
            assertEquals(0, turnOnTheSpot(task, position, towardsGold), DELTA);
            assertTrue(Math.abs(turnOnTheSpot(task, position, towardsGold + FastMath.PI)) > DELTA);
        }
    }

    /**
     * How the creature turns, standing on its spot at the end of its walk
     */
    private float turnOnTheSpot(CollectPay task, Vector2f position, float orientation) {
        SteerableEntity steerable = new SteerableEntity(goblin, 1, 0.25f, position.x, position.y, orientation);
        List<Vector2> path = List.of(new Vector2(6, 3), new Vector2(position.x, position.y));
        SteeringAcceleration<Vector2> steering = new SteeringAcceleration<>(new Vector2());
        EntitySteeringFactory.navigateToPoint(path, task.getTaskLocation(), steerable, task.getTaskLocation(),
                v -> 0.0, v -> false, false, false).calculateSteering(steering);
        return steering.angular;
    }

    private CreaturePay pay() {
        return entityData.getComponent(goblin, CreaturePay.class);
    }

    private CollectPay task() {
        IRoomController room = stub(IRoomController.class, (name, args) -> switch (name) {
            case "isRemoved", "isDestroyed" -> false;
            case "getOwnerId" -> KEEPER;
            default -> throw new UnsupportedOperationException(name);
        });
        IGameWorldController gameWorldController = stub(IGameWorldController.class, (name, args) -> {
            if (!"substractGold".equals(name) || args.length != 3) {
                throw new UnsupportedOperationException(name);
            }
            int amount = (Integer) args[0];
            int taken = Math.min(amount, goldInRoom);
            goldInRoom -= taken;
            return amount - taken;
        });

        return new CollectPay(null, null, new Point(3, 3), KEEPER, room, gameWorldController, entityData);
    }

    private ICreatureController creature() {
        return stub(ICreatureController.class, (name, args) -> goblin);
    }

    private CreatureController controller() {
        ITaskManager taskManager = stub(ITaskManager.class, (name, args) -> goldRoomFound);

        return new CreatureController(goblin, entityData, new Creature(), null, taskManager, () -> gameTime,
                Map.of(MiscType.DELAY_BEFORE_PAY_REEVALUATION_SECONDS,
                        PayDayFixtures.payDayVariable(MiscType.DELAY_BEFORE_PAY_REEVALUATION_SECONDS, REEVALUATION)),
                null, null, null, null, null, null, null);
    }

    private interface Answer {

        Object answer(String methodName, Object[] args);
    }

    @SuppressWarnings("unchecked")
    private static <T> T stub(Class<T> type, Answer answer) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (proxy, method, args) -> answer.answer(method.getName(), args));
    }

}
