# C3 — A\* connection reuse — **REJECTED**

**Date:** 2026-11-06  
**Roadmap item:** [acceleration-spatial-simulation.md § C3](acceleration-spatial-simulation.md#c3-astar-pools)  
**Decision:** **REJECTED** — source fully reverted  
**Workload:** `late-campaign-dense`, suite protocol  

## Attempt 1 — pooled `Connection` objects (**unsafe**)

Reused a scratch `Array` plus a pool of 8 `MapTileConnection` instances mutated in `MapIndexedGraph.getConnections`.

### Failure mode (**Verified**)

gdx-ai `IndexedAStarPathFinder.NodeRecord` **stores** `Connection<N> connection` for path reconstruction. Mutating pooled edges after they were recorded **corrupts came-from links**.

Suite symptoms (session `…T13-04-39…`):

| Metric | A3 baseline | Pooled C3 |
|--------|-------------|-----------|
| Median avg FPS | 37.7 | **~87.8** (invalid) |
| Draw calls | ~3015 | **~1147** |
| Lights uploaded | ~2163 | **~1477** |
| Heap peak | ~1.0 GB | **~6.0 GB** |
| Visual | — | **cp00/cp01 fail** |

FPS “gain” was contaminated simulation/render collapse, not a real win. **Do not pool Connection instances** while using stock IndexedA\*.

## Attempt 2 — Array reuse only + fresh edges

Kept scratch `Array`, allocated a new `MapTileConnection` per neighbour (no pool).

| Run | avg FPS | visual |
|-----|---------|--------|
| 1 | 35.9 | **3/3 pass** |
| 2 | — | **child crash** `exit=-805306369` |

No improvement vs A3 baseline (~37.7). Array-only saves negligible alloc vs anonymous `DefaultConnection`.

## Verdict

- **REJECTED** for suite FPS / frame-time.
- Lesson recorded for future pathfinding opts: NodeRecord retains connections → edges must be immutable after publish to A\*.
- Artifacts under `baselines/performance/after-astar-connection-pool/` (including the invalid pooled session for forensic comparison).

## Next

Prefer **C1** (door index) for FoW tick CPU, or **GPU VS attribution / A1** for avg FPS — not Connection pooling.
