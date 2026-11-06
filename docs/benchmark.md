# OpenKeeper performance & visual regression framework

Deterministic late-game stress suite with sustained metrics, visual parity checkpoints, multi-run baselines, and regression comparison.

Framework version: **2**

## Related docs

| Doc | Role |
|-----|------|
| [acceleration-structures.md](acceleration-structures.md) | Prioritized acceleration / optimization roadmap (index) |
| [acceleration-lighting.md](acceleration-lighting.md) | Lighting grids, indices, AmbientMap |
| [acceleration-spatial-simulation.md](acceleration-spatial-simulation.md) | FoW, A\*, sensing, picking |
| [acceleration-mesh-scene.md](acceleration-mesh-scene.md) | Pages, ceiling, chunk meshes |
| [rendering-pipeline-analysis.md](rendering-pipeline-analysis.md) | Original rendering audit |
| `benchmark-*-opt.md` / `benchmark-lightrules-lifecycle.md` | Measured before/after change reports |
| [benchmark-room-light-index.md](benchmark-room-light-index.md) | B1 room indices — **REJECTED** |
| [benchmark-shared-nonpage-light-index.md](benchmark-shared-nonpage-light-index.md) | B1-retry shared non-page index — **REJECTED** |
| [benchmark-a3-light-timers.md](benchmark-a3-light-timers.md) | A3 light CPU timers — **ACCEPTED tooling** |
| [benchmark-a2-soft-falloff.md](benchmark-a2-soft-falloff.md) | A2 soft-falloff sync — **ACCEPTED correctness** |
| [benchmark-astar-connection-pool.md](benchmark-astar-connection-pool.md) | C3 A\* connection reuse — **REJECTED** |
| [benchmark-fow-door-index.md](benchmark-fow-door-index.md) | C1 FoW door index — **REJECTED** |

## Quick start (fast path)

Default timings are short so a full baseline fits in a few minutes:

| Phase | Default |
|-------|---------|
| Settle | 2s |
| Warm-up | 3s |
| Measure | **20s** |
| Workloads | **1** (`late-campaign-dense`) |
| Baseline repeats | **3** |

```bash
# One suite run (~load + 25s)
./gradlew benchmarkSuite

# Official machine baseline: 3× primary workload
./gradlew benchmarkBaseline

# Optional: all three maps × 3 (slow)
./gradlew benchmarkBaselineFull
```

Compare later:

```bash
./gradlew run --args="-benchmark-suite build/benchmark-results/candidate.json -benchmark-workload late-campaign-dense -benchmark-compare baselines/performance/official-baseline-latest.json -nomovies"
```

## Workloads

| Id | Map | Default suite |
|----|-----|---------------|
| `late-campaign-dense` | Level20 | yes (primary) |
| `caverns-complex` | Caverns | `--full` only |
| `pressure-skirmish` | Pressure | `--full` only |

Override length anytime: `-benchmark-duration 60` or SuiteRunner `--duration 60`.

## Visual regression

- Refs: `baselines/performance/visual/<workload>/1920x1080/cpXX.png`
- Checkpoints at 0%, 50%, 100% of measure
- **Pass gate:** SSIM ≥ 0.92 and MAE ≤ 4.0 (pixel mismatch is diagnostic for live sim)
- Diff heatmaps on failure; first capture uses `-benchmark-update-visual-refs`

## Official baseline layout

```
baselines/performance/
  official-baseline-latest.json
  official-baseline-<machineId>.json
  session-<timestamp>/
  visual/<workload>/1920x1080/
```

## CLI (selected)

| Flag | Meaning |
|------|---------|
| `-benchmark-suite [out.json]` | Suite mode |
| `-benchmark-workload <id>` | Named workload |
| `-benchmark-duration / -warmup / -settle` | Timing overrides |
| `-benchmark-compare <baseline.json>` | Attach regression section |
| SuiteRunner `--full` | All three maps |
| SuiteRunner `--duration <sec>` | Override measure length |
| SuiteRunner `--runs <n>` | Repeat count (default 3) |
