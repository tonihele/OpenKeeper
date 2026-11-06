# C1 — Incremental FoW door index — **REJECTED**

**Date:** 2026-11-06  
**Roadmap item:** [acceleration-spatial-simulation.md § C1](acceleration-spatial-simulation.md#c1-door-index)  
**Decision:** **REJECTED** — source reverted  
**Compare:** `after-a3-light-timers` (same-day tooling baseline)  

## What was tried

Replace `FogOfWarController.updateDoors()` clear+full rebuild (every 0.15 s vision tick) with incremental maintain of `doorsByPosition` / `closedDoorTiles` from `EntitySet` add/remove/change, keyed by `EntityId → Point`.

## Suite (`late-campaign-dense`, 3×)

| Run | avg FPS | 1% low | visual |
|-----|---------|--------|--------|
| 1 | 37.121 | 4.958 | **3/3** |
| 2 | 34.330 | 2.780 | cp01 fail (noisy) |
| 3 | 39.511 | 5.146 | **3/3** |

| Metric (median) | A3 baseline | C1 candidate | Gate |
|-----------------|-------------|--------------|------|
| avg FPS | 37.657 | 37.121 | stable (−1.4%) |
| 1% low FPS | 4.583 | 4.958 | +8% (noise; CV↑) |
| frame time P99 | 63.647 | 64.415 | stable |
| avg FPS CV | 0.02 | **0.057** | less stable |

## Verdict

Correctness/visual OK on clean runs, but **no reproducible meaningful frame-time win**. Door count on Level20 is small; full rebuild cost was already negligible vs GPU-bound frame (A3). High run-to-run CV means the 1% low bump is not acceptance-grade.

**Source reverted.** Evidence kept under `baselines/performance/after-fow-door-index/`.

## Lesson

Incremental indexes must beat **measured** tick cost. After A3, prefer GPU/A1 for avg FPS; only revisit C1 on a door-dense micro-scenario with FoW-tick timers.
