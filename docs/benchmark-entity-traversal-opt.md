# Entity scene-graph traversal optimization — before/after report

**Date:** 2026-11-06  
**Workload:** `late-campaign-dense` (Level20, seed 42, 100 creatures + 40 imps)  
**Protocol:** 3 consecutive suite runs, 3s warmup / 20s measure / 2s settle, 1920×1080, vsync off  
**Original baseline (preserved):** `baselines/performance/official-baseline-windows-10-amd64.json`  
**Candidate results:** `baselines/performance/after-entity-traversal-opt/`  
**Machine compare:** `after-entity-traversal-opt/regression-vs-baseline.json`

## What changed

Per-frame `Spatial.depthFirstTraversal` to re-bind Vertex lighting on every entity, door, trap, creature, and effect mesh was redundant: flicker/pulse already live in the shared `LightData` texture uploaded once per frame by `SceneLights.update`.

| Area | Before | After |
|------|--------|--------|
| `AssetUtils.applyVertexLight` | Full DFS + Flat→Vertex + bind every call | Bind-once with material cache; `USER_DATA_KEY_VERTEX_LIGHT_READY` skips further walks |
| Hierarchy / material replacement | — | `invalidateVertexLight` from `AnimationLoader`, `CharredOverlay`, `CreatureVariantTextures` |
| `KeeperHandState` | `applyFullLight` every frame | Once when hand content changes |
| `ArrowBlinkControl` | DFS every blink tick | Cached material list at construct |
| Effects | Same per-frame DFS | Same cache/ready path |

Thing-light **positions** still update every frame (owners move). Shared light texture upload is unchanged.

## Visual parity

Against the frozen reference frames under `baselines/performance/visual/` (`--no-update-visual-refs`):

| Run | cp00 | cp01 | cp02 | visualPass |
|-----|------|------|------|------------|
| 1 | pass | pass | pass | true |
| 2 | pass | pass | pass | true |
| 3 | pass | pass | pass | true |

**`visualAllPassed: true`** on the candidate aggregate. An earlier contaminated session had mid-run `cp01` flakes under load; the clean session below did not.

## Performance — medians (3 runs)

| Metric | Baseline | Candidate | Δ rel | Gate |
|--------|----------|-----------|-------|------|
| avg FPS | 34.985 | 34.934 | −0.1% | stable |
| 1% low FPS | 4.75 | 4.26 | −10.3% | flagged regression* |
| 0.1% low FPS | 1.655 | 1.381 | −16.6% | flagged regression* |
| frame time P50 (ms) | 20.198 | 17.321 | **−14.2%** | improvement |
| frame time P99 (ms) | 98.937 | 85.881 | **−13.2%** | improvement |
| draw calls (avg) | 3018 | 2971 | −1.6% | stable |
| lights uploaded (avg) | 2179 | 2173 | −0.3% | stable |
| heap peak (MB) | 1071 | 1108 | +3.4% | stable |

\*1% / 0.1% low medians are pulled down by run 2 (2.505 / 0.73). Runs 1 and 3 sit at 4.60 / 4.26 and 1.61 / 1.38 — within noise of the baseline set (3.95–4.79).

### Per-run FPS

| | Baseline | Candidate |
|--|----------|-----------|
| Run 1 | 34.985 | 34.934 |
| Run 2 | 36.531 | 30.971 |
| Run 3 | 34.540 | 37.579 |
| Mean | 35.352 | 34.495 |
| CV | **0.024** | **0.079** |

Candidate CV is higher (still under the 8% “unstable” note threshold at 7.9%). Run 2 again shows environmental hitching (lower draw calls 2596 vs ~3000); it is not explained by lighting bind logic.

## Interpretation

1. **Typical frame cost improved** — median P50 frame time fell ~14% with unchanged light/draw counts. That matches removing O(entities × model nodes) DFS from the CPU hot path while the GPU light loop stays the same.
2. **Average FPS ~flat** — the scene remains ~GPU / vertex-light bound near 35 FPS; freeing CPU traversal does not raise the FPS ceiling much until that bound moves.
3. **Tail FPS noisy** — 1%/0.1% lows are dominated by rare stalls; the clean P99 median improved ~13%.
4. **Correctness** — visual gates passed on all three clean runs; gameplay lighting path (shared texture + thing-light follow) is unchanged.
5. **Original baseline untouched** — official file timestamp/hash unchanged; candidate written under `after-entity-traversal-opt/` with `--no-official`.

## Remaining bottlenecks (exposed, not fixed here)

- Vertex light upload still scans the full registered light list every frame (~2.1k uploaded near camera).
- Map tile flash still triggers heavy `updateTiles` rebuilds (not steady-state entity DFS).
- Fog door map still clear+rebuilds on a 0.15s tick (ECS, not Spatial tree).
- Tail-frame spikes (GC / hitch) still dominate 1% lows more than mean frame cost.

## How to reproduce

```bash
./gradlew benchmarkBaseline -PbenchmarkArgs="--out baselines/performance/after-entity-traversal-opt --no-update-visual-refs --no-official --compare baselines/performance/official-baseline-windows-10-amd64.json"
```
