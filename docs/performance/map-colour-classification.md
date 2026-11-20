# Map colour classification benchmark

## Change

`MapColourClassifier` now reuses each tile's stored location and passes the already-resolved `Terrain` into the solidity check. The old owned-tile path looked up the same map tile and terrain again. The default interface method keeps point-based behavior for other implementations; `MapInformation` uses the resolved terrain directly.

## Method

Run `.\gradlew.bat --offline mapColourClassifierBenchmark --console plain`. The paired benchmark scans an 85x85 owned floor/rock fixture, warms each path, alternates baseline and candidate order across nine measured batches, and checks both output arrays and checksums. Each value below is the median of six fresh benchmark JVM runs; times are per full map scan. Allocation is reported by `ThreadMXBean`.

| Run | Baseline (ms) | Candidate (ms) | Change |
|---:|---:|---:|---:|
| 1 | 0.2139 | 0.1560 | -27.1% |
| 2 | 0.1917 | 0.1287 | -32.9% |
| 3 | 0.2004 | 0.1454 | -27.4% |
| 4 | 0.1906 | 0.1274 | -33.1% |
| 5 | 0.1929 | 0.1257 | -34.8% |
| 6 | 0.1917 | 0.1601 | -16.5% |
| **Median** | **0.1923** | **0.1371** | **-28.7%** |

The median CPU saving is about 0.055 ms per 85x85 scan. Output arrays matched exactly. `ThreadMXBean` reported 0.00 KiB for both paths, so this benchmark does not show an allocation reduction.

## Validation and limits

Passed `MapColourClassifierTest`, `MapColourGridTest`, and `MinimapRasteriserTest`. The benchmark uses the map test double and an always-visible, room-free owned map. It isolates classification cost; it does not measure whole-game frame time, FPS, GPU cost, or other map/fog states. Treat the result as a local CPU improvement, not a game-wide FPS claim.
