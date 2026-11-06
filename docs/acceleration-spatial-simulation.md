# Spatial & simulation acceleration

**Parent roadmap:** [acceleration-structures.md](acceleration-structures.md)  
**Related reports:** [benchmark-entity-traversal-opt.md](benchmark-entity-traversal-opt.md) (mentions FoW door rebuild as remaining)

Simulation and interaction in OpenKeeper are **tile-native**. Prefer extending `PositionSystem` grids and FoW tables over continuous-space trees.

---

## Current structures (verified)

| Structure | Path | Role |
|-----------|------|------|
| `entitiesByMapTile[][]` / `obstaclesByMapTile` | `game/logic/PositionSystem.java` | O(1) who’s on a tile |
| Indexed A\* + `MapIndexedGraph` | `game/navigation/pathfinding/` | Tile graph search |
| FoW `BitSet` explored/perceived | `game/fogofwar/FogState.java` | Compact visibility |
| `LineOfSightTable` | `game/fogofwar/LineOfSightTable.java` | Precomputed vision rings (r≤7) |
| `TilePicker` DDA | tile picking utilities / selection | Analytical tile ray |
| Entity `collideWith(Ray)` | `view/PlayerInteractionState.java` | Interactive object hover |

No app-authored BVH/octree for creatures or shots.

---

## C1 — Incremental door index {#c1-door-index}

**Status: REJECTED (2026-11-06)** — [benchmark-fow-door-index.md](benchmark-fow-door-index.md).

| | |
|--|--|
| **Bottleneck** | FoW vision tick clear+rebuilds `doorsByPosition` each 0.15 s. |
| **Attempt** | Incremental EntitySet add/remove/change index (`EntityId → Point`). |
| **Result** | Visual OK on clean runs; median avg FPS **37.1** vs A3 **37.7** (no win); high CV. Reverted. |
| **Lesson** | Not hot on `late-campaign-dense`; retry only with FoW-tick timers + door-dense scenario. |

---

## C2 — Sensing cache / offset expansion {#c2-sensing-cache}

| | |
|--|--|
| **Bottleneck** | `PositionSystem.getSensedEntities` uses recursive flood; TODO references proper hearing/vision (#261). Work is O(range²) with revisits controlled by a set. |
| **Evidence** | **Verified** algorithm shape in `PositionSystem`; hotness under AI load is **Hypothesis** until logic-thread timers exist. |
| **Proposed structure** | (1) Per-tick cache `EntityId → sensed set`, or (2) expand via **precomputed offsets** (same idea as `LineOfSightTable`) reading `entitiesByMapTile`. Do **not** introduce a creature octree. |
| **Expected benefit** | AI/sense queries scale cleanly with movers × ring size. |
| **Complexity / memory** | S–M; cache maps or static offset tables. |
| **Risks** | Stale sets if cache key ignores tick/move; behaviour change if flood semantics drift. |
| **Bench** | Count tiles visited / sense call; creature-dense logic profiling; suite only if AI shows in frame hitch profiles. |

---

## C3 — A\* neighbour / connection reuse {#c3-astar-pools}

**Status: REJECTED (2026-11-06)** — [benchmark-astar-connection-pool.md](benchmark-astar-connection-pool.md).

| | |
|--|--|
| **Bottleneck** | `MapIndexedGraph.getConnections` allocates a new `Array` and `DefaultConnection` per expansion. |
| **Attempt** | (1) Pooled mutable `Connection`s — **unsafe**: gdx-ai `NodeRecord` retains `Connection` for path rebuild → corrupted paths, false ~88 FPS, visual/draw/heap collapse. (2) Scratch `Array` only + fresh edges — correct but **no suite FPS win**. |
| **Lesson** | Never pool/mutate `Connection` instances used with stock `IndexedAStarPathFinder`. |
| **Result** | Fully reverted. |

---

## C4 — Tile-first entity picking {#c4-picking}

| | |
|--|--|
| **Bottleneck** | Hover/selection for interactive objects uses `root.collideWith(Ray, CollisionResults)` on the entity scene graph (`PlayerInteractionState`), scaling with attached geometry. |
| **Evidence** | **Verified** call site; cost under hundreds of entities is **Hypothesis**. |
| **Proposed structure** | Reuse **tile DDA** (`TilePicker`) / cursor tile, then test only entities in `entitiesByMapTile` along the ray (or under the pointed tile). Optional per-tile world AABBs for large props. **Prefer this over a mesh BVH** — world is tile-quantized and occupancy already exists. |
| **Expected benefit** | Pick cost ~ O(tiles along ray × entities per tile), independent of total dungeon population. |
| **Complexity / memory** | M; may need multi-tile object registration. |
| **Risks** | Miss non-grid-aligned meshes; behaviour change vs jME collide precision. |
| **Bench** | Stress 500+ entities under cursor; pick time µs vs `collideWith`; correctness parity checklist. |

---

## Pathfinding / AI — non-goals

| Idea | Why not first |
|------|----------------|
| Navmesh | Tile + door costs define legality; A\* graph matches rules |
| Continuous steering spatial hash | Steering already runs on discrete movers; measure before adding |
| Global AI influence maps | Separate design concern; not required for current bottlenecks |

Navigation entry points: `game/navigation/NavigationService.java`, `game/logic/CreatureAiSystem.java`, `game/logic/MovementSystem.java`.
