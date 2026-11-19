# O3 implementation validation — 2026-11-18

Implementation: `feat/world-texture-filtering`, isolated worktree
`D:\Project\OpenKeeper-texture-filtering`, based on upstream master
`16f1df4617042db56e99ead51dcfc96e7325351f`, re-fetched and verified unchanged
during finalization. The original checkout's converted assets and investigation
documents were preserved. Publication and review coordination carry this evidence
without treating O3 as merged or agreed.

See [the policy, extension contract and contribution reconciliation](../texture-filtering.md).

## Automated coverage

| Test | Contract checked |
| --- | --- |
| `TextureFilteringTest` | Six stable enum names; Bilinear uses mips; absent/unknown preference migration; legacy and explicit AppSettings persistence |
| `GraphicsContextSettingsTest` | Pending pause-menu preferences compared with the running context; filtering-only changes require no restart; all compared renderer options require one |
| `WeakIdentityMapTest` | Independent equal keys, mutation, deterministic queued-reference reclamation and clear |
| `WorldTextureFilteringTest` | Small/large/rectangular/NPOT surfaces; exclusions; typed surface parameters; equal sampler clones; detached updates; hardware caps; safe derivatives; supplied/late/dirty/replaced mip images; defensive inconsistent sampler/image state; no-write cache hits; close |
| `WorldAssetManagerTest` | Actual jME misses/hits, shared images and independent samplers, null prototype keys, J3M options and J3O restoration order, stream loads, cache clear with old live textures, concurrent load versus refresh |
| `WorldModelCacheTest` | Existing `AssetUtils` strong and weak model caches retain current shared samplers without copying images |
| `CharredOverlayFilteringTest` | Keyless baked surface inherits filtering, changes live and is reused after the source's sampler hash changes |
| `CursorTextureDataTest` | Concurrent reads of cached cursor image data preserve the shared buffer position and pixel/byte-order interpretation |
| `WorldTextureFilteringGpuTest` (opt-in) | Actual mip allocation, base-only UI sharing, late/replacement-image adoption, all six GL sampler modes and effective anisotropy, stable GPU image IDs, changed lower-mip texels after dirty upload, exact framebuffer restoration and unchanged original base bytes |

The native test uses a hidden offscreen OpenGL 3.2 context. It deliberately
queries mip level 1 and `GL_TEXTURE_MAX_LEVEL`: Java's `isMipmapsGenerated()`
alone did not detect the late-upload defect in jME 3.9. A seeded textured quad
compares exact framebuffer bytes before and after cycling all modes back to
Bilinear. It needs no proprietary game assets.

Commands (run from the feature worktree):

```powershell
.\gradlew.bat build --offline --no-daemon --console=plain
.\gradlew.bat test --tests '*WorldTextureFilteringGpuTest' -PgpuTests=true --offline --no-daemon --console=plain
.\gradlew.bat build -PgpuTests=true --offline --no-daemon --console=plain --rerun-tasks
```

The final full build with native GPU tests enabled passed with **1,404 tests,
zero failures/errors and four skips** (the existing `MinimapAssetsTest` cases
which require converted assets inside the checkout). The default build also
passed; it additionally skips the opt-in GPU test. The GPU test passed both
independently and in the final full suite, including an assertion that switching
samplers does not mark image pixels for another upload. Existing
deprecation/native-access warnings do not prevent the build. XML results are
archived in the external evidence directory under `finalization/full-build-results/`.

The four asset-dependent minimap cases also passed with **zero skips** in a
separate focused run using an external Gradle init script to set the test working
directory to the original checkout's existing assets. No assets were copied or
reconverted. Thus every test in the 1,404-test suite executed successfully across
the full and asset-backed runs. Their earlier XML is in
`validation/real-minimap-asset-tests/`; the finalization rerun is recorded below.

## Gameplay comparison method

All variants use the same current master, Java 25, assets, runtime dependencies
and graphical settings. Historical PR heads were applied/ported semantically
onto that common base in disposable source trees, rather than comparing unrelated
old revisions:

- master baseline: `16f1df4617042db56e99ead51dcfc96e7325351f`;
- PR #885: `b77306c72626a2ec152f6856eb03c011c88266ea`, retaining its original
  listener, weak registry and sampler behavior; only startup/settings context was
  adapted to current master;
- PR #1083: `ec30836f2deeff2134e21f5b2943b8b1f29d48dc`, retaining its original
  helper and world call sites; its experimental Gradle benchmark task was unnecessary;
- candidate: the unified implementation in this worktree.

Hardware: AMD Ryzen 5 3600 (6 cores / 12 threads), NVIDIA GeForce RTX 2060 SUPER,
driver 617.42, OpenGL 3.2.0; Java 25. Game rendering uses an actual offscreen
LWJGL context at 1280×720, no MSAA, VSync disabled in AppSettings, identical
Trilinear selection and legacy anisotropy 0. The observed frame interval remains
approximately 6.94 ms (144 Hz); this is a paced workload, not a GPU-saturation
test. CPU callback times and main-world GPU times are reported separately.

The external harness bypasses the setup/conversion dialog, loads the existing
converted assets read-only, waits for the map, player, end of the opening
cinematic and stable HUD, and centers the camera on the player's dungeon heart.
It applies a fixed frame-indexed rotation/zoom script, warms 480 frames, then
measures 960 frames per run. Three runs per variant use alternating order on
Level20. Three baseline/candidate runs also exercise Level1. Gameplay simulation
continues normally; it is not a deterministic replay of every AI/physics event.
Asynchronous GPU elapsed queries cover the main world viewport and exclude GUI;
no blocking query retrieval or screenshot readback occurs in the timed window.
CPU callback duration includes the render-loop callbacks, not total CPU usage
across simulation threads. Frame interval includes pacing/scheduling.

The existing cursor cache is preinitialized identically for all comparison
variants. This avoids the pre-existing two-thread cache/image-buffer race found
during smoke validation. The production fix uses one-time synchronized
publication and an independent ByteBuffer view preserving byte order; it does
not add a per-frame lock after initialization.

Experimental harnesses, semantic PR ports, CSVs, logs, metadata and PNG captures
are kept outside production changes at:

```text
D:\Temp\openkeeper-o3-implementation-20261118\
    O3GameplayHarness.java
    O3MenuHarness.java
    run_comparison.py
    variants\baseline\, variants\pr885\, variants\pr1083\
    comparison\Level20-{baseline,pr885,pr1083,candidate}-{1,2,3}\
    comparison\Level1-{baseline,candidate}-{1,2,3}\
```

`frames.csv` contains per-frame interval/callback-CPU/world-GPU milliseconds.
`metadata.properties` identifies level, settings and GPU. First runs also capture
all six modes and return to Trilinear **after** timing, with sampler inventories.
Raw runs remain available for review; initial harness smoke attempts are separate
and excluded from comparison results.

## Results and visual checks

All **18 comparison runs completed**, with **17,280 measured frames** and no
missing GPU samples. Each cell below is P50 / P95 / P99 in milliseconds, taking
the median of the three independently calculated run percentiles (not a pooled
sample or a confidence interval).

| Level / implementation | Frame interval | Callback CPU | World GPU |
| --- | --- | --- | --- |
| Level20 master | 6.942 / 7.077 / 9.352 | 2.606 / 4.696 / 8.795 | 0.254 / 0.770 / 1.175 |
| Level20 PR #885 port | 6.943 / 7.037 / 9.330 | 2.634 / 4.164 / 9.248 | 0.244 / 0.252 / 1.104 |
| Level20 PR #1083 port | 6.943 / 7.032 / 8.737 | 2.835 / 4.780 / 8.325 | 0.237 / 0.252 / 1.114 |
| Level20 unified | 6.942 / 7.045 / 8.652 | 2.581 / 4.161 / 8.578 | 0.251 / 0.259 / 1.116 |
| Level1 master | 6.942 / 7.022 / 7.634 | 0.927 / 1.643 / 7.546 | 0.160 / 0.173 / 0.900 |
| Level1 unified | 6.942 / 7.024 / 7.612 | 0.955 / 1.607 / 7.468 | 0.160 / 0.289 / 0.910 |

No frame-pacing regression was observed against master in either workload.
There is also **no demonstrated FPS improvement**: median frame intervals are
effectively equal and GPU time is far below the pacing interval. Level20's lower
tail values are encouraging but do not prove a general tail-latency improvement.
Level1's CPU medians vary from 0.876–1.073 ms on master and 0.812–1.153 ms on the
candidate. Its slightly larger aggregate candidate CPU/GPU values are small
relative to that variability; GPU P95 is explicitly retained above. No GPU clocks
were locked, and simulation is nondeterministic. These results do not establish
zero sampling cost on a GPU-bound workload. The wider mip policy deliberately
differs from #1083's threshold policy, so its small GPU-time differences also
cannot be attributed solely to implementation overhead.

At Level20's dungeon heart, 269 ordinary DiffuseMap samplers followed all six
selected modes; 4 keyless diffuse animation-strip samplers retained base-only
Bilinear filtering. Particle strips, AmbientMap, LightData, minimap rasters,
health/status graphics and other UI samplers retained their own filters.
The inventories include nonresident/prewarmed assets; sampler count is not a
resident GPU allocation count.

Gameplay stills show the dungeon heart, terrain/walls, torches, HUD and minimap.
They are qualitative comparisons: animations, AI, particles, tooltip text and
simulation timing differ between captures. Exact cross-frame or cross-build
pixel parity is not claimed for live gameplay. Exact restoration belongs to the
controlled native regression fixture described above.

Both actual Graphics pages were exercised on the final policy/menu build:
all six choices were present; applying Bilinear in-game updated 269 world
samplers; returning to the main menu restored that choice; applying Anisotropic
16× updated 79 menu-scene world samplers and persisted the enum name. GPU image
IDs and main-viewport processor instances stayed unchanged across both Apply
operations. The widened dropdown displays the complete Bilinear label at 1280×720.
Screenshots and the assertion record are in `validation/menus-wide/`.

Two final candidate gameplay runs after the policy/menu refinements reproduced
the comparison envelope (an additional 1,920 frames):

| Final candidate | Frame interval P50 / P95 / P99 | Callback CPU P50 | World GPU P50 |
| --- | --- | --- | --- |
| Level20 | 6.944 / 7.025 / 8.350 ms | 2.727 ms | 0.258 ms |
| Level1 | 6.942 / 7.022 / 7.534 ms | 0.953 ms | 0.161 ms |

These also cycled all modes and captured the final scene inventories, under
`validation/final-Level20/` and `validation/final-Level1/`. The subsequent final
full build includes the explicit constructor injection for ModelViewer's map
loader. No production benchmark/instrumentation code or temporary assets are
included in this change.

## Final review fixes and rerun

The independent review identified no Critical or High findings. Its two Medium
findings are resolved: main-menu Apply detects context changes already saved in
the pause menu, and an existing sampler with a replacement image is prepared
again. The Low native-test finding is resolved by reading changed mip-level-one
pixels and querying effective anisotropy on the supported device. The complete
suite was rebuilt with `--rerun-tasks` after these fixes: 1,404 tests, zero
failures/errors, four asset-dependent skips. Those four minimap cases passed
again separately using the existing converted assets, with zero skips.

Actual menu validation was repeated on this final code. Filtering-only Apply in
both menus preserved image IDs and processor instances. A second run saved gamma
off during gameplay, returned to the main menu and applied the saved settings:
the saved preference, context settings and renderer all reported gamma **false**
after recreation. Previously the context and renderer stayed true. The filtering
selection survived and was saved. These runs and assertions are under
`finalization/menus-live/` and `finalization/context-restart/`.

Two further 960-frame runs, using the same method and assets, completed after the
review fixes. They cycled all modes after timing and captured the world/HUD/menu
results for inspection:

| Final reviewed candidate | Frame interval P50 / P95 / P99 | Callback CPU P50 / P95 / P99 | World GPU P50 / P95 / P99 |
| --- | --- | --- | --- |
| Level20 | 6.943 / 7.024 / 7.947 ms | 2.399 / 3.748 / 7.822 ms | 0.227 / 0.239 / 0.829 ms |
| Level1 | 6.942 / 7.008 / 7.521 ms | 0.858 / 1.373 / 7.374 ms | 0.158 / 0.160 / 0.775 ms |

Both runs have 960 valid GPU samples. These are single confirmation runs, not an
additional three-run comparison or evidence of a speedup. The earlier baseline
and PR comparisons remain the comparative evidence; all workload limitations
above still apply. Final CSVs, captures, logs and summary are in
`finalization/Level20/`, `finalization/Level1/` and `finalization/summary.json`.
No experimental harness, benchmark task, generated image or game asset enters
the production diff.

## Practical limits and maintainer follow-up

- Available native qualification is one NVIDIA device. AMD/Intel, low-memory
  devices, additional renderer/driver versions and extended play sessions remain
  useful release qualification. Hardware-cap fallbacks are also covered in unit tests.
- The same converted install reports missing optional `scling15`, `SM16`, `mist8`
  and `gsparkle16` resources in the baseline and candidates. Conversion was not
  rerun or altered. Those fallback assets limit whole-game visual claims and do
  not establish a texture-filtering regression.
- This is a short repeated gameplay comparison, not a cold-disk loading, VRAM
  residency or long-session memory benchmark. Load-to-ready includes a scripted
  cinematic and is not a texture-loading performance metric.
- No process-memory number is presented as VRAM. A square mip chain adds about
  one third to its base-level storage; thin images can approach twice the base.
  Live filtering changes share that chain. Late adoption can exceptionally retain
  separate raw and world GPU images until existing owners release them.
- The synthetic sampling benchmark in PR #1083 remains separate evidence. Its
  throughput result is not represented as a demonstrated gameplay improvement.

For upstream review, credit both authors, review the default/role decisions in
the architecture document, and carry one consolidation change. Resolve/supersede
the overlapping PRs only after maintainer/contributor agreement. The original
PRs and coordination issue remain open pending that decision.
