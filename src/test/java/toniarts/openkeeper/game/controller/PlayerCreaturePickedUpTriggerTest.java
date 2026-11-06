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
package toniarts.openkeeper.game.controller;

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
import toniarts.openkeeper.game.component.*;
import toniarts.openkeeper.game.controller.creature.CreatureState;
import toniarts.openkeeper.game.controller.player.PlayerHandControl;
import toniarts.openkeeper.game.controller.player.PlayerStatsControl;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.controller.room.AbstractRoomController.ObjectType;
import toniarts.openkeeper.game.controller.room.storage.IRoomObjectControl;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.event.GameEventService;
import toniarts.openkeeper.game.trigger.player.PlayerCreatureEventQueue;
import toniarts.openkeeper.game.trigger.player.PlayerTriggerLogicController;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.PlayerDropFixtures;
import static org.junit.jupiter.api.Assertions.*;
import static toniarts.openkeeper.tools.convert.map.TriggerGeneric.TargetType.*;

class PlayerCreaturePickedUpTriggerTest {

    @Test
    void realEnemyCreaturePickupQueuesOnlyThePickingPlayersScriptAfterCleanup() {
        try(Fixture f=new Fixture(255,true,4)) {
            assertTrue(f.pick(f.entity,(short)3));
            assertEquals(f.entity,f.hands.get((short)3).peek());
            assertEquals(1,f.data.getComponent(f.entity,Owner.class).ownerId);
            assertEquals(3,f.data.getComponent(f.entity,InHand.class).playerId);
            assertNull(f.data.getComponent(f.entity,Position.class));
            assertNull(f.data.getComponent(f.entity,CreatureAi.class));
            assertEquals(0,f.flags[0]);f.tick();
            assertEquals(1,f.flags[0]);assertEquals(0,f.flags[2]);
        }
    }

    @Test
    void anotherPlayerGetsItsOwnPickupEvent() {
        try(Fixture f=new Fixture(255,true,4)) {
            f.data.setComponent(f.entity,new Owner((short)1,(short)4));
            assertTrue(f.pick(f.entity,(short)4));f.tick();
            assertEquals(0,f.flags[0]);assertEquals(1,f.flags[2]);
        }
    }

    @ParameterizedTest
    @CsvSource({"0,0","6,12","255,255"})
    void authoredCreatureAndRoomOperandsDoNotFilterNativePickupEvents(int creature,int room) {
        try(Fixture f=new Fixture(255,false,4)) {
            f.records.put(1,PlayerDropFixtures.pickupCondition(1,-1,0,255,creature,room));
            f.bind();f.pick(f.entity,(short)3);f.tick();assertEquals(1,f.flags[0]);
        }
    }

    @Test
    void merelyRemainingInHandDoesNotRepeatTheEvent() {
        try(Fixture f=new Fixture(255,true,4)) {
            f.pick(f.entity,(short)3);f.tick();
            for(int i=0;i<12;i++) f.tick();
            assertEquals(1,f.flags[0]);assertEquals(1,f.notifications);
        }
    }

    @Test
    void existingPlayerPickupHistoryIsNotPolledAsAnEvent() {
        try(Fixture f=new Fixture(255,true,4)) {
            f.stats.get((short)3).creaturePickedUp(f.definition);
            for(int i=0;i<12;i++) f.tick();
            assertEquals(0,f.flags[0]);
        }
    }

    @Test
    void anAlreadyHeldRequestIsRejectedWithoutAnotherNotification() {
        try(Fixture f=new Fixture(255,true,4)) {
            assertTrue(f.pick(f.entity,(short)3));assertFalse(f.pick(f.entity,(short)3));
            f.tick();assertEquals(1,f.flags[0]);assertEquals(1,f.notifications);
        }
    }

    @Test
    void aFullHandRejectsPickupWithoutAnEventOrCleanup() {
        try(Fixture f=new Fixture(255,true,1)) {
            EntityId fill=f.creature();f.hands.get((short)3).push(fill);
            assertFalse(f.pick(f.entity,(short)3));f.tick();
            assertNotNull(f.data.getComponent(f.entity,Position.class));assertEquals(0,f.flags[0]);
        }
    }

    @ParameterizedTest
    @ValueSource(strings={"wrong-controller","not-interactable","not-pickable","dead","unconscious"})
    void rejectedCreatureRequestsDoNotNotify(String kind) {
        try(Fixture f=new Fixture(255,true,4)) {
            switch(kind) {
                case "wrong-controller" -> f.data.setComponent(f.entity,new Owner((short)1,(short)4));
                case "not-interactable" -> f.data.removeComponent(f.entity,Interaction.class);
                case "not-pickable" -> f.data.setComponent(f.entity,new Interaction(true,false,false,false,false));
                case "dead" -> f.data.removeComponent(f.entity,Health.class);
                case "unconscious" -> f.data.setComponent(f.entity,new Unconscious());
                default -> throw new AssertionError(kind);
            }
            assertFalse(f.pick(f.entity,(short)3));f.tick();
            assertNull(f.data.getComponent(f.entity,InHand.class));assertEquals(0,f.notifications);
            assertEquals(0,f.flags[0]);
        }
    }

    @Test
    void anObjectCanBePickedUpWithoutEmittingACreatureEvent() {
        try(Fixture f=new Fixture(255,true,4)) {
            f.data.removeComponent(f.entity,CreatureComponent.class);
            f.data.setComponents(f.entity,new ObjectComponent((short)4,ObjectType.SPELL_BOOK),new Owner((short)3,(short)3));
            assertTrue(f.pick(f.entity,(short)3));
            assertEquals(ViewType.OBJECT,f.data.getComponent(f.entity,InHand.class).type);
            f.tick();assertEquals(0,f.notifications);assertEquals(0,f.flags[0]);
        }
    }

    @Test
    void storageAndCaptivityCleanupFinishBeforeThePickupIsNotified() {
        try(Fixture f=new Fixture(255,true,4)) {
            EntityId room=f.data.createEntity();
            f.data.setComponents(f.entity,new Stored(room,ObjectType.PRISONER),
                    new CreatureImprisoned(0,0),new CreatureTortured(0,0,0),new Navigation());
            IRoomObjectControl<?> storage=proxy(IRoomObjectControl.class,(name,args)->switch(name) {
                case "removeItem" -> {f.data.removeComponent((EntityId)args[0],Stored.class);yield null;}
                default -> throw new UnsupportedOperationException(name);
            });
            f.storageRoom=proxy(IRoomController.class,(name,args)->switch(name) {
                case "getObjectControl" -> storage;
                default -> throw new UnsupportedOperationException(name);
            });
            assertTrue(f.pick(f.entity,(short)3));f.tick();assertEquals(1,f.flags[0]);
        }
    }

    @Test
    void twoAcceptedPickupsAtTheSameTimeRetainBothEvents() {
        try(Fixture f=new Fixture(255,true,4)) {
            f.pick(f.entity,(short)3);f.pick(f.creature(),(short)3);
            f.tick();assertEquals(2,f.flags[0]);assertEquals(2,f.notifications);
        }
    }

    @ParameterizedTest
    @CsvSource({"1,1","2,2","255,3"})
    void successfulRepickupsUseExistingRepeatLimits(int repeat,int expected) {
        try(Fixture f=new Fixture(repeat,true,4)) {
            for(int i=0;i<3;i++) {
                assertTrue(f.pick(f.entity,(short)3));f.replaceOnMap(f.entity,(short)3);
            }
            f.tick();assertEquals(expected,f.flags[0]);assertEquals(3,f.notifications);
        }
    }

    @Test
    void successfulPickupOnAProducerThreadDoesNotTraverseScripts() throws InterruptedException {
        try(Fixture f=new Fixture(255,true,4)) {
            AtomicReference<Throwable> failure=new AtomicReference<>();
            Thread producer=new Thread(()->{try {assertTrue(f.pick(f.entity,(short)3));} catch(Throwable t) {failure.set(t);}});
            producer.start();producer.join();assertNull(failure.get());assertEquals(0,f.flags[0]);
            f.tick();assertEquals(1,f.flags[0]);
        }
    }

    @Test
    void anEarlyPickupSurvivesUntilPlayerScriptControlsExist() {
        try(Fixture f=new Fixture(255,false,4)) {
            f.pick(f.entity,(short)3);f.bind();f.tick();assertEquals(1,f.flags[0]);
        }
    }

    @Test
    void pickupAndDropEventsAcrossPlayersPreserveArrivalOrder() {
        try(Fixture f=new Fixture(255,false,4)) {
            f.records.put(1,PlayerDropFixtures.pickupCondition(1,-1,3,255,255,255));
            f.records.put(3,PlayerDropFixtures.condition(3,PLAYER_CREATURE_DROPPED,-3,0,255));
            f.records.put(-3,PlayerDropFixtures.action(-3,3,0));f.bind();
            f.pick(f.entity,(short)3);f.queue.enqueueDropped((short)3);
            f.replaceOnMap(f.entity,(short)3);f.data.setComponent(f.entity,new Owner((short)1,(short)4));
            f.pick(f.entity,(short)4);f.tick();assertEquals(List.of(0,3,2),f.actionOrder);
        }
    }

    @Test
    void nestedConditionsUseDeliveryTimeWithoutLatchingEarlierPickups() {
        try(Fixture f=new Fixture(255,false,4)) {
            f.records.put(1,PlayerDropFixtures.pickupCondition(1,3,0,255,255,255));
            f.records.put(3,PlayerDropFixtures.condition(3,FLAG,-1,0,255));f.bind();
            f.pick(f.entity,(short)3);f.tick();assertEquals(0,f.flags[0]);
            f.flags[1]=1;for(int i=0;i<4;i++) f.tick();assertEquals(0,f.flags[0]);
            f.replaceOnMap(f.entity,(short)3);f.pick(f.entity,(short)3);f.flags[1]=0;
            f.tick();assertEquals(0,f.flags[0]);
            f.replaceOnMap(f.entity,(short)3);f.pick(f.entity,(short)3);f.flags[1]=1;
            f.tick();assertEquals(1,f.flags[0]);
        }
    }

    @Test
    void nestedPickupConditionsAreNotIndependentlyDispatched() {
        try(Fixture f=new Fixture(255,false,4)) {
            f.records.put(1,PlayerDropFixtures.condition(1,FLAG,3,0,255));
            f.records.put(3,PlayerDropFixtures.pickupCondition(3,-1,0,255,255,255));f.bind();
            f.flags[1]=1;f.pick(f.entity,(short)3);for(int i=0;i<5;i++) f.tick();assertEquals(0,f.flags[0]);
        }
    }

    @Test
    void everyMatchingRootRunsOnOnePickup() {
        try(Fixture f=new Fixture(255,false,4)) {
            f.records.put(1,PlayerDropFixtures.pickupCondition(1,-1,3,255,255,255));
            f.records.put(3,PlayerDropFixtures.pickupCondition(3,-3,0,255,255,255));
            f.records.put(-3,PlayerDropFixtures.action(-3,3,0));f.bind();
            f.pick(f.entity,(short)3);f.tick();assertEquals(1,f.flags[0]);assertEquals(1,f.flags[3]);
        }
    }

    @Test
    void eventDeliveryDoesNotMoveTheOrdinaryCursor() {
        try(Fixture f=new Fixture(255,false,4)) {
            f.records.put(1,PlayerDropFixtures.condition(1,FLAG,-3,3,255));
            f.records.put(3,PlayerDropFixtures.pickupCondition(3,-1,0,255,255,255));
            f.records.put(-3,PlayerDropFixtures.action(-3,3,0));f.bind();
            f.flags[1]=1;f.tick();f.pick(f.entity,(short)3);f.tick();
            assertEquals(1,f.flags[0]);assertEquals(1,f.flags[3]);
        }
    }

    @Test
    void playersWithoutScriptsAndOtherEventTypesAreIgnored() {
        try(Fixture f=new Fixture(255,true,4)) {
            f.queue.enqueuePickedUp((short)7);
            f.scripts.onCreatureEvent((short)3,PLAYER_CREATURE_SLAPPED);f.tick();assertEquals(0,f.flags[0]);
        }
    }

    @Test
    void clearingTheServerBufferDiscardsPendingPickupsAndDrops() {
        try(Fixture f=new Fixture(255,false,4)) {
            f.records.put(1,PlayerDropFixtures.pickupCondition(1,-1,3,255,255,255));
            f.records.put(3,PlayerDropFixtures.condition(3,PLAYER_CREATURE_DROPPED,-3,0,255));
            f.records.put(-3,PlayerDropFixtures.action(-3,3,0));f.bind();
            f.pick(f.entity,(short)3);f.queue.enqueueDropped((short)3);f.queue.clear();f.tick();
            assertEquals(0,f.flags[0]);assertEquals(0,f.flags[3]);
        }
    }

    private static final class Fixture implements AutoCloseable {
        final EntityData data=new DefaultEntityData();
        final EntityId entity=creature();
        final PlayerCreatureEventQueue queue=new PlayerCreatureEventQueue();
        final Map<Short,PlayerHandControl> hands=new HashMap<>();
        final Map<Short,PlayerStatsControl> stats=new HashMap<>();
        final Map<Short,IPlayerController> players=new HashMap<>();
        final int[] flags=new int[4];
        final Thread simulation=Thread.currentThread();
        final List<Integer> actionOrder=new ArrayList<>();
        final Map<Integer,toniarts.openkeeper.tools.convert.map.Trigger> records=new HashMap<>();
        final Creature definition=new Creature();
        final IKwdFile kwd;
        final ILevelInfo level;
        final IMapController map;
        final IGameController game;
        IRoomController storageRoom;
        PlayerTriggerLogicController scripts;
        int notifications;

        Fixture(int repeat,boolean bind,int capacity) {
            records.put(1,PlayerDropFixtures.pickupCondition(1,-1,0,repeat,255,255));
            records.put(-1,PlayerDropFixtures.action(-1,0,0));
            records.put(100,PlayerDropFixtures.pickupCondition(100,-100,0,255,255,255));
            records.put(-100,PlayerDropFixtures.action(-100,2,0));
            for(short id:new short[]{3,4}) {
                hands.put(id,new PlayerHandControl(new Keeper(false,id),capacity,data));
                stats.put(id,new PlayerStatsControl());final short player=id;
                players.put(id,proxy(IPlayerController.class,(name,args)->switch(name) {
                    case "getHandControl" -> hands.get(player);
                    case "getStatsControl" -> stats.get(player);
                    default -> throw new UnsupportedOperationException(name);
                }));
            }
            kwd=proxy(IKwdFile.class,(name,args)->switch(name) {
                case "getTriggers" -> records;
                case "getPlayers" -> Map.of((short)3,PlayerDropFixtures.player((short)3,1),(short)4,PlayerDropFixtures.player((short)4,100));
                case "getCreature" -> definition;
                default -> throw new UnsupportedOperationException(name);
            });
            level=proxy(ILevelInfo.class,(name,args)->switch(name) {
                case "getLevelData" -> kwd;
                case "getPlayers" -> Map.of((short)3,new Keeper(false,(short)3),(short)4,new Keeper(false,(short)4));
                case "getFlag" -> {assertSame(simulation,Thread.currentThread());yield flags[((Number)args[0]).intValue()];}
                case "setFlag" -> {assertSame(simulation,Thread.currentThread());int id=((Number)args[0]).intValue();flags[id]=(int)args[1];actionOrder.add(id);yield null;}
                default -> throw new UnsupportedOperationException(name);
            });
            game=proxy(IGameController.class,(name,args)->switch(name) {
                case "getPlayerController" -> players.get(((Number)args[0]).shortValue());
                default -> throw new UnsupportedOperationException(name);
            });
            map=proxy(IMapController.class,(name,args)->switch(name) {
                case "getMapData" -> null;
                case "getRoomControllerByCoordinates" -> storageRoom;
                default -> throw new UnsupportedOperationException(name);
            });
            if(bind) bind();
        }
        EntityId creature() {
            EntityId id=data.createEntity();CreatureComponent creature=new CreatureComponent();creature.creatureId=6;
            data.setComponents(id,creature,new Health(100,100),new Owner((short)1,(short)3),
                    new Position(0,new Vector3f()),new CreatureAi(0,CreatureState.IDLE,(short)6),
                    new Interaction(true,false,true,false,false));return id;
        }
        boolean pick(EntityId id,short player) {
            return GameWorldController.pickUpToHand(data,map,players,proxy(GameEventService.class,(name,args)->null),
                    pickingPlayer->{
                        EntityId held=hands.get(pickingPlayer).peek();
                        assertEquals(pickingPlayer.shortValue(),data.getComponent(held,InHand.class).playerId);
                        assertNull(data.getComponent(held,Position.class));assertNull(data.getComponent(held,CreatureAi.class));
                        assertNull(data.getComponent(held,CreatureImprisoned.class));assertNull(data.getComponent(held,CreatureTortured.class));
                        assertNull(data.getComponent(held,Stored.class));assertNull(data.getComponent(held,Navigation.class));
                        notifications++;queue.enqueuePickedUp(pickingPlayer);
                    },id,player);
        }
        void replaceOnMap(EntityId id,short player) {
            assertEquals(id,hands.get(player).pop());
            data.setComponents(id,new Position(0,new Vector3f()),new CreatureAi(0,CreatureState.IDLE,(short)6));
        }
        void bind() {scripts=new PlayerTriggerLogicController(game,level,null,map,null,null);}
        void tick() {queue.drain(scripts::onCreatureEvent);scripts.processTick(.25f);}
        @Override public void close() {queue.clear();if(scripts!=null) scripts.stop();data.close();}
    }

    @FunctionalInterface private interface Call {Object invoke(String name,Object[] args);}
    private static <T>T proxy(Class<T> type,Call call) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(),new Class<?>[]{type},(p,m,a)->call.invoke(m.getName(),a)));
    }
}
