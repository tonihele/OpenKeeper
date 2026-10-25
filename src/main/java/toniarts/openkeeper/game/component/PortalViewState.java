/*
 * Copyright (C) 2026 OpenKeeper
 *
 * OpenKeeper is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package toniarts.openkeeper.game.component;

import com.simsilica.es.EntityComponent;

/** The last Portal build-up event, separate from its server-side generation accounting. */
public final class PortalViewState implements EntityComponent {
    /** Scheduled build-up time in game seconds; negative means no build-up since capture. */
    public double preparationTime;

    public PortalViewState() {
    }

    public PortalViewState(double preparationTime) {
        this.preparationTime = preparationTime;
    }
}
