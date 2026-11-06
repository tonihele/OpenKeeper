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

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import toniarts.openkeeper.Main;

/**
 * Launches repeated multi-workload suite runs in child processes, then
 * aggregates an official machine baseline.
 * <p>
 * Example:
 * {@code java ... BenchmarkSuiteRunner --runs 3}
 * Optional long coverage: {@code --full} or {@code --workloads a,b,c}.
 */
public final class BenchmarkSuiteRunner {

    private static final Logger logger = System.getLogger(BenchmarkSuiteRunner.class.getName());

    private BenchmarkSuiteRunner() {
    }

    public static void main(String[] args) throws Exception {
        int runs = 3;
        List<String> workloads = new ArrayList<>();
        Path outRoot = Path.of("baselines", "performance");
        Path compareBaseline = null;
        boolean updateVisualRefsOnFirstRun = true;
        boolean writeOfficial = true;
        boolean fullSuite = false;
        double duration = -1;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--runs" -> runs = Integer.parseInt(args[++i]);
                case "--workloads" -> {
                    for (String part : args[++i].split(",")) {
                        workloads.add(part.trim());
                    }
                }
                case "--full" -> fullSuite = true;
                case "--out" -> outRoot = Path.of(args[++i]);
                case "--compare" -> compareBaseline = Path.of(args[++i]);
                case "--no-update-visual-refs" -> updateVisualRefsOnFirstRun = false;
                case "--no-official" -> writeOfficial = false;
                case "--duration" -> duration = Double.parseDouble(args[++i]);
                default -> throw new IllegalArgumentException("Unknown arg: " + args[i]);
            }
        }
        if (workloads.isEmpty()) {
            List<WorkloadProfile> profiles = fullSuite
                    ? WorkloadProfile.FULL_SUITE
                    : WorkloadProfile.DEFAULT_SUITE;
            for (WorkloadProfile profile : profiles) {
                workloads.add(profile.getId());
            }
        }

        Files.createDirectories(outRoot);
        String stamp = Instant.now().toString().replace(':', '-');
        Path sessionDir = outRoot.resolve("session-" + stamp);
        Files.createDirectories(sessionDir);

        List<Path> primaryRunFiles = new ArrayList<>();
        Map<String, Object> session = new LinkedHashMap<>();
        session.put("frameworkVersion", BenchmarkConfig.FRAMEWORK_VERSION);
        session.put("runsRequested", runs);
        session.put("workloads", workloads);
        List<Map<String, Object>> runRecords = new ArrayList<>();

        for (int run = 1; run <= runs; run++) {
            Path runDir = sessionDir.resolve("run-" + run);
            Files.createDirectories(runDir);
            Map<String, Object> runRecord = new LinkedHashMap<>();
            runRecord.put("runIndex", run);
            List<String> produced = new ArrayList<>();

            for (String workloadId : workloads) {
                WorkloadProfile profile = WorkloadProfile.byId(workloadId);
                Path outJson = runDir.resolve(profile.getId() + ".json");
                Path childLog = runDir.resolve(profile.getId() + ".log");
                int exit = launchMain(profile, outJson, childLog, run, updateVisualRefsOnFirstRun && run == 1,
                        duration, compareBaseline);
                produced.add(outJson.toString().replace('\\', '/'));
                if (exit != 0) {
                    throw new IllegalStateException("Benchmark child failed for " + profile.getId()
                            + " run " + run + " exit=" + exit + " (see " + childLog + ")");
                }
                if (WorkloadProfile.LATE_CAMPAIGN_DENSE.getId().equals(profile.getId())) {
                    primaryRunFiles.add(outJson);
                }
            }
            runRecord.put("results", produced);
            runRecords.add(runRecord);
        }

        session.put("runs", runRecords);
        BenchmarkResultWriter.write(sessionDir.resolve("session.json"), session);

        if (primaryRunFiles.size() == runs) {
            Map<String, Object> baseline = BaselineAggregator.aggregate(
                    primaryRunFiles, BaselineAggregator.defaultMachineId());
            baseline.put("sessionDir", sessionDir.toString().replace('\\', '/'));
            baseline.put("workloadId", WorkloadProfile.LATE_CAMPAIGN_DENSE.getId());
            Path aggregatePath = outRoot.resolve("candidate-baseline-" + BaselineAggregator.defaultMachineId() + ".json");
            BaselineAggregator.write(aggregatePath, baseline);
            System.out.println("CANDIDATE_BASELINE=" + aggregatePath.toAbsolutePath());

            if (writeOfficial) {
                Path official = outRoot.resolve("official-baseline-" + BaselineAggregator.defaultMachineId() + ".json");
                BaselineAggregator.write(official, baseline);
                Path latest = outRoot.resolve("official-baseline-latest.json");
                BaselineAggregator.write(latest, baseline);
                logger.log(Level.INFO, "Official baseline written to {0}", official.toAbsolutePath());
                System.out.println("OFFICIAL_BASELINE=" + official.toAbsolutePath());
            }

            if (compareBaseline != null && Files.isRegularFile(compareBaseline)) {
                Map<String, Object> compareReport = RegressionComparer.compareAggregates(
                        compareBaseline, aggregatePath);
                Path reportPath = outRoot.resolve("regression-vs-baseline.json");
                BenchmarkResultWriter.write(reportPath, compareReport);
                System.out.println("REGRESSION_REPORT=" + reportPath.toAbsolutePath());
                System.out.println("REGRESSION_STATUS=" + compareReport.get("overall"));
            }
        } else {
            logger.log(Level.WARNING, "Skipped official aggregate; primary workload files={0} runs={1}",
                    primaryRunFiles.size(), runs);
        }
    }

    private static int launchMain(WorkloadProfile profile, Path outJson, Path childLog, int runIndex,
            boolean updateVisualRefs, double durationOverride, Path compareBaseline) throws Exception {
        String java = ProcessHandle.current().info().command().orElse("java");
        String cp = System.getProperty("java.class.path");
        List<String> cmd = new ArrayList<>();
        cmd.add(java);
        // Cap heap so a runaway spawn cannot thrash the machine unnoticed
        cmd.add("-Xmx8g");
        cmd.add("-cp");
        cmd.add(cp);
        cmd.add(Main.class.getName());
        cmd.add("-benchmark-suite");
        cmd.add(outJson.toString());
        cmd.add("-benchmark-workload");
        cmd.add(profile.getId());
        cmd.add("-benchmark-run-index");
        cmd.add(Integer.toString(runIndex));
        cmd.add("-benchmark-baseline-dir");
        cmd.add("baselines/performance");
        if (durationOverride > 0) {
            cmd.add("-benchmark-duration");
            cmd.add(Double.toString(durationOverride));
        }
        if (updateVisualRefs) {
            cmd.add("-benchmark-update-visual-refs");
        }
        if (compareBaseline != null) {
            cmd.add("-benchmark-compare");
            cmd.add(compareBaseline.toString());
        }
        cmd.add("-nomovies");

        logger.log(Level.INFO, "Launching {0} runIndex={1} log={2}", profile.getId(), runIndex, childLog);
        Files.createDirectories(childLog.toAbsolutePath().getParent());
        ProcessBuilder pb = new ProcessBuilder(cmd);
        // Important: do NOT stream child logs through this process into Gradle's
        // stdout pipe — CreatureMoodSystem spam will deadlock the pipes.
        pb.redirectErrorStream(true);
        pb.redirectOutput(ProcessBuilder.Redirect.appendTo(childLog.toFile()));
        pb.directory(Path.of("").toAbsolutePath().toFile());
        Process process = pb.start();
        int code = process.waitFor();
        // Tail a few progress lines for the console
        if (Files.isRegularFile(childLog)) {
            List<String> lines = Files.readAllLines(childLog);
            for (String line : lines) {
                if (line.contains("Benchmark phase") || line.contains("Benchmark results")
                        || line.contains("Visual checkpoint") || line.contains("Benchmark summary")
                        || line.contains("Benchmark armed") || line.contains("ERROR")) {
                    System.out.println(line);
                }
            }
        }
        return code;
    }
}
