# Lighting acceleration structures

**Parent roadmap:** [acceleration-structures.md](acceleration-structures.md)  
**Related reports:** [benchmark-page-light-cull.md](benchmark-page-light-cull.md) · [benchmark-lighting-alloc-opt.md](benchmark-lighting-alloc-opt.md) · [benchmark-ambient-map-opt.md](benchmark-ambient-map-opt.md) · [benchmark-lightrules-lifecycle.md](benchmark-lightrules-lifecycle.md) · [benchmark-entity-traversal-opt.md](benchmark-entity-traversal-opt.md)

---

## Current pipeline (verified)

```
LightRules registries (tile / room / thing / cursor)
        ↓  modulators snapshot (alloc-free)
SceneLights.update: scan sources → frustum + FoW filter → LightData atlas
        ↓
rebuildTerrainPageIndices: 8×8 light cells → packed LightIndexData rows
        ↓
Vertex.vert: terrain pages loop indices (~20 avg); rooms/entities loop numLights (~2.2k)
(B1 + shared-index retry both **REJECTED** — keep non-page on the full-list variant)
```

| Piece | Path |
|-------|------|
| Ownership / lifecycle | `src/main/java/toniarts/openkeeper/view/lighting/LightRules.java` |
| Upload + page indices | `.../view/lighting/SceneLights.java` |
| Snapshot | `.../view/lighting/LightModulators.java` |
| Page AABB registration | `.../view/map/MapViewController.bindPageBatchMaterials` |
| Shader | `assets/Shaders/DkLighting/Vertex.vert`, `DkFalloff.glsllib` |
| Entity bind-once | `src/main/java/toniarts/openkeeper/utils/AssetUtils.java` |

**Verified suite facts**

- Official median avg FPS ~**35.0**; after page-light-cull ~**39.6** ([benchmark-page-light-cull.md](benchmark-page-light-cull.md)).
- Page light index avg ~**20.5**, max ~**135**, full-list fallbacks **0**.
- Entity bind-once: P50 frame time **−14%**, avg FPS ~flat → **GPU / VS still ceilings avg FPS** ([benchmark-entity-traversal-opt.md](benchmark-entity-traversal-opt.md)).
- AmbientMap mid-frame subimage upload: **regressed**, reverted ([benchmark-ambient-map-opt.md](benchmark-ambient-map-opt.md)).

---

## A3 — Instrumentation {#a3-instrumentation}

**Status: ACCEPTED tooling (2026-11-06)** — [benchmark-a3-light-timers.md](benchmark-a3-light-timers.md). CPU phase timers shipped; GPU VS timers still open.

| | |
|--|--|
| **Bottleneck** | Could not attribute frame time to lighting CPU phases vs the rest of the frame. |
| **Shipped** | Suite metrics `lightUpdateMs` / `lightModulatorMs` / `lightUploadMs` / `lightPageIndexMs`. |
| **Measured** | `lightUpdateMs` ≈ **0.63 ms** (~2.5% of ~25 ms frame) on `late-campaign-dense`. |
| **Still open** | GL timer queries for terrain-index VS vs room/entity full-list VS. |
| **Benefit** | Falsified B2 as next FPS lever; explains B1 rejection (GPU-bound, not room-index CPU). |

---

## A1 — Quality settings / MSAA {#a1-quality-settings}

**Status: ACCEPTED UX (2026-11-06)** — [benchmark-a1-settings.md](benchmark-a1-settings.md). Force-max launcher already gone; prefs/UI slice shipped. Quality presets still open.

| | |
|--|--|
| **Bottleneck** | Player AA / prefs stomps can dominate GPU; suite (`samples: 0` + SMAA Ultra) understates high-MSAA cost. |
| **Shipped** | First-run-only frame-rate floor; `MSAA_16` hidden from settings UI (still loadable). Default remains SMAA Ultra. |
| **Measured** | Suite vs A2: visual 3/3, `overall: pass` — no FPS claim (suite already on SMAA / 0 samples). |
| **Still open** | Named Low/Med/High/Ultra presets; off-suite MSAA matrix. |

---

## A2 — Soft-falloff sync {#a2-soft-falloff}

**Status: ACCEPTED correctness (2026-11-06)** — [benchmark-a2-soft-falloff.md](benchmark-a2-soft-falloff.md).

| | |
|--|--|
| **Bottleneck** | `LightConstants.SOFT_FALLOFF_TAIL_TILES` was **0.01** vs GLSL / page reach **0.1**. |
| **Shipped** | Java constant **0.1**; `PAGE_LIGHT_REACH_TAIL_TILES` aliases it. |
| **Measured** | Visual 3/3; lightsAvg +2.3%; avg FPS stable vs A3 (37.808 vs 37.657). |
| **Note** | Correctness only — not an FPS claim. |

---

## B1 — Room / water light index lists {#b1-room-water-light-indices}

**Status: REJECTED (2026-11-06)** — see [benchmark-room-light-index.md](benchmark-room-light-index.md). Source reverted.

| | |
|--|--|
| **Bottleneck** | Terrain pages evaluate ~20 lights/vertex; **rooms, water, and entities keep the full uploaded list** (`Vertex.vert` without `LIGHT_INDEX_DATA`). |
| **Evidence** | **Verified** dual shader paths + page-cull report. Room-index FPS win was **Hypothesis** — **falsified** on `late-campaign-dense`. |
| **Attempt** | Reused light-cell grid + `setIndexedMesh` for room AABBs (water/lava dropped after constant full-list overflow). Visual 3/3 pass on rooms-only. |
| **Result** | Median avg FPS **36.909** vs same-machine page-cull control **37.586** (−1.8%). No accept. |
| **Lesson** | Fewer lights/vertex on rooms ≠ frame win when those meshes are not the suite bottleneck; always pair with A3 / same-machine control. |
| **Retry only if** | Instrumentation shows room VS dominates, or a room-heavy micro-bench beats control. |

### B1-retry — Shared camera-neighbourhood index {#b1-retry-shared-camera-index}

**Status: REJECTED (2026-11-06)** — [benchmark-shared-nonpage-light-index.md](benchmark-shared-nonpage-light-index.md). Source reverted.

| | |
|--|--|
| **Attempt** | One shared `LightIndexData` row for all non-page Vertex mats from camera cell ±2. |
| **Result** | Median avg FPS **27.294** vs A3 **37.657** (−27.5%); cp01 visual fail 3/3; `lightPageIndexMs` ~4.7 ms. |
| **Lesson** | Binding `LIGHT_INDEX_DATA` on rooms/entities abandons the fast inlined full-list variant and still misses lights / overflows often. Do not retry without VS attribution + a shader path that keeps unbound materials on the classic loop. |

---

## B2 — Incremental light hash for upload culling {#b2-light-upload-hash}

| | |
|--|--|
| **Bottleneck** | `SceneLights.update` walks **all** registered sources every frame, then frustum-filters (`SceneLights.java` upload loop). Cost grows with map light count even when the camera sees a subset. |
| **Evidence** | **Verified** O(n_sources) scan; A3 measured upload ≈ **0.45 ms** (~1.8% of frame) — **below** the 5–10% gate. **Do not pursue for avg FPS.** |
| **Proposed structure** | Dense tile or 8×8 **cell hash**: insert each light into cells covered by its reach disk. Each frame, iterate cells overlapping the camera AABB (+ margin), then frustum-test. **Incremental** add/move/remove updates only touched cells (thing lights move every frame — budget carefully). |
| **Simpler alternative** | If A3 shows scan ≪ VS time, **skip** this item. |
| **Expected benefit** | CPU scales with visible region × density, not total dungeon lights. |
| **Complexity / memory** | M; heads/next arrays similar to page grid; render-thread only after `LightRules.isReady()`. |
| **Risks** | Stale cells when things move; large-radius over-insert; missing lights if cells wrong (**correctness**). |
| **Bench** | Synthetic “many lights, small camera”: CPU time for upload phase ↓ while `lightsAvg` and visuals match control. Suite regression vs page-cull candidate. |

---

## B3 — AmbientMap dirty regions / pages {#b3-ambientmap}

| | |
|--|--|
| **Bottleneck** | `setTileAmbient` marks the **whole** AmbientMap `setUpdateNeeded()`; FoW floods re-upload full RGBA8. |
| **Evidence** | **Verified** in `SceneLights.setTileAmbient`. Mid-frame `modifyTexture` coalescing: **Attempted**, FPS **regressed**, source reverted ([benchmark-ambient-map-opt.md](benchmark-ambient-map-opt.md)). Problem remains; prior *approach* failed. |
| **Proposed structure** | Prefer **frame-coalesced** single upload of a dirty AABB, or **paged AmbientMap** (e.g. 32×32 tile pages) uploading only dirty pages at end of frame — not mid-`update` GL subimage. |
| **Expected benefit** | Fewer FoW-related CPU→GPU spikes (1%/P99); steady explore avg FPS may be flat. |
| **Complexity / memory** | M; small bookkeeping + possible page textures. |
| **Risks** | Partial update bugs → FoW lighting seams; jME `Image` update semantics. |
| **Bench** | Forced FoW reveal scenario (upload bytes/frame); suite 1% lows; visual FoW edges. Compare against page-cull candidate; **abort** if avg FPS drops like the prior attempt. |

---

## E1 — Clustered / tiled forward {#e1-clustered}

| | |
|--|--|
| **Bottleneck** | After B1, VS may still dominate under extreme on-screen light density. |
| **Evidence** | **Hypothesis** only — do not start without A3 + failed/saturated B1. |
| **Proposed structure** | Screen or view-tile clusters assigning light index lists; evaluate `dkFalloff` unchanged; keep shared atlas or cluster UBOs carefully. |
| **Expected benefit** | Scales with on-screen complexity better than one global loop. |
| **Complexity / memory** | L; non-trivial shader + CPU cluster build. |
| **Risks** | Fidelity drift, seam bugs, high maintenance vs remake goals. |
| **Bench** | Nsight/RenderDoc VS time vs B1 baseline; mandatory visual 3×; treat as enhanced path if classic look drifts. |

---

## Non-goals for lighting

- Splitting per-tile materials to private light lists (seams).
- Optimizing unused Flat/`computeFlatLight` gameplay path first.
- Replacing `LightRules` publication with more opt-in gates (lifecycle already owns readiness).
