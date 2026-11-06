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

import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureHunger;
import toniarts.openkeeper.game.component.Damage;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Trigger;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.creature.CreatureController;
import toniarts.openkeeper.game.trigger.creature.CreatureTriggerLogicController;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.CreatureAngryFixtures;
import toniarts.openkeeper.tools.convert.map.CreatureHungerSatedFixtures;
import toniarts.openkeeper.tools.convert.map.CreatureSleepFixtures;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Thing;
import static toniarts.openkeeper.tools.convert.map.TriggerGeneric.TargetType.*;
import static org.junit.jupiter.api.Assertions.*;

class CreatureHungerSatedTriggerTest {

    @ParameterizedTest
    @CsvSource({"3,2,0", "2,1,0", "1,0,1", "0,0,1"})
    void realMealCompletionEmitsExactlyWhenRemainingHungerIsZero(int before, int after, int events) {
        try (Fixture f = new Fixture(255,before)) {
            f.controller.sate();
            assertEquals(after,f.hunger().amountNeeded);
            assertEquals(events,f.flags[0]);
            assertEquals(-7,f.data.getComponent(f.entity,Damage.class).damage);
        }
    }

    @Test
    void creatureCanStartFullAndBePolledWithoutEmittingAnEvent() {
        try (Fixture f = new Fixture(255,0)) {
            assertFalse(f.controller.isHungry());
            for (int i=0;i<12;i++) f.scripts.processTick(.25f);
            assertEquals(0,f.flags[0]);
        }
    }

    @Test
    void partialMealsThenFinalMealEmitOnlyOnce() {
        try (Fixture f = new Fixture(255,3)) {
            f.controller.sate();
            f.controller.sate();
            assertEquals(0,f.flags[0]);
            f.controller.sate();
            assertEquals(1,f.flags[0]);
            for (int i=0;i<12;i++) f.scripts.processTick(.25f);
            assertEquals(1,f.flags[0]);
        }
    }

    @Test
    void anotherFeedingWhileFullEmitsAgainWithoutANewHungerTransition() {
        try (Fixture f = new Fixture(255,1)) {
            f.controller.sate();
            f.controller.sate();
            assertEquals(2,f.flags[0]);
            assertEquals(0,f.hunger().amountNeeded);
        }
    }

    @Test
    void aLaterHungerCycleCanEmitAgain() {
        try (Fixture f = new Fixture(255,1)) {
            f.controller.sate();
            f.data.setComponent(f.entity,new CreatureHunger(10,2));
            f.controller.sate();
            assertEquals(1,f.flags[0]);
            f.controller.sate();
            assertEquals(2,f.flags[0]);
        }
    }

    @Test
    void eventObservesStoredHungerAndExistingMealHealingStillFollows() {
        try (Fixture f = new Fixture(255,1)) {
            f.controller.sate();
            assertEquals(0,f.hungerAtEvent);
            assertEquals(20,f.lastEatTimeAtEvent);
            assertEquals(0,f.pendingHealingAtEvent);
            assertEquals(-7,f.data.getComponent(f.entity,Damage.class).damage);
        }
    }

    @Test
    void partialMealPreservesLastFullMealTime() {
        try (Fixture f = new Fixture(255,2)) {
            f.controller.sate();
            assertEquals(10,f.hunger().lastEatTime);
            assertEquals(0,f.flags[0]);
        }
    }

    @ParameterizedTest
    @CsvSource({"1,1", "2,2", "255,3"})
    void repeatedMealsUseTheExistingScriptRepeatLimits(int repeats,int events) {
        try (Fixture f = new Fixture(repeats,0)) {
            for (int i=0;i<3;i++) f.controller.sate();
            assertEquals(events,f.flags[0]);
        }
    }

    @Test
    void allMatchingRootBranchesRunOnTheMealThatSatesTheCreature() {
        try (Fixture f = new Fixture(255,1)) {
            f.records.put(1,CreatureAngryFixtures.condition(1,CREATURE_HUNGER_SATED,-1,2,255));
            f.records.put(2,CreatureAngryFixtures.condition(2,CREATURE_HUNGER_SATED,-2,0,255));
            f.records.put(-2,CreatureAngryFixtures.action(-2,2));
            f.bindScripts();
            f.controller.sate();
            assertEquals(1,f.flags[0]);
            assertEquals(1,f.flags[2]);
        }
    }

    @Test
    void nestedConditionsObserveMealTimeWithoutLatching() {
        try (Fixture f = new Fixture(255,0)) {
            f.records.put(1,CreatureAngryFixtures.condition(1,CREATURE_HUNGER_SATED,2,0,255));
            f.records.put(2,CreatureAngryFixtures.condition(2,FLAG,-1,0,255));
            f.bindScripts();
            f.controller.sate();
            f.flags[1]=1;
            for (int i=0;i<8;i++) f.scripts.processTick(.25f);
            assertEquals(0,f.flags[0]);
            f.controller.sate();
            assertEquals(1,f.flags[0]);
        }
    }

    @Test
    void nestedEventsAndUnrelatedRootEventsDoNotReceiveTheMeal() {
        try (Fixture f = new Fixture(255,1)) {
            f.records.put(1,CreatureAngryFixtures.condition(1,FLAG,2,3,255));
            f.records.put(2,CreatureAngryFixtures.condition(2,CREATURE_HUNGER_SATED,-1,0,255));
            f.records.put(3,CreatureAngryFixtures.condition(3,CREATURE_ANGRY,-2,0,255));
            f.records.put(-2,CreatureAngryFixtures.action(-2,2));
            f.bindScripts();
            f.flags[1]=1;
            f.controller.sate();
            for (int i=0;i<8;i++) f.scripts.processTick(.25f);
            assertEquals(0,f.flags[0]);
            assertEquals(0,f.flags[2]);
        }
    }

    @Test
    void mealEventDoesNotInterruptOrdinaryTraversal() {
        try (Fixture f = new Fixture(255,1)) {
            f.records.put(1,CreatureAngryFixtures.condition(1,FLAG,-2,2,255));
            f.records.put(2,CreatureAngryFixtures.condition(2,CREATURE_HUNGER_SATED,-1,0,255));
            f.records.put(-2,CreatureAngryFixtures.action(-2,2));
            f.bindScripts();
            f.flags[1]=1;
            f.scripts.processTick(.25f);
            f.controller.sate();
            assertEquals(1,f.flags[0]);
            assertEquals(0,f.flags[2]);
            f.scripts.processTick(.25f);
            assertEquals(1,f.flags[2]);
        }
    }

    @Test
    void scriptBindingIsResolvedAtTheMealEvenBeforeAnOrdinaryTick() {
        try (Fixture f = new Fixture(255,0)) {
            f.data.removeComponent(f.entity,Trigger.class);
            f.controller.sate();
            assertEquals(0,f.flags[0]);
            f.data.setComponent(f.entity,new Trigger(99));
            f.controller.sate();
            assertEquals(0,f.flags[0]);
            f.data.setComponent(f.entity,new Trigger(1));
            f.controller.sate();
            assertEquals(1,f.flags[0]);
        }
    }

    @Test
    void anotherCreatureWithoutAScriptDoesNotTriggerThisCreatureBranch() {
        try (Fixture f = new Fixture(255,0)) {
            EntityId other=f.data.createEntity();
            f.data.setComponent(other,new CreatureComponent());
            f.scripts.onHungerSated(other);
            assertEquals(0,f.flags[0]);
        }
    }

    private static final class Fixture implements AutoCloseable {
        final EntityData data=new DefaultEntityData();
        final EntityId entity=data.createEntity();
        final int[] flags=new int[3];
        final Map<Integer,toniarts.openkeeper.tools.convert.map.Trigger> records=new HashMap<>();
        final Creature creature=CreatureHungerSatedFixtures.creature();
        final ILevelInfo level;
        final ICreaturesController creatures;
        final CreatureController controller;
        CreatureTriggerLogicController scripts;
        int hungerAtEvent=-1;
        double lastEatTimeAtEvent=-1;
        int pendingHealingAtEvent;

        Fixture(int repeats,int hunger) {
            records.put(1,CreatureAngryFixtures.condition(1,CREATURE_HUNGER_SATED,-1,0,repeats));
            records.put(-1,CreatureAngryFixtures.action(-1,0));
            CreatureComponent component=new CreatureComponent();
            component.creatureId=6;
            data.setComponents(entity,component,new Trigger(1),new Health(60,100),
                    new Owner((short)3,(short)3),new CreatureHunger(10,hunger));
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
                case "getFlag" -> flags[((Number)args[0]).intValue()];
                case "setFlag" -> {
                    int id=((Number)args[0]).intValue();
                    if (id==0) {
                        hungerAtEvent=hunger().amountNeeded;
                        lastEatTimeAtEvent=hunger().lastEatTime;
                        Damage damage=data.getComponent(entity,Damage.class);
                        pendingHealingAtEvent=damage==null ? 0 : damage.damage;
                    }
                    flags[id]=(int)args[1];yield null;
                }
                default -> throw new UnsupportedOperationException(name);
            });
            creatures=proxy(ICreaturesController.class,(name,args)->switch(name) {
                case "createController" -> controller();
                case "onHungerSated" -> {scripts.onHungerSated((EntityId)args[0]);yield null;}
                default -> throw new UnsupportedOperationException(name);
            });
            controller=new CreatureController(entity,data,creature,null,null,()->20,
                    Map.of(),creatures,null,null,level,null,null,null,id->{ });
            bindScripts();
        }

        private CreatureController controller() {return controller;}
        CreatureHunger hunger() {return data.getComponent(entity,CreatureHunger.class);}
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
