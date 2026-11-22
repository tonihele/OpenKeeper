/*
 * Copyright (C) 2014-2026 OpenKeeper
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package toniarts.openkeeper.tools.convert.map;

import toniarts.openkeeper.tools.convert.IValueEnum;

/**
 * IDs in DKII's standard creature animation descriptor table, not file byte
 * offsets or Java enum ordinals. A slot's meaning depends on the creature.
 * Extra Imp resources (including IMP-Dig2) are separate from this table.
 */
public enum AnimationSlot implements IValueEnum {
    SLOT_01(0x01, Creature.AnimationType.WALK), // walk.
    SLOT_02(0x02, Creature.AnimationType.RUN), // run.
    SLOT_03(0x03, Creature.AnimationType.DRAGGED), // dragged.
    SLOT_04(0x04, Creature.AnimationType.RECOIL_FORWARDS), // recoil forwards.
    SLOT_05(0x05, Creature.AnimationType.MELEE_ATTACK), // Melee; worker digging (Imp and Dwarf).
    SLOT_06(0x06, Creature.AnimationType.CAST_SPELL), // cast spell.
    SLOT_07(0x07, Creature.AnimationType.DIE), // die.
    SLOT_08(0x08, Creature.AnimationType.HAPPY), // happy.
    SLOT_09(0x09, Creature.AnimationType.ANGRY), // angry.
    SLOT_0A(0x0A, Creature.AnimationType.STUNNED), // stunned.
    SLOT_0B(0x0B, Creature.AnimationType.IN_HAND), // in hand.
    SLOT_0C(0x0C, Creature.AnimationType.SLEEPING), // Sleeping; worker wall reinforcement.
    SLOT_0D(0x0D, Creature.AnimationType.EATING), // Eating; worker floor claiming.
    SLOT_0E(0x0E, Creature.AnimationType.RESEARCHING), // researching.
    SLOT_0F(0x0F, Creature.AnimationType.NULL_2), // null 2.
    SLOT_10(0x10, Creature.AnimationType.NULL_1), // null 1.
    SLOT_11(0x11, Creature.AnimationType.TORTURED_WHEEL), // tortured wheel.
    SLOT_12(0x12, Creature.AnimationType.NULL_3), // null 3.
    SLOT_13(0x13, Creature.AnimationType.DRINKING), // Drinking; worker dragging.
    SLOT_14(0x14, Creature.AnimationType.IDLE_1), // idle 1.
    SLOT_15(0x15, Creature.AnimationType.RECOIL_BACKWARDS), // recoil backwards.
    SLOT_16(0x16, Creature.AnimationType.MANUFACTURING), // manufacturing.
    SLOT_17(0x17, Creature.AnimationType.PRAYING), // praying.
    SLOT_18(0x18, Creature.AnimationType.FALLBACK), // fallback.
    SLOT_19(0x19, Creature.AnimationType.TORTURED_CHAIR), // tortured chair.
    SLOT_1A(0x1A, Creature.AnimationType.TORTURED_CHAIR_SKELETON), // tortured chair skeleton.
    SLOT_1B(0x1B, Creature.AnimationType.GET_UP), // get up.
    SLOT_1C(0x1C, Creature.AnimationType.DANCE), // dance.
    SLOT_1D(0x1D, Creature.AnimationType.DRUNK), // drunk.
    SLOT_1E(0x1E, Creature.AnimationType.ENTRANCE), // entrance.
    SLOT_1F(0x1F, Creature.AnimationType.IDLE_2), // idle 2.
    SLOT_20(0x20, Creature.AnimationType.SPECIAL_1), // special 1.
    SLOT_21(0x21, Creature.AnimationType.SPECIAL_2), // special 2.
    SLOT_22(0x22, Creature.AnimationType.DRUNKED_WALK), // drunked walk.
    SLOT_23(0x23, Creature.AnimationType.ROAR), // roar.
    SLOT_24(0x24, Creature.AnimationType.NULL_4), // null 4.
    SLOT_25(0x25, Creature.AnimationType.DRUNKED_IDLE), // drunked idle.
    SLOT_26(0x26, Creature.AnimationType.BACK_OFF), // back off.
    SLOT_27(0x27, Creature.AnimationType.STAND_STILL), // stand still.
    SLOT_28(0x28, Creature.AnimationType.STEALTH_WALK), // stealth walk.
    SLOT_29(0x29, Creature.AnimationType.DEATH_POSE); // death pose.

    private final int value;
    private final Creature.AnimationType animationType;

    AnimationSlot(int value, Creature.AnimationType animationType) {
        this.value = value;
        this.animationType = animationType;
    }

    @Override
    public int getValue() {
        return value;
    }

    /** Existing resource key; retained for view-state and saved-data compatibility. */
    public Creature.AnimationType getAnimationType() {
        return animationType;
    }
}
