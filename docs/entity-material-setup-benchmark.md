# Entity Material Setup Benchmark

Base revision: `b5db078d` (`origin/master`). The benchmark isolates the repeated entity material pass in `PlayerEntityViewState`; it does not measure whole-game FPS, rendering, or GPU work.

## Method

The deterministic fixture has 480 models (128 objects, 256 creatures, 32 doors, 64 traps), five geometries per model, and the production `Flat` and `Vertex` material definitions. It times the former per-frame path, including four `EntityContainer.getArray()`-equivalent snapshots and `AssetUtils.applyVertexLight`, against the stable change-driven path, which checks ambient readiness and the pending-setup map. Each process performs one setup pass, 20 warmup frames, then 20 alternating samples of 100 frames per mode. The reported p95 is the nearest-rank p95 of those 20 batch-average frame times.

Runs used an AMD Ryzen 5 3600, Windows 10 x64, Eclipse Temurin 25.0.4.1, and Gradle 9.8.1.

## Results

| Run | One-time setup (480 models) | Legacy median / batch-p95 | Change-driven median / batch-p95 | Legacy allocation / changed allocation |
|---|---:|---:|---:|---:|
| 1 | 33.605 ms; 6,817,528 B | 0.4213 / 0.4955 ms/frame | 0.0001 / 0.0003 ms/frame | 128,704 / 0 B/frame |
| 2 | 28.107 ms; 6,817,528 B | 0.5078 / 0.5880 ms/frame | 0.0001 / 0.0003 ms/frame | 128,704 / 0 B/frame |

The recurring entity material pass falls by about 0.42 to 0.51 ms/frame in this 480-model fixture and removes about 126 KiB of allocation per frame. One-time material conversion and binding took 28.1 to 33.6 ms across runs; the optimization avoids repeating that work on stable models. Entity animation or door state changes re-run setup for replaced or attached geometry.

## Limits and Reproduction

This CPU microbenchmark does not include scene updates, attached-light updates, presentation, or a real gameplay capture. Treat the result as targeted CPU and allocation evidence, not a whole-game frame-rate claim. Repeat with:

```powershell
.\gradlew.bat --offline --console=plain entityMaterialSetupBenchmark
.\gradlew.bat --offline --console=plain test --warning-mode none
```
