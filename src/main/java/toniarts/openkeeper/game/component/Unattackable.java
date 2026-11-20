/* Copyright (C) 2026 OpenKeeper. SPDX-License-Identifier: GPL-3.0-or-later */
package toniarts.openkeeper.game.component;

import com.simsilica.es.EntityComponent;

/**
 * A creature whose authored WILL_BE_ATTACKED placement flag is off. Ordinary
 * creature and trap target selection ignores it, independently of whether it
 * will fight. This does not prevent damage from attacks that already hit it.
 * Absence of this component preserves normal targeting for runtime spawns.
 */
public final class Unattackable implements EntityComponent {

    public Unattackable() {
        // For serialization
    }
}
