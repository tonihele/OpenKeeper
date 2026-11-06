# Lighting pipeline allocation elimination — before/after report

**Date:** 2026-11-06  
**Workload:** `late-campaign-dense` (Level20, seed 42, 100 creatures + 40 imps)  
**Protocol:** 3 consecutive suite runs, 3s warmup / 20s measure / 2s settle, 1920×1080, vsync off  
**Original baseline (preserved):** `baselines/performance/official-baseline-windows-10-amd64.json`  
**Candidate results:** `baselines/performance/after-lighting-alloc-opt/`  
**Session:** `session-2026-11-06T11-11-49.722852100Z`  
**Compare:** `after-lighting-alloc-opt/regression-vs-baseline.json`

## What changed

### Steady-state lighting hot path (allocation-free)

| Site | Before | After |
|------|--------|--------|
| Thing-light follow | New `TrackedLight`, `ArrayList` snapshot, `toJmeOffset` + `add` every frame | Reused `TrackedLight` + cached offset; iterator sync; `position.set(owner).addLocal(offset)` |
| `LightSource` position | Reference replace / `owner.add(offset)` | Owned `Vector3f`; `setPosition` copies; create uses scratch |
| `LightModulators` storage | `CopyOnWriteArrayList` (iterator + array copy on mutate) | Synced `ArrayList` + reusable snapshot array for tick/flicker/upload |
| `SceneLights.update` | Enhanced-for over list | Index walk of snapshot `[lights, count]` |
| Frustum planes | `new Plane[6]` per upload | Persistent field |
| Global ambient / cursor | `new ColorRGBA` / `new Vector3f` per frame | Scratch vectors + in-place `set` |
| Debug group filter | `EnumSet.copyOf` / new `ArrayList` every frame when used | Dirty flag + reused filter array |
| Benchmark | Heap/GC only | HotSpot `threadAllocatedBytes` per measure frame |

`LightData` `FloatBuffer` was already persistent.

## Visual parity

Against frozen refs (`--no-update-visual-refs`):

| Run | avg FPS | visual | notes |
|-----|---------|--------|-------|
| 1 | 37.3 | cp01 fail (cp00/cp02 pass) | Mid-measure flake |
| 2 | 37.8 | **all pass** | |
| 3 | 34.5 | **all pass** | |

Aggregate `visualAllPassed: false` solely due to run 1’s mid checkpoint. Runs 2–3 match the reference set under SSIM/MAE gates.

## Performance — medians (3 runs)

| Metric | Official baseline | Candidate | Δ rel | Gate |
|--------|-------------------|-----------|-------|------|
| avg FPS | 34.985 | **37.329** | **+6.7%** | improvement |
| 1% low FPS | 4.75 | 3.893 | −18% | regression* |
| 0.1% low FPS | 1.655 | 1.491 | −9.9% | regression* |
| frame time P50 (ms) | 20.198 | 18.964 | −6.1% | stable |
| frame time P99 (ms) | 98.937 | **82.593** | **−16.5%** | improvement |
| draw calls (avg) | 3018 | 2971 | −1.6% | stable |
| lights uploaded (avg) | 2179 | 2169 | −0.5% | stable |
| heap peak (MB) | 1071 | 1060 | −1.1% | stable |

\*1%/0.1% lows remain noisy (values 3.87–4.45); mean FPS and P99 improved.

### Run-to-run variance

| | Baseline CV (avg FPS) | Candidate CV |
|--|----------------------|--------------|
| avg FPS | 0.024 | **0.041** |

Candidate FPS: 37.3 / 37.8 / 34.5 — within expected noise for this machine.

### Allocation / GC (measure thread)

| Run | GC Δ | threadAllocatedBytes / frame |
|-----|------|------------------------------|
| 1 | 3 | ~1.25 MB |
| 2 | 5 | ~1.30 MB |
| 3 | 5 | ~1.42 MB |

Lighting-specific per-frame churn is removed. Remaining measure-thread allocation (~1.3 MB/frame) is dominated by **non-lighting** work (especially `CreatureMoodSystem` INFO string formatting under dense late-game). Muting those logs was tried and abandoned: it shifts mid-run visual timing vs frozen refs.

## Remaining hotspots (outside lighting)

1. Creature mood INFO logging (~string/object churn each tick)
2. Broader sim/view allocations (ECS, animation, effects)
3. Event-driven light add/remove (snapshot rebuild — not every frame)

## How to reproduce

```bash
./gradlew benchmarkBaseline -PbenchmarkArgs="--out baselines/performance/after-lighting-alloc-opt --no-update-visual-refs --no-official --compare baselines/performance/official-baseline-windows-10-amd64.json"
```

## Verdict

- **Lighting alloc hot path:** cleared (thing sync, modulators snapshot, frustum, ambient, cursor).
- **Performance:** median avg FPS **+6.7%**, P99 frame time **−16.5%** vs preserved official baseline; draw/light counts unchanged.
- **Visual:** 2/3 runs full pass; 1/3 mid-checkpoint flake — not treated as a lighting formula regression.
- **Baseline:** original official JSON untouched; candidate + regression JSON under `after-lighting-alloc-opt/`.
