/* Copyright (C) 2026 OpenKeeper, licensed under GPL-3.0-or-later. */
package toniarts.openkeeper.view.animation;

import com.simsilica.es.base.DefaultEntityData;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.SimulationTime;
import static org.junit.jupiter.api.Assertions.*;

class PresentationClockTest {
    @Test void lateCreationUsesSimulationOriginAndPredictionStopsOnPauseOrStalledDelivery() {
        var data = new DefaultEntityData(); var id = data.createEntity();
        try (var clock = new PresentationClock(data)) {
            assertTrue(Double.isNaN(clock.getTime()));
            data.setComponent(id, new SimulationTime(20,.1)); clock.update(.05f);
            assertEquals(20,clock.getTime()); clock.update(.05f); assertEquals(20.05,clock.getTime(),1e-6);
            clock.setPaused(true); clock.update(100); assertEquals(20.05,clock.getTime(),1e-6);
            clock.setPaused(false); clock.update(100); clock.update(100); assertEquals(20.2,clock.getTime(),1e-6);
            data.setComponent(id,new SimulationTime(20.1,.1)); clock.update(.05f);
            assertEquals(20.2,clock.getTime(),1e-6); // Delayed clock packets never reverse a pose.
            data.setComponent(id,new SimulationTime(30,.1)); clock.update(0); assertEquals(30,clock.getTime());
        }
    }
}
