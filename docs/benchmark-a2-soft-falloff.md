# A2 — Soft-falloff Java ↔ GLSL sync — **ACCEPTED (correctness)**

**Date:** 2026-11-06  
**Roadmap item:** [acceleration-lighting.md § A2](acceleration-lighting.md#a2-soft-falloff)  
**Decision:** **ACCEPTED** — correctness fix; not claimed as an FPS optimization  
**Workload:** `late-campaign-dense` (Level20, seed 42), 3× suite protocol  

## What landed

`LightConstants.SOFT_FALLOFF_TAIL_TILES` was **0.01** while GLSL `DK_SOFT_FALLOFF_TAIL_TILES` and page-index reach were **0.1**. Frustum upload culling and `computeFlatLight` under-reached the GPU falloff.

- Set `LightConstants.SOFT_FALLOFF_TAIL_TILES = 0.1f`
- `SceneLights.PAGE_LIGHT_REACH_TAIL_TILES` now aliases that constant (single source of truth)

## Benchmarks

| Session | Role | Median avg FPS | Visual |
|---------|------|----------------|--------|
| `after-a3-light-timers` | Compare baseline | **37.657** | false (cp01 flake) |
| `after-a2-soft-falloff` | Candidate | **37.808** | **3/3** |

Suite vs A3: `overall: pass`, `regressionCount: 0`.

| Metric (median) | A3 | A2 | Status |
|-----------------|----|----|--------|
| avg FPS | 37.657 | 37.808 | stable (+0.4%) |
| lightsAvg | 2163.4 | 2213.2 | stable (+2.3%) |
| frameTime P99 | 63.647 | 61.166 | stable |
| visualAllPassed | false | **true** | pass |

### Per-run

| Run | avg FPS | 1% low | visual |
|-----|---------|--------|--------|
| 1 | 40.771 | 5.775 | pass |
| 2 | 37.808 | 5.117 | pass |
| 3 | 37.375 | 5.253 | pass |

## Interpretation

- **Verified:** CPU frustum / flat reach now match shader soft-falloff tail.
- **Verified:** Slightly larger uploaded light pool (~+50 median) as expected; no suite FPS regression.
- **Not claimed:** FPS win — median within noise of A3.

## Artifacts

- Candidate: `baselines/performance/after-a2-soft-falloff/`
- Compare: `baselines/performance/after-a3-light-timers/candidate-baseline-windows-10-amd64.json`
- `VERDICT.json` → `ACCEPTED`

## Follow-ups

Latest FPS-accepted lighting candidate remains **`after-page-light-cull`**. Next levers: **F1** (technique VS) / **A1b·P1** (presets / post) — see [acceleration-structures.md](acceleration-structures.md).
