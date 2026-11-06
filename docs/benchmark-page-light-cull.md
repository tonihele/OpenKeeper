# Per-page terrain light culling — before/after report

**Date:** 2026-11-06  
**Workload:** `late-campaign-dense` (Level20, seed 42, 100 creatures + 40 imps)  
**Protocol:** 3 consecutive suite runs, 3s warmup / 20s measure / 2s settle, 1920×1080, vsync off  
**Original baseline (preserved):** `baselines/performance/official-baseline-windows-10-amd64.json`  
**Pre-change candidate:** `baselines/performance/after-tile-rebuild-opt/`  
**Candidate results:** `baselines/performance/after-page-light-cull/`  
**Accepted session:** `session-2026-11-06T11-50-03.613472Z`  
**Compare vs official:** `after-page-light-cull/regression-vs-baseline.json` (`overall: pass`)

## Root cause

Terrain `BatchNode` pages (8×8 tiles) and every other Vertex material sampled the same shared `LightData` atlas and looped **all frustum-uploaded lights** (~2.1k–3.0k) at every vertex. CPU frustum culling was already in place; the GPU still paid O(lights × terrain vertices).

## Fix

Keep the shared `LightData` atlas (seam-safe), and add **per-page index lists**:

| Piece | Behaviour |
|-------|-----------|
| `SceneLights` | After each upload, bin lights into an 8×8 cell grid; for each terrain page, collect lights whose sphere intersects the page AABB (reach = radius + 0.1 soft tail). |
| `LightIndexData` | Shared RGBA32F atlas, **4 indices packed per texel**, ≤256 indices/page; overflow → full-list sentinel (`LightIndexCount < 0`). |
| `Vertex.vert` | Terrain pages with `LIGHT_INDEX_DATA` loop the index list; rooms / water / entities keep the original full `numLights` loop (inlined). |
| `MapViewController.bindPageBatchMaterials` | Registers page AABB + batched Vertex materials. |

Diagnostics: `pageLightIndexAvg`, `pageLightIndexMax`, `pageFullListFallbacks` in suite metrics.

### Iteration notes

1. First pass reduced per-page lights ~100× but **regressed FPS** — large RGBA index atlas upload + a shared `dkApplyLight` helper slowed the non-indexed room/entity path.
2. Packed atlas (4× less upload), grid binning, skipped redundant `setInt`, and **restored the inlined full loop** for non-page materials recovered and exceeded the prior candidate.

## Visual parity

| Run | avg FPS | visual |
|-----|---------|--------|
| 1 | 39.6 | **all pass** |
| 2 | 39.2 | **all pass** |
| 3 | 40.6 | **all pass** |

`visualAllPassed: true`

## Performance — medians (3 runs)

| Metric | Official | After tile rebuild | After page light cull | Δ vs official | Δ vs tile-rebuild |
|--------|----------|--------------------|-----------------------|---------------|-------------------|
| avg FPS | 34.985 | 39.141 | **39.631** | **+13.3%** | **+1.3%** |
| 1% low FPS | 4.75 | 5.048 | **5.112** | **+7.6%** | +1.3% |
| 0.1% low FPS | 1.655 | 1.705 | **1.769** | **+6.9%** | +3.8% |
| frame time P50 (ms) | 20.198 | 18.157 | 18.983 | −6.0% | +4.5%* |
| frame time P99 (ms) | 98.937 | 62.629 | **57.857** | **−41.5%** | **−7.6%** |
| lights uploaded (avg) | 2179 | 2178 | 2204 | stable | stable |
| draw calls (avg) | 3018 | 2973 | 3084 | stable | stable |
| heap peak (MB) | 1071 | 1057 | 1052 | stable | stable |

\*P50 slightly above the tile-rebuild candidate but still below official; avg FPS, lows, and P99 improved. Suite gate vs official: **pass**.

### Light evaluation reduction (measure window)

| Counter | Value |
|---------|-------|
| Lights uploaded (avg) | ~2200 |
| Page light index avg | **~20.5** (~**107×** fewer evaluations per terrain vertex) |
| Page light index max (avg of per-frame max) | ~135 |
| Full-list fallbacks | **0** |

Uploaded light count is unchanged (no artificial light culling on the atlas); only the **per-terrain-vertex trip count** shrinks.

### Run-to-run variance

| | Official CV | Tile-rebuild CV | Page-cull CV |
|--|-------------|-----------------|--------------|
| avg FPS | 0.024 | 0.034 | **0.015** |

## Correctness

- Shared `LightData` unchanged → tile-border continuity preserved.
- Page AABB expanded by shader soft-tail (0.1) so influence is not under-culled.
- Rooms / entities / water keep the full atlas loop (no missing contributions there).
- Overflow pages fall back to the full list rather than dropping lights.

## How to reproduce

```bash
./gradlew benchmarkBaseline -PbenchmarkArgs="--out baselines/performance/after-page-light-cull --no-update-visual-refs --no-official --compare baselines/performance/official-baseline-windows-10-amd64.json"
```

## Verdict

- **Redundant per-vertex lighting on terrain pages cut ~100×** (diagnostics).
- **Measurable win vs official** (+13% median FPS, −42% P99) and a small further win vs the tile-rebuild candidate on avg FPS / lows / P99.
- **Visual:** 3/3 full checkpoint pass.
- **Baselines:** official JSON untouched; candidate + regression under `after-page-light-cull/`.
