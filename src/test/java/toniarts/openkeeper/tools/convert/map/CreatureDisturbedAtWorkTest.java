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
package toniarts.openkeeper.tools.convert.map;

import com.badlogic.gdx.ai.fsm.DefaultStateMachine;
import com.badlogic.gdx.ai.fsm.StateMachine;
import com.jme3.math.FastMath;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.CreatureAi;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureMood;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.TaskComponent;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.creature.CreatureState;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.logic.CreatureDisturbanceSystem;
import toniarts.openkeeper.game.task.TaskType;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Like DKII, a creature at work in the Library or the Training Room is
 * angered by creatures that stand about in the room, and tells them off once
 * it is annoyed enough.
 */
class CreatureDisturbedAtWorkTest {

    private static final short LIBRARY = 6;
    private static final short TRAINING_ROOM = 7;
    private static final short WARLOCK = 5;

    private final EntityData entityData = new DefaultEntityData();
    private final Map<EntityId, StateMachine<ICreatureController, CreatureState>> stateMachines = new HashMap<>();
    private short roomId = LIBRARY;
    private double gameTime;

    @Test
    void researcherIsAngeredOnceASecondByACreatureStandingAbout() {
        EntityId researcher = createCreature(5, 5, TaskType.RESEARCH, true);
        createCreature(7, 6, null, false);
        CreatureDisturbanceSystem system = system();

        // Not on the ticks inside a second
        tick(system, 0.5);
        assertEquals(0, entityData.getComponent(researcher, CreatureMood.class).noWork);

        tick(system, 1);
        assertEquals(70 / 4, entityData.getComponent(researcher, CreatureMood.class).noWork);

        tick(system, 1.25);
        assertEquals(70 / 4, entityData.getComponent(researcher, CreatureMood.class).noWork);
        assertNull(stateMachines.get(researcher));
    }

    @Test
    void eachCreatureStandingAboutAngersTheResearcher() {
        EntityId researcher = createCreature(5, 5, TaskType.RESEARCH, true);
        createCreature(7, 6, null, false);
        createCreature(4, 5, TaskType.GO_TO_EAT, false);

        tick(system(), 1);

        assertEquals(2 * (70 / 4), entityData.getComponent(researcher, CreatureMood.class).noWork);
    }

    @Test
    void creaturesThatWorkInTheRoomOrAreFarOffDoNotDisturb() {
        EntityId researcher = createCreature(5, 5, TaskType.RESEARCH, true);

        // One on its way to its own bookcase, one 3 tiles away
        EntityId arriving = createCreature(6, 5, TaskType.RESEARCH, false);
        createCreature(8, 5, null, false);

        tick(system(), 1);

        assertEquals(0, entityData.getComponent(researcher, CreatureMood.class).noWork);
        assertEquals(0, entityData.getComponent(arriving, CreatureMood.class).noWork);
    }

    @Test
    void creatureOnItsWayToTheLibraryDisturbsTheTrainees() {
        roomId = TRAINING_ROOM;
        EntityId trainee = createCreature(5, 5, TaskType.TRAIN, true);
        createCreature(6, 5, TaskType.RESEARCH, false);

        tick(system(), 1);

        assertEquals(70 / 4, entityData.getComponent(trainee, CreatureMood.class).noWork);
    }

    @Test
    void annoyedTraineeTellsTheNearestCreatureOff() {
        roomId = TRAINING_ROOM;
        EntityId trainee = createCreature(5, 5, TaskType.TRAIN, true);
        createCreature(5, 7, null, false);
        createCreature(6, 5, null, false);

        // Over a tenth of the unhappy level in other anger
        int unhappy = CreatureMood.toRuntimeThreshold(50);
        entityData.setComponent(trainee, new CreatureMood(0, 0, 0, 0, 0, unhappy / 10 + 1));
        CreatureDisturbanceSystem system = system();

        tick(system, 1);

        // It turns to the nearest one instead of getting the anger for that second
        assertEquals(CreatureState.TELLING_OFF, stateMachines.get(trainee).getCurrentState());
        assertEquals(FastMath.HALF_PI, entityData.getComponent(trainee, Position.class).rotation, 0.001f);
        assertEquals(0, entityData.getComponent(trainee, CreatureMood.class).noWork);

        // While it tells the creature off it gets the anger on every tick
        entityData.setComponent(trainee, new CreatureAi(1, CreatureState.TELLING_OFF, WARLOCK));
        tick(system, 1.25);
        tick(system, 1.5);
        assertEquals(2 * (70 / 4), entityData.getComponent(trainee, CreatureMood.class).noWork);
    }

    @Test
    void creatureThatTellsAnotherOffIsNotInTheWayItself() {
        EntityId researcher = createCreature(5, 5, TaskType.RESEARCH, true);
        EntityId angry = createCreature(6, 5, null, false);
        entityData.setComponent(angry, new CreatureAi(0, CreatureState.TELLING_OFF, WARLOCK));

        tick(system(), 1);

        assertEquals(0, entityData.getComponent(researcher, CreatureMood.class).noWork);
    }

    @Test
    void contentTraineeOnlyGetsTheAnger() {
        roomId = TRAINING_ROOM;
        EntityId trainee = createCreature(5, 5, TaskType.TRAIN, true);
        createCreature(6, 5, null, false);
        entityData.setComponent(trainee, new CreatureMood(0, 0, 0, 0, 0, CreatureMood.toRuntimeThreshold(50) / 10));

        tick(system(), 1);

        assertNull(stateMachines.get(trainee));
        assertEquals(70 / 4, entityData.getComponent(trainee, CreatureMood.class).noWork);
    }

    private void tick(CreatureDisturbanceSystem system, double time) {
        float tpf = (float) (time - gameTime);
        gameTime = time;
        system.processTick(tpf);
    }

    private EntityId createCreature(int x, int y, TaskType taskType, boolean taskStarted) {
        EntityId entityId = entityData.createEntity();
        CreatureComponent creatureComponent = new CreatureComponent();
        creatureComponent.creatureId = WARLOCK;
        entityData.setComponents(entityId, creatureComponent, new CreatureMood(),
                new CreatureAi(0, taskType != null ? CreatureState.WORK : CreatureState.IDLE, WARLOCK),
                new Position(0, WorldUtils.pointToVector3f(x, y)));
        if (taskType != null) {
            entityData.setComponent(entityId, new TaskComponent(1, null, new Point(x, y), taskType, 0, taskStarted));
        }
        return entityId;
    }

    private CreatureDisturbanceSystem system() {
        Creature warlock = new Creature();
        warlock.getAttributes().setAngerNoWork((short) 70);
        warlock.getAttributes().setUnhappyThreshold((short) 50);
        IKwdFile kwdFile = (IKwdFile) Proxy.newProxyInstance(IKwdFile.class.getClassLoader(), new Class<?>[]{IKwdFile.class},
                (proxy, method, args) -> switch (method.getName()) {
            case "getCreature" -> warlock;
            default -> throw new UnsupportedOperationException(method.getName());
        });

        // The whole map is one room
        Room room = RoomFixtures.roomWithObjects(roomId);
        IRoomController roomController = (IRoomController) Proxy.newProxyInstance(IRoomController.class.getClassLoader(),
                new Class<?>[]{IRoomController.class}, (proxy, method, args) -> switch (method.getName()) {
            case "getRoom" -> room;
            case "equals" -> proxy == args[0];
            case "hashCode" -> 1;
            default -> throw new UnsupportedOperationException(method.getName());
        });
        IMapController mapController = (IMapController) Proxy.newProxyInstance(IMapController.class.getClassLoader(),
                new Class<?>[]{IMapController.class}, (proxy, method, args) -> switch (method.getName()) {
            case "getRoomControllerByCoordinates" -> roomController;
            default -> throw new UnsupportedOperationException(method.getName());
        });
        ICreaturesController creaturesController = (ICreaturesController) Proxy.newProxyInstance(ICreaturesController.class.getClassLoader(),
                new Class<?>[]{ICreaturesController.class}, (proxy, method, args) -> switch (method.getName()) {
            case "createController" -> Proxy.newProxyInstance(ICreatureController.class.getClassLoader(), new Class<?>[]{ICreatureController.class},
                    (creatureProxy, creatureMethod, creatureArgs) -> switch (creatureMethod.getName()) {
                case "getStateMachine" -> stateMachines.computeIfAbsent((EntityId) args[0], id -> new DefaultStateMachine<>());
                default -> throw new UnsupportedOperationException(creatureMethod.getName());
            });
            default -> throw new UnsupportedOperationException(method.getName());
        });
        return new CreatureDisturbanceSystem(entityData, kwdFile, mapController, creaturesController, () -> gameTime);
    }

}
