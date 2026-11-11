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
package toniarts.openkeeper.game.logic;

import com.jme3.math.Vector3f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import toniarts.openkeeper.game.component.CreatureAi;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureFall;
import toniarts.openkeeper.game.component.CreatureSleep;
import toniarts.openkeeper.game.component.CreatureSpells;
import toniarts.openkeeper.game.component.Gold;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.LeavingDungeon;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.Stored;
import toniarts.openkeeper.game.component.Trigger;
import toniarts.openkeeper.game.controller.CreaturesController;
import toniarts.openkeeper.game.controller.IGameController;
import toniarts.openkeeper.game.controller.IGameWorldController;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.IObjectsController;
import toniarts.openkeeper.game.controller.room.AbstractRoomController.ObjectType;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.controller.room.storage.IRoomObjectControl;
import toniarts.openkeeper.game.event.GameEventService;
import toniarts.openkeeper.game.trigger.creature.CreatureTriggerLogicController;
import toniarts.openkeeper.tools.convert.map.CreatureAngryFixtures;
import toniarts.openkeeper.tools.convert.map.CreatureSleepFixtures;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Thing;
import toniarts.openkeeper.utils.WorldUtils;
import static org.junit.jupiter.api.Assertions.*;
import static toniarts.openkeeper.tools.convert.map.TriggerGeneric.TargetType.*;

class CreatureSackedTriggerTest {

    @Test
    void manualDismissalWaitsForLandingThenRunsBothEventsBeforeRemoval() {
        try (Fixture f = new Fixture(255)) {
            f.markDrop();
            f.leaving.processTick(.25f);
            assertEquals(0, f.flags[0]);
            assertNotNull(f.data.getComponent(f.entity, CreatureFall.class));
            f.falling.processTick(1);
            assertEquals(0, f.flags[0], "the animation loop must not execute scripts");
            assertEquals(WorldUtils.UNDERFLOOR_HEIGHT, f.data.getComponent(f.entity, Position.class).position.y);
            f.leaving.processTick(.25f);
            assertEquals(2, f.flags[0]);
            assertEquals(List.of(100, 100), f.goldAtAction);
            assertEquals(List.of(100, 100), f.healthAtAction);
            assertNull(f.data.getComponent(f.entity, CreatureComponent.class));
            assertNull(f.data.getComponent(f.entity, Gold.class));
            f.leaving.processTick(.25f);
            f.scripts.processTick(.25f);
            assertEquals(2, f.flags[0]);
        }
    }

    @Test
    void commonDepartureEmitsOnceBeforeCleanupAndRemoval() {
        try (Fixture f = new Fixture(255)) {
            f.creatures.leaveDungeon(f.entity);
            assertEquals(1, f.flags[0]);
            assertEquals(List.of(100), f.goldAtAction);
            assertNull(f.data.getComponent(f.entity, CreatureComponent.class));
            f.creatures.leaveDungeon(f.entity);
            assertEquals(1, f.flags[0]);
        }
    }

    @ParameterizedTest
    @CsvSource({"1,1", "2,2", "255,2"})
    void manualDuplicateUsesExistingRepeatLimits(int repeats, int expected) {
        try (Fixture f = new Fixture(repeats)) {
            f.markDrop(); f.landAndLeave();
            assertEquals(expected, f.flags[0]);
        }
    }

    @Test
    void removalWithoutDepartureDoesNotEmit() {
        try (Fixture f = new Fixture(255)) {
            f.creatures.createController(f.entity).remove();
            assertEquals(100, f.looseGold);
            f.scripts.processTick(.25f);
            assertEquals(0, f.flags[0]);
        }
    }

    @Test
    void passiveLeavingMarkerDoesNotMakeOrdinaryPollingTrue() {
        try (Fixture f = new Fixture(255)) {
            f.data.setComponent(f.entity, new LeavingDungeon());
            for (int i=0; i<8; i++) f.scripts.processTick(.25f);
            assertEquals(0, f.flags[0]);
        }
    }

    @Test
    void unmarkedCreatureLandsNormallyAndDoesNotEmit() {
        try (Fixture f = new Fixture(255)) {
            f.data.setComponent(f.entity, new CreatureFall());
            f.landAndLeave();
            assertEquals(0, f.flags[0]);
            assertNotNull(f.data.getComponent(f.entity, CreatureAi.class));
            assertNotNull(f.data.getComponent(f.entity, CreatureComponent.class));
        }
    }

    @ParameterizedTest
    @ValueSource(booleans={false,true})
    void nestedConditionIsCheckedAtEachPreRemovalNotification(boolean active) {
        try (Fixture f = new Fixture(255)) {
            f.records.put(1, CreatureAngryFixtures.condition(1, CREATURE_SACKED, 2, 0, 255));
            f.records.put(2, CreatureAngryFixtures.condition(2, FLAG, -1, 0, 255));
            f.flags[1] = active ? 1 : 0;
            f.bindScripts(); f.markDrop(); f.landAndLeave();
            assertEquals(active ? 2 : 0, f.flags[0]);
        }
    }

    @Test
    void nestedSackIsNotDispatchedAsAnIndependentRoot() {
        try (Fixture f = new Fixture(255)) {
            f.records.put(1, CreatureAngryFixtures.condition(1, FLAG, 2, 0, 255));
            f.records.put(2, CreatureAngryFixtures.condition(2, CREATURE_SACKED, -1, 0, 255));
            f.bindScripts(); f.creatures.leaveDungeon(f.entity);
            assertEquals(0, f.flags[0]);
        }
    }

    @Test
    void allMatchingRootsRunAtBothStages() {
        try (Fixture f = new Fixture(255)) {
            f.records.put(1, CreatureAngryFixtures.condition(1, CREATURE_SACKED, -1, 2, 255));
            f.records.put(2, CreatureAngryFixtures.condition(2, CREATURE_SACKED, -2, 0, 255));
            f.records.put(-2, CreatureAngryFixtures.action(-2, 2));
            f.bindScripts(); f.markDrop(); f.landAndLeave();
            assertEquals(2, f.flags[0]); assertEquals(2, f.flags[2]);
        }
    }

    @Test
    void ordinaryTraversalCursorDoesNotSuppressDeparture() {
        try (Fixture f = new Fixture(255)) {
            f.records.put(1, CreatureAngryFixtures.condition(1, FLAG, -2, 2, 255));
            f.records.put(-2, CreatureAngryFixtures.action(-2, 2));
            f.records.put(2, CreatureAngryFixtures.condition(2, CREATURE_SACKED, -1, 0, 255));
            f.flags[1] = 1; f.bindScripts(); f.scripts.processTick(.25f);
            f.markDrop(); f.landAndLeave();
            assertEquals(2, f.flags[0]);
        }
    }

    @ParameterizedTest
    @ValueSource(ints={0,999})
    void unboundOrUnknownScriptStillLeavesWithoutAnEvent(int triggerId) {
        try (Fixture f = new Fixture(255)) {
            if (triggerId == 0) f.data.removeComponent(f.entity, Trigger.class);
            else f.data.setComponent(f.entity, new Trigger(triggerId));
            f.markDrop(); f.landAndLeave();
            assertEquals(0, f.flags[0]);
            assertNull(f.data.getComponent(f.entity, CreatureComponent.class));
        }
    }

    @Test
    void cleanupHappensAfterBothNotificationsAndDoesNotDropHeldGold() {
        try (Fixture f = new Fixture(255)) {
            EntityId bed=f.data.createEntity(), spell=f.data.createEntity();
            CreatureSleep sleep=new CreatureSleep(); sleep.lairObjectId=bed;
            f.data.setComponents(bed,new Position(0,new Vector3f(1,1,1)),new Stored(bed, ObjectType.LAIR));
            f.data.setComponents(f.entity,sleep,new CreatureSpells(List.of(spell)));
            f.data.setComponent(spell,new Health(1,1));
            f.markDrop(); f.landAndLeave();
            assertEquals(List.of(bed),f.freedStorage);
            assertNull(f.data.getComponent(spell,Health.class));
            assertEquals(2,f.flagsAtCleanup);
            assertEquals(0,f.looseGold);
            assertNull(f.data.getComponent(f.entity,Gold.class));
        }
    }

    @ParameterizedTest
    @ValueSource(booleans={false,true})
    void aScriptRemovingTheCreaturePreventsLaterCleanupFromDereferencingIt(boolean manual) {
        try (Fixture f = new Fixture(255)) {
            f.removeDuringAction=true;
            if (manual) { f.markDrop(); f.landAndLeave(); }
            else f.creatures.leaveDungeon(f.entity);
            assertEquals(1,f.flags[0]);
            assertNull(f.data.getComponent(f.entity,CreatureComponent.class));
        }
    }

    private static final class Fixture implements AutoCloseable {
        final Thread simulation=Thread.currentThread();
        final EntityData data=new DefaultEntityData();
        final EntityId entity=data.createEntity();
        final int[] flags=new int[3];
        final List<Integer> goldAtAction=new ArrayList<>(), healthAtAction=new ArrayList<>();
        final List<EntityId> freedStorage=new ArrayList<>();
        int flagsAtCleanup, looseGold;
        boolean removeDuringAction, scriptCatalog;
        final Map<Integer,toniarts.openkeeper.tools.convert.map.Trigger> records=new HashMap<>();
        final CreaturesController creatures;
        final CreatureFallSystem falling;
        final CreatureLeaveSystem leaving;
        final ILevelInfo level;
        CreatureTriggerLogicController scripts;

        Fixture(int repeats) {
            records.put(1,CreatureAngryFixtures.condition(1,CREATURE_SACKED,-1,0,repeats));
            records.put(-1,CreatureAngryFixtures.action(-1,0));
            IKwdFile kwd=proxy(IKwdFile.class,(name,args)->switch(name) {
                case "getThings" -> scriptCatalog && args[0]==Thing.GoodCreature.class
                        ? List.of(CreatureAngryFixtures.thing(1)) : List.of();
                case "getCreature" -> CreatureSleepFixtures.sleeper(1);
                case "getTriggers" -> records;
                default -> throw new UnsupportedOperationException(name);
            });
            level=proxy(ILevelInfo.class,(name,args)->switch(name) {
                case "getLevelData" -> kwd;
                case "getFlag" -> flags[((Number)args[0]).intValue()];
                case "setFlag" -> {
                    assertSame(simulation,Thread.currentThread());
                    assertNotNull(data.getComponent(entity,Trigger.class));
                    goldAtAction.add(data.getComponent(entity,Gold.class).gold);
                    healthAtAction.add(data.getComponent(entity,Health.class).health);
                    flags[((Number)args[0]).intValue()]=(int)args[1];
                    if(removeDuringAction) data.removeEntity(entity);
                    yield null;
                }
                default -> throw new UnsupportedOperationException(name);
            });
            IRoomObjectControl storage=proxy(IRoomObjectControl.class,(name,args)-> {
                if(name.equals("removeItem")) {
                    flagsAtCleanup=flags[0]; freedStorage.add((EntityId)args[0]);
                    data.removeEntity((EntityId)args[0]); return null;
                }
                throw new UnsupportedOperationException(name);
            });
            IRoomController room=proxy(IRoomController.class,(name,args)-> {
                if(name.equals("getObjectControl")) return storage;
                throw new UnsupportedOperationException(name);
            });
            IMapController map=proxy(IMapController.class,(name,args)-> {
                if(name.equals("getRoomControllerByCoordinates")) return room;
                throw new UnsupportedOperationException(name);
            });
            IObjectsController objects=proxy(IObjectsController.class,(name,args)-> {
                if(name.equals("addLooseGold")) { looseGold+=(int)args[2]; return null; }
                throw new UnsupportedOperationException(name);
            });
            IGameWorldController world=proxy(IGameWorldController.class,(name,args)->
                    name.equals("getObjectsController") ? objects : null);
            IGameController game=proxy(IGameController.class,(name,args)->switch(name) {
                case "onCreatureSacked" -> { assertSame(simulation,Thread.currentThread()); scripts.onSacked((EntityId)args[0]); yield null; }
                case "getGameWorldController" -> world;
                default -> null;
            });
            creatures=new CreaturesController(kwd,data,Map.of(),()->0,game,map,level);
            CreatureComponent component=new CreatureComponent(); component.creatureId=6;
            data.setComponents(entity,component,new Owner((short)3,(short)3),new Trigger(1),
                    new Health(100,100),new Gold(100,100),new Position(0,new Vector3f(1,WorldUtils.DROP_HEIGHT,1)));
            scriptCatalog=true;
            bindScripts();
            falling=new CreatureFallSystem(data,()->0,proxy(GameEventService.class,(name,args)->null));
            leaving=new CreatureLeaveSystem(data,creatures);
        }
        void bindScripts() {
            if(scripts!=null) scripts.stop();
            scripts=new CreatureTriggerLogicController(null,level,()->0,null,creatures,null,data);
        }
        void markDrop() { data.setComponents(entity,new LeavingDungeon(),new CreatureFall()); }
        void landAndLeave() { falling.processTick(1); leaving.processTick(.25f); }
        public void close() { falling.stop(); leaving.stop(); scripts.stop(); data.close(); }
    }
    @FunctionalInterface private interface Call { Object invoke(String name,Object[] args); }
    private static <T> T proxy(Class<T> type,Call call) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(),new Class<?>[]{type},
                (p,method,args)->call.invoke(method.getName(),args)));
    }
}
