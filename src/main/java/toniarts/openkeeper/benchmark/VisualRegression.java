/*
 * Copyright (C) 2014-2026 OpenKeeper
 *
 * OpenKeeper is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OpenKeeper is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenKeeper.  If not, see <http://www.gnu.org/licenses/>.
 */
package toniarts.openkeeper.benchmark;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages versioned visual reference frames and comparison reports.
 */
public final class VisualRegression {

    private final BenchmarkConfig config;
    private final Path runVisualDir;
    private final Path referenceDir;
    private final List<Map<String, Object>> checkpoints = new ArrayList<>();

    public VisualRegression(BenchmarkConfig config, Path runOutputDir) {
        this.config = config;
        this.runVisualDir = runOutputDir.resolve("visual");
        this.referenceDir = config.getBaselineDir().resolve("visual")
                .resolve(config.getWorkloadId())
                .resolve(config.getWidth() + "x" + config.getHeight());
    }

    public Path getRunVisualDir() {
        return runVisualDir;
    }

    public Path getReferenceDir() {
        return referenceDir;
    }

    public Map<String, Object> recordCheckpoint(String name, BufferedImage image) throws IOException {
        Path candidatePath = runVisualDir.resolve(name + ".png");
        FrameGrabber.writePng(image, candidatePath);

        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("name", name);
        entry.put("candidate", candidatePath.toString().replace('\\', '/'));
        entry.put("width", image.getWidth());
        entry.put("height", image.getHeight());

        Path refPath = referenceDir.resolve(name + ".png");
        if (config.isUpdateVisualRefs() || !Files.isRegularFile(refPath)) {
            FrameGrabber.writePng(image, refPath);
            entry.put("referenceUpdated", true);
            entry.put("reference", refPath.toString().replace('\\', '/'));
            entry.put("status", "reference-written");
            entry.put("passed", true);
        } else {
            BufferedImage reference = FrameGrabber.readPng(refPath);
            ImageMetrics.Result cmp = ImageMetrics.compare(reference, image, true);
            entry.put("reference", refPath.toString().replace('\\', '/'));
            entry.put("referenceUpdated", false);
            entry.putAll(cmp.toMap());
            boolean passed = cmp.passes(config.getMaxMismatchRatio(), config.getMinSsim(), config.getMaxMae());
            entry.put("passed", passed);
            entry.put("status", passed ? "pass" : "fail");
            if (cmp.diffImage != null && (!passed || cmp.mismatchedPixels > 0)) {
                Path diffPath = runVisualDir.resolve(name + ".diff.png");
                FrameGrabber.writePng(cmp.diffImage, diffPath);
                entry.put("diffImage", diffPath.toString().replace('\\', '/'));
            }
            // Hardware-tolerance note when soft thresholds pass but pixels differ
            if (passed && cmp.mismatchedPixels > 0) {
                entry.put("hardwareToleranceApplied", true);
            }
        }
        checkpoints.add(entry);
        return entry;
    }

    public Map<String, Object> summarize() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("referenceDir", referenceDir.toString().replace('\\', '/'));
        out.put("runVisualDir", runVisualDir.toString().replace('\\', '/'));
        out.put("checkpoints", checkpoints);
        long failed = checkpoints.stream().filter(c -> Boolean.FALSE.equals(c.get("passed"))).count();
        out.put("failedCount", failed);
        out.put("checkpointCount", checkpoints.size());
        // No successful captures ⇒ not a pass (avoids green results when grabber throws).
        out.put("passed", failed == 0 && !checkpoints.isEmpty());
        out.put("thresholds", Map.of(
                "maxMismatchRatio", config.getMaxMismatchRatio(),
                "minSsim", config.getMinSsim(),
                "maxMae", config.getMaxMae()));
        return out;
    }
}
