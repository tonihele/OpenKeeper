# B1-retry — Shared camera-neighbourhood light index — **REJECTED**

**Date:** 2026-11-06  
**Roadmap item:** [acceleration-lighting.md § B1](acceleration-lighting.md#b1-room-water-light-indices) (alternate approach)  
**Decision:** **REJECTED** — source reverted  
**Workload:** `late-campaign-dense` (Level20, seed 42), 3× suite protocol  

## What was tried

After B1’s per-room AABB indices failed to move FPS, try a **single shared `LightIndexData` row** for every non-page Vertex material (rooms / water / entities):

- Reuse the existing 8×8 light-cell grid from terrain page rebuild
- Pack lights from camera cell ±2 (5×5 neighbourhood) into one extra atlas row
- `bindMaterials` / `applyVertexLight` attach that row instead of omitting `LightIndexData`
- Overflow → full-list sentinel (`LightIndexCount < 0`), same as pages

## Benchmarks

| Session | Role | Median avg FPS | Visual |
|---------|------|----------------|--------|
| `after-a3-light-timers` | Compare baseline (same machine era) | **37.657** | 2/3 (cp01 flake historically) |
| `after-shared-nonpage-light-index` | Candidate | **27.294** | **0/3** (cp01 fail every run) |

Candidate vs A3: **−10.4 FPS (−27.5%)**. Suite `overall: fail` (avg FPS / lows / P50 / P99).

### Per-run

| Run | avg FPS | 1% low | visual |
|-----|---------|--------|--------|
| 1 | 27.294 | 3.738 | fail (cp01) |
| 2 | 25.965 | 3.555 | fail (cp01) |
| 3 | 27.873 | 3.680 | fail (cp01) |

### Instrumentation (run 1 averages)

| Metric | A3 era (typical) | Candidate |
|--------|------------------|-----------|
| `sharedNonPageIndexCount` | n/a | ~85 (min −1 overflow) |
| `sharedNonPageIndexOverflow` | n/a | **~41.5% of frames** |
| `lightUploadMs` | ~0.45 | ~0.51 |
| `lightPageIndexMs` | ~0.14 | **~4.68** |
| `lightUpdateMs` | ~0.63 | **~5.26** |

## Interpretation

- **Verified:** Binding `LightIndexData` on non-page materials is a large net loss on this suite — not a wash.
- **Verified:** Page-index rebuild CPU ballooned (~30×) from packing the shared row and pushing uniforms across the shared-material set as counts change every frame.
- **Verified:** Visual parity fails (cp01) — camera-cell neighbourhood misses lights that still affect on-screen rooms/entities, and ~40% overflow does not restore the classic non-indexed shader variant (materials stay on the `LIGHT_INDEX_DATA` technique with a branched full-list fallback).
- Echoes the page-cull lesson: the **inlined full `numLights` loop** for non-page mats must stay the default until GPU VS attribution proves otherwise.

## Artifacts

- Candidate: `baselines/performance/after-shared-nonpage-light-index/`
- Compare: `baselines/performance/after-a3-light-timers/candidate-baseline-windows-10-amd64.json`
- `VERDICT.json` → `REJECTED`

## Follow-ups

1. Do **not** re-enable `LIGHT_INDEX_DATA` on rooms/entities without a dedicated shader path that preserves the fast full-list variant when unbound.
2. Need **GPU VS attribution** (terrain vs room vs entity) before another non-page index attempt.
3. Next suite FPS levers remain **A1 settings** or real VS timers — not another B1 shape.
