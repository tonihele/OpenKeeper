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
package toniarts.openkeeper.game.component;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreatureMoodTest {

    @Test
    void frozenMoodIgnoresAddAndClear() {
        CreatureMood mood = new CreatureMood(1, 2, 3, 4, 5, 6).withFrozen(true);

        assertSame(mood, mood.add(CreatureMood.REASON_NO_PAY, 100));
        assertSame(mood, mood.clear(CreatureMood.REASON_NO_LAIR));
        assertEquals(5, mood.noPay);
        assertEquals(3, mood.noLair);
    }

    @Test
    void unfrozenMoodStaysUnfrozenAfterChanges() {
        CreatureMood mood = new CreatureMood(0, 0, 0, 0, 0, 0).add(CreatureMood.REASON_OTHER, 10);

        assertFalse(mood.frozen);
        assertEquals(10, mood.other);
    }

    @Test
    void withFrozenKeepsTheCountersAndLeavesTheOriginalAlone() {
        CreatureMood mood = new CreatureMood(1, 2, 3, 4, 5, 6);
        CreatureMood frozen = mood.withFrozen(true);

        assertTrue(frozen.frozen);
        assertFalse(mood.frozen);
        assertEquals(6, frozen.other);
        assertSame(frozen, frozen.withFrozen(true));
    }

    @Test
    void frozenFlagIsAFieldTheNetworkSerializerSends() throws NoSuchFieldException {
        // FieldSerializer sends every public, non-transient, non-static field
        Field field = CreatureMood.class.getField("frozen");

        assertEquals(boolean.class, field.getType());
        assertFalse(Modifier.isTransient(field.getModifiers()));
        assertFalse(Modifier.isStatic(field.getModifiers()));
        assertFalse(Modifier.isFinal(field.getModifiers()));
    }
}
