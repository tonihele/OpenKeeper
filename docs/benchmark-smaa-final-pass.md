# P1 — SMAA neighborhood as final filter pass — **ACCEPTED**

**Date:** 2026-11-06  
**Roadmap item:** [acceleration-structures.md § P1](acceleration-structures.md#p1-post-cost)  
**Decision:** **ACCEPTED**  
**Workload:** `late-campaign-dense` (Level20, seed 42), 3× suite vs `after-a3-gpu-timers`

## What changed

`SmaaFilter` previously ran edge → blend-weights → **neighborhood (own FBO)** → **Overlay 1:1 blit** into the filter output.

Neighborhood blend is now the filter’s final `material` pass (jME binds scene colour as `Texture`; weights come from the blend-weight FBO). That drops one RGBA8 intermediate target and one full-screen copy. Ultra quality defines / search steps are unchanged.

## Suite session

`baselines/performance/after-smaa-final-pass/session-2026-11-06T14-13-31.214178700Z`

| Run | avg FPS | gpuFrameTimeMs avg | cpuUpdateMs | cpuRenderMs | visual |
|-----|---------|--------------------|-------------|-------------|---------|
| 1 | 37.170 | **10.20** | 8.59 | 16.82 | 3/3 |
| 2 | 38.628 | **9.98** | 8.02 | 16.41 | 3/3 |
| 3 | 36.699 | **10.98** | 8.75 | 17.05 | 3/3 |

| Metric (median) | A3b | Candidate | Δ |
|-----------------|-----|-----------|---|
| avg FPS | 37.259 | 37.170 | −0.2% (stable) |
| 1% low FPS | 4.968 | 5.092 | +2.5% (stable) |
| frame time P50 | 20.337 | 20.299 | stable |
| **gpuFrameTimeMs** | **10.73** | **10.20** | **−0.53 ms (~−5% GPU)** |
| visualAllPassed | true | true | pass |
| regression overall | — | **pass** | |

## Interpretation

1. Removing the Overlay stage is fidelity-neutral (same neighborhood shader writing the filter output).
2. Suite avg FPS stays GPU/submit-bound and flat within noise; the win shows up on **A3b `gpuFrameTimeMs`**.
3. Shipping SMAA stays **Ultra**; this is wiring cost, not a quality downgrade (contrast [SMAA High reject](benchmark-smaa-high-default.md)).

## Artifacts

- Candidate: `baselines/performance/after-smaa-final-pass/`
- Compare: `after-a3-gpu-timers/candidate-baseline-windows-10-amd64.json`
- `VERDICT.json` → `ACCEPTED`
