# Fog-of-War Door Lookup Benchmark

`FogOfWarController.blocksSight()` runs for each line-of-sight sample. The previous implementation allocated a `Point` and queried a `HashMap` for every sample. Door positions change on vision updates, so a map-sized boolean index can answer these lookups directly. The implementation tracks touched indices and clears only those on refresh.

## Method

`FogDoorLookupBenchmark` runs the production LOS ring algorithm on a deterministic 85×85 map with 20 doors and 16, 64, or 128 vision sources. Each process warms up for 20 paired iterations, then records 21 alternating measurements. It compares rebuilding and querying the `Point` map with refreshing and querying the sparse-cleared grid; both paths include door-definition lookups and drain dirty tiles. Eleven separate JVM runs were collected on Java 25.0.4.1.

| Sources | Median Point path | Median grid path | Time reduction | Allocations per pass |
|---:|---:|---:|---:|---:|
| 16 | 0.153 ms | 0.116 ms | 24.2% | 42.5 → 5.3 KiB |
| 64 | 0.443 ms | 0.352 ms | 20.5% | 169.6 → 21.1 KiB |
| 128 | 0.524 ms | 0.296 ms | 43.5% | 332.9 → 36.1 KiB |

All eleven runs improved in each scenario. The benchmark also verifies explored-tile parity. `FogOfWarControllerTest` covers ordinary doors, barricades, and moving a door so its previous grid slot is cleared.

## Limits and reproduction

These are synthetic LOS/index timings, not whole-game frame-rate measurements. Run `gradlew fogDoorLookupBenchmark` to repeat them. Run `gradlew test` for the controller behavior and project test suite.
