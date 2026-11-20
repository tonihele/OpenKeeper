/*
 * Copyright (C) 2014-2026 OpenKeeper
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package toniarts.openkeeper.tools.convert.map;

/** Worker action meanings over the shared native descriptor table. */
public enum WorkerAnimationType {
    WALK(AnimationSlot.SLOT_01),
    // Native digging action 0x01 selects descriptor 0x05 directly. The Dwarf
    // uses its melee resource here, without needing the extra Imp dig resource.
    DIG(AnimationSlot.SLOT_05),
    REINFORCE_WALL(AnimationSlot.SLOT_0C),
    CLAIM_FLOOR(AnimationSlot.SLOT_0D),
    DRAG(AnimationSlot.SLOT_13);

    private final AnimationSlot slot;

    WorkerAnimationType(AnimationSlot slot) {
        this.slot = slot;
    }

    public AnimationSlot getSlot() {
        return slot;
    }
}
