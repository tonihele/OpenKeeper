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
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureTortured;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.Objective;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.Trigger;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.creature.CreatureController;
import toniarts.openkeeper.game.map.FakeMapData;
import toniarts.openkeeper.game.map.FakeMapInformation;
import toniarts.openkeeper.game.trigger.creature.CreatureTriggerLogicController;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.CreatureAngryFixtures;
import toniarts.openkeeper.tools.convert.map.CreatureSleepFixtures;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Thing;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable.MiscType;
import static toniarts.openkeeper.tools.convert.map.TriggerGeneric.TargetType.*;
import static org.junit.jupiter.api.Assertions.*;

class CreatureConvertedTriggerTest {

    @Test
    void explicitConversionDeliversAfterNewOwnershipAndObjectiveCleanup() {
        try (Fixture f=new Fixture(255)) {
            f.data.setComponent(f.entity,new Objective());
            f.controller.convertCreature((short)3);
            assertEquals(1,f.flags[0]);
            assertEquals(3,f.ownerAtEvent);
            assertEquals(3,f.controller.getOwnerId());
            assertEquals(3,f.data.getComponent(f.entity,Owner.class).controlId);
            assertFalse(f.objectiveAtEvent);
        }
    }

    @Test
    void realTortureSystemThresholdDeliversOnlyAfterConversionCompletes() {
        try (Fixture f=new Fixture(255)) {
            f.data.setComponent(f.entity,new CreatureTortured(8,0,0));
            CreatureTorturingSystem torture=f.tortureSystem();
            try {
                torture.processTick(1);
                assertEquals(0,f.flags[0]);
                assertEquals(1,f.controller.getOwnerId());
                torture.processTick(1);
                assertEquals(1,f.flags[0]);
                assertEquals(3,f.ownerAtEvent);
                assertFalse(f.torturedAtEvent);
                assertNull(f.data.getComponent(f.entity,CreatureTortured.class));
                for (int i=0;i<5;i++) torture.processTick(1);
                assertEquals(1,f.flags[0]);
            } finally {torture.stop();}
        }
    }

    @Test
    void normalTortureOfAnAlreadyOwnedCreatureDoesNotRequestAnotherConversion() {
        try (Fixture f=new Fixture(255)) {
            f.data.setComponents(f.entity,new Owner((short)3,(short)3),new CreatureTortured(20,0,0));
            CreatureTorturingSystem torture=f.tortureSystem();
            try {
                torture.processTick(1);
                assertEquals(0,f.flags[0]);
            } finally {torture.stop();}
        }
    }

    @Test
    void explicitSameOwnerConversionStillEmitsLikeTheOriginalRoutine() {
        try (Fixture f=new Fixture(255)) {
            f.controller.convertCreature((short)3);
            f.controller.convertCreature((short)3);
            assertEquals(2,f.flags[0]);
        }
    }

    @Test
    void conversionBackToTheScriptsOriginalPlayerIsStillAnEvent() {
        try (Fixture f=new Fixture(255)) {
            f.controller.convertCreature((short)3);
            f.controller.convertCreature((short)1);
            assertEquals(2,f.flags[0]);
            assertEquals(1,f.ownerAtEvent);
        }
    }

    @Test
    void differentInitialOwnerAndGenericOwnershipChangesDoNotPollAsConversion() {
        try (Fixture f=new Fixture(255)) {
            f.data.setComponent(f.entity,new Owner((short)4,(short)4));
            for (int i=0;i<10;i++) f.scripts.processTick(.25f);
            assertEquals(0,f.flags[0]);
            f.data.setComponent(f.entity,new Owner((short)3,(short)3));
            for (int i=0;i<10;i++) f.scripts.processTick(.25f);
            assertEquals(0,f.flags[0]);
        }
    }

    @Test
    void recruitingASensedNeutralDoesNotEmitATortureConversionEvent() {
        try (Fixture f=new Fixture(255)) {
            f.data.setComponent(f.entity,new Owner((short)3,(short)3));
            EntityId neutral=f.data.createEntity();
            f.data.setComponents(neutral,new Owner((short)2,(short)2),new CreatureComponent(),new Health(100,100));
            f.sensed.add(neutral);
            f.controller.checkSurroundings();
            assertEquals(3,f.data.getComponent(neutral,Owner.class).ownerId);
            assertEquals(0,f.flags[0]);
            for (int i=0;i<10;i++) f.scripts.processTick(.25f);
            assertEquals(0,f.flags[0]);
        }
    }

    @Test
    void eventIsNotRememberedOrRepeatedByOrdinaryTicks() {
        try (Fixture f=new Fixture(255)) {
            f.controller.convertCreature((short)3);
            for (int i=0;i<12;i++) f.scripts.processTick(.25f);
            assertEquals(1,f.flags[0]);
        }
    }

    @ParameterizedTest
    @CsvSource({"1,1", "2,2", "255,3"})
    void explicitConversionsUseTheExistingRepeatLimits(int repeats,int events) {
        try (Fixture f=new Fixture(repeats)) {
            for (int i=0;i<3;i++) f.controller.convertCreature((short)3);
            assertEquals(events,f.flags[0]);
        }
    }

    @Test
    void everyMatchingRootRunsAtConversionTime() {
        try (Fixture f=new Fixture(255)) {
            f.records.put(1,CreatureAngryFixtures.condition(1,CREATURE_CONVERTED,-1,2,255));
            f.records.put(2,CreatureAngryFixtures.condition(2,CREATURE_CONVERTED,-2,0,255));
            f.records.put(-2,CreatureAngryFixtures.action(-2,2));
            f.bindScripts();
            f.controller.convertCreature((short)3);
            assertEquals(1,f.flags[0]);
            assertEquals(1,f.flags[2]);
        }
    }

    @Test
    void nestedConditionsAreCheckedAtConversionWithoutLatching() {
        try (Fixture f=new Fixture(255)) {
            f.records.put(1,CreatureAngryFixtures.condition(1,CREATURE_CONVERTED,2,0,255));
            f.records.put(2,CreatureAngryFixtures.condition(2,FLAG,-1,0,255));
            f.bindScripts();
            f.controller.convertCreature((short)3);
            f.flags[1]=1;
            for (int i=0;i<8;i++) f.scripts.processTick(.25f);
            assertEquals(0,f.flags[0]);
            f.controller.convertCreature((short)3);
            assertEquals(1,f.flags[0]);
        }
    }

    @Test
    void nestedAndUnrelatedEventTypesDoNotReceiveTheConversion() {
        try (Fixture f=new Fixture(255)) {
            f.records.put(1,CreatureAngryFixtures.condition(1,FLAG,2,3,255));
            f.records.put(2,CreatureAngryFixtures.condition(2,CREATURE_CONVERTED,-1,0,255));
            f.records.put(3,CreatureAngryFixtures.condition(3,CREATURE_ANGRY,-2,0,255));
            f.records.put(-2,CreatureAngryFixtures.action(-2,2));
            f.bindScripts();
            f.flags[1]=1;
            f.controller.convertCreature((short)3);
            for (int i=0;i<8;i++) f.scripts.processTick(.25f);
            assertEquals(0,f.flags[0]);
            assertEquals(0,f.flags[2]);
        }
    }

    @Test
    void conversionDoesNotInterruptTheOrdinaryScriptCursor() {
        try (Fixture f=new Fixture(255)) {
            f.records.put(1,CreatureAngryFixtures.condition(1,FLAG,-2,2,255));
            f.records.put(2,CreatureAngryFixtures.condition(2,CREATURE_CONVERTED,-1,0,255));
            f.records.put(-2,CreatureAngryFixtures.action(-2,2));
            f.bindScripts();
            f.flags[1]=1;
            f.scripts.processTick(.25f);
            f.controller.convertCreature((short)3);
            assertEquals(1,f.flags[0]);
            assertEquals(0,f.flags[2]);
            f.scripts.processTick(.25f);
            assertEquals(1,f.flags[2]);
        }
    }

    @Test
    void currentBindingIsResolvedBeforeTheNextOrdinaryTick() {
        try (Fixture f=new Fixture(255)) {
            f.data.removeComponent(f.entity,Trigger.class);
            f.controller.convertCreature((short)3);
            assertEquals(0,f.flags[0]);
            f.data.setComponent(f.entity,new Trigger(99));
            f.controller.convertCreature((short)3);
            assertEquals(0,f.flags[0]);
            f.data.setComponent(f.entity,new Trigger(1));
            f.controller.convertCreature((short)3);
            assertEquals(1,f.flags[0]);
        }
    }

    private static final class Fixture implements AutoCloseable {
        final EntityData data=new DefaultEntityData();
        final EntityId entity=data.createEntity();
        final int[] flags=new int[3];
        final Set<EntityId> sensed=new LinkedHashSet<>();
        final Map<Integer,toniarts.openkeeper.tools.convert.map.Trigger> records=new HashMap<>();
        final Creature creature=CreatureAngryFixtures.creature(false);
        final ILevelInfo level;
        final ICreaturesController creatures;
        final CreatureController controller;
        CreatureTriggerLogicController scripts;
        int ownerAtEvent=-1;
        boolean objectiveAtEvent;
        boolean torturedAtEvent;

        Fixture(int repeats) {
            records.put(1,CreatureAngryFixtures.condition(1,CREATURE_CONVERTED,-1,0,repeats));
            records.put(-1,CreatureAngryFixtures.action(-1,0));
            CreatureComponent component=new CreatureComponent();
            component.creatureId=6;
            component.tortureTimeToConvert=10;
            data.setComponents(entity,component,new Trigger(1),new Health(100,100),
                    new Owner((short)1,(short)1),new Position(0,new Vector3f()));
            IKwdFile kwd=proxy(IKwdFile.class,(name,args)->switch(name) {
                case "getTriggers" -> records;
                case "getCreature" -> creature;
                case "getGameLevel" -> CreatureSleepFixtures.level(4);
                case "getThings" -> args[0]==Thing.GoodCreature.class
                        ? List.of(CreatureAngryFixtures.thing(1)) : List.of();
                default -> throw new UnsupportedOperationException(name);
            });
            level=proxy(ILevelInfo.class,(name,args)->switch(name) {
                case "getLevelData" -> kwd;
                case "getPlayer" -> new toniarts.openkeeper.game.data.Keeper(false,(short)args[0]);
                case "getFlag" -> flags[((Number)args[0]).intValue()];
                case "setFlag" -> {
                    int id=((Number)args[0]).intValue();
                    if (id==0) {
                        ownerAtEvent=data.getComponent(entity,Owner.class).ownerId;
                        objectiveAtEvent=data.getComponent(entity,Objective.class)!=null;
                        torturedAtEvent=data.getComponent(entity,CreatureTortured.class)!=null;
                    }
                    flags[id]=(int)args[1];yield null;
                }
                default -> throw new UnsupportedOperationException(name);
            });
            creatures=proxy(ICreaturesController.class,(name,args)->switch(name) {
                case "createController" -> controller();
                case "isValidEntity" -> false;
                case "onConverted" -> {scripts.onConverted((EntityId)args[0]);yield null;}
                default -> throw new UnsupportedOperationException(name);
            });
            IEntityPositionLookup lookup=proxy(IEntityPositionLookup.class,(name,args)->switch(name) {
                case "getSensedEntities" -> sensed;
                default -> throw new UnsupportedOperationException(name);
            });
            var settings=Map.of(MiscType.CREATURE_CRITICAL_HEALTH_PERCENTAGE_OF_MAX,
                    CreatureSleepFixtures.variable(MiscType.CREATURE_CRITICAL_HEALTH_PERCENTAGE_OF_MAX,10));
            controller=new CreatureController(entity,data,creature,null,null,()->20,
                    settings,creatures,lookup,null,level,null,null,null,id->{ });
            bindScripts();
        }

        private CreatureController controller() {return controller;}
        CreatureTorturingSystem tortureSystem() {
            FakeMapData grid=new FakeMapData(1,1);
            grid.getTile(0,0).ownerId=3;
            return new CreatureTorturingSystem(data,creatures,new FakeMapInformation(grid,Map.of()));
        }
        void bindScripts() {
            if (scripts!=null) scripts.stop();
            scripts=new CreatureTriggerLogicController(null,level,null,null,creatures,null,data);
        }
        @Override public void close() {scripts.stop();data.close();}
    }

    @FunctionalInterface private interface Call {Object invoke(String name,Object[] args);}
    private static <T>T proxy(Class<T> type,Call call) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(),new Class<?>[]{type},
                (p,m,a)->call.invoke(m.getName(),a)));
    }
}
