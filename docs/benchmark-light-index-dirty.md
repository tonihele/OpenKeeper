# LightIndex atlas dirty-skip upload — **REJECTED**

**Date:** 2026-11-06  
**Decision:** **REJECTED** — source reverted  
**Workload:** `late-campaign-dense`, 3× suite vs `after-smaa-final-pass`

## What was tried

In `SceneLights.rebuildTerrainPageIndices`, only call `LightIndexData` `setUpdateNeeded()` when a packed index float changes or a page’s `LightIndexCount` changes — on the hypothesis that atlas slot membership is often stable while `LightData` positions/flicker still update every frame.

## Results

| Run | avg FPS | visual |
|-----|---------|--------|
| 1 | 38.402 | pass |
| 2 | 35.145 | cp01 fail |
| 3 | 35.406 | cp01 fail |

Median **35.406** vs **37.170** (−4.7%). Not accepted.

## Lesson

Under dense movers, page membership churn + compare overhead dominate any skipped upload. Keep unconditional index atlas upload after rebuild.
