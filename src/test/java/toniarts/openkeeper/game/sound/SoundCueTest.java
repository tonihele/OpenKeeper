/*
 * Copyright (C) 2014-2025 OpenKeeper
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
package toniarts.openkeeper.game.sound;

import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.EntitySet;
import com.simsilica.es.base.DefaultEntityData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.SoundCue;
import toniarts.openkeeper.game.logic.SoundCueSystem;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The game logic only creates {@link SoundCue}s, the audio is the client's matter.
 */
class SoundCueTest {

    private EntityData entityData;
    private EntitySet cues;

    @BeforeEach
    void setUp() {
        entityData = new DefaultEntityData();
        cues = entityData.getEntities(SoundCue.class);
    }

    @AfterEach
    void tearDown() {
        cues.release();
        entityData.close();
    }

    @Test
    void emitCreatesAPositionalCueForEveryone() {
        SoundCues.emit(entityData, "CREATURE_IMP", ThingSounds.CREATURE_HIT, new Point(3, 4));

        assertTrue(cues.applyChanges());
        SoundCue cue = cues.iterator().next().get(SoundCue.class);
        assertEquals("CREATURE_IMP", cue.category);
        assertEquals(ThingSounds.CREATURE_HIT, cue.eventId);
        assertEquals(new Point(3, 4), cue.position);
        assertEquals(SoundCue.ALL_PLAYERS, cue.playerId);
        assertFalse(cue.exclusive);
    }

    @Test
    void emitToPlayerIsNotPositionalAndOnlyForThePlayer() {
        SoundCues.emitToPlayer(entityData, GlobalCategory.HAND, ThingSounds.TAG, (short) 2);

        cues.applyChanges();
        SoundCue cue = cues.iterator().next().get(SoundCue.class);
        assertNull(cue.position);
        assertEquals(2, cue.playerId);
    }

    @Test
    void thingWithoutSoundCategoryMakesNoCue() {
        SoundCues.emit(entityData, null, ThingSounds.CREATURE_HIT, new Point(1, 1));
        SoundCues.emit(entityData, "", ThingSounds.CREATURE_HIT, new Point(1, 1));

        assertFalse(cues.applyChanges());
    }

    @Test
    void exclusiveCueKnowsItsSource() {
        EntityId source = entityData.createEntity();
        SoundCues.emitExclusive(entityData, "CREATURE_IMP", ThingSounds.CREATURE_HIT, new Point(1, 1), source);

        cues.applyChanges();
        SoundCue cue = cues.iterator().next().get(SoundCue.class);
        assertTrue(cue.exclusive);
        assertEquals(source, cue.source);
    }

    @Test
    void cuesAreRemovedAfterTheyHadTimeToReachTheClients() {
        SoundCueSystem system = new SoundCueSystem(entityData);
        SoundCues.emit(entityData, "CREATURE_IMP", ThingSounds.CREATURE_HIT, new Point(1, 1));

        system.processTick(0.1f);
        system.processTick(0.1f);
        cues.applyChanges();
        assertEquals(1, cues.size(), "Too early, a client might not have seen it yet");

        system.processTick(5f);
        cues.applyChanges();
        assertTrue(cues.isEmpty());
        system.stop();
    }
}
