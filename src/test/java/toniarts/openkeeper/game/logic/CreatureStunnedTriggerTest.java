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
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import toniarts.openkeeper.game.component.CreatureAi;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureFall;
import toniarts.openkeeper.game.component.CreatureImprisoned;
import toniarts.openkeeper.game.component.CreatureRecuperating;
import toniarts.openkeeper.game.component.Damage;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.InHand;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.Possessed;
import toniarts.openkeeper.game.component.Trigger;
import toniarts.openkeeper.game.component.Unconscious;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.creature.CreatureController;
import toniarts.openkeeper.game.controller.creature.CreatureState;
import toniarts.openkeeper.game.controller.shot.LightningShotProcessor;
import toniarts.openkeeper.game.controller.shot.ShotRequest;
import toniarts.openkeeper.game.event.GameEventService;
import toniarts.openkeeper.game.map.IMapDataInformation;
import toniarts.openkeeper.game.trigger.creature.CreatureTriggerLogicController;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.CreatureAngryFixtures;
import toniarts.openkeeper.tools.convert.map.CreatureSleepFixtures;
import toniarts.openkeeper.tools.convert.map.CreatureStunnedFixtures;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.ShotFixtures;
import toniarts.openkeeper.tools.convert.map.Thing;
import static toniarts.openkeeper.tools.convert.map.TriggerGeneric.TargetType.*;
import static org.junit.jupiter.api.Assertions.*;

class CreatureStunnedTriggerTest {

    @Test
    void actualKnockdownStoresStateAndTimerBeforeQueueingForTheSimulation() {
        try(Fixture f=new Fixture(255,false,false)) {
            f.controller.knockDown();
            assertEquals(CreatureState.FALLEN,f.ai().getCreatureState());
            assertEquals(20,f.ai().stateStartTime);
            assertEquals(CreatureState.FALLEN,f.stateAtEnqueue);
            assertEquals(20,f.timeAtEnqueue);
            assertEquals(0,f.flags[0]);
            f.tick();assertEquals(1,f.flags[0]);
            for(int i=0;i<8;i++) f.tick();
            assertEquals(1,f.flags[0]);
        }
    }

    @Test
    void repeatedAcceptedKnockdownsRestartTimeAndEmitAgainWhileDown() {
        try(Fixture f=new Fixture(255,false,false)) {
            f.controller.knockDown();
            f.time=21;
            f.controller.knockDown();
            f.tick();
            assertEquals(2,f.flags[0]);assertEquals(21,f.ai().stateStartTime);
        }
    }

    @Test
    void pendingAiTargetIsPreservedAcrossKnockdown() {
        try(Fixture f=new Fixture(255,false,false)) {
            f.data.setComponent(f.entity,new CreatureAi(0,CreatureState.IDLE,(short)6,CreatureState.SLEEPING));
            f.controller.knockDown();
            assertEquals(CreatureState.SLEEPING,f.ai().getTargetState());
            f.tick();assertEquals(1,f.flags[0]);
        }
    }

    @ParameterizedTest
    @ValueSource(strings={"NO_AI","DEAD","UNCONSCIOUS","RECUPERATING","FALLING","IN_HAND"})
    void existingKnockdownRejectionsDoNotQueueAnEvent(String reason) {
        try(Fixture f=new Fixture(255,false,false)) {
            switch(reason) {
                case "NO_AI" -> f.data.removeComponent(f.entity,CreatureAi.class);
                case "DEAD" -> f.data.removeComponent(f.entity,Health.class);
                case "UNCONSCIOUS" -> f.data.setComponent(f.entity,new Unconscious());
                case "RECUPERATING" -> f.data.setComponent(f.entity,new CreatureRecuperating());
                case "FALLING" -> f.data.setComponent(f.entity,new CreatureFall());
                case "IN_HAND" -> f.data.setComponent(f.entity,new InHand());
            }
            f.controller.knockDown();f.tick();
            assertEquals(0,f.flags[0]);assertNull(f.stateAtEnqueue);
        }
    }

    @ParameterizedTest
    @ValueSource(strings={"STUNNED","FALLEN","GETTING_UP"})
    void passiveStatesDoNotPollAsAnAcceptedStun(CreatureState state) {
        try(Fixture f=new Fixture(255,false,false)) {
            f.controller.getStateMachine().changeState(state);
            f.data.setComponent(f.entity,new CreatureAi(0,state,(short)6));
            for(int i=0;i<8;i++) f.tick();
            assertEquals(0,f.flags[0]);
        }
    }

    @Test
    void ordinaryHandDropLandingIsNotAnExplicitKnockdownEvent() {
        try(Fixture f=new Fixture(255,false,false)) {
            f.data.setComponent(f.entity,new CreatureFall());
            GameEventService events=proxy(GameEventService.class,(name,args)->null);
            CreatureFallSystem falls=new CreatureFallSystem(f.data,()->20,events);
            try {
                falls.processTick(1);
                assertNull(f.data.getComponent(f.entity,CreatureFall.class));
                assertEquals(CreatureState.FALLEN,f.ai().getCreatureState());
                f.tick();assertEquals(0,f.flags[0]);
            } finally {falls.stop();}
        }
    }

    @Test
    void realLightningProcessorRunsTheRealControllerAndDeliversTheEvent() {
        try(Fixture f=new Fixture(255,false,false)) {
            f.strike();
            assertEquals(7,f.data.getComponent(f.entity,Damage.class).damage);
            assertEquals(CreatureState.FALLEN,f.ai().getCreatureState());
            assertEquals(0,f.flags[0]);
            f.tick();assertEquals(1,f.flags[0]);
        }
    }

    @ParameterizedTest
    @ValueSource(strings={"IMMUNE","HORNY","POSSESSED","IMPRISONED"})
    void lightningExclusionsDoNotGenerateAKnockdownEvent(String reason) {
        try(Fixture f=new Fixture(255,reason.equals("IMMUNE"),reason.equals("HORNY"))) {
            if(reason.equals("POSSESSED")) f.data.setComponent(f.entity,new Possessed());
            if(reason.equals("IMPRISONED")) f.data.setComponent(f.entity,new CreatureImprisoned());
            f.strike();f.tick();
            assertEquals(0,f.flags[0]);
            assertEquals(CreatureState.IDLE,f.ai().getCreatureState());
        }
    }

    @ParameterizedTest
    @CsvSource({"1,1","2,2","255,3"})
    void repeatedKnockdownsRespectExistingScriptRepeatLimits(int repeats,int events) {
        try(Fixture f=new Fixture(repeats,false,false)) {
            for(int i=0;i<3;i++) f.controller.knockDown();
            f.tick();assertEquals(events,f.flags[0]);
        }
    }

    @Test
    void aSessionThreadCanKnockDownWithoutExecutingScriptActions() throws InterruptedException {
        try(Fixture f=new Fixture(255,false,false)) {
            AtomicReference<Throwable> failure=new AtomicReference<>();
            Thread session=new Thread(()->{
                try {f.strike();}
                catch(Throwable t) {failure.set(t);}
            },"synthetic-lightning-session");
            session.start();session.join();
            assertNull(failure.get());assertEquals(0,f.flags[0]);
            f.tick();assertEquals(1,f.flags[0]);
        }
    }

    @Test
    void mixedSlapAndKnockdownEventsKeepTheirTypesAndArrivalOrder() {
        try(Fixture f=new Fixture(255,false,false)) {
            f.records.put(1,CreatureAngryFixtures.condition(1,CREATURE_STUNNED,-1,2,255));
            f.records.put(2,CreatureAngryFixtures.condition(2,CREATURE_SLAPPED,-2,0,255));
            f.records.put(-2,CreatureAngryFixtures.action(-2,2));
            f.bindScripts();
            f.controller.knockDown();f.scripts.enqueueSlapped(f.entity);f.controller.knockDown();
            f.tick();
            assertEquals(List.of(0,2,0),f.actionOrder);
            assertEquals(2,f.flags[0]);assertEquals(1,f.flags[2]);
        }
    }

    @Test
    void nestedConditionsObserveDeliveryStateWithoutLatching() {
        try(Fixture f=new Fixture(255,false,false)) {
            f.records.put(1,CreatureAngryFixtures.condition(1,CREATURE_STUNNED,2,0,255));
            f.records.put(2,CreatureAngryFixtures.condition(2,FLAG,-1,0,255));
            f.bindScripts();
            f.controller.knockDown();f.tick();
            f.flags[1]=1;for(int i=0;i<4;i++) f.tick();
            assertEquals(0,f.flags[0]);
            f.controller.knockDown();f.tick();assertEquals(1,f.flags[0]);
        }
    }

    @Test
    void eventDoesNotMoveTheOrdinaryTraversalCursor() {
        try(Fixture f=new Fixture(255,false,false)) {
            f.records.put(1,CreatureAngryFixtures.condition(1,FLAG,-2,2,255));
            f.records.put(2,CreatureAngryFixtures.condition(2,CREATURE_STUNNED,-1,0,255));
            f.records.put(-2,CreatureAngryFixtures.action(-2,2));
            f.bindScripts();f.flags[1]=1;f.tick();
            f.controller.knockDown();f.tick();
            assertEquals(1,f.flags[0]);assertEquals(1,f.flags[2]);
        }
    }

    @Test
    void stopDiscardsPendingKnockdownEvents() {
        try(Fixture f=new Fixture(255,false,false)) {
            f.controller.knockDown();f.scripts.stop();f.tick();
            assertEquals(0,f.flags[0]);
        }
    }

    private static final class Fixture implements AutoCloseable {
        final EntityData data=new DefaultEntityData();
        final EntityId entity=data.createEntity();
        final int[] flags=new int[3];
        final List<Integer> actionOrder=new ArrayList<>();
        final Thread simulation=Thread.currentThread();
        final Map<Integer,toniarts.openkeeper.tools.convert.map.Trigger> records=new HashMap<>();
        final Creature creature;
        final IKwdFile kwd;
        final ILevelInfo level;
        final ICreaturesController creatures;
        final CreatureController controller;
        CreatureTriggerLogicController scripts;
        CreatureState stateAtEnqueue;
        double timeAtEnqueue;
        double time=20;

        Fixture(int repeats,boolean immune,boolean horny) {
            creature=CreatureStunnedFixtures.creature(immune,horny);
            records.put(1,CreatureAngryFixtures.condition(1,CREATURE_STUNNED,-1,0,repeats));
            records.put(-1,CreatureAngryFixtures.action(-1,0));
            CreatureComponent component=new CreatureComponent();component.creatureId=6;component.stunDuration=1;
            data.setComponents(entity,component,new Trigger(1),new Health(100,100),
                    new Owner((short)3,(short)3),new Position(0,new Vector3f()),
                    new CreatureAi(0,CreatureState.IDLE,(short)6));
            kwd=proxy(IKwdFile.class,(name,args)->switch(name) {
                case "getTriggers" -> records;
                case "getCreature" -> creature;
                case "getGameLevel" -> CreatureSleepFixtures.level(4);
                case "getThings" -> args[0]==Thing.GoodCreature.class
                        ? List.of(CreatureAngryFixtures.thing(1)) : List.of();
                default -> throw new UnsupportedOperationException(name);
            });
            level=proxy(ILevelInfo.class,(name,args)->{
                assertSame(simulation,Thread.currentThread());
                return switch(name) {
                    case "getLevelData" -> kwd;
                    case "getFlag" -> flags[((Number)args[0]).intValue()];
                    case "setFlag" -> {
                        int id=((Number)args[0]).intValue();flags[id]=(int)args[1];actionOrder.add(id);yield null;
                    }
                    default -> throw new UnsupportedOperationException(name);
                };
            });
            creatures=proxy(ICreaturesController.class,(name,args)->switch(name) {
                case "createController" -> controller();
                case "onStunned" -> {
                    stateAtEnqueue=ai().getCreatureState();timeAtEnqueue=ai().stateStartTime;
                    scripts.enqueueStunned((EntityId)args[0]);yield null;
                }
                default -> throw new UnsupportedOperationException(name);
            });
            controller=new CreatureController(entity,data,creature,null,null,()->time,
                    Map.of(),creatures,null,null,level,null,null,null,id->{ });
            bindScripts();
        }
        private CreatureController controller() {return controller;}
        CreatureAi ai() {return data.getComponent(entity,CreatureAi.class);}
        void strike() {
            IMapDataInformation<?> mapData=proxy(IMapDataInformation.class,(name,args)->null);
            IMapController map=proxy(IMapController.class,(name,args)->switch(name) {
                case "getMapData" -> mapData;
                default -> throw new UnsupportedOperationException(name);
            });
            new LightningShotProcessor(data,()->time,kwd,map,creatures).process(
                    new ShotRequest(ShotFixtures.thunderbolt(),7,0,(short)3,new Vector3f(),entity));
        }
        void bindScripts() {
            if(scripts!=null) scripts.stop();
            scripts=new CreatureTriggerLogicController(null,level,null,null,creatures,null,data);
        }
        void tick() {scripts.processTick(.25f);}
        @Override public void close() {scripts.stop();data.close();}
    }

    @FunctionalInterface private interface Call {Object invoke(String name,Object[] args);}
    private static <T>T proxy(Class<T> type,Call call) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(),new Class<?>[]{type},
                (p,m,a)->call.invoke(m.getName(),a)));
    }
}
