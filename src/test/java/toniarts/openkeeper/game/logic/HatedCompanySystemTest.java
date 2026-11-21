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

import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.Map;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureMood;
import toniarts.openkeeper.game.component.TaskComponent;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.task.TaskType;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.ShotFixtures;
import toniarts.openkeeper.tools.convert.map.TrapFiringFixtures;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable.MiscType;
import toniarts.openkeeper.tools.convert.map.VariableFixtures;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Evil creatures and creatures that are not evil grow angrier while they share
 * a room they work in (DKII.EXE 0x4E8C60)
 */
class HatedCompanySystemTest {

    private static final short WARLOCK = 5;
    private static final short WIZARD = 20;
    private static final short SKELETON = 9;
    private static final Point LIBRARY = new Point(5, 5);
    private static final Point OTHER_LIBRARY = new Point(9, 9);
    private static final int TICKS_PER_SECOND = 4;

    private final EntityData entityData = new DefaultEntityData();
    private final Map<Short, Creature> creatures = Map.of(
            WARLOCK, TrapFiringFixtures.creature(WARLOCK, Creature.CreatureFlag.IS_EVIL),
            WIZARD, TrapFiringFixtures.creature(WIZARD, Creature.CreatureFlag.IS_GOOD),
            SKELETON, TrapFiringFixtures.creature(SKELETON, Creature.CreatureFlag.IS_EVIL, Creature.CreatureFlag.DOESNT_GET_ANGRY_WITH_ENEMIES));
    private final Map<Point, IRoomController> rooms = Map.of(LIBRARY, room(), OTHER_LIBRARY, room());
    private double gameTime;
    private final HatedCompanySystem system = createSystem();

    @Test
    void evilAndGoodSharingARoomGetAngryEvery32Ticks() {
        EntityId warlock = createWorker(WARLOCK, TaskType.RESEARCH, LIBRARY);
        EntityId wizard = createWorker(WIZARD, TaskType.RESEARCH, LIBRARY);
        EntityId skeleton = createWorker(SKELETON, TaskType.RESEARCH, LIBRARY);

        tick(31);
        assertEquals(0, entityData.getComponent(warlock, CreatureMood.class).other);

        // Eight times the 150 a second
        tick(1);
        assertEquals(1200, entityData.getComponent(warlock, CreatureMood.class).other);
        assertEquals(1200, entityData.getComponent(wizard, CreatureMood.class).other);
        assertEquals(0, entityData.getComponent(skeleton, CreatureMood.class).other);

        tick(32);
        assertEquals(2400, entityData.getComponent(wizard, CreatureMood.class).other);
    }

    @Test
    void creaturesOfOneSideOrInDifferentRoomsOrOnOtherJobsStayCalm() {
        EntityId warlock = createWorker(WARLOCK, TaskType.RESEARCH, LIBRARY);
        EntityId otherWarlock = createWorker(WARLOCK, TaskType.RESEARCH, LIBRARY);
        EntityId wizardElsewhere = createWorker(WIZARD, TaskType.RESEARCH, OTHER_LIBRARY);
        EntityId wizardPassingBy = createWorker(WIZARD, TaskType.GO_TO_EAT, LIBRARY);

        tick(32);

        assertEquals(0, entityData.getComponent(warlock, CreatureMood.class).other);
        assertEquals(0, entityData.getComponent(otherWarlock, CreatureMood.class).other);
        assertEquals(0, entityData.getComponent(wizardElsewhere, CreatureMood.class).other);
        assertEquals(0, entityData.getComponent(wizardPassingBy, CreatureMood.class).other);
    }

    private void tick(int ticks) {
        for (int i = 0; i < ticks; i++) {
            gameTime += 1.0 / TICKS_PER_SECOND;
            system.processTick(1f / TICKS_PER_SECOND);
        }
    }

    private EntityId createWorker(short creatureId, TaskType taskType, Point room) {
        EntityId worker = entityData.createEntity();
        CreatureComponent creatureComponent = new CreatureComponent();
        creatureComponent.creatureId = creatureId;
        entityData.setComponents(worker, creatureComponent, new CreatureMood(),
                new TaskComponent(worker.getId(), null, room, taskType, 0, true));
        return worker;
    }

    private static IRoomController room() {
        return (IRoomController) Proxy.newProxyInstance(IRoomController.class.getClassLoader(), new Class<?>[]{IRoomController.class},
                (proxy, method, args) -> switch (method.getName()) {
            case "hashCode" ->
                System.identityHashCode(proxy);
            case "equals" ->
                proxy == args[0];
            default ->
                null;
        });
    }

    private HatedCompanySystem createSystem() {
        IKwdFile kwdFile = (IKwdFile) Proxy.newProxyInstance(IKwdFile.class.getClassLoader(), new Class<?>[]{IKwdFile.class},
                (proxy, method, args) -> switch (method.getName()) {
            case "getCreature" ->
                creatures.get((Short) args[0]);
            case "getGameLevel" ->
                ShotFixtures.gameLevel(TICKS_PER_SECOND);
            default ->
                null;
        });
        IMapController mapController = (IMapController) Proxy.newProxyInstance(IMapController.class.getClassLoader(),
                new Class<?>[]{IMapController.class},
                (proxy, method, args) -> method.getName().equals("getRoomControllerByCoordinates") ? rooms.get((Point) args[0]) : null);

        return new HatedCompanySystem(entityData, kwdFile,
                Map.of(MiscType.MODIFY_ANGER_IN_COMPANY_OF_HATED_CREATURES_PER_SECOND,
                        VariableFixtures.miscVariable(MiscType.MODIFY_ANGER_IN_COMPANY_OF_HATED_CREATURES_PER_SECOND, 150)),
                mapController, () -> gameTime);
    }

}
