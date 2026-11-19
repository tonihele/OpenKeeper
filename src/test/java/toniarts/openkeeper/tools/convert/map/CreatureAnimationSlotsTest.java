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

import java.util.HashSet;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.sound.ThingSounds;
import static toniarts.openkeeper.tools.convert.map.Creature.AnimationSlot.*;
import static toniarts.openkeeper.tools.convert.map.Creature.AnimationType.*;
import static org.junit.jupiter.api.Assertions.*;

class CreatureAnimationSlotsTest {

    @Test
    void descriptorIdsAreExplicitAndIndependentOfReplicatedOrdinals() {
        assertEquals(0x05, MELEE_ATTACK.getSlot().getValue());
        assertEquals(0x26, BACK_OFF.getSlot().getValue());
        assertEquals(0x25, DRUNKED_IDLE.getSlot().getValue());
        assertEquals(0x29, DEATH_POSE.getSlot().getValue());
        assertEquals(0x2A, SWIPE.getSlot().getValue());
        assertEquals(0x2F, DIG.getSlot().getValue());
        assertNotEquals(DIG.ordinal(), DIG.getSlot().getValue());
        var ids = new HashSet<Integer>();
        for (var slot : Creature.AnimationSlot.values()) {
            assertTrue(ids.add(slot.getValue()));
            assertSame(slot, Creature.AnimationSlot.fromId(slot.getValue()));
        }
        assertEquals(47, ids.size());
        assertThrows(IllegalArgumentException.class, () -> Creature.AnimationSlot.fromId(0));
        assertThrows(IllegalArgumentException.class, () -> Creature.AnimationSlot.fromId(0x30));
    }

    @Test
    void oldReplicatedActionOrdinalsStayStable() {
        // Compatibility assertions, never used to compute a descriptor ID.
        assertEquals(0, WALK.ordinal());
        assertEquals(9, MELEE_ATTACK.ordinal());
        assertEquals(37, DIG.ordinal());
        assertEquals(47, NULL_4.ordinal());
        assertEquals(48, CLAIM_FLOOR.ordinal());
    }

    @Test
    void workerAndFighterActionsShareResourcesWithoutDuplicatingTheRegistry() {
        var creature = new Creature();
        var wall = resource("SyntheticWall");
        var floor = resource("SyntheticFloor");
        var haul = resource("SyntheticHaul");
        creature.setAnimation(SLOT_0C, wall);
        creature.setAnimation(SLOT_0D, floor);
        creature.setAnimation(SLOT_13, haul);
        assertSame(wall, creature.getAnimationForAction(REINFORCE_WALL));
        assertSame(wall, creature.getAnimationForAction(SLEEPING));
        assertSame(floor, creature.getAnimationForAction(CLAIM_FLOOR));
        assertSame(floor, creature.getAnimationForAction(EATING));
        assertSame(haul, creature.getAnimationForAction(HAUL));
        assertSame(haul, creature.getAnimationForAction(DRINKING));
        assertEquals(3, creature.getAnimationSlots().size());
        assertEquals(3, creature.getAnimations().size());
        assertThrows(UnsupportedOperationException.class, () -> creature.getAnimationSlots().clear());
    }

    @Test
    void impAndDwarfDigWithTheirPrimarySlotEvenIfOnlyTheImpHasTheExtraSlot() {
        var imp = new Creature();
        var dwarf = new Creature();
        var impPrimary = resource("SyntheticImpPrimary");
        var impExtra = resource("SyntheticImpExtra");
        var dwarfPrimary = resource("SyntheticDwarfPrimary");
        imp.setAnimation(SLOT_05, impPrimary);
        imp.setAnimation(SLOT_2F, impExtra);
        dwarf.setAnimation(SLOT_05, dwarfPrimary);
        assertSame(impPrimary, imp.getAnimationForAction(DIGGING));
        assertSame(dwarfPrimary, dwarf.getAnimationForAction(DIGGING));
        assertSame(impExtra, imp.getAnimation(DIG));
        assertNull(dwarf.getAnimation(DIG));
        assertSame(dwarfPrimary, dwarf.getAnimationForAction(MELEE_ATTACK));
    }

    @Test
    void playbackFallbackDoesNotChangeAvailabilityOrPopulateMissingSlots() {
        var creature = new Creature();
        var primary = resource("SyntheticPrimary");
        var absent = new ArtResource();
        absent.setType(ArtResource.ArtResourceType.NONE);
        creature.setAnimation(SLOT_05, primary);
        creature.setAnimation(SLOT_16, absent);
        assertSame(absent, creature.getAnimation(MANUFACTURING));
        assertSame(primary, creature.getAnimationForAction(MANUFACTURING));
        assertNull(creature.getAnimation(ANGRY));
        assertSame(primary, creature.getAnimationForAction(ANGRY));
        // Native fallback for hauling is 0x09, not recursively 0x09 -> 0x05.
        assertNull(creature.getAnimationForAction(HAUL));
        var angry = resource("SyntheticAngry");
        creature.setAnimation(SLOT_09, angry);
        assertSame(angry, creature.getAnimationForAction(HAUL));
        assertNull(creature.getAnimationForAction(REINFORCE_WALL));
        assertNull(creature.getAnimationForAction(DIG));
        assertEquals(3, creature.getAnimationSlots().size());
    }

    @Test
    void sharedResourceSlotsDoNotConfuseActionSounds() {
        assertEquals(ThingSounds.CREATURE_DIG, ThingSounds.getCreatureEvent(DIGGING));
        assertEquals(ThingSounds.CREATURE_MELEE, ThingSounds.getCreatureEvent(MELEE_ATTACK));
        assertEquals(ThingSounds.CREATURE_CLAIM, ThingSounds.getCreatureEvent(CLAIM_FLOOR));
        assertEquals(ThingSounds.CREATURE_EAT, ThingSounds.getCreatureEvent(EATING));
        assertEquals(ThingSounds.CREATURE_REINFORCE, ThingSounds.getCreatureEvent(REINFORCE_WALL));
        assertEquals(ThingSounds.CREATURE_SLEEP, ThingSounds.getCreatureEvent(SLEEPING));
        assertEquals(-1, ThingSounds.getCreatureEvent(HAUL));
        assertEquals(ThingSounds.CREATURE_DRINK, ThingSounds.getCreatureEvent(DRINKING));
    }

    private static ArtResource resource(String name) {
        var resource = new ArtResource();
        resource.setName(name);
        resource.setType(ArtResource.ArtResourceType.ANIMATING_MESH);
        resource.setData(ArtResource.KEY_FRAMES, 45);
        resource.setData(ArtResource.KEY_FPS, 30);
        return resource;
    }
}
