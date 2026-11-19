/* Copyright (C) 2026 OpenKeeper. SPDX-License-Identifier: GPL-3.0-or-later */
package toniarts.openkeeper.game.navigation.steering;

import toniarts.openkeeper.tools.convert.map.Creature;

/** Authored horizontal body size, independent of level-scaled visual height. */
public final class CreatureSizing {

    private CreatureSizing() {
    }

    /** Native pilots use half the definition width as their contact radius. */
    public static float getBodyRadius(Creature definition) {
        return definition.getAttributes().getWidth() * 0.5f;
    }
}
