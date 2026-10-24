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
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.CreatureMood;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The level script's creature moods switch (trigger action SET_CREATURE_MOODS)
 * freezes the anger counters of the creatures.
 */
class CreatureMoodSwitchSystemTest {

    private EntityData entityData;
    private AtomicBoolean enabled;
    private CreatureMoodSwitchSystem system;

    @BeforeEach
    void setUp() {
        entityData = new DefaultEntityData();
        enabled = new AtomicBoolean(true);
        system = new CreatureMoodSwitchSystem(entityData, enabled::get);
    }

    @AfterEach
    void tearDown() {
        system.stop();
    }

    @Test
    void moodChangesStayWhenTheSwitchIsOn() {
        EntityId creature = createCreature(new CreatureMood());
        system.processTick(0.25f);

        entityData.setComponent(creature, new CreatureMood().add(CreatureMood.REASON_OTHER, 500));
        system.processTick(0.25f);

        assertEquals(500, mood(creature).other);
    }

    @Test
    void moodChangesAreUndoneWhenTheSwitchIsOff() {
        EntityId creature = createCreature(new CreatureMood());
        enabled.set(false);
        system.processTick(0.25f);

        entityData.setComponent(creature, mood(creature).add(CreatureMood.REASON_NO_PAY, 40000));
        system.processTick(0.25f);

        assertEquals(0, mood(creature).noPay);
    }

    @Test
    void clearedMoodStaysWhenTheSwitchIsOff() {
        EntityId creature = createCreature(new CreatureMood(0, 0, 900, 0, 0, 0));
        enabled.set(false);
        system.processTick(0.25f);

        entityData.setComponent(creature, mood(creature).clear(CreatureMood.REASON_NO_LAIR));
        system.processTick(0.25f);

        assertEquals(900, mood(creature).noLair);
    }

    @Test
    void creatureAddedWhileOffStartsFrozen() {
        enabled.set(false);
        system.processTick(0.25f);
        EntityId creature = createCreature(new CreatureMood());
        system.processTick(0.25f);

        entityData.setComponent(creature, mood(creature).add(CreatureMood.REASON_GENERAL, 700));
        system.processTick(0.25f);

        assertEquals(0, mood(creature).general);
    }

    @Test
    void moodChangesApplyAgainWhenTheSwitchIsTurnedBackOn() {
        EntityId creature = createCreature(new CreatureMood());
        enabled.set(false);
        system.processTick(0.25f);
        enabled.set(true);
        system.processTick(0.25f);

        entityData.setComponent(creature, mood(creature).add(CreatureMood.REASON_OTHER, 300));
        system.processTick(0.25f);

        assertEquals(300, mood(creature).other);
    }

    private EntityId createCreature(CreatureMood mood) {
        EntityId id = entityData.createEntity();
        entityData.setComponent(id, mood);
        return id;
    }

    private CreatureMood mood(EntityId id) {
        return entityData.getComponent(id, CreatureMood.class);
    }

}
