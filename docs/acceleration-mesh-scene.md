# Mesh, page, and scene-update acceleration

**Parent roadmap:** [acceleration-structures.md](acceleration-structures.md)  
**Related reports:** [benchmark-tile-rebuild-opt.md](benchmark-tile-rebuild-opt.md) · [rendering-pipeline-analysis.md](rendering-pipeline-analysis.md) (S1 / L1)

Terrain and rooms are built as **8×8 `BatchNode` pages** plus separate room constructor trees. Acceleration here means **incremental dirty regions and dependency tracking**, not a continuous BVH for static dungeon stone.

---

## Current behaviour (verified)

| Piece | Path | Notes |
|-------|------|--------|
| Page generation / `updateTiles` | `view/map/MapViewController.java` | Tile mesh rebuild, room expand, page `batch()` |
| Coalesce + terrain-id room gate | `view/PlayerMapViewState.java` | FOW/component refreshes skip room teardown |
| Ceiling | `refreshCeilingSurface` / clearance helpers | Full-map solid scan paths when ceiling visible |
| Water/lava | batch list size gated | Rebuild when batch membership count changes |
| Rebuild stats | `TileRebuildStats` (benchmark diagnostics) | Suite hitch attribution |

**Verified:** FOW-driven room teardown storms reduced ([benchmark-tile-rebuild-opt.md](benchmark-tile-rebuild-opt.md)).  
**Verified remaining:** topology dig/claim still expensive; ceiling paths can still scan broadly when enabled (pipeline analysis S1 / addendum).

---

## Instrumentation {#instrumentation}

Extend suite/`TileRebuildStats` (coordinate with [acceleration-lighting.md A3](acceleration-lighting.md#a3-instrumentation)):

- Tiles touched, pages rebatched, rooms rebuilt, ceiling rebuild yes/no per flush
- Wall-clock for `updateTiles` vs coalesce wait
- Separate **ceiling-on** dig scenario (default suite may hide ceiling cost)

Without this, D1–D3 are easy to over-engineer.

---

## D1 — Incremental clearance / ceiling {#d1-clearance-ceiling}

| | |
|--|--|
| **Bottleneck** | Visible ceiling rebuilds can rescan map solidity and sample clearance windows (e.g. `ClearanceField` search radius) over large regions during mesh build. |
| **Evidence** | **Verified** full-grid / windowed clearance patterns in map/ceiling code paths; hitch severity is **Hypothesis** for default suite (ceiling often off in menu; in-game depends on settings). |
| **Proposed structure** | **Dirty-rect distance field** (brush-fire / jumping flood) updated only around dig/claim/destroy tiles; ceiling geometry rebuild limited to **pages** whose openness/clearance changed. |
| **Expected benefit** | Removes construction hitches when ceiling is enabled; FoW-only updates stay cheap (already gated). |
| **Complexity / memory** | M–L; clearance buffer W×H + dirty queue. |
| **Risks** | Stale clearance → poke-through or missing ceiling; FoW vs geometry inconsistency. |
| **Bench** | Dig 50+ tiles with ceiling **on**; hitch histogram + `TileRebuildStats`; visual ceiling edges; compare P99 to tile-rebuild candidate. |

---

## D2 — Page dirty dependency graph / residency {#d2-page-dependencies}

| | |
|--|--|
| **Bottleneck** | Tile updates still fan out to neighbours / rooms / pages; large maps keep all pages in the scene graph even when the camera is local. jME frustum culls draws, not CPU rebuild work. |
| **Evidence** | **Verified** update fan-out in `MapViewController.updateTiles`; residency win is **Hypothesis** for Level20-sized maps (stronger on huge skirmish maps). |
| **Proposed structure** | Explicit **deps**: tile → pages, tile → room instance, tile → ceiling page. Flush only dirty sinks. Optional **2-level page grid** for “is this page near camera?” to skip CPU refresh (not a deep octree). |
| **Expected benefit** | Late-game CPU when acting in one wing of a large map; clearer rebuild accounting. |
| **Complexity / memory** | L; dependency tables must stay correct on room topology changes. |
| **Risks** | Missed edges → stale meshes / lighting binds; pop-in if residency too aggressive. |
| **Bench** | Oversized map + local dig: pages touched/frame metric; visual; suite on Level20 should stay ≥ tile-rebuild/page-cull candidates. |

---

## D3 — Immutable chunk meshes {#d3-chunk-meshes}

| | |
|--|--|
| **Bottleneck** | Mutating `BatchNode` children + rebatch on topology change remains the structural cost behind dig/claim hitches (pipeline **L1**). |
| **Evidence** | **Verified** architecture; replacing it is **Hypothesis** for net FPS until prototyped. Prior gating (S1 partial) was the correct first step. |
| **Proposed structure** | Immutable chunk meshes with **pooled rebuild** and double-buffer swap; tile→chunk dependency graph (builds on D2). |
| **Expected benefit** | Predictable rebuild cost; better frame pacing under construction. |
| **Complexity / memory** | L; high touch surface (rooms, water, torches, light page slots). |
| **Risks** | Seams, batching regressions, long destabilization; lighting page registration must move with chunks. |
| **Bench** | Only after D1/D2; dig/claim stress + full visual suite vs `after-tile-rebuild-opt` / `after-page-light-cull`. |

---

## Relationship to lighting pages

Terrain **light** page slots (`TerrainPageLightSlot`) are registered when pages batch. Any mesh residency or chunk swap (D2/D3) must:

1. Update or clear light page AABBs/materials in `SceneLights`
2. Respect `LightRules` READY (no loader-thread mesh publish races)
3. Keep room index work ([B1](acceleration-lighting.md#b1-room-water-light-indices)) in sync with room mesh lifetime

Treat lighting registration as part of the mesh dependency graph, not a separate ad-hoc pass.
