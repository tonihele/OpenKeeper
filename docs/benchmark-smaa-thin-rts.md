# P1 — SMAA thinner intermediate RTs (RGB8 / Depth16) — **REJECTED**

**Date:** 2026-11-06  
**Graphics preset:** **High** (SMAA Ultra)  
**Compare:** `after-smaa-final-pass` @ `65bb3679`  
**Decision:** **REJECTED** — source reverted  

## What was tried

Keep Ultra colour-edge / search quality; store edge buffer as **RGB8** (PS only writes RG) and use **Depth16** on edge/blend intermediate FBOs instead of RGBA8 + default Depth.

## Results (3× High suite)

| Run | avg FPS | gpu ms | visual |
|-----|---------|--------|--------|
| 1 | 31.91 | 10.83 | 3/3 |
| 2 | 36.69 | 8.92 | cp01 fail |
| 3 | 35.26 | 11.35 | cp01 fail |

Median avg FPS **35.263** vs **37.170** (−5.1%, regression). Overall **fail**.

## Lesson

Format thinning is not a High-path win here; retain RGBA8 edge/blend targets from the accepted final-pass wiring.
