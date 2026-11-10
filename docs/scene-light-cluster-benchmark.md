# Scene light page lists

## Change

`SceneLights` now builds conservative light-index lists for 4x4-tile pages. The vertex shader chooses the page for each vertex and evaluates only its candidate lights. Candidates use the same global `LightData` order and include the shader's 0.1-tile falloff tail, so shared page edges remain covered. Page records and indices are packed into a 512-wide `R32F` texture. Unchanged lists do not request a texture upload. Vertices outside the map and oversized/over-capacity lists fall back to the full light list.

For an 85x85 map, the packed list texture is 512x99 `R32F` (202,752 bytes), compared with about 2 MB for the rejected 1,024-entry fixed row per page.

## Benchmark method

Run with `.\gradlew.bat --offline --console=plain sceneLightsClusterBenchmark`. The harness renders the production `DkLighting` vertex shader offscreen at 1280x720 over a 281x161 vertex grid. It compares the original full scan and page lists at 64, 256, 1,024, 2,048, and 3,084 lights. Static runs and a stress case that shifts every light across page boundaries alternate order in two passes. Each mode has 120 warmup and 120 measured frames. GL elapsed-time queries measure GPU work; `SceneLights.update()` and render-thread CPU time are reported separately. RGBA framebuffer captures compare both movement phases byte for byte.

Hardware: NVIDIA GeForce RTX 2060 SUPER, OpenGL 3.2, driver 617.42, jMonkeyEngine 3.9.0. The benchmark uses synthetic 4.5-tile lights and is not a whole-game FPS measurement.

## Results

GPU medians in milliseconds; each cell shows pass 1 / pass 2. Reduction uses the mean of the paired medians.

| Lights | Full scan | Page lists | GPU reduction |
|---:|---:|---:|---:|
| 64 | 0.180 / 0.179 | 0.070 / 0.086 | 57% |
| 256 | 0.651 / 0.651 | 0.262 / 0.260 | 60% |
| 1,024 | 2.075 / 1.981 | 0.960 / 0.727 | 58% |
| 2,048 | 3.806 / 4.387 | 1.434 / 1.436 | 65% |
| 3,084 | 6.282 / 6.375 | 2.155 / 2.152 | 66% |

In the two-pass moving-light stress case, 1,024 lights measured 1.901 / 1.980 ms for the full scan and 0.940 / 0.735 ms for page lists (57% lower). At 3,084 lights, results were 6.271 / 6.339 ms and 2.134 / 2.131 ms (66% lower). Every captured static frame and both movement phases had zero differing RGBA channels.

Static page-list CPU update medians were 0.013, 0.030, 0.108, 0.229, and 0.319 ms at 64, 256, 1,024, 2,048, and 3,084 lights; full-list updates measured 0.004, 0.005, 0.015, 0.042, and 0.054 ms. Moving 3,084 lights measured 0.30 ms versus 0.03 ms. Render-thread CPU medians stayed below 0.09 ms in the moving case. GPU p95s were noisier than medians, so the tables report paired medians.

## Experiments and limits

- Rejected fixed 512-entry rows: crowded pages overflowed to the full scan at 3,084 lights, eliminating the GPU gain. Re-uploading the padded 1 MB texture each frame also raised render-thread CPU time to about 6.8 ms.
- Replaced fixed rows with compact offsets and indices, a bounded capacity based on the standard 4.5-tile radius, and change detection. This retained exact output, improved dense-scene GPU time, and reduced the 85x85-map list texture to about 0.2 MB.
- Custom lights that require more than 16 pages, or exceed total packed capacity, conservatively disable clustering for that update. Such oversized-light scenes were not included in the timing runs.
- The isolated grid does not measure whole-game FPS, camera culling, terrain batching, or the visual distribution of real level lights. Verify those on representative gameplay scenes before treating these figures as game-wide gains.
