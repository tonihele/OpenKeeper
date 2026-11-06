# P1 / A1b — SMAA High as default preset — **REJECTED**

**Date:** 2026-11-06  
**Roadmap item:** [acceleration-structures.md § P1 / A1b](acceleration-structures.md#p1-post-cost)  
**Decision:** **REJECTED** — source reverted  
**Workload:** `late-campaign-dense`, 3× suite vs `after-a3-gpu-timers`

## What was tried

- Official SMAA **HIGH** preset as product/suite default (Ultra kept as optional mode)
- Material `SmaaUltra` define wiring; suite pinned `SMAA_HIGH`

## Results

| Session | Median avg FPS | Visual |
|---------|----------------|--------|
| `after-a3-gpu-timers` (Ultra-era default path) | **37.259** | 3/3 |
| `after-smaa-high-default` | **35.760** | 2/3 (cp01 fail run 1) |

No FPS improvement (−4% median, within/below noise, wrong direction). Automated regression flagged 1% lows. Source fully reverted.

## Lesson

SMAA High is not a free suite win on this machine/workload; keep Ultra as the shipping SMAA quality until an off-suite AA matrix proves otherwise. Artifacts retained under `baselines/performance/after-smaa-high-default/`.
