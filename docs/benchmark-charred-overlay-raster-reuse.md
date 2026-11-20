# Charred overlay raster reuse benchmark

## Change

`CharredOverlay` now reuses one `ColorRGBA` scratch value when sampling the overlay. The previous `ImageRaster.getPixel(x, y)` convenience overload created one `ColorRGBA` for every output pixel. The cached baked image and blend math are unchanged.

## Method

The standalone benchmark compares the old sampling loop with the production loop on deterministic RGBA8 images (512 x 512 diffuse and 128 x 128 overlay). It warms each path for three bakes, runs seven paired samples in alternating order with two bakes per sample, and records median time and Java heap bytes allocated per bake. Six separate runs used the same machine and JDK. Outputs are retained during timing, as the production cache retains baked textures. The parity test compares complete output buffers for RGBA8/sRGB and ABGR8/linear inputs with differently sized overlays.

Machine: AMD Ryzen 5 3600, 6 cores / 12 threads. Runtime: OpenJDK 25.0.4.1.

## Results

| Run | Original median | Reused scratch median | Time reduction | Heap allocation per bake |
|---|---:|---:|---:|---:|
| 1 | 16.311 ms | 14.589 ms | 10.6% | 8192.8 KiB -> 0.8 KiB |
| 2 | 16.345 ms | 14.593 ms | 10.7% | 8192.8 KiB -> 0.8 KiB |
| 3 | 16.284 ms | 14.537 ms | 10.7% | 8192.8 KiB -> 0.8 KiB |
| 4 | 16.833 ms | 15.439 ms | 8.3% | 8192.8 KiB -> 0.8 KiB |
| 5 | 16.354 ms | 14.640 ms | 10.5% | 8192.8 KiB -> 0.8 KiB |
| 6 | 16.339 ms | 14.604 ms | 10.6% | 8192.8 KiB -> 0.8 KiB |

The median per-run time reduction is 10.6% (range 8.3-10.7%). The matched synthetic bake saves about 1.7 ms and 8 MiB of transient Java heap allocation at 512 x 512. The test confirms byte-for-byte output parity. This is a CPU/allocation result for texture generation; it does not establish an FPS change. Converted DK II game assets are absent from this checkout, so an in-game death-burst scenario could not be measured.

## Reproduction

```powershell
.\gradlew.bat test --tests toniarts.openkeeper.view.CharredOverlayTest
.\gradlew.bat charredOverlayBenchmark
```
