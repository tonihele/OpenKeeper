# A1 — Settings prefs / MSAA UI — **ACCEPTED (UX)**

**Date:** 2026-11-06  
**Roadmap item:** [acceleration-lighting.md § A1](acceleration-lighting.md#a1-quality-settings)  
**Decision:** **ACCEPTED** as settings hygiene — not claimed as a suite FPS optimization  
**Workload:** `late-campaign-dense` (Level20, seed 42), 3× suite protocol  

## Context

Historical Q1 (`applyMaximumGraphicsSettings()` forcing 16× MSAA / SSAO / AF every launch) is **already gone**. Remaining A1 slice:

1. Stop stomping persisted `frameRate` on every launch (`setFrameRate(max(200, refresh))` ran unconditionally).
2. Hide **MSAA 16×** from the settings dropdown (`AntiAliasingMode.VALUES`); enum value kept for saved-pref migration.

Default remains **SMAA Ultra** (`samples: 0`). Suite already runs that path, so this change cannot show a large suite FPS delta — player wins are for installs that still had prefs overwritten or UI-driven 16× MSAA.

## What landed

| Change | File |
|--------|------|
| First-run-only high frame-rate floor | `Settings` constructor |
| `VALUES` omits `MSAA_16` | `AntiAliasingMode` |

## Benchmarks

| Session | Role | Median avg FPS | Visual |
|---------|------|----------------|--------|
| `after-a2-soft-falloff` | Compare baseline | **37.808** | 3/3 |
| `after-a1-settings` | Candidate | **38.885** | **3/3** |

Suite vs A2: `overall: pass`, `regressionCount: 0`. Median FPS +2.8% is within same-day noise (CV low); **not** treated as an FPS accept.

### Per-run

| Run | avg FPS | 1% low | visual |
|-----|---------|--------|--------|
| 1 | 38.885 | 5.245 | pass |
| 2 | 39.139 | 5.175 | pass |
| 3 | 38.871 | 5.397 | pass |

## Interpretation

- **Verified:** No suite regression; visuals clean.
- **Verified:** Prefs overwrite for frame rate removed for returning players.
- **Still open:** Named Low/Med/High/Ultra quality presets; explicit MSAA×SMAA matrix outside the default suite.

## Artifacts

- Candidate: `baselines/performance/after-a1-settings/`
- Compare: `baselines/performance/after-a2-soft-falloff/candidate-baseline-windows-10-amd64.json`
- `VERDICT.json` → `ACCEPTED`

## Follow-ups

1. Quality presets (S4 / remainder of A1).
2. **GPU VS attribution** for the next real suite FPS lever.
