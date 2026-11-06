# Skip off-camera terrain page light indices — **REJECTED**

**Date:** 2026-11-06  
**Roadmap item:** High-preset lighting CPU follow-up (page-index rebuild)  
**Decision:** **REJECTED** — source reverted  
**Workload:** `late-campaign-dense` (Level20, seed 42), 3× suite, **High** (SMAA Ultra)  
**Hardware:** Windows 10 amd64, NVIDIA GeForce RTX 2060 SUPER  
**Git revision (control / candidate tree):** `73f60937` (+ candidate patch, then reverted)

## What was tried

In `SceneLights.rebuildTerrainPageIndices`, skip gathering/packing `LightIndexData` for terrain pages whose XZ AABB lies outside the camera frustum expanded by `FRUSTUM_MARGIN_TILES` (same margin as light upload). Keep the last index row and `LightIndexCount` for those pages; only mark the index atlas dirty when at least one on-camera page rebuilds. Suite metric: `pageIndexSkippedOffCamera`.

Hypothesis: most Level20 pages are off-screen; skipping their per-frame light gathers would cut `lightPageIndexMs` and help CPU/submit without changing on-screen lighting.

## Suite session

**Control (no skip):** `baselines/performance/control-high-before-page-frustum/session-2026-11-06T15-00-33.703389200Z`  
**Candidate:** `baselines/performance/after-page-frustum-index-skip/session-2026-11-06T15-02-48.850131800Z`

| Run | Control avg FPS | Candidate avg FPS | Candidate skipPages avg | visual (cand) |
|-----|-----------------|-------------------|-------------------------|---------------|
| 1 | 41.270 | 39.554 | 81.6 | 3/3 |
| 2 | 41.062 | 38.182 | 82.1 | cp01 fail |
| 3 | 39.631 | 36.124 | 80.9 | 3/3 |

| Metric (median) | Control | Candidate | Δ |
|-----------------|---------|-----------|---|
| avg FPS | 41.062 | 38.182 | **−7.0%** |
| 1% low FPS | 5.528 | 4.742 | −14.2% |
| frame time P50 | 17.848 | 20.413 | +14.4% |
| gpuFrameTimeMs | 8.969 | 9.301 | +0.33 ms |
| lightPageIndexMs | 0.134 | 0.127 | ~flat (−0.007 ms) |
| pageIndexSkippedOffCamera | — | ~81.6 | working as coded |
| visualAllPassed | false (cp01 run1) | false (cp01 run2) | flake both sides |
| regression overall | — | **fail** | |

## Interpretation

1. The skip counter proves ~80+ pages/frame were bypassed, but page-index CPU was already **~0.13 ms** — the shared 8×8 light-grid build still runs every frame, so the gather loop was never a High-suite lever.
2. Avg FPS and 1% lows moved the wrong way beyond control CV (~1.8%); treat as a real reject, not noise.
3. On-screen fidelity was not the acceptance path: FPS/stability failed first. Source reverted in full.

## Lesson

Do **not** chase further page-index CPU micro-skips for Level20 High avg FPS. Lighting CPU remains ~sub-millisecond; prefer **F1** GPU VS attribution and hitch-oriented work (**D1**) over index rebuild elision.

## Artifacts

- Control: `baselines/performance/control-high-before-page-frustum/`
- Candidate: `baselines/performance/after-page-frustum-index-skip/` (`VERDICT.json` → **REJECTED**)
- Compare: control candidate vs after-page-frustum-index-skip candidate
