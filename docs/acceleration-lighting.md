# Lighting acceleration structures

**Parent roadmap:** [acceleration-structures.md](acceleration-structures.md) (authoritative status)  
**Related reports:** [benchmark-page-light-cull.md](benchmark-page-light-cull.md) · [benchmark-page-frustum-index-skip.md](benchmark-page-frustum-index-skip.md) · [benchmark-vs-distance-reject.md](benchmark-vs-distance-reject.md) · [benchmark-lighting-alloc-opt.md](benchmark-lighting-alloc-opt.md) · [benchmark-ambient-map-opt.md](benchmark-ambient-map-opt.md) · [benchmark-lightrules-lifecycle.md](benchmark-lightrules-lifecycle.md) · [benchmark-entity-traversal-opt.md](benchmark-entity-traversal-opt.md) · [benchmark-a3-light-timers.md](benchmark-a3-light-timers.md) · [benchmark-a3-gpu-timers.md](benchmark-a3-gpu-timers.md) · [benchmark-a2-soft-falloff.md](benchmark-a2-soft-falloff.md) · [benchmark-a1-settings.md](benchmark-a1-settings.md) · [benchmark-room-light-index.md](benchmark-room-light-index.md) · [benchmark-shared-nonpage-light-index.md](benchmark-shared-nonpage-light-index.md)

---

## Current pipeline (verified in code)

```
LightRules registries (tile / room / thing / cursor)
        ↓  modulators snapshot (alloc-free)
SceneLights.update: scan sources → frustum + FoW filter → LightData atlas
        ↓
rebuildTerrainPageIndices: 8×8 light cells → packed LightIndexData rows
        ↓
Vertex.vert: terrain pages loop indices (~20 avg); rooms/entities/water use
inlined full numLights loop (~2.2k) — B1 + shared-index **REJECTED**
```

| Piece | Path |
|-------|------|
| Ownership / lifecycle | `src/main/java/toniarts/openkeeper/view/lighting/LightRules.java` |
| Upload + page indices | `.../view/lighting/SceneLights.java` |
| Snapshot | `.../view/lighting/LightModulators.java` |
| Soft-falloff constant | `LightConstants.SOFT_FALLOFF_TAIL_TILES` (= 0.1) |
| Page AABB registration | `.../view/map/MapViewController.bindPageBatchMaterials` |
| Shader | `assets/Shaders/DkLighting/Vertex.vert`, `DkFalloff.glsllib` |
| Entity bind-once | `src/main/java/toniarts/openkeeper/utils/AssetUtils.java` |

**Verified suite facts**

- Official median avg FPS ~**35.0**; after page-light-cull ~**39.6**.
- Page light index avg ~**20.5**, max ~**135**, full-list fallbacks **0**.
- Entity bind-once: P50 frame time **−14%**, avg FPS ~flat.
- A3: `lightUpdateMs` ≈ **0.63 ms**; A3b: `gpuFrameTimeMs` ≈ **10.8 ms**, `cpuUpdateMs` ≈ **8.4 ms**, `cpuRenderMs` ≈ **16.8 ms**.
- AmbientMap mid-frame subimage upload: **REJECTED**, reverted.

---

## A3 — Instrumentation {#a3-instrumentation}

**Status: IMPLEMENTED tooling (PARTIAL vs original “technique VS” goal)** — [benchmark-a3-light-timers.md](benchmark-a3-light-timers.md) · [benchmark-a3-gpu-timers.md](benchmark-a3-gpu-timers.md).

| | |
|--|--|
| **Shipped** | `lightUpdateMs` / `lightModulatorMs` / `lightUploadMs` / `lightPageIndexMs`; `gpuFrameTimeMs` / `cpuUpdateMs` / `cpuRenderMs`. |
| **Measured** | Lighting CPU ≈ **0.63 ms**; GPU elapsed ≈ **10.8 ms**; CPU update ≈ **8.4 ms**; CPU render ≈ **16.8 ms**. |
| **Still open** | Per-technique VS split → roadmap **F1**. |
| **Benefit** | Falsified B2; whole-frame GPU is real but wall frame is not GPU-only. |

---

## A1 — Quality settings / MSAA {#a1-quality-settings}

**Status: PARTIAL** — [benchmark-a1-settings.md](benchmark-a1-settings.md). Remainder → roadmap **A1b**.

| | |
|--|--|
| **Shipped** | Force-max launcher **gone** (historical); first-run-only frame-rate floor; `MSAA_16` hidden from UI (still loadable). Default **SMAA Ultra**. |
| **Measured** | Suite vs A2: visual 3/3, stable — **no FPS claim** (suite already SMAA / 0 samples). |
| **Still open** | Named Low/Med/High/Ultra presets; off-suite AA matrix (**A1b**, **P1**). |

---

## A2 — Soft-falloff sync {#a2-soft-falloff}

**Status: IMPLEMENTED (correctness)** — [benchmark-a2-soft-falloff.md](benchmark-a2-soft-falloff.md).

| | |
|--|--|
| **Shipped** | `LightConstants.SOFT_FALLOFF_TAIL_TILES = 0.1f`; `PAGE_LIGHT_REACH_TAIL_TILES` aliases it; GLSL `DK_SOFT_FALLOFF_TAIL_TILES 0.1`. |
| **Measured** | Visual 3/3; lightsAvg +2.3%; avg FPS stable. |

---

## B1 — Room / water light index lists {#b1-room-water-light-indices}

**Status: REJECTED** — [benchmark-room-light-index.md](benchmark-room-light-index.md). Source reverted.

| | |
|--|--|
| **Attempt** | Per-room AABB index rows (water dropped after constant overflow). |
| **Result** | Median avg FPS **36.909** vs control **37.586** (−1.8%). |
| **Retry only if** | **F1** shows room VS dominates, or a room-heavy micro-bench beats control. |

### B1-retry — Shared camera-neighbourhood index {#b1-retry-shared-camera-index}

**Status: REJECTED** — [benchmark-shared-nonpage-light-index.md](benchmark-shared-nonpage-light-index.md). Source reverted.

| | |
|--|--|
| **Result** | Median avg FPS **27.294** (−27.5%); cp01 fail 3/3; `lightPageIndexMs` ~4.7 ms. |
| **Lesson** | Binding `LIGHT_INDEX_DATA` on non-page mats abandons the fast full-list variant. |

---

## B2 — Incremental light hash for upload culling {#b2-light-upload-hash}

**Status: DEFERRED** (not an avg-FPS lever).

| | |
|--|--|
| **Evidence** | A3 upload ≈ **0.45 ms** (~1.8% of frame) — below 5–10% gate. |
| **Proposal** | Keep on file for extreme light-count / small-camera scale tests only. |
| **Do not** | Prioritize for suite avg FPS. |

---

## B3 — AmbientMap dirty regions / pages {#b3-ambientmap}

**Status: DEFERRED** for suite avg FPS (mid-frame + coalesce-flag **REJECTED**).

| | |
|--|--|
| **Bottleneck** | `SceneLights.setTileAmbient` still calls whole-image `setUpdateNeeded()` (**Verified** 2026-11-06). |
| **Evidence** | Level20 ≈ 29 KB; ~2 full uploads/s under suite — not an avg-FPS lever ([ambient-map report](benchmark-ambient-map-opt.md)). |
| **Proposal** | Reopen only with FoW-stress microbench + **post-draw** subimage (not mid-`update`). |
| **Detail** | [acceleration-structures.md § B3](acceleration-structures.md#b3-ambientmap). |

---

## E1 — Clustered / tiled forward {#e1-clustered}

**Status: NOT STARTED** — **blocked on F1**.

| | |
|--|--|
| **Evidence** | **Hypothesis** until technique VS split exists. B1 failure does not justify skipping F1. |
| **Detail** | [acceleration-structures.md § E1](acceleration-structures.md#e1-clustered). |

---

## Non-goals for lighting

- Splitting per-tile materials to private light lists (seams).
- Optimizing unused Flat/`applyFlatLight` path first (no external callers; entities use Vertex).
- Replacing `LightRules` publication with more opt-in gates.
- Another non-page `LIGHT_INDEX_DATA` bind without F1 + a shader path that keeps unbound materials on the classic loop.
