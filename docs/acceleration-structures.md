# Acceleration structures & performance roadmap

**Date:** 2026-11-06  
**Scope:** Practical CPU/GPU acceleration for OpenKeeper’s tile-based DK remake  
**Method:** Static code audit + existing deterministic suite results. New FPS numbers are **not** invented where suite evidence is missing.

This is the **central index**. Implementation detail lives in the companion docs; measured change reports live in the `benchmark-*.md` series. Do not duplicate those reports here.

| Document | Contents |
|----------|----------|
| **This file** | Inventory, priority roadmap, evidence rules, what to avoid |
| [acceleration-lighting.md](acceleration-lighting.md) | Light grids, page/room indices, AmbientMap, clustered lighting |
| [acceleration-spatial-simulation.md](acceleration-spatial-simulation.md) | Tile occupancy, FoW indexes, A\*, sensing, picking |
| [acceleration-mesh-scene.md](acceleration-mesh-scene.md) | Page batches, ceiling clearance, dirty deps, chunk meshes |
| [rendering-pipeline-analysis.md](rendering-pipeline-analysis.md) | Earlier end-to-end rendering review (partially superseded status) |
| [benchmark.md](benchmark.md) | Suite protocol |

### Measured change reports (do not rewrite)

| Report | Topic | Status |
|--------|--------|--------|
| [benchmark-entity-traversal-opt.md](benchmark-entity-traversal-opt.md) | Bind-once Vertex materials | Shipped |
| [benchmark-lighting-alloc-opt.md](benchmark-lighting-alloc-opt.md) | Alloc-free light hot path | Shipped |
| [benchmark-tile-rebuild-opt.md](benchmark-tile-rebuild-opt.md) | FOW/room rebuild gating | Shipped (partial) |
| [benchmark-page-light-cull.md](benchmark-page-light-cull.md) | Per-page terrain light indices | Shipped |
| [benchmark-ambient-map-opt.md](benchmark-ambient-map-opt.md) | AmbientMap mid-frame upload | **Reverted** |
| [benchmark-lightrules-lifecycle.md](benchmark-lightrules-lifecycle.md) | `LightRules` READY publication | Shipped |
| [benchmark-room-light-index.md](benchmark-room-light-index.md) | B1 room light indices | **REJECTED** (reverted) |
| [benchmark-shared-nonpage-light-index.md](benchmark-shared-nonpage-light-index.md) | B1-retry shared camera neighbourhood index | **REJECTED** (reverted) |
| [benchmark-a3-light-timers.md](benchmark-a3-light-timers.md) | A3 lighting CPU phase timers | **ACCEPTED (tooling)** |

**Latest FPS-accepted lighting candidate:** `baselines/performance/after-page-light-cull/` (median avg FPS **~39.6** vs official **~35.0**).  
**Official baseline (preserved):** `baselines/performance/official-baseline-windows-10-amd64.json`.

---

## Evidence legend

| Tag | Meaning |
|-----|---------|
| **Verified** | Observed in code and/or suite metrics / before-after docs |
| **Hypothesis** | Plausible from complexity analysis; needs the listed measurement before committing to the design |
| **Attempted** | Implemented and measured; kept or reverted as noted |

---

## Architecture fit

OpenKeeper is **tile-quantized**: simulation, FoW, pathfinding, and most lighting ownership key off the map grid. The accelerators that fit are **multi-resolution grids** (tile → 8×8 page → light cell → dirty region) with **incremental maintenance**, not general BVH/octrees for continuous space.

Preserve always:

- Shared `LightData` atlas (tile-border continuity)
- Exact `dkFalloff` behaviour
- FoW explored gating
- `LightRules` lifecycle (`LOADING` → `READY` → `DISPOSED`) — consumers must not race the loader

---

## Inventory — structures already in place

| Structure | Location | Role | Evidence |
|-----------|----------|------|----------|
| 8×8 terrain `BatchNode` pages | `view/map/MapViewController.java` (`PAGE_SQUARE_SIZE`) | Draw-call batching + light page slots | **Verified** — suite draw calls ~3k |
| Frustum light cull + `LightData` | `view/lighting/SceneLights.java` | Upload near-camera lights only | **Verified** — ~2.1–2.2k lights uploaded in dense suite |
| 8×8 light cell lists → page indices | `SceneLights.rebuildTerrainPageIndices`, `Vertex.vert` `LIGHT_INDEX_DATA` | Terrain VS evaluates ~20 lights/page | **Verified** — [benchmark-page-light-cull.md](benchmark-page-light-cull.md) |
| Entity Vertex bind-once | `utils/AssetUtils.java`, entity view containers | Removes per-frame DFS | **Verified** — P50 frame time −14%, avg FPS flat (GPU-bound) |
| Alloc-free light snapshot | `view/lighting/LightModulators.java` | Reusable `LightSource[]` | **Verified** |
| Tile rebuild coalesce + terrain-id gate | `view/PlayerMapViewState.java` | FOW skips room teardown | **Verified** — [benchmark-tile-rebuild-opt.md](benchmark-tile-rebuild-opt.md) |
| `entitiesByMapTile` / obstacles | `game/logic/PositionSystem.java` | O(1) occupancy | **Verified** in code |
| Indexed A\* tile graph | `game/navigation/pathfinding/*` | Pathfinding | **Verified** in code |
| FoW `BitSet` + `LineOfSightTable` | `game/fogofwar/*` | Compact visibility + precomputed rings | **Verified** in code |
| `LightRules.Phase` publication | `view/lighting/LightRules.java` | Load-race elimination | **Verified** — [benchmark-lightrules-lifecycle.md](benchmark-lightrules-lifecycle.md) |

**Not present (and mostly not first-choice):** BVH, octree, quadtree, continuous creature spatial hash, navmesh, clustered/deferred lighting.

---

## Ranked implementation order

Effort: **S** small · **M** medium · **L** large.  
Impact is expected real-world gain given current suite behaviour (GPU VS still dominates avg FPS after page cull).

| # | ID | Recommendation | Effort | Impact | Status | Detail |
|---|----|----------------|--------|--------|--------|--------|
| 1 | A3 | CPU lighting phase timers in suite | M | Enables ranking | **ACCEPTED tooling** — [benchmark-a3-light-timers.md](benchmark-a3-light-timers.md) | [lighting](acceleration-lighting.md#a3-instrumentation) |
| 2 | A1 | Stop forced max graphics; MSAA/quality presets | S–M | High GPU (player settings) | Open | [lighting](acceleration-lighting.md#a1-quality-settings) |
| 3 | A2 | Sync soft-falloff Java ↔ GLSL (0.1) | S | Correctness | Open | [lighting](acceleration-lighting.md#a2-soft-falloff) |
| 4 | B1 | Extend light **index lists** to rooms / water | M | High GPU **Hypothesis** | **REJECTED** (+ shared-index retry) — [benchmark-room-light-index.md](benchmark-room-light-index.md) · [benchmark-shared-nonpage-light-index.md](benchmark-shared-nonpage-light-index.md) | [lighting](acceleration-lighting.md#b1-room-water-light-indices) |
| 5 | C1 | Incremental door spatial index for FoW | S–M | FoW tick / tails | **REJECTED** — [benchmark-fow-door-index.md](benchmark-fow-door-index.md) | [spatial](acceleration-spatial-simulation.md#c1-door-index) |
| 6 | C3 | A\* connection / neighbour reuse | S | Path GC / hitch | **REJECTED** — [benchmark-astar-connection-pool.md](benchmark-astar-connection-pool.md) | [spatial](acceleration-spatial-simulation.md#c3-astar-pools) |
| 7 | B3 | AmbientMap dirty pages / coalesced upload | M | FoW upload spikes | Open (prior attempt **reverted**) | [lighting](acceleration-lighting.md#b3-ambientmap) |
| 8 | B2 | Incremental light cell hash for **upload** cull | M | CPU scale | **Deferred** — A3: upload ~0.45 ms/frame (~1.8% of frame) | [lighting](acceleration-lighting.md#b2-light-upload-hash) |
| 9 | C2 | Sensing ring cache / offset table | S–M | AI tick | Open | [spatial](acceleration-spatial-simulation.md#c2-sensing-cache) |
| 10 | C4 | Tile-DDA + occupancy pick (not mesh BVH) | M | Cursor CPU | Open | [spatial](acceleration-spatial-simulation.md#c4-picking) |
| 11 | D1 | Incremental clearance / ceiling dirty pages | M–L | Dig hitch (ceiling on) | Open | [mesh](acceleration-mesh-scene.md#d1-clearance-ceiling) |
| 12 | D2 | Page dirty dependency graph / residency | L | Large-map CPU | Open | [mesh](acceleration-mesh-scene.md#d2-page-dependencies) |
| 13 | E1 | Clustered forward (`dkFalloff`) | L | Late-game VS | Defer until A3 names the VS bottleneck | [lighting](acceleration-lighting.md#e1-clustered) |
| 14 | D3 | Immutable chunk meshes (replace BatchNode mutate) | L | Topology hitches | Defer | [mesh](acceleration-mesh-scene.md#d3-chunk-meshes) |

### Iteration log (acceleration implementation)

| Date | Candidate | Decision | Notes |
|------|-----------|----------|-------|
| 2026-11-06 | B1 room light indices | **REJECTED** | −1.8% vs same-machine control; code reverted; [report](benchmark-room-light-index.md) |
| 2026-11-06 | A3 light CPU timers | **ACCEPTED tooling** | `lightUpdateMs` ~0.63 ms (~2.5% of frame); [report](benchmark-a3-light-timers.md) |
| 2026-11-06 | C3 A\* connection reuse | **REJECTED** | Pooled edges corrupt IndexedA\* NodeRecords; array-only no FPS win; [report](benchmark-astar-connection-pool.md) |
| 2026-11-06 | C1 FoW door index | **REJECTED** | No suite FPS/P99 win; door rebuild not hot on Level20; [report](benchmark-fow-door-index.md) |
| 2026-11-06 | B1-retry shared non-page light index | **REJECTED** | −27.5% FPS vs A3; visual fail; page-index CPU ~4.7 ms; [report](benchmark-shared-nonpage-light-index.md) |

**Next for measurable suite FPS:** **GPU VS attribution** or **A1 settings** (player builds). Sim hitch opts need dedicated micro-scenarios + timers — Level20 suite alone will not move the needle. Do **not** re-bind `LIGHT_INDEX_DATA` on non-page mats without new evidence.

### Already shipped (keep; do not re-litigate without new evidence)

| ID | Item | Report |
|----|------|--------|
| Q3 | Translucent decoupled from SSAO | Pipeline analysis |
| Q4† | Shader loop uses `numLights` (+ page indices) | Page-cull report († full `DK_MAX_LIGHTS` still compile-time cap) |
| Q5 | Entity bind-once | [benchmark-entity-traversal-opt.md](benchmark-entity-traversal-opt.md) |
| Q7 | Lighting alloc reuse | [benchmark-lighting-alloc-opt.md](benchmark-lighting-alloc-opt.md) |
| S1† | FOW/room rebuild gating | [benchmark-tile-rebuild-opt.md](benchmark-tile-rebuild-opt.md) († ceiling/topology remain) |
| S2† | Terrain page light indices | [benchmark-page-light-cull.md](benchmark-page-light-cull.md) († rooms still full list) |
| S5 | `LightRules` lifecycle | [benchmark-lightrules-lifecycle.md](benchmark-lightrules-lifecycle.md) |

---

## What not to build first

| Idea | Why not (here) |
|------|----------------|
| General creature BVH / octree | Tile occupancy grid already matches movement quantization |
| Navmesh | Rules are tile + doors; indexed A\* matches gameplay |
| Full deferred / PBR | Fights remake lighting identity |
| Mid-frame AmbientMap `modifyTexture` | **Attempted** — FPS regression; see ambient-map report |
| Per-material light lists without shared atlas | Border seams (reason `LightData` exists) |
| Optimizing `computeFlatLight` first | No gameplay call sites under `src/main/java` (entities use Vertex) |

---

## Cross-cutting validation protocol

For every acceleration change:

1. Unit / concurrency tests where registries or loader races are involved.
2. `./gradlew benchmarkBaseline` with `--out baselines/performance/<candidate> --no-update-visual-refs --no-official --compare baselines/performance/official-baseline-windows-10-amd64.json`.
3. Compare also to the **latest accepted lighting candidate** (currently `after-page-light-cull`).
4. Freeze visual refs; require cp00/cp02 solid; treat cp01 flakes as known noise unless MAE/SSIM collapse on all runs.
5. Add a **targeted micro-scenario** for the structure under test (see companion docs).
6. Revert if avg FPS / P99 / visual gates fail without a clear measurement explaining why.
7. Always run a **same-machine control** at the prior accepted HEAD when machine noise may dominate (see B1 rejection).

Suite defaults and CLI: [benchmark.md](benchmark.md).

---

## Remaining bottlenecks (post–A3 measurement)

| Bottleneck | Confidence | Next probe |
|------------|------------|------------|
| GPU Vertex lighting on **non-page** materials (entities; water/lava full list) | **Verified** high — light CPU only ~2.5% of frame | GPU timers / RenderDoc; entity index only with proof |
| CPU light upload / page-index | **Verified** low (~0.6 ms total) | Do **not** prioritize B2 |
| FoW / topology hitches | Medium (tails) | C1, D1; suite P99 |
| Player MSAA / quality settings | High off-suite | A1 matrix |

FPS-accepted lighting baseline remains **`after-page-light-cull`**. A3 tooling shipped; no new FPS accel this iteration.
