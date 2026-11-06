# Spatial & simulation acceleration

**Parent roadmap:** [acceleration-structures.md](acceleration-structures.md) (authoritative status)  
**Related reports:** [benchmark-fow-door-index.md](benchmark-fow-door-index.md) · [benchmark-astar-connection-pool.md](benchmark-astar-connection-pool.md) · [benchmark-entity-traversal-opt.md](benchmark-entity-traversal-opt.md)

Simulation and interaction in OpenKeeper are **tile-native**. Prefer extending `PositionSystem` grids and FoW tables over continuous-space trees.

---

## Current structures (verified)

| Structure | Path | Role | Notes |
|-----------|------|------|-------|
| `entitiesByMapTile[][]` / `obstaclesByMapTile` | `game/logic/PositionSystem.java` | O(1) occupancy | |
| Indexed A\* + `MapIndexedGraph` | `game/navigation/pathfinding/` | Tile graph search | Still allocates `Array` + `DefaultConnection` per expansion |
| FoW `BitSet` explored/perceived | `game/fogofwar/FogState.java` | Compact visibility | |
| `LineOfSightTable` | `game/fogofwar/LineOfSightTable.java` | Precomputed vision rings (r≤7) | |
| Door map for FoW | `FogOfWarController.doorsByPosition` | Vision blockers | **clear + rebuild** each FoW tick (C1 rejected) |
| Sense cache | `PositionSystem.sensedEntitiesByEntity` | Per-tick `computeIfAbsent` | Cleared each process; **flood fill** on miss |
| `TilePicker` DDA | tile picking utilities | Analytical tile ray | |
| Entity `collideWith(Ray)` | `view/PlayerInteractionState.java` | Interactive object hover | Still full entity-root collide |

No app-authored BVH/octree for creatures or shots.

---

## C1 — Incremental door index {#c1-door-index}

**Status: REJECTED** — [benchmark-fow-door-index.md](benchmark-fow-door-index.md). Source reverted.

| | |
|--|--|
| **Bottleneck** | FoW vision tick clear+rebuilds `doorsByPosition` each ~0.15 s (**Verified** still present). |
| **Result** | Median avg FPS **37.1** vs A3 **37.7**; high CV. |
| **Lesson** | Not hot on `late-campaign-dense`; retry only with FoW-tick timers + door-dense scenario. |

---

## C2 — Sensing cache / offset expansion {#c2-sensing-cache}

**Status: PARTIAL** — per-tick entity→set cache exists; recursive flood remains. Suite BFS attempt **REJECTED** ([benchmark-sensing-bfs.md](benchmark-sensing-bfs.md)).

| | |
|--|--|
| **Bottleneck** | `addSensedEntities` recursive flood; TODO #261. Cache cleared each `PositionSystem` process. |
| **Evidence** | **Verified** in `PositionSystem`; suite avg FPS **not** improved by BFS+visited. |
| **Remaining work** | Offset table / logic timers on a creature-dense micro-scenario — not Level20 suite alone. |
| **Detail** | [acceleration-structures.md § C2](acceleration-structures.md#c2-sensing). |

---

## C3 — A\* neighbour / connection reuse {#c3-astar-pools}

**Status: REJECTED** — [benchmark-astar-connection-pool.md](benchmark-astar-connection-pool.md). Source reverted.

| | |
|--|--|
| **Current code** | `MapIndexedGraph.getConnections` still allocates a new `Array` and `DefaultConnection`s (**Verified**). |
| **Lesson** | Never pool/mutate `Connection` instances used with stock `IndexedAStarPathFinder` (`NodeRecord` retains edges). |
| **Retry** | Only with path-alloc micro-bench; suite avg FPS is the wrong gate. |

---

## C4 — Tile-first entity picking {#c4-picking}

**Status: NOT STARTED**.

| | |
|--|--|
| **Bottleneck** | `playerEntityViewState.getRoot().collideWith(ray, results)` (**Verified**). |
| **Proposal** | Tile DDA / cursor tile → occupancy tests. |
| **Detail** | [acceleration-structures.md § C4](acceleration-structures.md#c4-picking). |

---

## Pathfinding / AI — non-goals

| Idea | Why not first |
|------|----------------|
| Navmesh | Tile + door costs define legality; A\* graph matches rules |
| Continuous steering spatial hash | Measure before adding |
| Global AI influence maps | Separate design concern |

Entry points: `NavigationService`, `CreatureAiSystem`, `MovementSystem`.
