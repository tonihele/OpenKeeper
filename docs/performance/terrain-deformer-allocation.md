# Terrain deformer allocation benchmark

Run with:

~~~powershell
.\gradlew.bat terrainDeformerBenchmark --console plain
~~~

The deterministic fixture uses an 85×85 tile field and a 171×171 position grid
(29,241 vertices) with position and tangent buffers. The cold phase constructs a
fresh deformer and fills its lazy field; the warm phase reuses that field. The
synthetic tile-shape provider allocates a TileShape and its lean arrays on each
lookup, matching the allocation behavior of TileShape.of in the map view.
Each JVM run takes nine measured samples after three warmups. The table reports
the median of the per-process medians from six alternating baseline and
optimized JVM runs. The process-median ranges are shown in parentheses.
Allocation counts come from HotSpot's per-thread allocation counter.

| Phase | Baseline | Optimized | Change |
|---|---:|---:|---:|
| Cold deformation time | 55.45 ms (52.60–61.00) | 45.39 ms (41.97–47.89) | −18.2% |
| Cold allocation per deformation | 126.48 MiB | 37.62 MiB | −70.3% |
| Warm deformation time | 4.11 ms (4.06–4.26) | 3.89 ms (3.75–3.95) | −5.5% |
| Warm allocation per deformation | 2.46 MiB | 0.90 MiB | −63.6% |
| Cold tile-shape provider calls | 4,366,437 | 7,225 | −99.8% |

All baseline and optimized runs produced the same position/tangent checksum:
10046f274bb42c54. TerrainDeformerTest.invalidatingGivesTheSameAsFreshField
also verifies that changing a tile and invalidating it refreshes the cached
shape correctly.

This is a CPU component benchmark, not a full-game FPS or GPU measurement. The
checkout does not include the original KWD/KMF gameplay assets, so the fixture
uses deterministic synthetic terrain shapes and a map-sized mesh.
