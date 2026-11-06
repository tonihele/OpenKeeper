# VS LightData early distance-reject — **REJECTED**

**Date:** 2026-11-06  
**Roadmap item:** High-preset GPU follow-up (full-list Vertex fetch cost)  
**Decision:** **REJECTED** — source reverted  
**Workload:** `late-campaign-dense` (Level20, seed 42), 3× suite, **High** (SMAA Ultra)  
**Hardware:** Windows 10 amd64, NVIDIA GeForce RTX 2060 SUPER  
**Git revision:** `73f60937` (+ candidate shader patch, then reverted)

## What was tried

In `DkFalloff.glsllib`, add `dkLightOutOfRange` using the same soft-tail `r2`/`d2` test as `dkPointLightContribution`. In both `Vertex.vert` light loops (page-index and full-list), fetch `posRadius` first and `continue` before colour/flags `texelFetch` when out of range.

Hypothesis: rooms/entities/water still loop ~2.1–3.0k uploaded lights; most are out of radius for a given vertex, so skipping 2 RGBA32F fetches per miss should cut VS cost → lower `gpuFrameTimeMs` without changing lit pixels.

## Suite session

**Control:** `baselines/performance/control-high-before-vs-distance-reject/session-2026-11-06T15-10-43.664171900Z`  
**Candidate:** `baselines/performance/after-vs-distance-reject/session-2026-11-06T15-13-16.758652200Z`

| Run | Control avg FPS | Control gpu ms | Candidate avg FPS | Candidate gpu ms | visual (cand) |
|-----|-----------------|----------------|-------------------|------------------|---------------|
| 1 | 39.709 | 8.994 | 39.430 | 10.115 | 3/3 |
| 2 | 36.203 | 9.692 | 41.782 | 9.881 | 3/3 |
| 3 | 34.945 | 9.890 | 40.747 | 9.978 | cp01 fail |

| Metric (median) | Control | Candidate | Δ |
|-----------------|---------|-----------|---|
| avg FPS | 36.203 | 40.747 | +12.6% (reported improvement) |
| 1% low FPS | 4.840 | 5.378 | +11.1% |
| frame time P50 | 20.890 | 18.232 | −12.7% |
| **gpuFrameTimeMs** | **9.692** | **9.978** | **+0.29 ms (wrong way)** |
| lightsAvg | ~2211 | ~2203 | stable |
| visualAllPassed | false (cp01) | false (cp01) | known flake |
| regressionCount | — | **0** | |
| control CV(avgFps) | **5.5%** | candidate CV 2.4% | unstable control |

## Interpretation

1. FPS and frame-time medians moved up, but the **hypothesized lever did not**: `gpuFrameTimeMs` was flat-to-worse.
2. Control runs declined 39.7 → 36.2 → 34.9 (CV 5.5%), so the paired Δ is not a trustworthy attribution to the shader.
3. cp01 failed once on both sides; cp00/cp02 stayed within hardware tolerance — not treated as a lighting regression by itself.
4. Without a `gpuFrameTimeMs` (or F1 VS) win, this does not meet “verified performance benefit” for High accept. Reverted pending F1 / stabler thermal pairing if revisited.

## Lesson

Shader fetch early-outs are still plausible after **F1** shows full-list VS dominate, but suite accept must see the GPU timer move (same bar as SMAA final-pass), not only avg FPS on a high-CV control.

## Artifacts

- Control: `baselines/performance/control-high-before-vs-distance-reject/`
- Candidate: `baselines/performance/after-vs-distance-reject/` (`VERDICT.json` → **REJECTED**)
