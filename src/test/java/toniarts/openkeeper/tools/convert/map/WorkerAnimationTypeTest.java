/*
 * Copyright (C) 2014-2026 OpenKeeper
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package toniarts.openkeeper.tools.convert.map;

import java.util.EnumSet;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WorkerAnimationTypeTest {

    private Creature creature(boolean worker) {
        Creature creature = new Creature();
        creature.setFlags(worker ? EnumSet.of(Creature.CreatureFlag.IS_WORKER)
                : EnumSet.noneOf(Creature.CreatureFlag.class));
        return creature;
    }

    private ArtResource resource() {
        ArtResource resource = new ArtResource();
        resource.setName("SyntheticAnimation");
        resource.setType(ArtResource.ArtResourceType.ANIMATING_MESH);
        return resource;
    }

    @Test
    void dwarfDigsWithoutTheAdditionalImpResource() {
        Creature dwarf = creature(true);
        ArtResource melee = resource();
        dwarf.setAnimation(AnimationSlot.SLOT_05, melee);

        assertNull(dwarf.getAnimation(Creature.AnimationType.DIG));
        assertSame(melee, dwarf.getAnimation(WorkerAnimationType.DIG));
        assertSame(melee, dwarf.getAnimationForState(Creature.AnimationType.DIG));
    }

    @Test
    void impDigActionDoesNotSelectTheAdditionalImpResource() {
        Creature imp = creature(true);
        ArtResource regularDig = resource();
        ArtResource additionalDig = resource();
        imp.setAnimation(AnimationSlot.SLOT_05, regularDig);
        imp.setAnimation(Creature.AnimationType.DIG, additionalDig);

        assertSame(additionalDig, imp.getAnimation(Creature.AnimationType.DIG));
        assertSame(regularDig, imp.getAnimationForState(Creature.AnimationType.DIG));
    }

    @Test
    void workerAndFighterMeaningsShareOneResourceEntry() {
        Creature worker = creature(true);
        ArtResource drag = resource();
        worker.setAnimation(AnimationSlot.SLOT_13, drag);

        assertSame(drag, worker.getAnimation(WorkerAnimationType.DRAG));
        assertSame(drag, worker.getAnimation(Creature.AnimationType.DRINKING));
        assertEquals(1, worker.getAnimations().size());
    }

    @Test
    void fighterDoesNotUseTheWorkerProfile() {
        Creature fighter = creature(false);
        ArtResource melee = resource();
        ArtResource extra = resource();
        fighter.setAnimation(AnimationSlot.SLOT_05, melee);
        fighter.setAnimation(Creature.AnimationType.DIG, extra);

        assertNull(fighter.getAnimation(WorkerAnimationType.DIG));
        assertSame(melee, fighter.getAnimationForState(Creature.AnimationType.MELEE_ATTACK));
        assertSame(extra, fighter.getAnimationForState(Creature.AnimationType.DIG));
    }

    @Test
    void absentWorkerResourceStaysAbsent() {
        Creature worker = creature(true);
        worker.setAnimation(AnimationSlot.SLOT_01, resource());

        assertNull(worker.getAnimationForState(Creature.AnimationType.DIG));
        assertNull(worker.getAnimation(WorkerAnimationType.DRAG));
    }

    @Test
    void workerClaimingUsesTheSharedSlots() {
        Creature worker = creature(true);
        ArtResource wall = resource();
        ArtResource floor = resource();
        worker.setAnimation(AnimationSlot.SLOT_0C, wall);
        worker.setAnimation(AnimationSlot.SLOT_0D, floor);

        assertSame(wall, worker.getAnimation(WorkerAnimationType.REINFORCE_WALL));
        assertSame(floor, worker.getAnimation(WorkerAnimationType.CLAIM_FLOOR));
        assertSame(wall, worker.getAnimationForState(Creature.AnimationType.SLEEPING));
        assertSame(floor, worker.getAnimationForState(Creature.AnimationType.EATING));
    }
}
