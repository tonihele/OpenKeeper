# Ceiling mesh clearance-sample cache

**Date:** 2026-11-09  
**Base:** `origin/master` at `617997096b8018e2bd3bb3979cabb731912c0e08`  
**Related work:** [PR #1012](https://github.com/tonihele/OpenKeeper/pull/1012), roadmap item D1

## Finding and change

`MapViewController.refreshCeilingSurface` rebuilds a visible ceiling from the whole map. `Ceiling.createMesh` repeatedly samples the same `ClearanceField` coordinates for shared tile edges, quad centres, and vertex normals. Those coordinates lie on a quarter-tile grid. The new per-rebuild `ClearanceFieldSamples` cache stores each exact result once and is shared by all material batches. Non-grid queries fall back to the original calculation. The mesh topology, vertex attributes, material grouping, and draw count are unchanged.

The dense cache adds about 0.47 MB per rebuild on an 85×85 map. It is enabled only when at least 1,024 ceiling tiles are built; smaller reveals use direct sampling.

## Method

`./gradlew benchmarkCeilingBuild` builds the production ceiling meshes for a deterministic synthetic 85×85 dungeon mask with four material batches. It compares direct sampling with cached sampling at 64, 256, 1,024, and all 6,265 visible tiles. Each profile uses four warm-up pairs and nine measured pairs, alternating A/B order. The reported values are medians; the task was run four times in separate JVMs, including the final validation run. `ThreadMXBean` measures current-thread allocations. Explicit GC runs between, outside the timed mesh builds.

Machine: AMD Ryzen 5 3600 (12 logical processors), NVIDIA GeForce RTX 2060 SUPER, Java 25.0.4.1. The benchmark builds CPU meshes and does not create a rendering context.

## Results

| Visible tiles | Uncached mesh build | Cached mesh build | CPU reduction | Allocation per build |
|---:|---:|---:|---:|---:|
| 64 | 1.237–1.513 ms | 1.025–1.491 ms | 1.4–19.0% | 0.103 → 0.571 MB |
| 256 | 2.255–3.530 ms | 1.172–1.772 ms | 28.9–49.8% | 0.438 → 0.906 MB |
| 1,024 | 8.868–10.636 ms | 3.408–4.177 ms | 59.1–61.9% | 1.758 → 2.226 MB |
| 6,265 | 54.472–58.631 ms | 17.919–19.241 ms | 65.5–68.1% | 10.781–10.782 → 11.250 MB |

The 1,024-tile threshold avoids the less predictable, sub-millisecond savings at smaller reveals. The cache keeps all existing material batches; it does not trade CPU work for more draw calls.

## Correctness and limits

`ClearanceFieldSamplesTest` compares every quarter-grid sample against the original calculation, checks arbitrary-coordinate fallback, and compares cached versus uncached mesh positions, UVs, normals, indices, and triangle counts exactly.

This is a CPU mesh-build microbenchmark, not a gameplay FPS or GPU benchmark. It excludes the map scan, material setup, scene attachment, and driver work in `refreshCeilingSurface`; the whole-refresh speedup will therefore be smaller than the mesh-build result. The regular late-campaign suite keeps the ceiling hidden, so it does not measure this path. A ceiling-on Level20 dig scenario remains necessary before making a frame-time or gameplay hitch claim.

The benchmark records allocated bytes but not GC pause duration or frequency; explicit GC between samples makes those values unsuitable for a GC comparison.

Validation on the feature branch: `./gradlew test`, `./gradlew build`, and three repeated `./gradlew benchmarkCeilingBuild` runs.
