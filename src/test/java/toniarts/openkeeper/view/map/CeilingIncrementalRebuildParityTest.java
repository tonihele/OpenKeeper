/*
 * Copyright (C) 2014-2026 OpenKeeper
 *
 * OpenKeeper is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package toniarts.openkeeper.view.map;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CeilingIncrementalRebuildParityTest {

    @Test
    void dirtyChunkMeshesMatchFullRebuildAfterDigAndFogReveal() {
        for (CeilingIncrementalBenchmark.Scenario scenario : CeilingIncrementalBenchmark.parityScenarios()) {
            for (int chunkSize : CeilingIncrementalBenchmark.candidateChunkSizes()) {
                assertTrue(CeilingIncrementalBenchmark.verifyParity(scenario, chunkSize),
                        () -> scenario.name() + " differs from a full ceiling rebuild at chunk size " + chunkSize);
            }
        }
    }
}
