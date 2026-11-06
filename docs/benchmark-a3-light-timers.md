# A3 — Lighting CPU phase timers — **ACCEPTED (tooling)**

**Date:** 2026-11-06  
**Roadmap item:** [acceleration-lighting.md § A3](acceleration-lighting.md#a3-instrumentation)  
**Decision:** **ACCEPTED as instrumentation** — not claimed as an FPS optimization  
**Workload:** `late-campaign-dense`, 3× suite  

## What landed

Per-frame `System.nanoTime` splits on the lighting hot path, exported through the suite:

| Metric | Source |
|--------|--------|
| `lightUpdateMs` | Whole `LightRules.update` |
| `lightModulatorMs` | Modulator tick/flicker + snapshot |
| `lightUploadMs` | Frustum filter + `LightData` write |
| `lightPageIndexMs` | Terrain page index rebuild |

Code: `LightRules`, `SceneLights`, `BenchmarkSampler`.

## Suite session

`baselines/performance/after-a3-light-timers/session-2026-11-06T12-59-07.279438600Z`

| Run | avg FPS | Notes |
|-----|---------|--------|
| 1 | 39.2 | Timers present |
| 2 | 37.7 | |
| 3 | 37.5 | |

Timers did not introduce a measurable FPS regression vs same-day page-cull control (~37.6).

### Run 1 phase times (measure window)

| Phase | avg ms/frame | Share of ~25.5 ms frame |
|-------|--------------|-------------------------|
| `lightUpdateMs` | **0.628** | ~2.5% |
| └ `lightUploadMs` | 0.454 | ~1.8% |
| └ `lightPageIndexMs` | 0.135 | ~0.5% |
| └ `lightModulatorMs` | 0.037 | ~0.1% |

## Ranking implications (**Verified**)

1. **B2 (light upload spatial hash)** — rejected as next FPS bet: upload CPU is **&lt;0.5 ms**; even a perfect cut saves little of a ~25 ms frame.
2. **B1 retry / more page-index CPU work** — page-index rebuild is **~0.14 ms**; not the lever.
3. **Remaining avg-FPS headroom** is outside this CPU path (GPU Vertex on non-indexed materials, post, sim, etc.). Need GPU timers or A1 (settings) for the next FPS claim.
4. **C1 / C3 / D\*** remain relevant for **tails / hitches**, not steady avg FPS.

## Acceptance note

A3 does **not** replace a performance baseline. Latest FPS-accepted lighting candidate remains **`after-page-light-cull`**. A3 is required infrastructure for honest accel ranking.
