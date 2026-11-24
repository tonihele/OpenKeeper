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
package toniarts.openkeeper.game.logic;

import com.jme3.math.Vector3f;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.Set;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.CreatureMeleeAttack;
import toniarts.openkeeper.game.component.CreatureViewState;
import toniarts.openkeeper.game.component.ObjectViewState;
import toniarts.openkeeper.game.component.ObjectViewState.GameObjectAnimState;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.TaskComponent;
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.task.TaskType;
import toniarts.openkeeper.tools.convert.map.Creature.AnimationType;
import toniarts.openkeeper.tools.convert.map.GameObject;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.TrainingDummyFixtures;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

/**
 * The training dummy recoils from the trainee's blows
 */
class TrainingDummySystemTest {

    private static final float TICK = 0.25f;
    private static final short DUMMY_OBJECT = 14;
    private static final short GOBLIN = 7;
    private static final float GOBLIN_SWING = 1.3f;
    private static final Point DUMMY_TILE = new Point(2, 2);

    private final DefaultEntityData entityData = new DefaultEntityData();
    private final EntityId dummy = entityData.createEntity();
    private final double[] gameTime = new double[1];
    private final TrainingDummySystem system;

    TrainingDummySystemTest() {
        entityData.setComponents(dummy, new Position(0, new Vector3f(DUMMY_TILE.x, 0, DUMMY_TILE.y)),
                new ObjectViewState(DUMMY_OBJECT, GameObject.State.NONE, GameObjectAnimState.MESH_RESOURCE, true));

        // The Training Dummy's recoil, as in DK II's data: 20 frames at 30 fps
        GameObject dummyObject = TrainingDummyFixtures.trainingDummy(DUMMY_OBJECT, 20, 30);
        IKwdFile kwdFile = (IKwdFile) Proxy.newProxyInstance(
                IKwdFile.class.getClassLoader(), new Class<?>[]{IKwdFile.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getObject" -> ((Integer) args[0]) == DUMMY_OBJECT ? dummyObject : null;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        IRoomController trainingRoom = (IRoomController) Proxy.newProxyInstance(
                IRoomController.class.getClassLoader(), new Class<?>[]{IRoomController.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getFloorFurniture" -> Set.of(dummy);
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        IMapController mapController = (IMapController) Proxy.newProxyInstance(
                IMapController.class.getClassLoader(), new Class<?>[]{IMapController.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getRoomControllerByCoordinates" -> DUMMY_TILE.equals(args[0]) ? trainingRoom : null;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        IGameTimer gameTimer = () -> gameTime[0];
        system = new TrainingDummySystem(entityData, kwdFile, mapController, gameTimer);
    }

    @Test
    void dummyRecoilsWhenTheBlowLands() {
        trainee(AnimationType.MELEE_ATTACK);

        // DK II lands the blow at the end of the swing. The goblin is seen swinging on the first tick, at 0.25 seconds
        tick(1.5f);
        assertEquals(GameObjectAnimState.MESH_RESOURCE, dummyAnimation());
        tick(TICK);
        assertEquals(GameObjectAnimState.ADDITIONAL_RESOURCE_1, dummyAnimation());
    }

    @Test
    void dummyStandsStillAfterTheRecoil() {
        trainee(AnimationType.MELEE_ATTACK);
        tick(1.75f);

        // The recoil lasts 20 frames at 30 fps, 0.67 seconds
        tick(TICK * 2);
        assertEquals(GameObjectAnimState.ADDITIONAL_RESOURCE_1, dummyAnimation());
        tick(TICK);
        assertEquals(GameObjectAnimState.MESH_RESOURCE, dummyAnimation());
    }

    @Test
    void everyBlowRestartsTheRecoil() {
        trainee(AnimationType.MELEE_ATTACK);
        tick(1.75f);
        ObjectViewState firstBlow = entityData.getComponent(dummy, ObjectViewState.class);

        // The next swing lands 1.3 seconds after the first, at 2.85 seconds
        tick(1.0f);
        assertEquals(GameObjectAnimState.MESH_RESOURCE, dummyAnimation());
        tick(TICK);
        assertEquals(GameObjectAnimState.ADDITIONAL_RESOURCE_1, dummyAnimation());
        assertNotSame(firstBlow, entityData.getComponent(dummy, ObjectViewState.class));
    }

    @Test
    void walkingTraineeDoesNotHitTheDummy() {
        trainee(AnimationType.WALK);

        tick(5);
        assertEquals(GameObjectAnimState.MESH_RESOURCE, dummyAnimation());
    }

    private void trainee(AnimationType animation) {
        EntityId goblin = entityData.createEntity();
        entityData.setComponents(goblin,
                new TaskComponent(1, null, DUMMY_TILE, TaskType.TRAIN, 0, true),
                new CreatureViewState(GOBLIN, 0, animation),
                new CreatureMeleeAttack(0, 10, GOBLIN_SWING, 0.45f));
    }

    private void tick(float seconds) {
        for (int i = 0; i < Math.round(seconds / TICK); i++) {
            gameTime[0] += TICK;
            system.processTick(TICK);
        }
    }

    private GameObjectAnimState dummyAnimation() {
        return entityData.getComponent(dummy, ObjectViewState.class).animState;
    }

}
