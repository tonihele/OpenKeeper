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

import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.component.CreatureAi;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureImprisoned;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.Stored;
import toniarts.openkeeper.game.component.Trigger;
import toniarts.openkeeper.game.component.Unconscious;
import toniarts.openkeeper.game.controller.CreaturesController;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.IGameController;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.creature.CreatureController;
import toniarts.openkeeper.game.controller.creature.CreatureState;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.room.AbstractRoomController.ObjectType;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.controller.room.storage.RoomPrisonerControl;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.task.worker.CarryEnemyCreatureToPrison;
import toniarts.openkeeper.game.trigger.creature.CreatureTriggerEventQueue;
import toniarts.openkeeper.game.trigger.creature.CreatureTriggerLogicController;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.CreatureAngryFixtures;
import toniarts.openkeeper.tools.convert.map.CreatureImprisonedFixtures;
import toniarts.openkeeper.tools.convert.map.CreatureSleepFixtures;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.RoomFixtures;
import toniarts.openkeeper.tools.convert.map.Thing;
import toniarts.openkeeper.utils.Point;
import static toniarts.openkeeper.tools.convert.map.TriggerGeneric.TargetType.*;
import static org.junit.jupiter.api.Assertions.*;

class CreatureImprisonedTriggerTest {

    @Test
    void actualAdmissionStoresCaptiveStateBeforeSimulationDelivery() {
        try(Fixture f=new Fixture(255,true)) {
            f.data.setComponent(f.entity,new Unconscious());
            f.controller.imprison((short)3,f.roomId);
            assertEquals(0,f.flags[0]);
            assertEquals(20,f.data.getComponent(f.entity,Health.class).health);
            assertNull(f.data.getComponent(f.entity,Unconscious.class));
            assertEquals(f.roomId,f.data.getComponent(f.entity,Stored.class).room);
            assertEquals(1,f.data.getComponent(f.entity,Owner.class).ownerId);
            assertEquals(3,f.data.getComponent(f.entity,Owner.class).controlId);
            f.tick();assertEquals(1,f.flags[0]);
            assertEquals(3,f.controlAtEvent);assertTrue(f.imprisonedAtEvent);
        }
    }

    @Test
    void admissionWithoutHealthIsRejectedWithoutAnEvent() {
        try(Fixture f=new Fixture(255,true)) {
            f.data.removeComponent(f.entity,Health.class);
            f.controller.imprison((short)3,f.roomId);f.tick();
            assertEquals(0,f.flags[0]);assertNull(f.lastAdmission);
        }
    }

    @Test
    void actualCarryTaskOnlyEmitsWhenDeliveryIsExecuted() {
        try(Fixture f=new Fixture(255,true)) {
            CarryEnemyCreatureToPrison task=new CarryEnemyCreatureToPrison(null,null,
                    new Point(1,1),(short)3,f.room,null,f.controller);
            f.tick();assertEquals(0,f.flags[0]);
            task.executeTask(null,.25f);
            assertEquals(0,f.flags[0]);
            f.tick();assertEquals(1,f.flags[0]);
        }
    }

    @Test
    void realLevelLoadingRetainsAdmissionUntilScriptControlsExist() {
        try(Fixture f=new Fixture(255,false)) {
            f.data.removeComponent(f.entity,Trigger.class);
            f.factory(true);
            assertNotNull(f.lastAdmission);
            assertTrue(f.data.getComponent(f.lastAdmission,Health.class).health>0);
            assertNotNull(f.data.getComponent(f.lastAdmission,CreatureImprisoned.class));
            assertEquals(1,f.data.getComponent(f.lastAdmission,Trigger.class).triggerId);
            assertEquals(0,f.flags[0]);
            f.bindScripts();f.tick();
            assertEquals(1,f.flags[0]);assertTrue(f.imprisonedAtEvent);
            for(int i=0;i<8;i++) f.tick();
            assertEquals(1,f.flags[0]);
        }
    }

    @Test
    void realRuntimeSpawnIntoPrisonAlsoDeliversAnAdmission() {
        try(Fixture f=new Fixture(255,false)) {
            f.data.removeComponent(f.entity,Trigger.class);
            CreaturesController factory=f.factory(false);
            f.bindScripts();
            factory.spawnCreature(CreatureImprisonedFixtures.hero(),new Vector2f(1,1));
            assertEquals(0,f.flags[0]);f.tick();assertEquals(1,f.flags[0]);
        }
    }

    @Test
    void spawningInAnOwnedPrisonDoesNotCreateAPrisonerAdmission() {
        try(Fixture f=new Fixture(255,false)) {
            f.instance.setOwnerId((short)1);
            f.factory(true);
            assertNull(f.lastAdmission);
        }
    }

    @Test
    void spawningOutsidePrisonDoesNotCreateAnAdmission() {
        try(Fixture f=new Fixture(255,false)) {
            f.inPrison=false;f.factory(true);
            assertNull(f.lastAdmission);
        }
    }

    @Test
    void aDeadBodyPlacedInPrisonDoesNotEmitAnAdmission() {
        try(Fixture f=new Fixture(255,false)) {
            f.loadBody=true;f.factory(true);
            assertNull(f.lastAdmission);
        }
    }

    @Test
    void merelySettingPrisonStateDoesNotPollAsAnAdmission() {
        try(Fixture f=new Fixture(255,true)) {
            f.data.setComponent(f.entity,new CreatureImprisoned(0,0));
            f.controller.getStateMachine().changeState(CreatureState.IMPRISONED);
            for(int i=0;i<8;i++) f.tick();
            assertEquals(0,f.flags[0]);
        }
    }

    @Test
    void beingImprisonedDoesNotLatchOrRepeatTheDeliveredEvent() {
        try(Fixture f=new Fixture(255,true)) {
            f.controller.imprison((short)3,f.roomId);f.tick();
            for(int i=0;i<12;i++) f.tick();
            assertEquals(1,f.flags[0]);
        }
    }

    @ParameterizedTest
    @CsvSource({"1,1","2,2","255,3"})
    void repeatedExplicitAdmissionsUseExistingScriptRepeatLimits(int repeats,int events) {
        try(Fixture f=new Fixture(repeats,true)) {
            for(int i=0;i<3;i++) f.controller.imprison((short)3,f.roomId);
            f.tick();assertEquals(events,f.flags[0]);
        }
    }

    @Test
    void admissionOnAProducerThreadDoesNotRunScriptActionsThere() throws InterruptedException {
        try(Fixture f=new Fixture(255,true)) {
            AtomicReference<Throwable> failure=new AtomicReference<>();
            Thread producer=new Thread(()->{
                try {f.controller.imprison((short)3,f.roomId);}
                catch(Throwable t) {failure.set(t);}
            },"synthetic-prison-admission");
            producer.start();producer.join();
            assertNull(failure.get());assertEquals(0,f.flags[0]);
            f.tick();assertEquals(1,f.flags[0]);
        }
    }

    @Test
    void sharedEarlyQueuePreservesMixedEventTypesAndOrder() {
        try(Fixture f=new Fixture(255,false)) {
            f.records.put(1,CreatureAngryFixtures.condition(1,CREATURE_IMPRISONED,-1,2,255));
            f.records.put(2,CreatureAngryFixtures.condition(2,CREATURE_SLAPPED,-2,3,255));
            f.records.put(3,CreatureAngryFixtures.condition(3,CREATURE_STUNNED,-3,0,255));
            f.records.put(-2,CreatureAngryFixtures.action(-2,2));
            f.records.put(-3,CreatureAngryFixtures.action(-3,3));
            f.controller.imprison((short)3,f.roomId);
            f.queue.enqueueSlapped(f.entity);f.queue.enqueueStunned(f.entity);
            f.bindScripts();f.tick();
            assertEquals(List.of(0,2,3),f.actionOrder);
        }
    }

    @Test
    void nestedConditionsObserveDeliveryTimeWithoutLatching() {
        try(Fixture f=new Fixture(255,true)) {
            f.records.put(1,CreatureAngryFixtures.condition(1,CREATURE_IMPRISONED,2,0,255));
            f.records.put(2,CreatureAngryFixtures.condition(2,FLAG,-1,0,255));
            f.bindScripts();
            f.controller.imprison((short)3,f.roomId);f.tick();
            f.flags[1]=1;for(int i=0;i<4;i++) f.tick();
            assertEquals(0,f.flags[0]);
            f.controller.imprison((short)3,f.roomId);f.tick();assertEquals(1,f.flags[0]);
        }
    }

    @Test
    void ordinaryCursorIsNotMovedByAdmissionDelivery() {
        try(Fixture f=new Fixture(255,true)) {
            f.records.put(1,CreatureAngryFixtures.condition(1,FLAG,-2,2,255));
            f.records.put(2,CreatureAngryFixtures.condition(2,CREATURE_IMPRISONED,-1,0,255));
            f.records.put(-2,CreatureAngryFixtures.action(-2,2));
            f.bindScripts();f.flags[1]=1;f.tick();
            f.controller.imprison((short)3,f.roomId);f.tick();
            assertEquals(1,f.flags[0]);assertEquals(1,f.flags[2]);
        }
    }

    @Test
    void stoppingDiscardsPendingStartupAndAdmissionEvents() {
        try(Fixture f=new Fixture(255,true)) {
            f.controller.imprison((short)3,f.roomId);f.scripts.stop();f.tick();
            assertEquals(0,f.flags[0]);
        }
    }

    private static final class Fixture implements AutoCloseable {
        final EntityData data=new DefaultEntityData();
        final EntityId entity=data.createEntity();
        final EntityId roomId=data.createEntity();
        final int[] flags=new int[4];
        final List<Integer> actionOrder=new ArrayList<>();
        final Thread simulation=Thread.currentThread();
        final Map<Integer,toniarts.openkeeper.tools.convert.map.Trigger> records=new HashMap<>();
        final CreatureTriggerEventQueue queue=new CreatureTriggerEventQueue();
        final Creature creature=CreatureImprisonedFixtures.creature();
        final RoomInstance instance=new RoomInstance(RoomFixtures.room((short)11,0));
        final IKwdFile kwd;
        final ILevelInfo level;
        final ICreaturesController creatures;
        final IGameController game;
        final IMapController map;
        final IRoomController room;
        final CreatureController controller;
        RoomPrisonerControl storage;
        CreatureTriggerLogicController scripts;
        EntityId lastAdmission;
        short controlAtEvent;
        boolean imprisonedAtEvent;
        boolean includeThings=true;
        boolean loadBody;
        boolean inPrison=true;

        Fixture(int repeats,boolean bind) {
            records.put(1,CreatureAngryFixtures.condition(1,CREATURE_IMPRISONED,-1,0,repeats));
            records.put(-1,CreatureAngryFixtures.action(-1,0));
            instance.setOwnerId((short)3);instance.addCoordinate(new Point(1,1));
            CreatureComponent component=new CreatureComponent();component.creatureId=6;
            data.setComponents(entity,component,new Trigger(1),new Health(100,100),
                    new Owner((short)1,(short)1),new Position(0,new Vector3f(1,0,1)),
                    new CreatureAi(0,CreatureState.IDLE,(short)6));
            kwd=proxy(IKwdFile.class,(name,args)->switch(name) {
                case "getTriggers" -> records;
                case "getCreature","getImp" -> creature;
                case "getCreatureStats" -> null;
                case "getGameLevel" -> CreatureSleepFixtures.level(4);
                case "getThings" -> !includeThings ? List.of() : loadBody
                        ? args[0]==Thing.DeadBody.class ? List.of(CreatureImprisonedFixtures.body()) : List.of()
                        : args[0]==Thing.GoodCreature.class ? List.of(CreatureImprisonedFixtures.hero()) : List.of();
                default -> throw new UnsupportedOperationException(name);
            });
            level=proxy(ILevelInfo.class,(name,args)->switch(name) {
                case "getLevelData" -> kwd;
                case "getPlayer" -> new Keeper(false,(short)args[0]);
                case "getFlag" -> {assertSame(simulation,Thread.currentThread());yield flags[((Number)args[0]).intValue()];}
                case "setFlag" -> {
                    assertSame(simulation,Thread.currentThread());int id=((Number)args[0]).intValue();
                    flags[id]=(int)args[1];actionOrder.add(id);
                    if(id==0) {
                        controlAtEvent=data.getComponent(lastAdmission,Owner.class).controlId;
                        imprisonedAtEvent=data.getComponent(lastAdmission,CreatureImprisoned.class)!=null;
                    }
                    yield null;
                }
                default -> throw new UnsupportedOperationException(name);
            });
            creatures=proxy(ICreaturesController.class,(name,args)->switch(name) {
                case "createController" -> ((EntityId)args[0]).equals(entity) ? controller() : proxy(ICreatureController.class,
                        (method,a)->{throw new UnsupportedOperationException(method);});
                case "onImprisoned" -> {admit((EntityId)args[0]);yield null;}
                default -> throw new UnsupportedOperationException(name);
            });
            game=proxy(IGameController.class,(name,args)->switch(name) {
                case "onCreatureImprisoned" -> {admit((EntityId)args[0]);yield null;}
                default -> throw new UnsupportedOperationException(name);
            });
            room=proxy(IRoomController.class,(name,args)->switch(name) {
                case "getEntityId" -> roomId;
                case "getOwnerId" -> instance.getOwnerId();
                case "getRoomInstance" -> instance;
                case "hasObjectControl" -> args[0]==ObjectType.PRISONER;
                case "getObjectControl" -> storage;
                default -> throw new UnsupportedOperationException(name);
            });
            storage=new RoomPrisonerControl(kwd,room,data,()->20) {
                @Override protected List<Point> getCoordinates() {return List.of(new Point(1,1));}
                @Override protected int getNumberOfAccessibleTiles() {return 1;}
            };
            map=proxy(IMapController.class,(name,args)->switch(name) {
                case "getRoomControllerByCoordinates" -> inPrison ? room : null;
                default -> throw new UnsupportedOperationException(name);
            });
            controller=new CreatureController(entity,data,creature,null,null,()->20,
                    Map.of(),creatures,null,map,level,null,null,null,id->{ });
            if(bind) bindScripts();
        }
        private CreatureController controller() {return controller;}
        void admit(EntityId id) {lastAdmission=id;queue.enqueueImprisoned(id);}
        CreaturesController factory(boolean populated) {
            includeThings=populated;
            CreaturesController result=new CreaturesController(kwd,data,Map.of(),()->20,game,map,level);
            includeThings=true;return result;
        }
        void bindScripts() {
            if(scripts!=null) scripts.stop();
            scripts=new CreatureTriggerLogicController(null,level,null,map,creatures,null,data,queue);
        }
        void tick() {scripts.processTick(.25f);}
        @Override public void close() {if(scripts!=null) scripts.stop();data.close();}
    }

    @FunctionalInterface private interface Call {Object invoke(String name,Object[] args);}
    private static <T>T proxy(Class<T> type,Call call) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(),new Class<?>[]{type},
                (p,m,a)->call.invoke(m.getName(),a)));
    }
}
