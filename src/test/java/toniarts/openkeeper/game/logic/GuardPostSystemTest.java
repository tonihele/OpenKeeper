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
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.EntitySet;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.component.CreatureAi;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.Navigation;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.TaskComponent;
import toniarts.openkeeper.game.component.Threat;
import toniarts.openkeeper.game.component.TrapViewState;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.creature.CreatureState;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.task.TaskType;
import toniarts.openkeeper.tools.convert.map.CreatureSleepFixtures;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.tools.convert.map.ShotFixtures;
import toniarts.openkeeper.tools.convert.map.Trap;
import toniarts.openkeeper.tools.convert.map.TrapFiringFixtures;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable.MiscType;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A Guard Post that sees enemies its keeper's creatures there are no match for
 * sends the guards of the keeper's nearest Guard Room to them (DKII.EXE
 * 0x4A84E0, 0x4FAC40, 0x4754D0)
 */
class GuardPostSystemTest {

    private static final short KEEPER = 3;
    private static final short ENEMY = 4;
    private static final short GUARD_POST_ID = 11;
    private static final short SENTRY_ID = 3;
    private static final Point POST = new Point(20, 20);
    private static final Point GUARD_ROOM = new Point(10, 20);
    private static final Point FAR_GUARD_ROOM = new Point(40, 20);
    private static final Point ENEMY_TILE = new Point(25, 20);

    private final List<EntitySet> entitySets = new ArrayList<>();
    private final Set<EntitySet> setsThatTookChanges = new HashSet<>();
    private final EntityData entityData = takingNoteOfEntitySets(new DefaultEntityData());
    private final Map<Point, List<EntityId>> entitiesByTile = new HashMap<>();
    private final List<EntityId> sent = new ArrayList<>();
    private final Map<Point, IRoomController> guardRooms = new HashMap<>();
    private boolean sight = true;
    private double gameTime;

    @Test
    void sendsTheGuardsOfTheNearestGuardRoomUntilTheirThreatMakesUpTheDifference() {
        addGuardRoom(GUARD_ROOM);
        addGuardRoom(FAR_GUARD_ROOM);
        createPost(GUARD_POST_ID, false);
        createCreature(ENEMY, ENEMY_TILE, 100);
        createCreature(ENEMY, new Point(27, 21), 100);

        // 40 of our own threat near the enemy, three quarters of it count: 200 - 30 to meet
        createCreature(KEEPER, new Point(24, 20), 40);
        EntityId first = createGuard(GUARD_ROOM, 90);
        EntityId second = createGuard(GUARD_ROOM, 90);
        createGuard(GUARD_ROOM, 90);
        createGuard(FAR_GUARD_ROOM, 90);

        tick();

        assertEquals(List.of(first, second), sent);
    }

    @Test
    void sendsNobodyWhenTheKeepersCreaturesThereAreAMatch() {
        addGuardRoom(GUARD_ROOM);
        createPost(GUARD_POST_ID, false);
        createCreature(ENEMY, ENEMY_TILE, 90);
        createCreature(KEEPER, new Point(24, 20), 120);
        createGuard(GUARD_ROOM, 90);

        tick();

        assertTrue(sent.isEmpty());
    }

    @Test
    void enemyOutOfReachOrOutOfSightIsNotReported() {
        addGuardRoom(GUARD_ROOM);
        createPost(GUARD_POST_ID, false);
        createGuard(GUARD_ROOM, 90);
        createCreature(ENEMY, new Point(27, 20), 100);

        tick();
        assertTrue(sent.isEmpty());

        createCreature(ENEMY, ENEMY_TILE, 100);
        sight = false;
        tick();
        assertTrue(sent.isEmpty());
    }

    @Test
    void sendsNobodyWhileACalledCreatureIsOnItsWayThere() {
        addGuardRoom(GUARD_ROOM);
        createPost(GUARD_POST_ID, false);
        createCreature(ENEMY, ENEMY_TILE, 100);
        createGuard(GUARD_ROOM, 90);
        EntityId onItsWay = createCreature(KEEPER, new Point(12, 20), 10);
        entityData.setComponents(onItsWay, new CreatureAi(0, CreatureState.HEART_CALL, (short) 1),
                new Navigation(new Point(23, 22), null, List.of()));

        tick();

        assertTrue(sent.isEmpty());
    }

    @Test
    void onlyABuiltGuardPostWithAGuardRoomReports() {
        createCreature(ENEMY, ENEMY_TILE, 100);
        createPost(SENTRY_ID, false);
        createPost(GUARD_POST_ID, true);
        EntityId post = createPost(GUARD_POST_ID, false);
        GuardPostSystem system = createSystem();

        // No Guard Room yet
        system.processTick(0.25f);
        assertTrue(sent.isEmpty());

        // Eight ticks on, eight off, by the post's id
        addGuardRoom(GUARD_ROOM);
        EntityId guard = createGuard(GUARD_ROOM, 90);
        int reports = 0;
        for (int tick = 0; tick < 16; tick++) {
            sent.clear();
            gameTime = tick / 4.0;
            system.processTick(0.25f);
            if (!sent.isEmpty()) {
                assertEquals(List.of(guard), sent);
                assertTrue(((tick + post.getId()) & 8) != 0);
                reports++;
            }
        }
        assertEquals(8, reports);
    }

    @Test
    void takesTheChangesOfItsEntitySetsOnATickWithNoEnemyInSight() {

        // Until a set takes its changes they stay queued in it: every task change of the game, for as long as no post sees an enemy
        createPost(GUARD_POST_ID, false);
        GuardPostSystem system = createSystem();

        gameTime = 8 / 4.0;
        system.processTick(0.25f);

        assertEquals(new HashSet<>(entitySets), setsThatTookChanges);
    }

    private EntityData takingNoteOfEntitySets(EntityData store) {
        return (EntityData) Proxy.newProxyInstance(EntityData.class.getClassLoader(), new Class<?>[]{EntityData.class}, (proxy, method, args) -> {
            Object result = method.invoke(store, args);
            if (!(result instanceof EntitySet set)) {
                return result;
            }
            EntitySet noted = (EntitySet) Proxy.newProxyInstance(EntitySet.class.getClassLoader(), new Class<?>[]{EntitySet.class},
                    (setProxy, setMethod, setArgs) -> switch (setMethod.getName()) {
                case "hashCode" ->
                    System.identityHashCode(setProxy);
                case "equals" ->
                    setProxy == setArgs[0];
                default -> {
                    if (setMethod.getName().equals("applyChanges")) {
                        setsThatTookChanges.add((EntitySet) setProxy);
                    }
                    yield setMethod.invoke(set, setArgs);
                }
            });
            entitySets.add(noted);
            return noted;
        });
    }

    private void tick() {

        // A tick on which every post here has its turn
        gameTime = 8 / 4.0;
        createSystem().processTick(0.25f);
    }

    private void addGuardRoom(Point centre) {
        RoomInstance roomInstance = new RoomInstance(new Room());
        roomInstance.addCoordinate(centre);
        guardRooms.put(centre, (IRoomController) Proxy.newProxyInstance(IRoomController.class.getClassLoader(), new Class<?>[]{IRoomController.class},
                (proxy, method, args) -> switch (method.getName()) {
            case "getRoomInstance" ->
                roomInstance;
            case "hashCode" ->
                System.identityHashCode(proxy);
            case "equals" ->
                proxy == args[0];
            default ->
                null;
        }));
    }

    private EntityId createPost(short trapId, boolean blueprint) {
        EntityId post = entityData.createEntity();
        entityData.setComponents(post, new TrapViewState(trapId, blueprint), new Owner(KEEPER, KEEPER),
                new Position(0, WorldUtils.pointToVector3f(POST)));
        return post;
    }

    private EntityId createGuard(Point guardRoom, int threat) {
        EntityId guard = createCreature(KEEPER, guardRoom, threat);
        entityData.setComponent(guard, new TaskComponent(guard.getId(), null, guardRoom, TaskType.GUARD, 0, true));
        return guard;
    }

    private EntityId createCreature(short ownerId, Point tile, int threat) {
        EntityId creature = entityData.createEntity();
        entityData.setComponents(creature, new CreatureComponent(), new Owner(ownerId, ownerId), new Health(100, 100), new Threat(threat),
                new Position(0, WorldUtils.pointToVector3f(tile)));
        entitiesByTile.computeIfAbsent(tile, key -> new ArrayList<>()).add(creature);
        return creature;
    }

    private GuardPostSystem createSystem() {
        IKwdFile kwdFile = (IKwdFile) Proxy.newProxyInstance(IKwdFile.class.getClassLoader(), new Class<?>[]{IKwdFile.class},
                (proxy, method, args) -> switch (method.getName()) {
            case "getGameLevel" ->
                ShotFixtures.gameLevel(4);
            case "getTrapById" -> {
                Trap trap = TrapFiringFixtures.trap(((Number) args[0]).shortValue(), Trap.TriggerType.PRESSURE, 0, 0, (short) 0);
                if (trap.getTrapId() == GUARD_POST_ID) {
                    trap.getFlags().add(Trap.TrapFlag.GUARD_POST);
                }
                yield trap;
            }
            default ->
                null;
        });
        Keeper keeper = new Keeper(false, KEEPER);
        ILevelInfo levelInfo = (ILevelInfo) Proxy.newProxyInstance(ILevelInfo.class.getClassLoader(), new Class<?>[]{ILevelInfo.class},
                (proxy, method, args) -> switch (method.getName()) {
            case "getLevelData" ->
                kwdFile;
            case "getPlayer" ->
                keeper;
            default ->
                null;
        });
        IMapController mapController = (IMapController) Proxy.newProxyInstance(IMapController.class.getClassLoader(),
                new Class<?>[]{IMapController.class}, (proxy, method, args) -> switch (method.getName()) {
            case "getRoomsByFunction" ->
                new ArrayList<>(guardRooms.values());
            case "getRoomControllerByCoordinates" ->
                guardRooms.get((Point) args[0]);
            default ->
                null;
        });
        IEntityPositionLookup positionLookup = (IEntityPositionLookup) Proxy.newProxyInstance(IEntityPositionLookup.class.getClassLoader(),
                new Class<?>[]{IEntityPositionLookup.class}, (proxy, method, args) -> switch (method.getName()) {
            case "getEntitiesInLocation" ->
                entitiesByTile.getOrDefault(new Point((int) args[0], (int) args[1]), List.of());
            case "hasLineOfSight" ->
                sight;
            default ->
                null;
        });
        ICreaturesController creaturesController = (ICreaturesController) Proxy.newProxyInstance(ICreaturesController.class.getClassLoader(),
                new Class<?>[]{ICreaturesController.class}, (proxy, method, args) -> Proxy.newProxyInstance(
                        ICreatureController.class.getClassLoader(), new Class<?>[]{ICreatureController.class}, (controller, controllerMethod, controllerArgs) -> {
                            if (controllerMethod.getName().equals("answerHeartCall")) {
                                assertEquals(ENEMY_TILE, controllerArgs[0]);
                                sent.add((EntityId) args[0]);
                                return true;
                            }
                            return null;
                        }));

        return new GuardPostSystem(entityData,
                Map.of(MiscType.GUARD_POST_REPORTING_DISTANCE_TILES, CreatureSleepFixtures.variable(MiscType.GUARD_POST_REPORTING_DISTANCE_TILES, 7)),
                levelInfo, mapController, positionLookup, creaturesController, () -> gameTime);
    }

}
