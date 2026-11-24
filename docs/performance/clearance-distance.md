# Clearance Distance Benchmark

`ClearanceField.clearanceAt` can inspect up to 121 nearby tiles. It now compares squared distances and takes one square root after selecting the nearest solid tile, instead of taking a square root for every solid candidate.

## Measurement

Run `.\gradlew.bat --offline clearanceFieldDistanceBenchmark`. The harness uses a deterministic 85x85 map and 118,336 quarter-tile sample positions, including the margin used for ceiling normals. It compares the previous calculation with the optimized method in the same JVM, alternating order across 20 warmup pairs and 21 measured pairs. Environment: Java 25.0.4.1.

| Run | Previous median | Optimized median | Change |
| --- | ---: | ---: | ---: |
| 1 | 18.752 ms | 17.653 ms | -5.9% |
| 2 | 18.795 ms | 17.984 ms | -4.3% |
| 3 | 19.346 ms | 16.835 ms | -13.0% |
| 4 | 21.094 ms | 18.632 ms | -11.7% |
| 5 | 22.580 ms | 20.560 ms | -8.9% |

The median observed reduction was 8.9%, with runs ranging from 4.3% to 13.0%. Absolute times varied between invocations; use the median reduction as the representative result.

The exact-output test compares float bits against the previous calculation on 1x1, 7x9, and 85x85 maps, including empty, patterned, and random solidity grids. It samples quarter-tile positions and 2,000 off-grid points per map. Run it with `.\gradlew.bat --offline test --tests toniarts.openkeeper.view.map.ClearanceFieldDistanceParityTest`.

This measures the ceiling CPU helper directly. It does not claim whole-game FPS or GPU improvement; the rendering work is unchanged.
