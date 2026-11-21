/* Copyright (C) 2026 OpenKeeper. SPDX-License-Identifier: GPL-3.0-or-later */
package toniarts.openkeeper.game.logic;

import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.data.Keeper;

/** Owner/alliance policy, separate from physical contact and secret-door knowledge. */
final class DoorAccessRules {

    private DoorAccessRules() {
    }

    static boolean isFriendlyOwner(short doorOwner, short creatureOwner, ILevelInfo levelInfo) {
        // Native door tick 0x4A5E9E calls the owner's alliance check at 0x4BF3D0.
        if (doorOwner == creatureOwner) {
            return true;
        }
        Keeper keeper = levelInfo == null ? null : levelInfo.getPlayer(doorOwner);
        return keeper != null && keeper.isAlly(creatureOwner);
    }
}
