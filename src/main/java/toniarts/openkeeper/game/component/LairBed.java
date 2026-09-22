/*
 * Copyright (C) 2014-2026 OpenKeeper
 *
 * OpenKeeper is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package toniarts.openkeeper.game.component;

import com.simsilica.es.EntityComponent;

/** Identifies the creature variant that owns a lair-bed object. */
public final class LairBed implements EntityComponent {

    public short creatureId;

    public LairBed() {
        // For serialization
    }

    public LairBed(short creatureId) {
        this.creatureId = creatureId;
    }
}
