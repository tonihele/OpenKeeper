# Minimap Selection Highlight Scan

## Finding and change

The minimap selection and pending-tag overlay scans every map cell during its refresh. The point-based path allocated a coordinate object per cell and repeated map lookups for selection, fog visibility, and highlightability. The optimized path resolves each tile once, then uses tile-aware queries. Point-based defaults preserve compatibility with other fog implementations.

## Benchmark method

Run with:

```powershell
.\gradlew.bat --offline --no-daemon minimapSelectionHighlightBenchmark --console plain
```

The benchmark compares the original point scan with the production helper on an 85x85 synthetic map in clear, sparse-selection/tag, and dense-selection/tag scenarios. It alternates measurement order, checks exact colour-grid equality and checksums, and records thread allocations with `ThreadMXBean`. Each process warms up for six batches and measures nine paired batches. Results below summarize six fresh JVM runs; times are the median of the six per-process medians.

## Results

| Scenario | Original scan | Tile-aware scan | Median time saved | Allocated bytes per scan |
| --- | ---: | ---: | ---: | ---: |
| Clear | 0.3781 ms | 0.2422 ms | 35.9% | 169.34 KiB -> 0 |
| Sparse | 0.4808 ms | 0.3026 ms | 37.1% | 171.38 KiB -> 0 |
| Dense | 0.4090 ms | 0.2562 ms | 37.4% | 196.31 KiB -> 0 |

All six paired runs improved scan time in every scenario, and the output checksums matched. Per-run times varied: clear original/candidate ranged 0.340-0.474/0.183-0.282 ms, sparse 0.353-0.558/0.214-0.440 ms, and dense 0.245-0.576/0.137-0.418 ms. Dense baseline allocation was 196.31 KiB in five runs and 169.34 KiB in one; the candidate reported zero in all runs.

## Validation and limits

Focused tests passed for the highlighter, fog pending tags, map colour grid, and colour classifier. This synthetic microbenchmark measures CPU time and Java allocation for the overlay scan only. It does not establish whole-game FPS, frame-time, GPU, or gameplay-scene gains; those require a matched runtime capture.
