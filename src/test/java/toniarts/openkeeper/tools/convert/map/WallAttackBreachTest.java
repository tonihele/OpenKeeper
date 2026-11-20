/*
 * Copyright (C) 2026 OpenKeeper
 *
 * OpenKeeper is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OpenKeeper is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenKeeper. If not, see <http://www.gnu.org/licenses/>.
 */
package toniarts.openkeeper.tools.convert.map;

import com.simsilica.es.base.DefaultEntityData;
import com.badlogic.gdx.ai.pfa.DefaultGraphPath;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.game.task.objective.KillPlayer;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.MapController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.event.GameEventService;
import toniarts.openkeeper.game.task.objective.ObjectiveAttackWallTask;
import toniarts.openkeeper.game.task.objective.ObjectiveDigTileTask;
import toniarts.openkeeper.game.trigger.player.PlayerTriggerLogicController;
import toniarts.openkeeper.utils.Point;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static toniarts.openkeeper.tools.convert.map.Terrain.TerrainFlag.*;
import static toniarts.openkeeper.tools.convert.map.TriggerGeneric.TargetType.*;
import static toniarts.openkeeper.tools.convert.map.Variable.MiscVariable.MiscType.DIG_ENEMY_WALL_HEALTH;

class WallAttackBreachTest {
    private static final short WALL = 9, FLOOR = 3, ATTACKER = 3, DEFENDER = 4, NEUTRAL = 2;
    private static final Point POINT = new Point(0, 0);
    private record Event(int kind, short recipient, short terrain, short owner) { }

    @Test
    void enemyCollapseMatchesTheFourNativeEventsAndTheirTiming() {
        try (Fixture f = new Fixture(DEFENDER, 1)) {
            assertTrue(f.map.attackWall(POINT, ATTACKER));
            assertEquals(List.of(new Event(56, DEFENDER, WALL, DEFENDER),
                    new Event(57, ATTACKER, WALL, DEFENDER),
                    new Event(56, ATTACKER, FLOOR, NEUTRAL),
                    new Event(57, NEUTRAL, FLOOR, NEUTRAL)), f.events);
            assertEquals(1, f.flags[DEFENDER*2]);
            assertEquals(1, f.flags[ATTACKER*2+1]);
            assertEquals(1, f.flags[ATTACKER*2]);
            assertEquals(1, f.flags[NEUTRAL*2+1]);
            // Destination terrain has nonzero starting health: collapse cannot be tested from that value.
            assertEquals(10, f.map.getMapData().getTile(POINT).getHealth());
            f.scripts.processTick(.25f);
            assertEquals(1, f.flags[ATTACKER*2]);
        }
    }

    @Test
    void partialDamageEmitsNothing() {
        try (Fixture f = new Fixture(DEFENDER, 5)) {
            assertFalse(f.map.attackWall(POINT, ATTACKER));
            assertEquals(3, f.map.getMapData().getTile(POINT).getHealth());
            assertTrue(f.events.isEmpty());
        }
    }

    @Test
    void ownWallHasOnlyTheSupplementalPairAsInTheNativeTrace() {
        try (Fixture f = new Fixture(ATTACKER, 1)) {
            assertTrue(f.map.attackWall(POINT, ATTACKER));
            assertEquals(List.of(new Event(56, ATTACKER, FLOOR, NEUTRAL),
                    new Event(57, NEUTRAL, FLOOR, NEUTRAL)), f.events);
        }
    }

    @Test
    void neutralWallIsNotAttacked() {
        try (Fixture f = new Fixture(NEUTRAL, 1)) {
            assertFalse(f.map.attackWall(POINT, ATTACKER));
            assertEquals(1, f.map.getMapData().getTile(POINT).getHealth());
            assertTrue(f.events.isEmpty());
        }
    }

    @ParameterizedTest
    @CsvSource({"true,false,0", "false,true,0", "true,true,10"})
    void excludesNonsolidImpenetrableAndGoldTerrain(boolean solid, boolean penetrable, int gold) {
        EnumSet<Terrain.TerrainFlag> flags = TerrainFixtures.flags(OWNABLE, ATTACKABLE);
        if (solid) flags.add(SOLID);
        if (!penetrable) flags.add(IMPENETRABLE);
        try (Fixture f = new Fixture(DEFENDER, 1, flags, gold, FLOOR, TerrainFixtures.flags(), 255)) {
            assertFalse(f.map.attackWall(POINT, ATTACKER));
            assertTrue(f.events.isEmpty());
            assertEquals(1, f.map.getMapData().getTile(POINT).getHealth());
        }
    }

    @Test
    void readsTheActualPostDamageOwnerRatherThanAssumingNeutral() {
        try (Fixture f = new Fixture(DEFENDER, 1, TerrainFixtures.flags(SOLID, OWNABLE, ATTACKABLE),
                0, FLOOR, TerrainFixtures.flags(OWNABLE), 255)) {
            f.map.attackWall(POINT, ATTACKER);
            assertEquals(new Event(57, DEFENDER, FLOOR, DEFENDER), f.events.getLast());
        }
    }

    @Test
    void unchangedTerrainStillGetsTheSupplementalPair() {
        try (Fixture f = new Fixture(DEFENDER, 1, TerrainFixtures.flags(SOLID, OWNABLE, ATTACKABLE),
                0, WALL, TerrainFixtures.flags(), 255)) {
            f.map.attackWall(POINT, ATTACKER);
            assertEquals(List.of(new Event(56, ATTACKER, WALL, DEFENDER),
                    new Event(57, DEFENDER, WALL, DEFENDER)), f.events);
        }
    }

    @ParameterizedTest
    @CsvSource({"1,1", "2,2", "255,3"})
    void bothKindsRespectScriptRepeatsAcrossNewCollapses(int repeats, int expected) {
        try (Fixture f = new Fixture(DEFENDER, 1, TerrainFixtures.flags(SOLID, OWNABLE, ATTACKABLE),
                0, FLOOR, TerrainFixtures.flags(), repeats)) {
            for (int i=0; i<3; i++) {
                f.rebuild(DEFENDER, 1);
                f.map.attackWall(POINT, ATTACKER);
            }
            assertEquals(expected, f.flags[DEFENDER*2]);
            assertEquals(expected, f.flags[ATTACKER*2]);
            assertEquals(expected, f.flags[ATTACKER*2+1]);
            assertEquals(expected, f.flags[NEUTRAL*2+1]);
        }
    }

    @Test
    void ordinaryDiggingDoesNotEmitTheSupplementalPair() {
        try (Fixture f = new Fixture(DEFENDER, 1)) {
            f.map.damageTile(POINT, ATTACKER, null);
            assertEquals(List.of(new Event(56, DEFENDER, WALL, DEFENDER),
                    new Event(57, ATTACKER, WALL, DEFENDER)), f.events);
        }
    }

    @Test
    void partyWallWorkCallsTheRealWallPathOncePerSecondAndStopsAfterCollapse() {
        try (Fixture f = new Fixture(DEFENDER, 3)) {
            ICreatureController worker = worker(true);
            ObjectiveAttackWallTask task = new ObjectiveAttackWallTask(null, f.map, POINT, ATTACKER);
            assertTrue(task.isValid(worker));
            assertFalse(new ObjectiveDigTileTask(null, f.map, POINT, ATTACKER).isValid(worker));
            task.assign(worker, false);
            task.executeTask(worker, .75f);
            assertEquals(3, f.map.getMapData().getTile(POINT).getHealth());
            task.executeTask(worker, 1f);
            assertEquals(1, f.map.getMapData().getTile(POINT).getHealth());
            task.executeTask(worker, 1.5f);
            assertTrue(f.events.isEmpty());
            task.executeTask(worker, 2f);
            assertEquals(4, f.events.size());
            assertFalse(task.isValid(worker));
            task.executeTask(worker, 3f);
            assertEquals(4, f.events.size());
        }
    }

    @Test
    void nonworkerLeadersDoNotExecuteWorkerWallWork() {
        try (Fixture f = new Fixture(DEFENDER, 1)) {
            ICreatureController leader = worker(false);
            ObjectiveAttackWallTask task = new ObjectiveAttackWallTask(null, f.map, POINT, ATTACKER);
            task.assign(leader, false);
            task.executeTask(leader, 1f);
            assertTrue(f.events.isEmpty());
        }
    }

    @Test
    void completedWallWorkDoesNotRepeatWhenTheDestroyedTypeIsUnchanged() {
        try (Fixture f = new Fixture(DEFENDER, 1, TerrainFixtures.flags(SOLID, OWNABLE, ATTACKABLE),
                0, WALL, TerrainFixtures.flags(), 255)) {
            ICreatureController worker = worker(true);
            ObjectiveAttackWallTask task = new ObjectiveAttackWallTask(null, f.map, POINT, ATTACKER);
            task.assign(worker, false);
            task.executeTask(worker, 1f);
            assertEquals(2, f.events.size());
            assertFalse(task.isValid(worker));
            task.executeTask(worker, 2f);
            assertEquals(2, f.events.size());
        }
    }

    @ParameterizedTest
    @CsvSource({"false,wall", "true,dig"})
    void killPlayerPlanningChoosesWallWorkOrDwarfTunneling(boolean diggable, String expected) {
        EnumSet<Terrain.TerrainFlag> flags = TerrainFixtures.flags(SOLID, OWNABLE, ATTACKABLE);
        if (diggable) flags.add(DWARF_CAN_DIG_THROUGH);
        try (Fixture f = new Fixture(DEFENDER, 1, flags, 0, FLOOR, TerrainFixtures.flags(), 255)) {
            List<String> calls = new ArrayList<>();
            RoomInstance room = new RoomInstance(new Room());
            room.addCoordinate(POINT);
            IRoomController roomController = proxy(IRoomController.class, (method,args)-> room);
            IMapController map = proxy(IMapController.class, (method,args)->switch(method) {
                case "getRoomControllerByCoordinates" -> roomController;
                case "getMapData" -> f.map.getMapData();
                case "getTerrain" -> f.map.getTerrain((toniarts.openkeeper.game.map.IMapTileInformation)args[0]);
                case "attackWall" -> {calls.add("wall");yield true;}
                case "damageTile" -> {calls.add("dig");yield 0;}
                default -> throw new UnsupportedOperationException(method);
            });
            DefaultGraphPath<toniarts.openkeeper.game.map.IMapTileInformation> path = new DefaultGraphPath<>();
            path.add(f.map.getMapData().getTile(POINT));
            INavigationService navigation = proxy(INavigationService.class, (method,args)->switch(method) {
                case "findPath" -> path;
                case "isAccessible" -> false;
                default -> throw new UnsupportedOperationException(method);
            });
            Keeper target = new Keeper(false,DEFENDER);
            target.setDungeonHeartLocation(POINT);
            ILevelInfo level = proxy(ILevelInfo.class,(method,args)->switch(method) {
                case "getPlayer" -> target;
                case "getLevelData" -> f.kwd;
                default -> throw new UnsupportedOperationException(method);
            });
            ICreatureController worker = worker(true);
            KillPlayer objective = new KillPlayer(navigation,map,level,DEFENDER,worker);
            var task = objective.getTaskQueue().peekFirst();
            assertNotNull(task);
            task.assign(worker,false);
            task.executeTask(worker,1f);
            assertEquals(List.of(expected),calls);
        }
    }

    @Test
    void dungeonBreachChildConditionsAreEvaluatedAtEventTime() {
        try (Fixture f = new Fixture(DEFENDER,1)) {
            f.records.put(41,PlayerBreachFixtures.condition(41,PLAYER_DUNGEON_BREACHED,43,42,255));
            f.records.put(43,PlayerBreachFixtures.condition(43,FLAG,-41,0,255));
            f.scripts = new PlayerTriggerLogicController(null,f.level,null,null,null,null);
            f.map.attackWall(POINT,ATTACKER);
            assertEquals(0,f.flags[DEFENDER*2]);
            f.flags[1]=1;
            for(int i=0;i<8;i++)f.scripts.processTick(.25f);
            assertEquals(0,f.flags[DEFENDER*2]);
            f.rebuild(DEFENDER,1);
            f.map.attackWall(POINT,ATTACKER);
            assertEquals(1,f.flags[DEFENDER*2]);
        }
    }

    private static ICreatureController worker(boolean worker) {
        return proxy(ICreatureController.class, (method, args) -> switch(method) {
            case "isWorker" -> worker;
            case "getOwnerId" -> ATTACKER;
            case "getCreatureCoordinates" -> POINT;
            case "getParty", "setAssignedTask", "addGold" -> null;
            default -> throw new UnsupportedOperationException(method);
        });
    }

    private static final class Fixture implements AutoCloseable {
        final DefaultEntityData data = new DefaultEntityData();
        final List<Event> events = new ArrayList<>();
        final int[] flags = new int[10];
        final MapController map;
        PlayerTriggerLogicController scripts;
        final IKwdFile kwd;
        final ILevelInfo level;
        final Map<Integer,Trigger> records = new HashMap<>();
        Fixture(short owner, int health) {
            this(owner, health, TerrainFixtures.flags(SOLID, OWNABLE, ATTACKABLE), 0, FLOOR, TerrainFixtures.flags(), 255);
        }
        Fixture(short owner, int health, EnumSet<Terrain.TerrainFlag> wallFlags, int gold,
                short destroyed, EnumSet<Terrain.TerrainFlag> floorFlags, int repeats) {
            Terrain wall = TerrainFixtures.terrain(WALL, wallFlags, gold, health, 10, WALL);
            wall.setDestroyedTypeTerrainId(destroyed);
            Terrain floor = TerrainFixtures.terrain(FLOOR, floorFlags, 0, 10, 10, FLOOR);
            GameMap raw = new GameMap(1,1);
            raw.getTiles()[0][0] = new Tile() {
                @Override public short getTerrainId() { return WALL; }
                @Override public short getPlayerId() { return owner; }
            };
            Map<Short,Keeper> keepers = new HashMap<>();
            Map<Short,Player> players = new HashMap<>();
            for (short id=1; id<=4; id++) {
                keepers.put(id, new Keeper(false,id));
                int a=id*10+1, b=a+1;
                players.put(id, PlayerBreachFixtures.player(id,a));
                records.put(a, PlayerBreachFixtures.condition(a,PLAYER_DUNGEON_BREACHED,-a,b,repeats));
                records.put(b, PlayerBreachFixtures.condition(b,PLAYER_ENEMY_BREACHED,-b,0,repeats));
                records.put(-a, PlayerBreachFixtures.action(-a,id*2,0));
                records.put(-b, PlayerBreachFixtures.action(-b,id*2+1,0));
            }
            kwd=proxy(IKwdFile.class,(method,args)->switch(method) {
                case "getMap" -> raw;
                case "getTerrain" -> (short)args[0]==WALL ? wall : floor;
                case "getTriggers" -> records;
                case "getPlayers" -> players;
                default -> throw new UnsupportedOperationException(method);
            });
            level=proxy(ILevelInfo.class,(method,args)->switch(method) {
                case "getPlayers" -> keepers;
                case "getLevelData" -> kwd;
                case "getFlag" -> flags[((Number)args[0]).intValue()];
                case "setFlag" -> {flags[((Number)args[0]).intValue()]=(int)args[1];yield null;}
                default -> throw new UnsupportedOperationException(method);
            });
            scripts=new PlayerTriggerLogicController(null,level,null,null,null,null);
            map=new MapController(kwd,null,Map.of(DIG_ENEMY_WALL_HEALTH,
                    CreatureSleepFixtures.variable(DIG_ENEMY_WALL_HEALTH,2)),null,data,level,
                    proxy(GameEventService.class,(method,args)->null),
                    id->event(57,id),id->event(56,id));
        }
        void rebuild(short owner,int hp) {
            var tile=map.getMapData().getTile(POINT);
            tile.setTerrainId(WALL);tile.setOwnerId(owner);tile.setHealth(hp);
        }
        void event(int kind,short id) {
            var tile=map.getMapData().getTile(POINT);
            events.add(new Event(kind,id,tile.getTerrainId(),tile.getOwnerId()));
            if(kind==56)scripts.onDungeonBreached(id); else scripts.onEnemyBreached(id);
        }
        @Override public void close() {data.close();}
    }
    @FunctionalInterface private interface Call {Object invoke(String method,Object[] args);}
    private static <T>T proxy(Class<T>type,Call call) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(),new Class<?>[]{type},(p,m,a)->switch(m.getName()) {
            case "hashCode"->System.identityHashCode(p);
            case "equals"->p==a[0];
            default->call.invoke(m.getName(),a);
        }));
    }
}
