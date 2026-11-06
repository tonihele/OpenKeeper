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

| | |
|--|--|
| **Bottleneck** | Player/default AA and quality toggles can dominate GPU before custom lighting is tuned. Historical analysis flagged force-max / 16× MSAA paths; suite often records `samples: 0`, so **suite alone understates** this cost. |
| **Evidence** | **Verified** in `Settings` AA plumbing (`game/data/Settings.java`); **Hypothesis** for magnitude on shipping defaults — needs MSAA matrix. |
| **Proposal** | Quality presets (Low/Med/High/Ultra); do not overwrite user prefs every launch; prefer SMAA / modest MSAA for classic look. |
| **Benefit** | Large GPU headroom on real configs (**Hypothesis** until measured). |
| **Complexity / memory** | S–M / none. |
| **Risks** | Perceived quality drop if defaults change without UI clarity. |
| **Bench** | Explicit samples/SMAA matrix outside default suite; compare frame time + visual SSIM. **Not** a substitute for lighting structure work. |

---

## A2 — Soft-falloff sync {#a2-soft-falloff}

| | |
|--|--|
| **Bottleneck** | `LightConstants.SOFT_FALLOFF_TAIL_TILES = 0.01f` vs GLSL `DK_SOFT_FALLOFF_TAIL_TILES 0.1`. Page-index AABB reach already uses **0.1** in Java (`SceneLights.PAGE_LIGHT_REACH_TAIL_TILES`). CPU frustum / flat paths disagree with GPU falloff. |
| **Evidence** | **Verified** constant mismatch in source. |
| **Proposal** | Single shared value **0.1** (match shader / page cull). |
| **Benefit** | Correctness; may slightly increase uploaded lights. |
| **Complexity / memory** | S / none. |
| **Risks** | Low — slightly larger light pools. |
| **Bench** | Visual parity; `lightsAvg` median; no FPS claim without measure. |

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
