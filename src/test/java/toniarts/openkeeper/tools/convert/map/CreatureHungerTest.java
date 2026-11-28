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
package toniarts.openkeeper.tools.convert.map;

import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureHunger;
import toniarts.openkeeper.game.component.TaskComponent;
import toniarts.openkeeper.game.controller.creature.CreatureController;
import toniarts.openkeeper.game.task.TaskType;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Hunger adds up over the meals a creature misses, except while it gambles
 */
class CreatureHungerTest {

    private static final int HUNGER_RATE = 30;
    private static final int HUNGER_FILL = 2;

    private final EntityData entityData = new DefaultEntityData();
    private final EntityId entityId = entityData.createEntity();
    private double gameTime;
    private CreatureController creature;

    @BeforeEach
    void setUp() {
        Creature data = new Creature();
        data.getAttributes().setHungerRate(HUNGER_RATE);
        CreatureComponent creatureComponent = new CreatureComponent();
        creatureComponent.hungerFill = HUNGER_FILL;
        entityData.setComponents(entityId, creatureComponent, new CreatureHunger(0, 0));

        creature = new CreatureController(entityId, entityData, data, null, null, () -> gameTime, Map.of(),
                null, null, null, null, null, null, null);
    }

    @Test
    void creatureGetsHungryWhenTheHungerRateHasPassed() {
        gameTime = HUNGER_RATE - 1;
        assertFalse(creature.isHungry());

        gameTime = HUNGER_RATE;
        assertTrue(creature.isHungry());
        assertEquals(HUNGER_FILL, hunger().amountNeeded);
    }

    @Test
    void everyMissedMealIsOwed() {
        gameTime = 3 * HUNGER_RATE + 5;

        assertTrue(creature.isHungry());
        assertEquals(3 * HUNGER_FILL, hunger().amountNeeded);

        // Asking again adds nothing until the next meal is missed
        assertTrue(creature.isHungry());
        assertEquals(3 * HUNGER_FILL, hunger().amountNeeded);
    }

    @Test
    void everyChickenTakesOneOffAndStartsTheClockAgain() {
        gameTime = HUNGER_RATE;
        creature.isHungry();

        gameTime = HUNGER_RATE + 20;
        creature.sate();
        assertEquals(HUNGER_FILL - 1, hunger().amountNeeded);

        // A meal later by the old clock, but not by the one the chicken started
        gameTime = 2 * HUNGER_RATE + 10;
        assertTrue(creature.isHungry());
        assertEquals(HUNGER_FILL - 1, hunger().amountNeeded);

        gameTime = 2 * HUNGER_RATE + 20;
        creature.isHungry();
        assertEquals(2 * HUNGER_FILL - 1, hunger().amountNeeded);
    }

    @Test
    void noHungerBuildsUpWhileGamblingButAMealIsDueOnceItStops() {
        entityData.setComponent(entityId, new TaskComponent(1, null, new Point(1, 1), TaskType.GAMBLE, 0, true));

        // DK II's Casino states have flag 8, so 0x477C20 adds no meal while gambling
        gameTime = 3 * HUNGER_RATE + 5;
        assertFalse(creature.isHungry());
        assertEquals(0, hunger().amountNeeded);

        // Its clock ran on, so one meal is owed as soon as it stops, and the clock starts again
        entityData.removeComponent(entityId, TaskComponent.class);
        gameTime += 1;
        assertTrue(creature.isHungry());
        assertEquals(HUNGER_FILL, hunger().amountNeeded);

        gameTime += HUNGER_RATE - 2;
        creature.isHungry();
        assertEquals(HUNGER_FILL, hunger().amountNeeded);
    }

    @Test
    void creatureHungryBeforeGamblingStaysHungry() {
        gameTime = HUNGER_RATE;
        creature.isHungry();
        entityData.setComponent(entityId, new TaskComponent(1, null, new Point(1, 1), TaskType.GAMBLE, 0, true));

        gameTime = 3 * HUNGER_RATE;
        assertTrue(creature.isHungry());
        assertEquals(HUNGER_FILL, hunger().amountNeeded);
    }

    private CreatureHunger hunger() {
        return entityData.getComponent(entityId, CreatureHunger.class);
    }

}
