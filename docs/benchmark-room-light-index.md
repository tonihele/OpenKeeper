# B1 — Room light-index extension — **REJECTED**

**Date:** 2026-11-06  
**Roadmap item:** [acceleration-lighting.md § B1](acceleration-lighting.md#b1-room-water-light-indices)  
**Decision:** **REJECTED** — source reverted  
**Workload:** `late-campaign-dense` (Level20, seed 42), 3× suite protocol  

## What was tried

Extend the existing terrain **8×8 light-cell → packed `LightIndexData`** path to room Vertex materials (and briefly water/lava):

- `SceneLights.setIndexedMesh` / `clearIndexedMesh` for non-page AABB slots
- `MapViewController.handleRoom` registered room coordinates’ AABB + lighting materials
- Index rebuild generalized to scan all cells overlapping each AABB (±1 cell)

Water/lava registration was removed after the first session showed **constant `pageFullListFallbacks = 2`** (map-wide sheets overflow `MAX_INDICES_PER_PAGE` every frame while still paying large-cell rebuild cost).

## Benchmarks

| Session | Role | Median avg FPS | Visual |
|---------|------|----------------|--------|
| Historical `after-page-light-cull` | Prior accepted lighting candidate | **39.631** | 3/3 |
| `control-page-cull-same-machine` | Same-machine control (page-cull HEAD, no B1) | **37.586** | 2/3 (cp01 flake) |
| `after-room-light-index` (rooms-only) | B1 candidate | **36.909** | **3/3** |

Candidate vs control: **−0.68 FPS (−1.8%)** — within/below noise, **not an improvement**.

Suite gate vs historical page-cull JSON: `overall: fail` (avg FPS / lows / P99 regressions). That comparison mixes machine eras; the **same-machine control** is decisive for acceptance.

### Rooms-only per-run

| Run | avg FPS | 1% low | visual |
|-----|---------|--------|--------|
| 1 | 36.909 | 4.865 | pass |
| 2 | 37.751 | 4.885 | pass |
| 3 | 36.818 | 4.803 | pass |

## Interpretation

- **Verified:** Mechanism works (visual pass; rooms can consume index rows).
- **Verified:** Does **not** raise FPS on `late-campaign-dense` vs matched control.
- **Hypothesis (why):** In this camera/workload, room pixels are a modest share of Vertex work vs terrain (already indexed) + entities (still full list) + water/lava (full list). Extra index rebuild for many room AABBs adds CPU without enough GPU win.
- Operation-count reduction on rooms ≠ frame-time win when those meshes are not the bottleneck.

## Artifacts

- Candidate: `baselines/performance/after-room-light-index/`
- Control: `baselines/performance/control-page-cull-same-machine/`
- `VERDICT.json` → `REJECTED`

## Follow-ups

1. **A3 instrumentation** — attribute VS time (terrain / room / entity) before retrying mesh indexing.
2. Consider **entity** index lists only with proof entities dominate VS (maintenance cost high — movers).
3. Do **not** re-land room indexing without a room-heavy micro-benchmark that beats same-machine control.
