# OpenKeeper performance & visual regression framework

Deterministic late-game stress suite with sustained metrics, visual parity checkpoints, multi-run baselines, and regression comparison.

Framework version: **2**

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
