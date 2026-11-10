# Ceiling Chunking Benchmark

## Method

The CPU harness compares the current full refresh with rebuilding dirty spatial chunks. It includes tile grouping, `ClearanceField` construction, and mesh generation on a deterministic 85x85 map (6,265 ceiling tiles). Each case uses six warmup pairs and nine alternating measurements; two process runs were recorded. Scenarios cover a single dig, 8x8 and 16x16 room digs, a 24x24 room dig, and a 16x16 fog reveal.

The renderer harness uses the production `DkLighting` vertex shader and ambient-map path, 256 fixed synthetic lights, a 1280x720 offscreen OpenGL viewport, and a 31x17 tile view centered on the edited area. Each mode has 120 warmup and 120 measured frames, in forward and reverse order, across two process runs. Hardware: NVIDIA RTX 2060 SUPER, driver 617.42, OpenGL 3.2, jMonkeyEngine 3.9.0.

Run with `.\gradlew.bat --offline ceilingIncrementalBenchmark` and `.\gradlew.bat --offline ceilingIncrementalRenderBenchmark`.

## Results

| Scenario / mode | Full | 48-tile chunks | Change |
| --- | ---: | ---: | ---: |
| Single-tile dig refresh | 61.4 ms | 18.7 ms | -69.6% |
| 8x8 room dig refresh | 61.7 ms | 18.8 ms | -69.5% |
| 16x16 room dig refresh | 61.2 ms | 19.2 ms | -68.7% |
| 24x24 room dig refresh | 58.6 ms | 59.9 ms | +2.3% |
| 16x16 fog reveal refresh | 59.4 ms | 60.7 ms | +2.1% |
| GPU ceiling pass, median | 0.470 ms | 0.160 ms | -65.9% |
| GPU ceiling pass, median of p95 values | 0.472 ms | 0.166 ms | -64.8% |

For a single dig, estimated Java allocation fell from 11.15 MiB to 3.32 MiB; rebuilt mesh buffers fell from 2.30 MiB to 0.75 MiB. The 48-tile GPU view intersects four chunk meshes and 18,304 vertices, versus all four full-map meshes and 56,485 vertices. Total map geometry remains 16 meshes at this chunk size, so offscreen portions are culled without increasing visible draw bounds in this view.

Chunk sizes 8, 16, 32, 48, and 64 were CPU-swept. Size 32 used 16 view-intersecting meshes and measured 0.291 ms GPU median; size 64 retained 32,557 view-intersecting vertices and measured the same 0.291 ms. Size 48 had four view-intersecting meshes and the lowest measured GPU time. Sizes 8 and 16 were not selected because they create 121 and 96 meshes map-wide; they were not GPU-tested.

## Decision and limits

The implementation uses 48-tile spatial chunks, rebuilds only chunks within the clearance/deformer dirty margin, and shares each terrain/ceiling material across its chunks. Exact per-tile position, UV, normal, and index parity passed for digs, fog reveals, chunk-boundary edits, map-edge edits, and nonzero terrain deformation. The full test suite passed.

Broad edits touch every chunk and show a repeatable 1-2% CPU regression; the code retains chunking to preserve view culling after those updates. GPU timings are from a synthetic light distribution, not a live level, and CPU render-submit samples drifted by pass order, so no whole-game FPS or CPU-submission gain is claimed. One renderer pass also reported a 0.752 ms chunk-48 GPU p95 outlier; the other three were 0.165-0.166 ms.
