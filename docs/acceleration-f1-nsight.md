# F1 — Technique-level GPU VS attribution (Nsight / RenderDoc) — **ACCEPTED (process)**

**Date:** 2026-11-06  
**Roadmap item:** [acceleration-structures.md § F1](acceleration-structures.md#f1-vs-split)  
**Decision:** **ACCEPTED as process doc** — no FPS claim; **required** before E1 / B1-retry  
**Graphics preset:** High (SMAA Ultra)  
**Suite context:** `gpuFrameTimeMs` ≈ 10.2 ms (`after-smaa-final-pass`); terrain pages ~20 lights; rooms/entities/water still loop ~2.1–3.0k uploaded lights

## Goal

Split High-suite GPU time into:

1. Terrain Vertex with `LIGHT_INDEX_DATA` (page indices)
2. Full-list Vertex (rooms / water / entities)
3. Post (SMAA Ultra + translucent resolve)
4. Everything else (UI, clear, present)

## Capture procedure (Nsight Graphics / RenderDoc)

1. Launch the game with the same suite knobs as accept/reject: 1920×1080, vsync off, **High** preset, Level20 seed 42 (or attach to a mid-measure suite child).
2. Capture 1–3 frames during steady camera motion over a dense torch region.
3. In the frame debugger, group draws by material / technique:
   - `DkLighting/Vertex` **with** `LIGHT_INDEX_DATA` define → terrain pages
   - `DkLighting/Vertex` **without** index → rooms / entities / water
   - `MatDefs/Post/SMAA/*` → SMAA passes
4. Record VS time (or duration) share for (1) vs (2) vs (3).
5. Paste numbers into a new `docs/benchmark-f1-vs-split.md` with GPU name + driver + git SHA.

## Accept criteria for this item

- Written share table with at least terrain-indexed vs full-list VS % on High.
- No code change required for **this** accept; code hooks (timer queries) optional later.

## Do not

- Re-bind `LIGHT_INDEX_DATA` on non-page materials without this evidence (B1 / shared-index **REJECTED**).
- Start clustered lighting (**E1**) until full-list VS is shown to dominate.
