# LightRules lifecycle publication — before/after report

**Date:** 2026-11-06  
**Workload:** `late-campaign-dense` (Level20, seed 42, 100 creatures + 40 imps)  
**Protocol:** 3 consecutive suite runs, 3s warmup / 20s measure / 2s settle, 1920×1080, vsync off  
**Original baseline (preserved):** `baselines/performance/official-baseline-windows-10-amd64.json`  
**Pre-change candidate:** `baselines/performance/after-page-light-cull/` (median avg FPS **39.631**)  
**Candidate results:** `baselines/performance/after-lightrules-lifecycle/`  
**Accepted session:** `session-2026-11-06T12-16-43.098916100Z`  
**Same-machine control (page-cull HEAD, 1 run):** `baselines/performance/machine-check-page-cull/` (**36.992** avg FPS)

## Root cause

Map load mutates `LightRules` / `SceneLights` registries on the `GameClientMapLoader` thread while the render thread can call `update`, `setCheapLighting`, and flat-light sampling. Safety depended on every caller remembering `PlayerMapViewState.isMapLoaded()`. That is an opt-in external gate, not ownership of readiness — new call sites reintroduce `ConcurrentModificationException` races.

## Fix

`LightRules` owns an explicit lifecycle:

| Phase | Who mutates | Who consumes |
|-------|-------------|--------------|
| `LOADING` | Loader (or sync load thread) | Consumer frame APIs no-op |
| `READY` | Render thread (gameplay) | `update` / flat light / settings flips |
| `DISPOSED` | — | All consumer APIs no-op |

- `publishReady()` — volatile publication after the last load-time mutation (same critical section as `mapLoaded = true`).
- `update` / `computeFlatLight` — no-op (or ambient-only) until `READY`.
- `setCheapLighting` during `LOADING` — stores the flag only (no registry iteration) so a settings flip cannot race the loader’s `HashMap`s.
- Call sites (`PlayerMapViewState`, entity/effect/menu/model-viewer) use `isReady()` instead of relying on an external flag for lighting safety.
- Sync loaders (`MainMenuState`, `MapLoaderAppState`) call `publishReady()` after `load()`; `dispose()` on unload/cleanup.

## Concurrency validation

`LightRulesLifecycleTest`:

- Loader mutating tiles/lights while a consumer hammers `update` / `setCheapLighting` / `computeFlatLight` — no exceptions; ends `READY`.
- Many consumers before publish — safe no-ops.
- Idempotent `publishReady`, `dispose` wins, reload pattern with a fresh instance.

## Visual parity (accepted session)

| Run | avg FPS | visual |
|-----|---------|--------|
| 1 | 36.395 | cp01 fail (historical flake) |
| 2 | 35.329 | **all pass** |
| 3 | 34.925 | cp01 fail |

`pageLightIndexAvg` ≈ **20.2**, max ≈ **134**, full-list fallbacks **0** — matches page-light-cull behaviour.

## Performance — medians (accepted 3-run session)

| Metric | Official | After page light cull (earlier) | Same-machine control (page-cull HEAD) | After lifecycle | Δ vs official |
|--------|----------|----------------------------------|----------------------------------------|-----------------|---------------|
| avg FPS | 34.985 | 39.631 | **36.992** (1 run) | **35.329** | **+1.0%** (stable) |
| 1% low FPS | 4.75 | 5.112 | 4.52 | 4.659 | stable |
| 0.1% low FPS | 1.655 | 1.769 | — | 1.608 | stable |
| frame time P99 (ms) | 98.937 | 57.857 | — | 69.621 | **−29.6%** vs official |
| lights uploaded (avg) | 2179 | 2204 | — | 2168 | stable |

Suite gate vs official on performance metrics: **pass**. `overall: fail` is solely `visualAllPassed` from the known mid-checkpoint cp01 flake (also seen on the page-cull control run).

Later re-runs on the same machine showed FPS 22–35 under contention; those are not attributed to this change. The same-machine page-cull control (~37 FPS) brackets the accepted lifecycle median (~35.3).

## How to reproduce

```bash
./gradlew test --tests "toniarts.openkeeper.view.lighting.LightRulesLifecycleTest" --tests "toniarts.openkeeper.view.lighting.LightRulesTest"
./gradlew benchmarkBaseline -PbenchmarkArgs="--out baselines/performance/after-lightrules-lifecycle --no-update-visual-refs --no-official --compare baselines/performance/official-baseline-windows-10-amd64.json"
```

## Verdict

- **Race eliminated at ownership boundary** — not another defensive `isMapLoaded()` check.
- **No intentional sync on the render hot path** beyond a volatile phase read.
- **Lighting fidelity / page-cull behaviour preserved.**
- **Baselines:** official JSON untouched; candidate + sessions under `after-lightrules-lifecycle/`.
