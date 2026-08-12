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

/** Marks a wooden bridge tile that will burn away over lava. */
public final class WoodenBridgeDecay implements EntityComponent {

    public double endTime;

    public WoodenBridgeDecay() {
    }

    public WoodenBridgeDecay(double endTime) {
        this.endTime = endTime;
    }
}
