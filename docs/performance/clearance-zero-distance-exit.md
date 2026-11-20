# Clearance Zero-Distance Early Exit

`ClearanceField.clearanceAt` searches nearby tiles for the closest solid tile. Once a candidate has zero distance, no later candidate can improve the result, so the query returns immediately.

## Measurement

Run `.\gradlew.bat --offline clearanceFieldDistanceBenchmark`. The benchmark compares the previous implementation with the early-exit implementation on a deterministic 85x85 map and 118,336 quarter-tile sample positions. It runs 20 warmup pairs and 21 measured pairs, alternating method order in the same JVM. Environment: Java 25.0.4.1.

| Run | Previous median | Early-exit median | Change |
| --- | ---: | ---: | ---: |
| 1 | 18.871 ms | 16.451 ms | -12.8% |
| 2 | 18.788 ms | 16.756 ms | -10.8% |
| 3 | 18.764 ms | 16.422 ms | -12.5% |
| 4 | 18.807 ms | 16.470 ms | -12.4% |
| 5 | 19.340 ms | 16.982 ms | -12.2% |

The median reduction was 12.4%. The exact-output test compares float bits against the prior calculation across dense quarter-tile positions and 2,000 off-grid points on 1x1, 7x9, and 85x85 maps.

Run the parity test with `.\gradlew.bat --offline test --tests toniarts.openkeeper.view.map.ClearanceFieldDistanceParityTest`. This measures the CPU helper only and does not claim whole-game FPS or GPU improvement.
