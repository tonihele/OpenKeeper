# Frustum light margin 5 → 3 — **REJECTED**

**Date:** 2026-11-06  
**Decision:** **REJECTED** — source reverted  
**Workload:** `late-campaign-dense`, 3× suite vs `after-smaa-final-pass`

## What was tried

Reduce `SceneLights.FRUSTUM_MARGIN_TILES` from **5** to **3** so fewer off-screen lights enter the shared atlas (rooms/entities still loop the full uploaded list).

## Results

| Metric (median) | Baseline | Candidate |
|-----------------|----------|-----------|
| avg FPS | 37.170 | **35.883** (−3.5%) |
| lightsUploaded | 2213 | **2062** (−6.9%) |
| visualAllPassed | true | **false** |

Suite regression **fail** (1% lows + visual). Margin **5** restored.

## Lesson

Uploaded-count cuts at the frustum edge are not free: visual edge lighting regresses before suite FPS benefits. Prefer **F1** (VS attribution) before more cull knobs.
