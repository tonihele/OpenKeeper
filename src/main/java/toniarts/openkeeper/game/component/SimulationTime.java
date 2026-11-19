/* Copyright (C) 2026 OpenKeeper, licensed under GPL-3.0-or-later. */
package toniarts.openkeeper.game.component;

import com.simsilica.es.EntityComponent;

/** One clock entity per game; published on the simulation thread. */
public final class SimulationTime implements EntityComponent {
    public double seconds;
    public double tickSeconds;
    public SimulationTime() { }
    public SimulationTime(double seconds, double tickSeconds) {
        this.seconds = seconds; this.tickSeconds = tickSeconds;
    }
}
