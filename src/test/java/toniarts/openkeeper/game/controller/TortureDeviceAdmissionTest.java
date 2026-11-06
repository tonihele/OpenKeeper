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

import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.HashSet;
import java.util.Set;
import java.util.Map;
import java.util.List;
import toniarts.openkeeper.game.logic.HealthSystem;
import toniarts.openkeeper.game.logic.IEntityPositionLookup;
import toniarts.openkeeper.tools.convert.map.CreatureSleepFixtures;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable.MiscType;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.component.*;
import toniarts.openkeeper.game.controller.creature.CreatureState;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.controller.room.AbstractRoomController.ObjectType;
import toniarts.openkeeper.game.controller.room.storage.RoomTortureeControl;
import toniarts.openkeeper.game.event.GameEventService;
import toniarts.openkeeper.game.logic.CreatureTorturingSystem;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.CreatureImprisonedFixtures;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.RoomFixtures;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;
import static org.junit.jupiter.api.Assertions.*;

class TortureDeviceAdmissionTest {

    @ParameterizedTest
    @ValueSource(shorts={8,61})
    void handDropOntoAChairOrWheelStoresAdmissionBeforeNotifying(short type) {
        try(Fixture f=new Fixture(type)) {
            f.drop(f.creature,new Vector2f());
            assertEquals(1,f.events.get());
            assertNotNull(f.data.getComponent(f.creature,CreatureTortured.class));
            assertEquals(3,f.data.getComponent(f.creature,Owner.class).controlId);
            assertEquals(1,f.data.getComponent(f.creature,Owner.class).ownerId);
            assertEquals(f.roomId,f.data.getComponent(f.creature,Stored.class).room);
            assertEquals(CreatureState.TORTURED,f.ai(f.creature));
            assertNull(f.data.getComponent(f.creature,CreatureFall.class));
            assertEquals(WorldUtils.FLOOR_HEIGHT,f.position(f.creature).y);
        }
    }

    @Test
    void emptyFloorDropReleasesStaleTortureAndFallsWithoutAnEvent() {
        try(Fixture f=new Fixture((short)61)) {
            f.data.setComponents(f.creature,new CreatureTortured(4,0,0),
                    new Stored(f.roomId,ObjectType.TORTUREE),new Owner((short)1,(short)3));
            f.drop(f.creature,new Vector2f(1,0));
            f.assertReleased(f.creature);
            assertNotNull(f.data.getComponent(f.creature,CreatureFall.class));
            assertEquals(WorldUtils.DROP_HEIGHT,f.position(f.creature).y);
            assertEquals(0,f.events.get());
        }
    }

    @Test
    void rejectedDropOutsideAnyRoomAlsoClearsCaptivity() {
        try(Fixture f=new Fixture((short)61)) {
            f.data.setComponents(f.creature,new CreatureTortured(4,0,0),
                    new Stored(f.roomId,ObjectType.TORTUREE));
            f.roomExists=false;f.drop(f.creature,new Vector2f());
            f.assertReleased(f.creature);assertEquals(0,f.events.get());
        }
    }

    @Test
    void adjacentDeviceNeverCapturesACreatureOnEmptyFloor() {
        try(Fixture f=new Fixture((short)61)) {
            assertFalse(f.storage.tryAdmit(f.creature,new Vector2f(1,0)));
            assertNull(f.data.getComponent(f.creature,Stored.class));
            assertEquals(0,f.storage.getCurrentCapacity());
        }
    }

    @Test
    void anotherTorturedCreatureBlocksHandDropAndReleasingItFreesTheDevice() {
        try(Fixture f=new Fixture((short)61)) {
            assertTrue(f.storage.tryAdmit(f.creature,new Vector2f()));
            EntityId other=f.creature((short)6,(short)1);
            f.drop(other,new Vector2f());
            assertNull(f.data.getComponent(other,CreatureTortured.class));
            assertEquals(0,f.events.get());assertEquals(1,f.storage.getCurrentCapacity());
            f.storage.removeItem(f.creature);RoomTortureeControl.release(f.data,f.creature,20);
            f.drop(other,new Vector2f());assertEquals(1,f.events.get());
            assertEquals(1,f.storage.getCurrentCapacity());
        }
    }

    @Test
    void idleBystandersDoNotReserveDevices() {
        try(Fixture f=new Fixture((short)61)) {
            f.creature((short)6,(short)1);
            assertTrue(f.storage.tryAdmit(f.creature,new Vector2f()));
        }
    }

    @Test
    void repeatedAdmissionOfTheSameCreatureDoesNotInflateCapacity() {
        try(Fixture f=new Fixture((short)61)) {
            f.drop(f.creature,new Vector2f());f.drop(f.creature,new Vector2f());
            assertEquals(2,f.events.get());assertEquals(1,f.storage.getCurrentCapacity());
            f.storage.removeItem(f.creature);f.storage.removeItem(f.creature);
            assertEquals(0,f.storage.getCurrentCapacity());
        }
    }

    @Test
    void pickingUpTheOccupantReleasesItsTile() {
        try(Fixture f=new Fixture((short)61)) {
            assertTrue(f.storage.tryAdmit(f.creature,new Vector2f()));
            f.data.removeComponent(f.creature,Position.class);
            f.data.removeComponent(f.creature,CreatureAi.class);
            f.data.removeComponent(f.creature,CreatureTortured.class);
            f.storage.removeItem(f.creature);
            assertTrue(f.storage.tryAdmit(f.creature((short)6,(short)1),new Vector2f()));
        }
    }

    @Test
    void proximityUsesNativeFixedPointDistanceTruncation() {
        try(Fixture f=new Fixture((short)61)) {
            f.devicePosition(-.3f,-.4f);
            assertFalse(f.storage.tryAdmit(f.creature,new Vector2f(.3005f,.4005f)));
            assertTrue(f.storage.tryAdmit(f.creature,new Vector2f(.3f,.4f)));
            assertTrue(f.storage.tryAdmit(f.creature,new Vector2f(.29f,.39f)));
        }
    }

    @ParameterizedTest
    @ValueSource(shorts={7,9,62})
    void decorationsAndOtherRoomFurnitureAreNotTortureDevices(short type) {
        try(Fixture f=new Fixture(type)) {
            f.drop(f.creature,new Vector2f());assertEquals(0,f.events.get());
            assertNull(f.data.getComponent(f.creature,CreatureTortured.class));
        }
    }

    @Test
    void wallFurnitureCanProvideAUsableDevice() {
        try(Fixture f=new Fixture((short)8)) {
            f.floor.clear();f.wall.add(f.device);
            assertTrue(f.storage.tryAdmit(f.creature,new Vector2f()));
        }
    }

    @Test
    void removedFurnitureInAStaleRoomSetIsNotUsable() {
        try(Fixture f=new Fixture((short)61)) {
            f.data.removeEntity(f.device);
            f.drop(f.creature,new Vector2f());assertEquals(0,f.events.get());
            assertNull(f.data.getComponent(f.creature,CreatureTortured.class));
        }
    }

    @Test
    void furnitureRemovedFromTheRoomCannotAdmitEvenIfTheEntityStillExists() {
        try(Fixture f=new Fixture((short)61)) {
            f.floor.remove(f.device);
            assertFalse(f.storage.tryAdmit(f.creature,new Vector2f()));
        }
    }

    @Test
    void occupyingOneDeviceDoesNotBlockAnotherDeviceInTheSameRoom() {
        try(Fixture f=new Fixture((short)61)) {
            EntityId second=f.data.createEntity();
            f.data.setComponents(second,new ObjectComponent((short)8,ObjectType.TORTUREE),
                    new Position(0,new Vector3f(1,0,0)));
            f.floor.add(second);f.storage.updateMaxCapacity();
            f.drop(f.creature,new Vector2f());
            EntityId other=f.creature((short)6,(short)1);f.drop(other,new Vector2f(1,0));
            assertEquals(2,f.events.get());assertEquals(2,f.storage.getCurrentCapacity());
            assertEquals(2,f.storage.getMaxCapacity());
        }
    }

    @Test
    void movingFurnitureToAnotherTileInvalidatesTheOldDropPoint() {
        try(Fixture f=new Fixture((short)61)) {
            f.devicePosition(1,0);
            assertFalse(f.storage.tryAdmit(f.creature,new Vector2f()));
            assertTrue(f.storage.tryAdmit(f.creature,new Vector2f(1,0)));
        }
    }

    @Test
    void deviceOutsideTheRoomCannotAdmitACreature() {
        try(Fixture f=new Fixture((short)61)) {
            f.devicePosition(2,0);
            assertFalse(f.storage.tryAdmit(f.creature,new Vector2f(2,0)));
        }
    }

    @Test
    void ownRoomNonImpCanBeTortured() {
        try(Fixture f=new Fixture((short)61)) {
            f.instance.setOwnerId((short)1);
            f.drop(f.creature,new Vector2f());assertEquals(1,f.events.get());
            assertEquals(1,f.data.getComponent(f.creature,Owner.class).controlId);
        }
    }

    @Test
    void ownRoomImpIsReleasedWithoutAnEvent() {
        try(Fixture f=new Fixture((short)61)) {
            EntityId imp=f.creature((short)1,(short)3);
            f.drop(imp,new Vector2f());assertEquals(0,f.events.get());
            assertNull(f.data.getComponent(imp,CreatureTortured.class));
        }
    }

    @ParameterizedTest
    @ValueSource(ints={0,-1})
    void deadOrUnhealthyCreaturesCannotReserveADevice(int health) {
        try(Fixture f=new Fixture((short)61)) {
            if(health<0) f.data.removeComponent(f.creature,Health.class);
            else f.data.setComponent(f.creature,new Health(health,100));
            assertFalse(f.storage.tryAdmit(f.creature,new Vector2f()));
            assertEquals(0,f.storage.getCurrentCapacity());
        }
    }

    @Test
    void removalDuringTortureReleasesStateInsteadOfContinuingConversion() {
        try(Fixture f=new Fixture((short)61)) {
            f.drop(f.creature,new Vector2f());f.data.removeEntity(f.device);
            f.systemTick();f.assertReleased(f.creature);
            assertEquals(CreatureState.IDLE,f.ai(f.creature));
            assertEquals(0,f.storage.getCurrentCapacity());assertEquals(1,f.events.get());
        }
    }

    @Test
    void destroyedRoomReleasesItsIndexedCreatures() {
        try(Fixture f=new Fixture((short)61)) {
            f.drop(f.creature,new Vector2f());f.storage.destroy();
            f.assertReleased(f.creature);assertEquals(CreatureState.IDLE,f.ai(f.creature));
        }
    }

    @Test
    void discardingANewControlDuringRoomReconstructionDoesNotReleaseExistingCaptives() {
        try(Fixture f=new Fixture((short)61)) {
            f.drop(f.creature,new Vector2f());f.storage().destroy();
            assertNotNull(f.data.getComponent(f.creature,CreatureTortured.class));
            assertEquals(1,f.storage.getCurrentCapacity());
        }
    }

    @Test
    void missingRoomAtSimulationValidationReleasesTheCreature() {
        try(Fixture f=new Fixture((short)61)) {
            f.drop(f.creature,new Vector2f());f.roomExists=false;f.systemTick();
            f.assertReleased(f.creature);assertEquals(CreatureState.IDLE,f.ai(f.creature));
        }
    }

    @Test
    void aValidAdmissionSurvivesSimulationValidationWithoutAnotherEvent() {
        try(Fixture f=new Fixture((short)61)) {
            f.drop(f.creature,new Vector2f());f.systemTick();
            assertNotNull(f.data.getComponent(f.creature,CreatureTortured.class));
            assertEquals(1,f.events.get());
        }
    }

    @Test
    void removedDeviceDoesNotApplyAnotherTortureHealthChange() {
        try(Fixture f=new Fixture((short)61)) {
            f.drop(f.creature,new Vector2f());f.data.removeEntity(f.device);
            f.healthTick();assertEquals(100,f.data.getComponent(f.creature,Health.class).health);
            f.assertReleased(f.creature);
        }
    }

    @Test
    void validDeviceStillAppliesTheConfiguredTortureHealthChange() {
        try(Fixture f=new Fixture((short)61)) {
            f.drop(f.creature,new Vector2f());f.healthTick();
            assertEquals(90,f.data.getComponent(f.creature,Health.class).health);
        }
    }

    @Test
    void concurrentProducersCannotBothClaimOneDevice() throws InterruptedException {
        try(Fixture f=new Fixture((short)61)) {
            EntityId other=f.creature((short)6,(short)1);
            CountDownLatch ready=new CountDownLatch(2),start=new CountDownLatch(1);
            AtomicInteger accepted=new AtomicInteger();AtomicReference<Throwable> failure=new AtomicReference<>();
            Thread[] threads=new Thread[2];EntityId[] ids={f.creature,other};
            for(int i=0;i<2;i++) {
                final EntityId id=ids[i];
                threads[i]=new Thread(()->{
                    ready.countDown();
                    try {start.await();if(f.storage.tryAdmit(id,new Vector2f())) accepted.incrementAndGet();}
                    catch(Throwable t) {failure.set(t);}
                });threads[i].start();
            }
            ready.await();start.countDown();for(Thread thread:threads) thread.join();
            assertNull(failure.get());assertEquals(1,accepted.get());assertEquals(1,f.storage.getCurrentCapacity());
        }
    }

    private static final class Fixture implements AutoCloseable {
        final EntityData data=new DefaultEntityData();
        final EntityId roomId=data.createEntity(),device=data.createEntity();
        final Set<EntityId> floor=new HashSet<>(),wall=new HashSet<>();
        final RoomInstance instance=new RoomInstance(RoomFixtures.room((short)12,0));
        final AtomicInteger events=new AtomicInteger();
        final IKwdFile kwd;
        final IRoomController room;
        final RoomTortureeControl storage;
        final EntityId creature;
        boolean roomExists=true;

        Fixture(short type) {
            instance.setOwnerId((short)3);instance.addCoordinate(new Point(0,0));instance.addCoordinate(new Point(1,0));
            data.setComponents(device,new ObjectComponent(type,ObjectType.TORTUREE),new Position(0,new Vector3f()));
            floor.add(device);
            kwd=proxy(IKwdFile.class,(name,args)->switch(name) {
                case "getImp" -> CreatureImprisonedFixtures.imp();
                case "getCreature" -> CreatureImprisonedFixtures.tortureVictim();
                default -> throw new UnsupportedOperationException(name);
            });
            room=proxy(IRoomController.class,(name,args)->switch(name) {
                case "getEntityId" -> roomId;
                case "getOwnerId" -> instance.getOwnerId();
                case "getRoomInstance" -> instance;
                case "getFloorFurniture" -> floor;
                case "getWallFurniture" -> wall;
                case "hasObjectControl" -> args[0]==ObjectType.TORTUREE;
                case "getObjectControl" -> currentStorage();
                default -> throw new UnsupportedOperationException(name);
            });
            storage=storage();creature=creature((short)6,(short)1);
        }
        RoomTortureeControl currentStorage() {return storage;}
        RoomTortureeControl storage() {
            return new RoomTortureeControl(kwd,room,data,()->20) {
                @Override protected int getNumberOfAccessibleTiles() {return getCoordinates().size();}
            };
        }
        EntityId creature(short type,short owner) {
            EntityId id=data.createEntity();CreatureComponent component=new CreatureComponent();
            component.creatureId=type;component.tortureTimeToConvert=10000;
            data.setComponents(id,component,new Health(100,100),new Owner(owner,owner),
                    new Position(0,new Vector3f()),new CreatureAi(0,CreatureState.IDLE,type),
                    new CreatureViewState(type,0,Creature.AnimationType.IDLE_1));return id;
        }
        void drop(EntityId id,Vector2f position) {
            GameWorldController.dropCreature(data,()->20,proxy(GameEventService.class,(name,args)->null),
                    entity->{throw new AssertionError("Unexpected prison admission");},
                    entity->{
                        assertNotNull(data.getComponent(entity,CreatureTortured.class));
                        assertNotNull(data.getComponent(entity,Stored.class));
                        assertEquals(CreatureState.TORTURED,ai(entity));events.incrementAndGet();
                    },roomExists?room:null,id,WorldUtils.vectorToPoint(position),position,instance.getOwnerId(),captive->null);
        }
        void systemTick() {
            IMapController map=proxy(IMapController.class,(name,args)->switch(name) {
                case "getRoomControllerByCoordinates" -> roomExists?room:null;
                default -> throw new UnsupportedOperationException(name);
            });
            ICreaturesController creatures=proxy(ICreaturesController.class,(name,args)->{throw new UnsupportedOperationException(name);});
            CreatureTorturingSystem system=new CreatureTorturingSystem(data,creatures,map);
            try {system.processTick(.25f);} finally {system.stop();}
        }
        void healthTick() {
            var settings=Map.of(
                    MiscType.CREATURE_DYING_STATE_DURATION_SECONDS,CreatureSleepFixtures.variable(MiscType.CREATURE_DYING_STATE_DURATION_SECONDS,10),
                    MiscType.PRISON_MODIFY_CREATURE_HEALTH_PER_SECOND,CreatureSleepFixtures.variable(MiscType.PRISON_MODIFY_CREATURE_HEALTH_PER_SECOND,0),
                    MiscType.MODIFY_HEALTH_OF_CREATURE_IN_LAIR_PER_SECOND,CreatureSleepFixtures.variable(MiscType.MODIFY_HEALTH_OF_CREATURE_IN_LAIR_PER_SECOND,0));
            IMapController map=proxy(IMapController.class,(name,args)->switch(name) {
                case "getRoomControllerByCoordinates" -> roomExists?room:null;
                default -> throw new UnsupportedOperationException(name);
            });
            ILevelInfo level=proxy(ILevelInfo.class,(name,args)->switch(name) {
                case "getLevelData" -> kwd;
                default -> throw new UnsupportedOperationException(name);
            });
            HealthSystem system=new HealthSystem(data,proxy(IEntityPositionLookup.class,(name,args)->{throw new UnsupportedOperationException(name);}),
                    settings,null,level,List.of(),map,()->21,proxy(GameEventService.class,(name,args)->null));
            try {system.processTick(21);} finally {system.stop();}
        }
        void devicePosition(float x,float y) {data.setComponent(device,new Position(0,new Vector3f(x,0,y)));}
        CreatureState ai(EntityId id) {return data.getComponent(id,CreatureAi.class).getCreatureState();}
        Vector3f position(EntityId id) {return data.getComponent(id,Position.class).position;}
        void assertReleased(EntityId id) {
            assertNull(data.getComponent(id,CreatureTortured.class));assertNull(data.getComponent(id,Stored.class));
            Owner owner=data.getComponent(id,Owner.class);assertEquals(owner.ownerId,owner.controlId);
        }
        @Override public void close() {data.close();}
    }

    @FunctionalInterface private interface Call {Object invoke(String name,Object[] args);}
    private static <T>T proxy(Class<T> type,Call call) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(),new Class<?>[]{type},(p,m,a)->call.invoke(m.getName(),a)));
    }
}
