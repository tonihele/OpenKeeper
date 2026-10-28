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
package toniarts.openkeeper.game.state;

import java.lang.reflect.Field;
import java.util.EnumSet;
import java.util.Map;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.tools.convert.map.ArtResource;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.Creature.CreatureFlag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class CreaturePortraitResolverTest {

    @Test
    void allThirteenEliteIdsShareTheirRegularCardAndPortrait() throws ReflectiveOperationException {
        Map<Short, Short> eliteToRegular = Map.ofEntries(
                Map.entry((short) 32, (short) 12), Map.entry((short) 33, (short) 7),
                Map.entry((short) 34, (short) 5), Map.entry((short) 35, (short) 10),
                Map.entry((short) 36, (short) 6), Map.entry((short) 37, (short) 9),
                Map.entry((short) 38, (short) 4), Map.entry((short) 39, (short) 11),
                Map.entry((short) 40, (short) 24), Map.entry((short) 41, (short) 3),
                Map.entry((short) 42, (short) 8), Map.entry((short) 43, (short) 22),
                Map.entry((short) 44, (short) 23));

        for (var entry : eliteToRegular.entrySet()) {
            Creature regular = creature(entry.getValue(), (short) 0, false);
            Creature elite = creature(entry.getKey(), entry.getValue(), true);
            ArtResource portrait = new ArtResource();
            set(regular, "portraitResource", portrait);
            assertEquals(entry.getValue(), CreaturePortraitResolver.cardTypeId(elite));
            assertSame(portrait, CreaturePortraitResolver.resolve(elite, id -> id.equals(entry.getValue()) ? regular : null));
        }
    }

    @Test
    void regularCreatureKeepsItsOwnPortraitAndCard() throws ReflectiveOperationException {
        Creature regular = creature((short) 7, (short) 0, false);
        ArtResource portrait = new ArtResource();
        set(regular, "portraitResource", portrait);
        assertEquals((short) 7, CreaturePortraitResolver.cardTypeId(regular));
        assertSame(portrait, CreaturePortraitResolver.resolve(regular, id -> null));
    }

    private static Creature creature(short id, short cloneId, boolean unique) throws ReflectiveOperationException {
        Creature creature = new Creature();
        set(creature, "creatureId", id);
        set(creature, "cloneCreatureId", cloneId);
        set(creature, "flags", unique ? EnumSet.of(CreatureFlag.IS_UNIQUE) : EnumSet.noneOf(CreatureFlag.class));
        return creature;
    }

    private static void set(Object object, String name, Object value) throws ReflectiveOperationException {
        Field field = object.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(object, value);
    }
}
