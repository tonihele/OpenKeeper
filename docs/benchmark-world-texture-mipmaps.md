# World texture mipmap benchmark

## Change

World model and map-surface textures at least 128 x 128 now use trilinear minification. jMonkeyEngine generates missing mip levels on the GPU when the texture is first uploaded. Smaller textures and the existing UI/minimap atlases keep their current filters. The base image is unchanged.

## Method

`mipSamplingBenchmark` compares base-level bilinear filtering with the same texture and GPU-generated mip chain. It renders one 1920 x 1080 offscreen view containing a deterministic 30 x 17 tile grid (510 surfaces, one draw call). Each tile maps the complete source texture to about 64 x 64 pixels, producing 8, 16, or 32 source texels per screen pixel for the 512, 1024, and 2048 inputs. Each run warms for 40 frames and measures 120 frames with OpenGL timer queries. It reports median and p95 GPU viewport time plus CPU render-submit time; the benchmark checks the base-level bytes before exit.

Machine: NVIDIA GeForce RTX 2060 SUPER, OpenGL 3.2, driver 617.42. Runtime: OpenJDK 25.0.4.1, jMonkeyEngine 3.9.0-stable. Each mode and size was run twice in alternating order.

## Results

| Texture | Base GPU median (runs) | Mip GPU median (runs) | Base GPU p95 (runs) | Mip GPU p95 (runs) | Estimated RGBA8 storage |
|---:|---:|---:|---:|---:|---:|
| 512 x 512 | 0.217 / 0.215 ms | 0.043 / 0.043 ms | 0.218 / 0.216 ms | 0.043 / 0.043 ms | 1.0 -> 1.3 MiB |
| 1024 x 1024 | 0.219 / 0.217 ms | 0.043 / 0.042 ms | 0.221 / 0.219 ms | 0.043 / 0.043 ms | 4.0 -> 5.3 MiB |
| 2048 x 2048 | 0.243 / 0.241 ms | 0.042 / 0.043 ms | 0.245 / 0.246 ms | 0.043 / 0.043 ms | 16.0 -> 21.3 MiB |

The GPU viewport median fell by 80-82% in this strongly minified, single-texture workload. The estimated full mip chain adds 33.3% texture storage. CPU render-submit medians overlapped between modes (0.072-0.136 ms across runs), with no consistent CPU improvement.

## Limits and reproduction

This is an isolated GPU sampling benchmark, not an OpenKeeper gameplay run. Converted DK II assets are absent from this checkout, so no game FPS, representative-level frame-time, actual GPU-memory, or visual acceptance claim is made. The test confirms base-level byte parity; mip filtering changes distant samples as intended. The first-upload mip-generation cost is not isolated from texture upload and shader warmup. Results are specific to the listed GPU and driver.

```powershell
.\gradlew.bat test
.\gradlew.bat mipSamplingBenchmark -PmipmapMode=false -PmipmapTextureSize=1024
.\gradlew.bat mipSamplingBenchmark -PmipmapMode=true -PmipmapTextureSize=1024
```
