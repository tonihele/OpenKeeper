# A1b — Graphics quality presets (High = suite/shipping) — **ACCEPTED (UX / reproducibility)**

**Date:** 2026-11-06  
**Roadmap item:** [acceleration-structures.md § A1b](acceleration-structures.md#a1b-presets)  
**Decision:** **ACCEPTED as UX + suite pinning** — **not** an avg-FPS claim on High  
**Git (docs land with this change):** see commit after `65bb3679`  
**Hardware:** Windows 10 amd64 (same machine as `after-smaa-final-pass`)

## What shipped

| Preset | AA | SSAO | AF | Cheap lighting |
|--------|-----|------|----|----------------|
| Low | Off | off | 0 | on |
| Medium | MSAA 4× | off | 4 | off |
| **High** | **SMAA Ultra** | **off** | **0** | **off** |
| Ultra | SMAA Ultra + MSAA 4× | on | 16 | off |

- Default / `GRAPHICS_QUALITY_PRESET` = **High** (matches historical SMAA Ultra shipping).
- Main menu + pause graphics UI: preset dropdown syncs AA / SSAO / AF.
- `BenchmarkSuiteRunner` defaults `--graphics-preset HIGH` so accept/reject suites never silently use Low.
- CLI: `-benchmark-graphics-preset NAME` for **explicit** matrix probes only.

## Low matrix probe

Aborted (child crash + product rule: accept/reject stays on High). Artifacts under `baselines/performance/after-graphics-preset-low/` marked **ABORTED**.

## Frustum margin note

Separate High-suite attempt `FRUSTUM_MARGIN` 5→3 **REJECTED** ([benchmark-frustum-margin-3.md](benchmark-frustum-margin-3.md)).

## Acceptance

Presets enable player headroom and reproducible High suites. **Do not** treat Low FPS as a High-path win.
