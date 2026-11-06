# A3b — GPU frame + CPU update/render timers — **ACCEPTED (tooling)**

**Date:** 2026-11-06  
**Roadmap item:** [acceleration-lighting.md § A3](acceleration-lighting.md#a3-instrumentation) (GPU follow-up)  
**Decision:** **ACCEPTED as instrumentation** — not claimed as an FPS optimization  
**Workload:** `late-campaign-dense` (Level20, seed 42), 3× suite protocol  

## What landed

| Metric | Source |
|--------|--------|
| `gpuFrameTimeMs` | Double-buffered `GL_TIME_ELAPSED` via jME `Renderer.startProfiling` / `stopProfiling`, wrapped around `AppStep.RenderFrame`→`EndFrame` |
| `cpuUpdateMs` | Wall time `BeginFrame`→`RenderFrame` |
| `cpuRenderMs` | Wall time `RenderFrame`→`EndFrame` |

Code: `GpuFrameTimer`, `BenchmarkFrameProfiler`, `BenchmarkSampler`, `BenchmarkAppState` (profiler attached for the measure window only; ignores steps until the first `BeginFrame` after attach).

**Still open:** per-technique VS split (terrain `LIGHT_INDEX_DATA` vs full-list rooms/entities). Nested per-draw queries are too invasive; use Nsight/RenderDoc for that cut.

## Suite session

`baselines/performance/after-a3-gpu-timers/session-2026-11-06T13-47-27.414258700Z`

| Run | avg FPS | gpuFrameTimeMs avg | cpuUpdateMs avg | cpuRenderMs avg | visual |
|-----|---------|--------------------|-----------------|-----------------|--------|
| 1 | 36.497 | **10.922** | 8.707 | 17.059 | pass |
| 2 | 38.218 | **10.721** | 8.153 | 16.521 | pass |
| 3 | 37.259 | **10.731** | 8.447 | 16.869 | pass |

Median avg FPS **37.259** vs A1 **38.885** (−4.2%, gate `stable`). Visual **3/3**. Automated regression flagged 1% low / P99 noise (`overall: fail`) — accepted as tooling like A3 CPU timers.

## Ranking implications (**Verified**)

On `late-campaign-dense`:

| Bucket | ~ms / frame | Share of ~27 ms wall |
|--------|-------------|----------------------|
| GPU render elapsed | **~10.8** | ~40% |
| CPU render (submit / RM) | **~16.8** | ~62% of wall (overlaps GPU) |
| CPU update | **~8.4** | ~31% |
| Lighting CPU (`lightUpdateMs`) | ~0.6 | ~2% (A3) |

1. **GPU work is real (~11 ms)** but wall frame is not “GPU-only” — CPU render path is large.
2. **Lighting CPU remains a non-lever** for avg FPS (A3).
3. **Technique-level VS attribution** (indexed terrain vs full-list) is the remaining GPU question — not answered by whole-frame queries.
4. Next FPS bets: Nsight VS split, post/SMAA cost, or sim hitch scenarios — not another B1-shaped index bind.

## Artifacts

- Candidate: `baselines/performance/after-a3-gpu-timers/`
- Compare: `baselines/performance/after-a1-settings/candidate-baseline-windows-10-amd64.json`
- `VERDICT.json` → `ACCEPTED`

## Acceptance note

Does **not** replace the FPS-accepted lighting baseline (`after-page-light-cull`). Required infrastructure for ranking GPU vs CPU headroom.
