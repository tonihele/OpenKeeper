# Tile/room rebuild storm elimination — before/after report

**Date:** 2026-11-06  
**Workload:** `late-campaign-dense` (Level20, seed 42, 100 creatures + 40 imps)  
**Protocol:** 3 consecutive suite runs, 3s warmup / 20s measure / 2s settle, 1920×1080, vsync off  
**Original baseline (preserved):** `baselines/performance/official-baseline-windows-10-amd64.json`  
**Pre-fix candidate:** `baselines/performance/after-lighting-alloc-opt/`  
**Candidate results:** `baselines/performance/after-tile-rebuild-opt/`  
**Accepted session:** `session-2026-11-06T11-30-39.186820600Z`  
**Compare vs official:** `after-tile-rebuild-opt/regression-vs-baseline.json` (`overall: pass`)

## Root cause

`MapViewController.updateTiles` treated every dirty tile as a topology event:

1. Any dirty point inside (or beside) a room tore down the **entire** room instance and rebuilt all of its tiles plus neighbours.
2. Every rebuild also reconstructed the **whole-map ceiling** mesh — even while the ceiling stayed hidden (normal play until possession).
3. Selection flash (`FlashTileViewState`, 0.5s) called the same full geometry path to toggle a material Tint.
4. FOW + ECS often enqueued separate rebuilds for overlapping points in the same frame.

Steady-state creature vision (~0.15s) therefore repeatedly destroyed and rebuilt rooms whose terrain id had not changed.

## Fix

| Change | Effect |
|--------|--------|
| Reason-gated room teardown | Room instances rebuild only when a coalesced batch includes a **terrain-id change** (dig/claim/destroy). FOW/component refreshes update tile lights/geometry without `removeRoomInstances`. |
| Deferred ceiling rebuild | While `ceilingVisible` is false, mark dirty and skip the whole-map ceiling mesh; rebuild once on show (possession). |
| Flash overlay path | `flashTile` only retints existing page floor/wall/top materials (clone + Tint); no room teardown / rebatch / ceiling. |
| Frame coalesce | Dirty points from FOW + ECS merge into one enqueue flush per frame. |
| Unique-room expansion | When rooms *do* rebuild, collect instances once instead of re-walking the same room per point. |
| Diagnostics | `TileRebuildStats` sampled into suite metrics (`tileRebuild.*`) for the measure window. |

## Attempt note

First session (`session-2026-11-06T11-25-18…`) coalesced flushes onto the same `update()` tick **without** terrain-gated room teardown. That retained the room storm, destabilized FPS (median ~31.8, one run at 21 FPS), and failed mid visual gates. Those results are kept on disk; the implementation was revised rather than accepted.

## Visual parity

Against frozen refs (`--no-update-visual-refs`):

| Run | avg FPS | visual |
|-----|---------|--------|
| 1 | 40.7 | **all pass** |
| 2 | 37.4 | **all pass** |
| 3 | 39.1 | **all pass** |

`visualAllPassed: true`

## Performance — medians (3 runs)

| Metric | Official | After lighting alloc | After tile rebuild | Δ vs official | Δ vs lighting |
|--------|----------|----------------------|--------------------|---------------|---------------|
| avg FPS | 34.985 | 37.329 | **39.141** | **+11.9%** | **+4.9%** |
| 1% low FPS | 4.75 | 3.893 | **5.048** | **+6.3%** | **+29.7%** |
| 0.1% low FPS | 1.655 | 1.491 | 1.705 | +3.0% | +14.4% |
| frame time P50 (ms) | 20.198 | 18.964 | **18.157** | **−10.1%** | −4.3% |
| frame time P99 (ms) | 98.937 | 82.593 | **62.629** | **−36.7%** | **−24.2%** |
| draw calls (avg) | 3018 | 2971 | 2973 | stable | stable |
| lights uploaded (avg) | 2179 | 2169 | 2178 | stable | stable |
| heap peak (MB) | 1071 | 1060 | 1057 | stable | stable |

Suite regression gate vs official: **pass** (0 regressions).

### Run-to-run variance

| | Official CV | Lighting CV | Rebuild CV |
|--|-------------|-------------|------------|
| avg FPS | 0.024 | 0.041 | **0.034** |

Candidate FPS: 40.7 / 37.4 / 39.1.

### Rebuild diagnostics (measure window, median run ≈ run 3)

| Counter | Value | Meaning |
|---------|-------|---------|
| `geometryRebuildCalls` | 30–36 | Coalesced flushes (~1.5–1.8/s) |
| `geometryWithRoomRebuild` | **0** | No room teardowns in steady-state measure |
| `geometryWithoutRoomRebuild` | 30–36 | FOW/component path only |
| `ceilingRebuilds` | **0** | Ceiling stayed hidden |
| `ceilingSkippedHidden` | 30–36 | Whole-map ceiling work eliminated |
| `geometryTotalMs` | ~104–123 ms / 20 s | ~5–6 ms/s of rebuild CPU |
| `overlayRefreshCalls` | 0 | No selection flash in this workload |

Compared with the rejected first attempt (same workload, room teardown still on): measure window still spent ~126–278 ms in geometry rebuilds **with** room expansion and still skipped ceiling — gating rooms is what recovered FPS and visual stability.

## Correctness

- Dig/claim/destroy still set `rebuildRooms` via terrain-id delta against `lastTerrainIds`.
- Adjacent room merge/split still runs when terrain changes (surrounding room collection unchanged on that path).
- Possession still forces a ceiling rebuild if dirtied while hidden.
- Flash Tint clear uses Vertex `Tint`/`TintOverlay` → `BlackNoAlpha` (no geometry rebuild).

## How to reproduce

```bash
./gradlew benchmarkBaseline -PbenchmarkArgs="--out baselines/performance/after-tile-rebuild-opt --no-update-visual-refs --no-official --compare baselines/performance/official-baseline-windows-10-amd64.json"
```

## Verdict

- **Architectural source addressed:** FOW-driven room teardown + hidden ceiling rebuild + flash geometry path.
- **Measured win:** median avg FPS **+11.9%** vs official, **+4.9%** vs prior lighting-alloc candidate; P99 **−37%** vs official; **all** visual checkpoints passed on all 3 runs.
- **Diagnostics:** measure-window room rebuild count **0**; ceiling rebuild count **0** while hidden.
- **Baselines:** official JSON untouched; candidate + regression under `after-tile-rebuild-opt/` (including the rejected first session for audit).
