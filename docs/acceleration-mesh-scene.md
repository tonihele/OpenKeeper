# Mesh, page, and scene-update acceleration

**Parent roadmap:** [acceleration-structures.md](acceleration-structures.md) (authoritative status)  
**Related reports:** [benchmark-tile-rebuild-opt.md](benchmark-tile-rebuild-opt.md) · [rendering-pipeline-analysis.md](rendering-pipeline-analysis.md)

Terrain and rooms are built as **8×8 `BatchNode` pages** plus separate room constructor trees. Acceleration here means **incremental dirty regions and dependency tracking**, not a continuous BVH for static dungeon stone.

---

## Current behaviour (verified)

| Piece | Path | Notes |
|-------|------|--------|
| Page generation / `updateTiles` | `view/map/MapViewController.java` | Tile mesh rebuild, room expand, page `batch()` + light page rebind |
| Coalesce + terrain-id room gate | `view/PlayerMapViewState.java` | FOW/component refreshes skip room teardown — **PARTIAL** S1 |
| Ceiling | `refreshCeilingSurface` | Skips rebuild when ceiling hidden; **full** `ClearanceField` + map scan when visible/forced |
| Water/lava | batch list size gated | Rebuild when batch membership count changes |
| Rebuild stats | `TileRebuildStats` | Suite hitch attribution |

**Verified:** FOW-driven room teardown storms reduced ([benchmark-tile-rebuild-opt.md](benchmark-tile-rebuild-opt.md)).  
**Verified remaining:** topology dig/claim still expensive; ceiling paths still scan broadly when ceiling is shown.

---

## Instrumentation {#instrumentation}

**Status: PARTIAL** — `TileRebuildStats` exists; A3/A3b cover lighting/GPU frame, not `updateTiles` wall-clock splits.

Extend suite/`TileRebuildStats`:

- Tiles touched, pages rebatched, rooms rebuilt, ceiling rebuild yes/no per flush
- Wall-clock for `updateTiles` vs coalesce wait
- Separate **ceiling-on** dig scenario (default suite may hide ceiling cost)

Without this, D1–D3 are easy to over-engineer.

---

## D1 — Incremental clearance / ceiling {#d1-clearance-ceiling}

**Status: NOT STARTED**.

| | |
|--|--|
| **Bottleneck** | Visible ceiling rebuild constructs a full-map `ClearanceField` and iterates tiles (**Verified** in `refreshCeilingSurface`). |
| **Detail** | [acceleration-structures.md § D1](acceleration-structures.md#d1-clearance). |

---

## D2 — Page dirty dependency graph / residency {#d2-page-dependencies}

**Status: NOT STARTED**.

| | |
|--|--|
| **Bottleneck** | `updateTiles` fan-out; all pages stay in the scene graph. |
| **Detail** | [acceleration-structures.md § D2](acceleration-structures.md#d2-page-deps). |

---

## D3 — Immutable chunk meshes {#d3-chunk-meshes}

**Status: NOT STARTED** — defer after D1/D2.

| | |
|--|--|
| **Bottleneck** | Mutating `BatchNode` + rebatch on topology change. |
| **Detail** | [acceleration-structures.md § D3](acceleration-structures.md#d3-chunks). |

---

## Relationship to lighting pages

Terrain **light** page slots are registered when pages batch (`bindPageBatchMaterials` → `LightRules.setTerrainPage`). Any mesh residency or chunk swap (D2/D3) must:

1. Update or clear light page AABBs/materials in `SceneLights`
2. Respect `LightRules` READY
3. **Not** assume room light-index registration (B1 **REJECTED**) — rooms stay on the full-list Vertex path until F1 + a new design

Treat lighting registration as part of the mesh dependency graph, not a separate ad-hoc pass.
