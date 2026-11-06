# C2 — Sensing BFS flood (visited tiles) — **REJECTED**

**Date:** 2026-11-06  
**Roadmap item:** [acceleration-structures.md § C2](acceleration-structures.md#c2-sensing)  
**Decision:** **REJECTED** — source reverted  
**Workload:** `late-campaign-dense`, 3× suite vs `after-a3-gpu-timers`

## What was tried

Replace recursive `addSensedEntities` with a BFS + packed-coordinate visited set (tile object identity is not stable). First broken attempt used `HashSet<IMapTileInformation>` and exploded (~29 FPS).

## Results (coordinate-keyed BFS)

| Run | avg FPS | visual |
|-----|---------|--------|
| 1 | 33.228 | fail cp01 |
| 2 | 23.530 | pass |
| 3 | 37.639 | pass |

Median vs A3b **37.259**: no reproducible win; high CV / hitchy run 2. Source reverted.

## Lesson

Sense flood is not a `late-campaign-dense` avg-FPS lever. Retry only with logic-thread timers + creature-dense micro-scenario. Keep the per-tick `sensedEntitiesByEntity` cache as-is.
