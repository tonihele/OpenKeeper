# AmbientMap update pipeline — investigation report (no ship)

**Date:** 2026-11-06  
**Workload:** `late-campaign-dense` (Level20, seed 42)  
**Pre-change candidate:** `baselines/performance/after-page-light-cull/` (median avg FPS **39.631**)  
**Experiment results:** `baselines/performance/after-ambient-map-opt/`  
**Verdict:** **Reverted.** Keep the page-light-cull implementation; do not ship AmbientMap changes from this session.

## Problem analysis

`SceneLights.setTileAmbient` wrote one RGBA8 texel then called `Image.setUpdateNeeded()` per tile. That flags the **entire** AmbientMap for re-upload on the next bind.

| Fact | Implication |
|------|-------------|
| `setUpdateNeeded()` is a boolean | N tile writes in one frame still produce **one** GPU upload when the texture is bound |
| Level20 AmbientMap ≈ 85×85×4 ≈ **29 KB** | Full re-upload cost is tiny vs lighting / sim work |
| Measure window dirties the map ~**33–36× / 20 s** | ~1.7 uploads/s even under FoW + dig |

So S3 in `docs/rendering-pipeline-analysis.md` is real as *API shape*, but **not** a first-order FPS hotspot on the established suite.

## Approaches tried

### A — Dirty AABB + mid-frame `GLRenderer.modifyTexture` (partial `glTexSubImage2D`)

- Coalesced tile writes into one dirty rectangle (~77 tiles → ~547 bytes/upload).
- Flushed during `LightRules.update` via zero-copy subimage upload.
- **Result (session `…T11-58-44…`):** median avg FPS **34.7** (−12% vs page-light-cull), 1% lows regressed, mid visual checkpoint flaky.
- **Interpretation:** Texture-unit bind + `GL_UNPACK_ROW_LENGTH` during AppState update costs more than it saves at this update rate.

### B — Dirty AABB + single coalesced `setUpdateNeeded()` (no subimage)

- Same coalescing; one full-texture upload flag per dirty frame (matches prior GPU behaviour, fewer redundant flag sets).
- **Result (session `…T12-02-48…`):** avg FPS **33.5 / 24.6 / 25.1** — unstable and worse. Not accepted.

## Decision

Per acceptance criteria (*if partial updates cost more than they save, retain the best verified approach*):

1. **Revert all AmbientMap code changes** to `after-page-light-cull` / commit `4cb727c0`.
2. Preserve experiment JSON under `baselines/performance/after-ambient-map-opt/` for audit.
3. Leave S3 as a **future optional** for pathological cases (huge maps × continuous full-map FoW floods), preferably as a **render-thread post-pass** subimage that does not disturb the main update GL state — only if a dedicated AmbientMap stress workload shows a clear win.

## What was *not* wrong with the diagnosis

Coalescing diagnostics from attempt A still prove tile writes batch well:

| Metric (run 1, attempt A) | Value |
|---------------------------|-------|
| Tile writes coalesced | 2624 |
| GPU uploads | 34 |
| Bytes uploaded (partial) | ~18.6 KB |
| Equivalent full uploads | 34 × ~29 KB ≈ 1 MB |

The optimization moved bytes the right way; it just was not on the critical path, and the integration tax dominated.

## How to re-open later

1. Add an AmbientMap-heavy microbench (continuous FoW paint / mass dig).
2. If full uploads exceed ~several MB/s sustained, retry subimage **after** the frame’s draw (SceneProcessor / end-of-frame callback), not inside `LightRules.update`.
3. Gate on beating `after-page-light-cull` median FPS with visual 3/3 pass.
