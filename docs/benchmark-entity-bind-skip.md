# Q5b — Entity Vertex-light steady-state skip — **REJECTED**

**Date:** 2026-11-06  
**Roadmap item:** Follow-up to [benchmark-entity-traversal-opt.md](benchmark-entity-traversal-opt.md) / Q5  
**Decision:** **REJECTED** — source reverted  
**Workload:** `late-campaign-dense`, 3× suite vs `after-smaa-final-pass`

## What was tried

Skip the per-frame `applyVertexLight` sweeps once every object/creature/door/trap reports `VertexLightReady`, re-running only when models spawn or `AssetUtils.invalidateVertexLight` bumps a generation counter.

## Results

| Run | avg FPS | cpuUpdateMs | visual |
|-----|---------|-------------|--------|
| 1 | 36.573 | 8.44 | pass |
| 2 | 36.262 | 8.73 | pass |
| 3 | 37.966 | 8.51 | **cp01 fail** |

Median avg FPS **36.573** vs SMAA final-pass **37.170** (−1.6%, stable). 1%/0.1% lows **regressed**. `cpuUpdateMs` not improved (anim swaps invalidate often under this workload).

## Lesson

Bind-once already removed DFS cost; the remaining O(entities) ready-check is not a suite lever. Keep the simple every-frame early-return loop.
