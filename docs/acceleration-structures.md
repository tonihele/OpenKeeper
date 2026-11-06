# Acceleration structures & performance roadmap

**Date:** 2026-11-06 (audit refresh)  
**Scope:** Practical CPU/GPU acceleration for OpenKeeper’s tile-based DK remake  
**Method:** Static code audit + Git history + deterministic suite VERDICTs / reports. FPS numbers are taken only from measured baselines.

This file is the **authoritative index** for completed work and remaining opportunities. Companion docs hold subsystem detail; `benchmark-*.md` files hold before/after evidence. Do not invent FPS claims without suite artifacts.

| Document | Contents |
|----------|----------|
| **This file** | Status taxonomy, inventory, ranked backlog, evidence rules |
| [acceleration-lighting.md](acceleration-lighting.md) | Light grids, indices, AmbientMap, clustered lighting |
| [acceleration-spatial-simulation.md](acceleration-spatial-simulation.md) | Occupancy, FoW, A\*, sensing, picking |
| [acceleration-mesh-scene.md](acceleration-mesh-scene.md) | Pages, ceiling, dirty deps, chunk meshes |
| [rendering-pipeline-analysis.md](rendering-pipeline-analysis.md) | Original pipeline audit (**status superseded** by this index) |
| [benchmark.md](benchmark.md) | Suite protocol |

### Measured change reports

| Report | Topic | Status |
|--------|--------|--------|
| [benchmark-entity-traversal-opt.md](benchmark-entity-traversal-opt.md) | Bind-once Vertex materials | **IMPLEMENTED** |
| [benchmark-lighting-alloc-opt.md](benchmark-lighting-alloc-opt.md) | Alloc-free light hot path | **IMPLEMENTED** |
| [benchmark-tile-rebuild-opt.md](benchmark-tile-rebuild-opt.md) | FOW/room rebuild gating | **PARTIAL** (ceiling/topology remain) |
| [benchmark-page-light-cull.md](benchmark-page-light-cull.md) | Per-page terrain light indices | **IMPLEMENTED** (FPS-accepted) |
| [benchmark-ambient-map-opt.md](benchmark-ambient-map-opt.md) | AmbientMap mid-frame upload | **REJECTED** (reverted) |
| [benchmark-lightrules-lifecycle.md](benchmark-lightrules-lifecycle.md) | `LightRules` READY publication | **IMPLEMENTED** |
| [benchmark-room-light-index.md](benchmark-room-light-index.md) | B1 room light indices | **REJECTED** (reverted) |
| [benchmark-shared-nonpage-light-index.md](benchmark-shared-nonpage-light-index.md) | B1-retry shared camera index | **REJECTED** (reverted) |
| [benchmark-a3-light-timers.md](benchmark-a3-light-timers.md) | A3 lighting CPU phase timers | **IMPLEMENTED** (tooling) |
| [benchmark-a3-gpu-timers.md](benchmark-a3-gpu-timers.md) | A3b GPU + CPU update/render timers | **IMPLEMENTED** (tooling; technique VS still open) |
| [benchmark-a2-soft-falloff.md](benchmark-a2-soft-falloff.md) | A2 soft-falloff Java ↔ GLSL | **IMPLEMENTED** (correctness) |
| [benchmark-a1-settings.md](benchmark-a1-settings.md) | A1 prefs / hide MSAA 16× | **PARTIAL** (presets still open) |
| [benchmark-fow-door-index.md](benchmark-fow-door-index.md) | C1 FoW door index | **REJECTED** (reverted) |
| [benchmark-astar-connection-pool.md](benchmark-astar-connection-pool.md) | C3 A\* connection reuse | **REJECTED** (reverted) |

**Latest FPS-accepted lighting candidate:** `baselines/performance/after-page-light-cull/` (median avg FPS **~39.6** vs official **~35.0**).  
**Official baseline (preserved):** `baselines/performance/official-baseline-windows-10-amd64.json`.  
**Latest tooling HEAD (not an FPS claim):** `after-a3-gpu-timers` / `after-a1-settings` / `after-a2-soft-falloff`.

---

## Status taxonomy

| Status | Meaning |
|--------|---------|
| **IMPLEMENTED** | Intended behaviour is in tree **and** (where applicable) suite/visual evidence supports keeping it |
| **PARTIAL** | Core slice shipped; documented remainder still open |
| **NOT STARTED** | Proposed; no accepted implementation in tree |
| **REJECTED** | Implemented, measured, reverted or permanently declined; do not retry without new evidence |
| **DEFERRED** | Falsified as an avg-FPS lever (or blocked on deps); keep for scale/hitch scenarios only |
| **SUPERSEDED** | Historical proposal replaced by a later decision or architecture |

---

## Evidence legend

| Tag | Meaning |
|-----|---------|
| **Verified** | Observed in code and/or suite metrics / before-after docs |
| **Hypothesis** | Plausible from complexity analysis; needs the listed measurement before design lock-in |
| **Attempted** | Implemented and measured; kept or reverted as noted |

---

## Architecture fit

OpenKeeper is **tile-quantized**: simulation, FoW, pathfinding, and most lighting ownership key off the map grid. Prefer **multi-resolution grids** (tile → 8×8 page → light cell → dirty region) with **incremental maintenance**, not general BVH/octrees.

Preserve always:

- Shared `LightData` atlas (tile-border continuity)
- Exact `dkFalloff` behaviour
- FoW explored gating
- `LightRules` lifecycle (`LOADING` → `READY` → `DISPOSED`)
- Non-page Vertex materials on the **inlined full-list** shader variant (B1 / shared-index retries failed)

---

## Inventory — structures already in place

| Structure | Location | Role | Evidence |
|-----------|----------|------|----------|
| 8×8 terrain `BatchNode` pages | `MapViewController` (`PAGE_SQUARE_SIZE`) | Draw-call batching + light page slots | **Verified** |
| Frustum light cull + `LightData` | `SceneLights` | Upload near-camera lights | **Verified** — ~2.1–2.2k lights in dense suite |
| 8×8 light cells → page indices | `SceneLights.rebuildTerrainPageIndices`, `Vertex.vert` `LIGHT_INDEX_DATA` | Terrain VS ~20 lights/page | **Verified** — [page-cull](benchmark-page-light-cull.md) |
| Soft-falloff tail **0.1** | `LightConstants`, `DkFalloff.glsllib` | CPU reach matches GPU | **Verified** — [A2](benchmark-a2-soft-falloff.md) |
| Entity Vertex bind-once | `AssetUtils`, entity views | Removes per-frame DFS | **Verified** — P50 −14%, avg FPS flat |
| Alloc-free light snapshot | `LightModulators` | Reusable `LightSource[]` | **Verified** |
| Tile rebuild coalesce + terrain-id gate | `PlayerMapViewState` | FOW skips room teardown | **Verified** — [tile-rebuild](benchmark-tile-rebuild-opt.md) |
| Ceiling skip when hidden | `MapViewController.refreshCeilingSurface` | Defers full rebuild | **Verified** in code (still full rebuild when visible) |
| Per-tick sense cache | `PositionSystem.sensedEntitiesByEntity` | Clears each process; flood on miss | **Verified** — flood remains |
| `entitiesByMapTile` / obstacles | `PositionSystem` | O(1) occupancy | **Verified** |
| Indexed A\* tile graph | `game/navigation/pathfinding/*` | Pathfinding | **Verified** — still allocates connections |
| FoW `BitSet` + `LineOfSightTable` | `game/fogofwar/*` | Visibility + rings | **Verified** — doors still clear+rebuild |
| `LightRules.Phase` publication | `LightRules` | Load-race elimination | **Verified** — [lifecycle](benchmark-lightrules-lifecycle.md) |
| Suite light CPU timers | `LightRules` / `SceneLights` / `BenchmarkSampler` | Ranking | **Verified** — [A3](benchmark-a3-light-timers.md) |
| Suite GPU + CPU frame timers | `GpuFrameTimer`, `BenchmarkFrameProfiler` | Ranking | **Verified** — [A3b](benchmark-a3-gpu-timers.md) |
| Translucent ≠ SSAO | `PostProcessPipeline` | Always plan translucent first | **Verified** |
| SMAA Ultra default; MSAA 16× hidden | `AntiAliasingMode`, `Settings` | Player AA | **Verified** — [A1](benchmark-a1-settings.md) |

**Not present (mostly not first-choice):** BVH, octree, continuous creature spatial hash, navmesh, clustered/deferred lighting, quality presets, AmbientMap dirty pages, technique-level GPU VS queries.

---

## Master status matrix

| ID | Item | Status | Notes |
|----|------|--------|-------|
| Q3 | Translucent decoupled from SSAO | **IMPLEMENTED** | Pipeline |
| Q4 | Shader loop `numLights` (+ terrain page indices) | **IMPLEMENTED** | Cap `DK_MAX_LIGHTS` remains |
| Q5 | Entity bind-once | **IMPLEMENTED** | |
| Q7 | Lighting alloc reuse | **IMPLEMENTED** | |
| S1 | FOW/room rebuild gating | **PARTIAL** | Ceiling/topology remain → D1/D2 |
| S2 | Terrain page light indices | **IMPLEMENTED** | Rooms/entities full list by design after B1 reject |
| S5 | `LightRules` lifecycle | **IMPLEMENTED** | |
| A3 | Lighting CPU phase timers | **IMPLEMENTED** | Tooling |
| A3b | GPU frame + CPU update/render timers | **PARTIAL** | Whole-frame GPU done; technique VS open → F1 |
| A1 | Prefs / MSAA UI hygiene | **PARTIAL** | FrameRate + hide 16× done; presets → A1b |
| A2 | Soft-falloff sync 0.1 | **IMPLEMENTED** | Correctness |
| B1 / B1-retry | Non-page light indices | **REJECTED** | Reverted; do not re-bind `LIGHT_INDEX_DATA` on non-page without F1 |
| B2 | Light upload spatial hash | **DEFERRED** | Upload ~0.45 ms (~1.8% frame) |
| B3 | AmbientMap dirty pages | **DEFERRED** | Suite mid-frame + coalesce **REJECTED**; FoW-stress only |
| C1 | FoW door index | **REJECTED** | Reverted |
| C2 | Sensing offset / better cache | **PARTIAL** | Per-tick map cache exists; recursive flood remains; BFS attempt **REJECTED** on suite |
| C3 | A\* connection reuse | **REJECTED** | Unsafe pool; array-only no FPS win |
| C4 | Tile-first entity picking | **NOT STARTED** | `collideWith` still used |
| D1 | Incremental clearance / ceiling | **NOT STARTED** | Full `ClearanceField` when ceiling visible |
| D2 | Page dirty dependency graph | **NOT STARTED** | |
| D3 | Immutable chunk meshes | **NOT STARTED** | Defer after D1/D2 |
| E1 | Clustered forward | **NOT STARTED** | Defer until F1 |
| A1b | Quality presets Low–Ultra | **PARTIAL** | High = shipping/suite; UI + suite pin **ACCEPTED**; Low matrix aborted |
| F1 | Nsight/RenderDoc VS split | **NOT STARTED** | Process + optional suite hooks |
| Q1 force-max | `applyMaximumGraphicsSettings` | **SUPERSEDED** | Method gone; A1 addresses leftovers |

---

## Ranked remaining backlog

Effort: **S** small · **M** medium · **L** large.  
Order is **recommended implementation / investigation sequence** given current suite evidence (A3/A3b).

### 1 — F1 · Technique-level GPU VS attribution {#f1-vs-split}

| | |
|--|--|
| **Status** | **NOT STARTED** (A3b only measures whole-frame GPU) |
| **Proposal** | Nsight/RenderDoc capture splitting terrain `LIGHT_INDEX_DATA` VS vs full-list rooms/entities/water; optional later suite markers |
| **Subsystem / files** | `assets/Shaders/DkLighting/Vertex.vert`; materials from `MapViewController`, rooms, `AssetUtils` |
| **Expected benefit** | Names the real GPU lever before another index or cluster attempt (**enables** E1 / selective indexing) |
| **Complexity / risks** | S–M process; low code risk. Mis-attribution if captures aren’t representative |
| **Dependencies** | A3b shipped |
| **Bench / accept** | Document VS time share; no FPS gate required. **Do not** start E1/B1-retry without this |

### 2 — A1b · Quality presets + off-suite AA matrix {#a1b-presets}

| | |
|--|--|
| **Status** | **PARTIAL** — High shipping/suite pin + UI **ACCEPTED** ([benchmark-a1b-graphics-presets.md](benchmark-a1b-graphics-presets.md)); optional Off matrix not for High accept/reject |
| **Proposal** | Named Low/Med/High/Ultra; suite **always High** unless explicitly probing |
| **Subsystem / files** | `GraphicsQualityPreset`, `Settings`, MainMenu/Pause UI, `BenchmarkSuiteRunner` |
| **Expected benefit** | Player headroom on Low/Med; reproducible High suites |
| **Complexity / risks** | M; never silently downgrade accept/reject path |
| **Dependencies** | A1 prefs hygiene done |
| **Bench / accept** | High suite = visual + FPS gates; other presets = opt-in matrix only |

### 3 — P1 · Post / SMAA cost characterization {#p1-post-cost}

| | |
|--|--|
| **Status** | **PARTIAL** — Overlay/FBO elim **ACCEPTED**; SMAA High-as-default quality name **REJECTED**; thin RTs **REJECTED** ([benchmark-smaa-thin-rts.md](benchmark-smaa-thin-rts.md)); Off matrix not for High accept path |
| **Proposal** | Suite/micro matrix: SMAA Off vs Ultra vs High, SSAO on/off, using A3b timers — **measure before** changing defaults |
| **Subsystem / files** | `PostProcessPipeline`, `SmaaFilter`, benchmark config hooks |
| **Expected benefit** | Quantifies remaining post share after final-pass wiring (~10.2 ms GPU median) |
| **Complexity / risks** | S; visual refs differ if AA mode changes mid-suite |
| **Dependencies** | A3b; pairs with A1b |
| **Bench / accept** | Report Δ gpu/cpuRender; change defaults only with visual + FPS accept |

### 4 — B3 · AmbientMap dirty pages / end-of-frame coalesce {#b3-ambientmap}

| | |
|--|--|
| **Status** | **DEFERRED** for suite avg FPS — mid-frame **and** coalesce-flag attempts **REJECTED** ([benchmark-ambient-map-opt.md](benchmark-ambient-map-opt.md)); reopen only with FoW-stress microbench + end-of-draw subimage |
| **Proposal** | Dirty AABB / pages; upload **after** draw (SceneProcessor), never mid-`LightRules.update` |
| **Subsystem / files** | `SceneLights.setTileAmbient`, FoW writers |
| **Expected benefit** | FoW upload spikes / 1% lows on huge maps; Level20 steady explore is ~29 KB × ~2 uploads/s |
| **Complexity / risks** | M; FoW seams; GL unpack state |
| **Dependencies** | AmbientMap-heavy workload first |
| **Bench / accept** | Forced FoW reveal; upload bytes; abort if avg FPS drops |

### 5 — D1 · Incremental clearance / ceiling {#d1-clearance}

| | |
|--|--|
| **Status** | **NOT STARTED** |
| **Proposal** | Dirty-rect clearance field; rebuild ceiling only for pages whose openness changed |
| **Subsystem / files** | `MapViewController.refreshCeilingSurface`, `ClearanceField`, `TileRebuildStats` |
| **Expected benefit** | Dig/possession hitches when ceiling enabled (suite often hides this) |
| **Complexity / risks** | M–L; poke-through / missing ceiling if dirty tracking wrong |
| **Dependencies** | Prefer ceiling-on micro-scenario + rebuild timers first |
| **Bench / accept** | Dig 50+ tiles ceiling **on**; hitch histogram; visual edges; P99 vs tile-rebuild candidate |

### 6 — C4 · Tile-first entity picking {#c4-picking}

| | |
|--|--|
| **Status** | **NOT STARTED** |
| **Proposal** | Tile DDA / cursor tile → `entitiesByMapTile` tests; avoid full `root.collideWith` |
| **Subsystem / files** | `PlayerInteractionState` (~collideWith), `PositionSystem`, `TilePicker` |
| **Expected benefit** | Cursor CPU independent of total entity mesh count (**Hypothesis**) |
| **Complexity / risks** | M; miss non-grid meshes; behaviour change |
| **Dependencies** | Pick-time µs micro-bench before suite FPS claims |
| **Bench / accept** | 500+ entities under cursor; parity checklist; suite only if tails show |

### 7 — C2 · Sensing offset table {#c2-sensing}

| | |
|--|--|
| **Status** | **PARTIAL** (per-tick cache; recursive flood). Suite BFS **REJECTED** ([benchmark-sensing-bfs.md](benchmark-sensing-bfs.md)) |
| **Proposal** | Precomputed ring offsets (like `LineOfSightTable`) reading `entitiesByMapTile` |
| **Subsystem / files** | `PositionSystem.getSensedEntities` / `addSensedEntities` |
| **Expected benefit** | AI sense CPU under dense movers (**Hypothesis** until logic timers) |
| **Complexity / risks** | S–M; semantic drift vs flood |
| **Dependencies** | Logic-thread timers / creature-dense scenario — **not** Level20 suite avg FPS |
| **Bench / accept** | Tiles visited / sense call; no suite FPS claim without hitch proof |

### 8 — D2 · Page dirty dependency graph / residency {#d2-page-deps}

| | |
|--|--|
| **Status** | **NOT STARTED** |
| **Proposal** | Explicit tile→page/room/ceiling deps; optional near-camera CPU residency |
| **Subsystem / files** | `MapViewController.updateTiles`, light page registration |
| **Expected benefit** | Large-map local dig CPU (**Hypothesis** on Level20) |
| **Complexity / risks** | L; stale meshes; lighting page desync |
| **Dependencies** | D1 instrumentation; keep `LightRules` READY |
| **Bench / accept** | Oversized map + local dig; pages touched/frame; Level20 ≥ page-cull |

### 9 — E1 · Clustered / tiled forward {#e1-clustered}

| | |
|--|--|
| **Status** | **NOT STARTED** — **blocked on F1** |
| **Proposal** | Screen/view-tile light lists; keep `dkFalloff` + shared atlas |
| **Subsystem / files** | `Vertex.vert`, `SceneLights` |
| **Expected benefit** | Late-game VS if F1 shows full-list path dominates |
| **Complexity / risks** | L; fidelity drift; B1 lessons apply |
| **Dependencies** | **F1 required**; B1 rejected shapes are not a shortcut |
| **Bench / accept** | VS time vs page-cull; visual 3×; enhanced mode if look drifts |

### 10 — D3 · Immutable chunk meshes {#d3-chunks}

| | |
|--|--|
| **Status** | **NOT STARTED** — defer after D1/D2 |
| **Proposal** | Pooled immutable chunks + double-buffer swap |
| **Subsystem / files** | `MapViewController` batching, rooms, water, light pages |
| **Expected benefit** | Predictable dig/claim rebuild cost (**Hypothesis**) |
| **Complexity / risks** | L; long destabilization |
| **Dependencies** | D1/D2 |
| **Bench / accept** | Dig/claim stress + full visual vs tile-rebuild / page-cull |

### Explicitly do not pursue for suite avg FPS

| ID | Status | Reason |
|----|--------|--------|
| B2 | **DEFERRED** | Upload ~0.45 ms |
| B1 / shared index | **REJECTED** | No win / large loss |
| C1 | **REJECTED** | Not hot on Level20 |
| C3 connection pool | **REJECTED** | Unsafe or no win |

---

## Iteration log

| Date | Candidate | Decision | Notes |
|------|-----------|----------|-------|
| 2026-11-06 | Page light cull | **IMPLEMENTED** | ~+13% vs official; [report](benchmark-page-light-cull.md) |
| 2026-11-06 | AmbientMap mid-frame upload | **REJECTED** | FPS regression; reverted |
| 2026-11-06 | LightRules lifecycle | **IMPLEMENTED** | Correctness/stability |
| 2026-11-06 | B1 room light indices | **REJECTED** | −1.8% vs control; reverted |
| 2026-11-06 | A3 light CPU timers | **IMPLEMENTED** | ~0.63 ms lighting CPU |
| 2026-11-06 | C3 A\* connection reuse | **REJECTED** | Unsafe / no win |
| 2026-11-06 | C1 FoW door index | **REJECTED** | Flat suite |
| 2026-11-06 | B1-retry shared non-page index | **REJECTED** | −27.5% FPS; visual fail |
| 2026-11-06 | A2 soft-falloff sync | **IMPLEMENTED** | Correctness |
| 2026-11-06 | A1 settings prefs / MSAA UI | **PARTIAL** | Presets remain |
| 2026-11-06 | A3b GPU frame timers | **PARTIAL** | gpu≈10.8 ms; technique VS → F1 |
| 2026-11-06 | P1/A1b SMAA High default | **REJECTED** | −4% FPS vs Ultra path; reverted; [report](benchmark-smaa-high-default.md) |
| 2026-11-06 | C2 sensing BFS | **REJECTED** | No suite FPS win / unstable; reverted; [report](benchmark-sensing-bfs.md) |
| 2026-11-06 | P1 SMAA final-pass (drop Overlay) | **ACCEPTED** | gpuFrameTimeMs −0.53 ms; visual 3/3; [report](benchmark-smaa-final-pass.md) |
| 2026-11-06 | Q5b entity bind steady-state skip | **REJECTED** | No CPU/FPS win; visual flake; reverted; [report](benchmark-entity-bind-skip.md) |
| 2026-11-06 | LightIndex dirty-skip upload | **REJECTED** | −4.7% FPS / visual fail; reverted; [report](benchmark-light-index-dirty.md) |
| 2026-11-06 | Frustum margin 5→3 | **REJECTED** | Lights −7%; FPS/visual fail; reverted; [report](benchmark-frustum-margin-3.md) |
| 2026-11-06 | A1b graphics presets (High pin) | **PARTIAL** | Suite defaults High; [report](benchmark-a1b-graphics-presets.md) |
| 2026-11-06 | P1 SMAA thin RTs (RGB8/Depth16) | **REJECTED** | −5% FPS on High; visual flake; reverted; [report](benchmark-smaa-thin-rts.md) |

---

## Verified completed optimizations (summary)

| Item | Benefit class | Suite / visual |
|------|---------------|----------------|
| Terrain page light indices | **FPS** | Median avg FPS ~39.6 vs official ~35.0; visual gates in report |
| Tile FOW/room rebuild gating | **Hitch / P99** | Partial; ceiling still expensive when shown |
| Entity Vertex bind-once | **CPU P50** | −14% P50; avg FPS flat (GPU-bound) |
| Lighting alloc reuse | **GC / alloc** | Report |
| Shader `numLights` bound | **GPU correctness/perf** | With page cull |
| Translucent ≠ SSAO | **Correctness** | |
| LightRules READY | **Correctness** | |
| Soft-falloff 0.1 | **Correctness** | Visual 3/3; lightsAvg +2.3% |
| A1 prefs / hide MSAA 16× | **UX** | Suite stable; no FPS claim |
| A3 / A3b timers | **Tooling** | Ranking inputs |
| SMAA final-pass (no Overlay FBO) | **GPU** | gpuFrameTimeMs −0.53 ms; visual 3/3 |

---

## What not to build first

| Idea | Why not |
|------|---------|
| General creature BVH / octree | Tile occupancy already matches movement |
| Navmesh | Tile + doors; indexed A\* matches rules |
| Full deferred / PBR | Fights remake lighting identity |
| Mid-frame AmbientMap `modifyTexture` | **REJECTED** |
| LightIndex dirty-skip upload | **REJECTED** (suite) |
| Per-material light lists without shared atlas | Border seams |
| Re-binding `LIGHT_INDEX_DATA` on rooms/entities | **REJECTED** (−27% / no win) without F1 + separate full-list shader path |
| Entity bind steady-state skip (Q5b) | **REJECTED** (suite) |
| SMAA High as default | **REJECTED** |
| Sensing BFS visited set (suite FPS) | **REJECTED** |
| Optimizing `computeFlatLight` / `applyFlatLight` first | **No callers** outside `AssetUtils` definitions; entities use Vertex |
| B2 upload hash for avg FPS | Upload ≪ 5% of frame |

---

## Cross-cutting validation protocol

1. Unit / concurrency tests where registries or loader races are involved.
2. `./gradlew benchmarkBaseline` with `--out baselines/performance/<candidate> --no-update-visual-refs --no-official --compare` against official **and** latest relevant candidate.
3. Freeze visual refs; require cp00/cp02 solid; treat isolated cp01 flakes as known noise unless MAE/SSIM collapse on all runs.
4. Add a **targeted micro-scenario** for hitch/AI/ceiling work — Level20 suite alone will not move the needle.
5. Revert if avg FPS / P99 / visual gates fail without a clear measurement explaining why.
6. Same-machine control when machine noise may dominate (see B1 rejection).

Suite defaults: [benchmark.md](benchmark.md).

---

## Remaining bottlenecks (post–A3 / A3b)

| Bottleneck | Confidence | Next probe |
|------------|------------|------------|
| World + post GPU (~10.2 ms after SMAA final-pass) vs CPU render submit (~16.8 ms) | **Verified** magnitudes | **F1** VS split; remaining P1 Off/SSAO matrix |
| Non-page full-list Vertex path | **Hypothesis** share of GPU | **F1** before any index retry |
| Lighting CPU | **Verified** low (~0.6 ms) | Do **not** prioritize B2 / index dirty-skip |
| Ceiling rebuild when visible | **Verified** full-map path | **D1** + ceiling-on scenario |
| Player quality / AA | High off-suite | **A1b** |
| FoW AmbientMap full upload | **Verified** API shape; not suite avg-FPS | **B3** deferred (FoW-stress only) |

**Next actionable step:** High-preset suite wins only (SMAA Ultra). **F1** (Nsight VS split) before E1/B1-retry. Prefer post/render bandwidth and hitch (**D1**) over quality downgrades. **B3** deferred.
