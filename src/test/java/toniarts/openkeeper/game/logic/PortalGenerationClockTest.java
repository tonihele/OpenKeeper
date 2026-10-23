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

import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.controller.GameTimeController;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.RoomControllerFactory;
import toniarts.openkeeper.game.controller.player.PlayerCreatureControl;
import toniarts.openkeeper.game.controller.player.PlayerRoomControl;
import toniarts.openkeeper.game.controller.room.ICreatureEntrance;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.event.GameEventService;
import toniarts.openkeeper.game.listener.RoomListener;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.tools.convert.map.Variable;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable.MiscType;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static toniarts.openkeeper.tools.convert.map.PortalGenerationFixtures.creature;
import static toniarts.openkeeper.tools.convert.map.PortalGenerationFixtures.creaturePool;
import static toniarts.openkeeper.tools.convert.map.PortalGenerationFixtures.miscVariable;
import static toniarts.openkeeper.tools.convert.map.PortalGenerationFixtures.portal;

/**
 * The Portal's creature generation clock, as in the original: a Portal checks
 * every (base interval + n²) seconds, n being the creatures that came in
 * through it since it was claimed, and every check restarts the clock, also
 * when no creature can come in.
 */
class PortalGenerationClockTest {

    private static final short PLAYER_ID = 3;
    private static final short GOBLIN_ID = 7;
    private static final int BASE_INTERVAL = 18;
    private static final float TICK = 0.25f;

    private final List<Double> arrivals = new ArrayList<>();
    private DefaultEntityData entities;
    private PlayerRoomControl rooms;
    private CreatureSpawnSystem system;
    private ICreatureEntrance portal;
    private double time;

    @BeforeEach
    void setUp() {
        entities = new DefaultEntityData();
        Keeper keeper = new Keeper(false, PLAYER_ID);
        Creature goblin = creature(GOBLIN_ID);
        Creature imp = creature((short) 1);
        Room portalDefinition = portal((short) 3);
        rooms = new PlayerRoomControl(keeper, List.of(portalDefinition));
        PlayerCreatureControl creatures = new PlayerCreatureControl(keeper, imp, List.of(goblin, imp));
        creatures.setTypeAvailable(goblin, true);
        IPlayerController player = stub(IPlayerController.class, (method, args) -> switch (method) {
            case "getKeeper" ->
                keeper;
            case "getRoomControl" ->
                rooms;
            case "getCreatureControl" ->
                creatures;
            default ->
                throw new AssertionError(method);
        });
        Variable.CreaturePool pool = creaturePool(100);
        IKwdFile level = stub(IKwdFile.class, (method, args) -> switch (method) {
            case "getCreaturePool" ->
                Map.of((int) GOBLIN_ID, pool);
            case "getPortal" ->
                portalDefinition;
            case "getRoomById" ->
                null; // No Hero Portal in this level
            default ->
                throw new AssertionError(method);
        });
        List<RoomListener> listeners = new ArrayList<>();
        IMapController map = stub(IMapController.class, (method, args) -> {
            if (!method.equals("addListener")) {
                throw new AssertionError(method);
            }
            listeners.add((RoomListener) args[1]);
            return null;
        });
        ICreaturesController spawner = stub(ICreaturesController.class, (method, args) -> {
            if (!method.equals("spawnCreature")) {
                throw new AssertionError(method);
            }
            arrivals.add(time);
            return entities.createEntity();
        });
        Map<MiscType, Variable.MiscVariable> settings = new EnumMap<>(MiscType.class);
        settings.put(MiscType.ENTRANCE_GENERATION_SPEED_SECONDS, miscVariable(MiscType.ENTRANCE_GENERATION_SPEED_SECONDS, BASE_INTERVAL));
        settings.put(MiscType.MINIMUM_IMP_THRESHOLD, miscVariable(MiscType.MINIMUM_IMP_THRESHOLD, 0));
        settings.put(MiscType.CREATURES_SUPPORTED_BY_FIRST_PORTAL, miscVariable(MiscType.CREATURES_SUPPORTED_BY_FIRST_PORTAL, 100));
        settings.put(MiscType.CREATURES_SUPPORTED_PER_ADDITIONAL_PORTAL, miscVariable(MiscType.CREATURES_SUPPORTED_PER_ADDITIONAL_PORTAL, 5));
        settings.put(MiscType.TIME_BEFORE_FREE_IMP_GENERATED_SECONDS, miscVariable(MiscType.TIME_BEFORE_FREE_IMP_GENERATED_SECONDS, 1000));
        system = new CreatureSpawnSystem(entities, spawner, List.of(player), settings,
                stub(ILevelInfo.class, (method, args) -> level), map,
                stub(GameEventService.class, (method, args) -> null));

        RoomInstance roomInstance = new RoomInstance(portalDefinition);
        roomInstance.setOwnerId(PLAYER_ID);
        roomInstance.addCoordinate(new Point(5, 5));
        portal = (ICreatureEntrance) RoomControllerFactory.constructRoom(entities, level, roomInstance, null, settings, new GameTimeController());
        rooms.onBuild(portal);
        listeners.forEach(listener -> listener.onBuild(portal));
    }

    @Test
    void closedPortalKeepsTheGenerationClockRunning() {
        rooms.setPortalsOpen(false);
        advanceTo(30);
        assertEquals(List.of(), arrivals);

        // Reopening doesn't bring a creature at once, it comes at the next check
        rooms.setPortalsOpen(true);
        advanceTo(2 * BASE_INTERVAL + TICK);
        assertEquals(1, arrivals.size());
        assertTrue(arrivals.get(0) >= 2 * BASE_INTERVAL, "Came in at " + arrivals.get(0));
    }

    @Test
    void generationIntervalGrowsWithTheCreaturesThatCameIn() {
        advanceTo(200);

        assertEquals(BASE_INTERVAL + 1, arrivals.get(1) - arrivals.get(0), 0.001);
        assertEquals(BASE_INTERVAL + 4, arrivals.get(2) - arrivals.get(1), 0.001);
        assertEquals(BASE_INTERVAL + 9, arrivals.get(3) - arrivals.get(2), 0.001);
    }

    @Test
    void scriptedCreatureCountsButDoesNotRestartTheClock() {
        advanceTo(BASE_INTERVAL + 1);
        assertEquals(1, arrivals.size());
        double first = arrivals.get(0);

        // A level script brings in a creature through the Portal
        portal.onCreatureEntered(entities.createEntity());

        advanceTo(200);
        assertEquals(BASE_INTERVAL + 4, arrivals.get(1) - first, 0.001);
    }

    @Test
    void claimingResetsTheCreatureCount() {
        advanceTo(100);
        portal.captured(PLAYER_ID);
        arrivals.clear();

        advanceTo(200);
        assertEquals(BASE_INTERVAL + 1, arrivals.get(1) - arrivals.get(0), 0.001);
    }

    private void advanceTo(double until) {
        while (time + TICK <= until + 0.0001) {
            time += TICK;
            system.processTick(TICK);
        }
    }

    private interface Calls {

        Object call(String method, Object[] args);
    }

    private static <T> T stub(Class<T> type, Calls calls) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (proxy, method, args) -> calls.call(method.getName(), args)));
    }
}
