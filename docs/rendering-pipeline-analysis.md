# OpenKeeper Rendering Pipeline — Architecture Review

**Date:** 2026-11-06  
**Scope:** End-to-end rendering analysis (architecture, algorithms, fidelity, performance, resources, hardware)  
**Method:** Static codebase analysis of OpenKeeper (jMonkeyEngine 3.9 + LWJGL3 OpenGL). No GPU profiler capture was taken in this session; items needing measurement are marked.

---

## Verdict

OpenKeeper’s world look is driven by a solid, DK-faithful custom lighting path (`DkLighting` + shared `LightData`), not stock jME Phong. The biggest risks are:

1. **Per-frame CPU work on every entity** (`applyVertexLight`)
2. **Catastrophic mesh rebuilds on tile changes** (`MapViewController.updateTiles`)
3. A few **correctness / visual bugs** (soft-falloff mismatch, translucent filter coupling)
4. **Forced max settings (16× MSAA)** that tax the GPU before the custom pipeline is tuned

---

## Architecture snapshot

| Layer | Mechanism | Assessment |
|--------|-----------|------------|
| API | OpenGL via LWJGL3 / jME 3.9 | Fine for fidelity goal; no Vulkan / DirectX / Metal path |
| Terrain / rooms | 8×8 `BatchNode` pages + room constructors | Good draw-call reduction; painful to mutate |
| Lighting | CPU light list → RGBA32F `LightData` → GLSL per-vertex loop | Authentic DK model; cost scales with lights × vertices |
| Entities | `applyVertexLight` every frame | Correct for flicker coupling; expensive as written |
| Post | Optional SSAO + `TranslucentBucketFilter` | Useful; incorrectly coupled |
| Shadows | Modes tagged; no in-game shadow maps | Intentional / incomplete (ModelViewer only) |

### Pipeline overview

```
DK2 assets → convert → j3o / textures
         ↓
MapViewController builds tile / room meshes (BatchNode)
         ↓
Materials swapped to DkLighting Vertex / Flat (before batch)
         ↓
LightRules owns LightSources (torches, terrain, objects, cursor…)
         ↓
each frame: LightModulators → SceneLights uploads LightData + AmbientMap
         ↓
Vertex.vert lights terrain per-vertex; entities bind shared LightData
         ↓
optional SSAO + TranslucentBucketFilter (Main.setViewProcessors)
         ↓
Nifty HUD / minimap (mostly Unshaded)
```

### Key files

| Area | Paths |
|------|--------|
| Boot / post | `src/main/java/toniarts/openkeeper/Main.java` |
| Settings | `src/main/java/toniarts/openkeeper/game/data/Settings.java` |
| Map view | `view/PlayerMapViewState.java`, `view/map/MapViewController.java`, `Water.java`, `Ceiling.java`, `construction/RoomConstructor.java` |
| Lighting | `view/lighting/{SceneLights,LightRules,LightConstants,LightSource}.java` |
| Materials | `utils/AssetUtils.java`, `assets/MatDefs/DkLighting/*`, `assets/Shaders/DkLighting/*` |
| Entities / FX | `view/PlayerEntityViewState.java`, `view/control/*`, `view/effect/*` |

---

## Custom lighting model (core)

Two techniques under `assets/MatDefs/DkLighting/`:

1. **`Vertex.j3md`** — terrain / rooms  
   - Shared `LightData` texture (RGBA32F, up to **3084** lights): header + 3 texels/light  
   - `AmbientMap`: per-tile ambient + explored flag, bilinearly blended  
   - Shader loops lights per vertex: DK falloff, fog, torch override, soft-clamp  

2. **`Flat.j3md`** — legacy flat evaluation (CPU `computeFlatLight`)  
   - Entities increasingly use **Vertex** via `AssetUtils.applyVertexLight`

**Preserve:** shared `LightData` (avoids tile-border light seams), exact `dkFalloff` behaviour (including documented out-of-range limb), FoW gating.

**Deliberate deviations from original DK** (do not “fix” without A/B): soft falloff tail, torch edge tint, soft-clamp of summed light, optional min ambient floor.

---

## Prioritized findings

Priority order used:

1. **Quick wins** — low effort, high impact  
2. **Severity** — correctness, stability, quality, performance  
3. **Long-term** — architecture and advanced techniques  

---

### 1. Quick wins

#### Q1 — Cap MSAA; stop forcing Ultra every launch

| | |
|--|--|
| **Problem** | `Settings.applyMaximumGraphicsSettings()` always forces Samples=16, SSAO on, AF 16, etc., overwriting user prefs. 16× MSAA is among the most expensive desktop GL quality toggles and rarely beats 4× at 1080p. |
| **Evidence** | `Settings.java` → `applyMaximumGraphicsSettings()` |
| **Solution** | Remove force-every-launch (or gate behind a one-shot preset). Default MSAA to **4**; keep 8/16 optional. Persist real user settings. |
| **Benefits** | Large GPU frame-time win; restores settings UX |
| **Complexity** | S |
| **Risk** | Low |
| **Affected code** | `game/data/Settings.java` |

#### Q2 — Soft-falloff constant mismatch (Java 0.01 vs GLSL 0.1)

| | |
|--|--|
| **Problem** | Comments require `LightConstants.SOFT_FALLOFF_TAIL_TILES` == `DK_SOFT_FALLOFF_TAIL_TILES`. They disagree → CPU frustum reach / `computeFlatLight` disagree with GPU falloff. |
| **Evidence** | `LightConstants.SOFT_FALLOFF_TAIL_TILES = 0.01f`; `DkFalloff.glsllib` `#define DK_SOFT_FALLOFF_TAIL_TILES 0.1` |
| **Solution** | Single shared value (prefer **0.1**, matching shader comments’ deliberate soften). |
| **Benefits** | Correctness + visual consistency (terrain vs entities / culling) |
| **Complexity** | S |
| **Risk** | Low (slightly larger light pools) |
| **Affected code** | `view/lighting/LightConstants.java`, `assets/Shaders/DkLighting/DkFalloff.glsllib` |

#### Q3 — Decouple `TranslucentBucketFilter` from SSAO

| | |
|--|--|
| **Problem** | `Main.setViewProcessors()` only adds `TranslucentBucketFilter` when SSAO is on. Torches/particles in the Translucent bucket lose correct depth ordering when SSAO is off. |
| **Evidence** | `Main.setViewProcessors()`; torch `Bucket.Translucent` + “flames after water” comments |
| **Solution** | Always attach `TranslucentBucketFilter`; add SSAO as an optional extra filter. |
| **Benefits** | Correct particle / water compositing at all quality presets |
| **Complexity** | S |
| **Risk** | Low |
| **Affected code** | `Main.java` |

#### Q4 — Fix vertex light loop bound

| | |
|--|--|
| **Problem** | `Vertex.vert` loops `for (i = 0; i < 3084; i++)` with an inner `break`. A compile-time trip count of 3084 is a classic driver / occupancy hazard. |
| **Evidence** | `Vertex.vert`; `SceneLights.MAX_LIGHTS = 3084` |
| **Solution** | `for (int i = 0; i < numLights; ++i)` using header count. Optionally lower `MAX_LIGHTS` after measuring near-camera peaks (e.g. 256–512). |
| **Benefits** | Lower terrain vertex shader cost |
| **Complexity** | S |
| **Risk** | Low |
| **Affected code** | `assets/Shaders/DkLighting/Vertex.vert` (± `SceneLights.java` if cap changes) |

#### Q5 — Stop per-frame entity scene-graph walks

| | |
|--|--|
| **Problem** | `PlayerEntityViewState.update` calls `AssetUtils.applyVertexLight` on every object / creature / door / trap **every frame** (DFS + `ArrayList`). `bindMaterials` itself is mostly a no-op after first bind (`LightData`/`AmbientMap` set only if null); the cost is repeated traversals and allocations, not texture rebinding. |
| **Evidence** | `PlayerEntityViewState` (~249–281); `AssetUtils.applyVertexLight`; `SceneLights.bindMaterials` |
| **Solution** | Convert + bind once on spawn; per-frame only refresh thing light positions (`trackedLights`). Keep global light upload in `LightRules.update`. |
| **Benefits** | Likely top CPU win in gameplay (**hypothesis until profiled**; path is clearly O(entities × geoms) / frame) |
| **Complexity** | S–M |
| **Risk** | Medium (must not miss first AmbientMap bind / `IgnoreObjectLights`) |
| **Affected code** | `PlayerEntityViewState.java`, `AssetUtils.java` |

#### Q7 — Cut per-frame lighting allocations

| | |
|--|--|
| **Problem** | Hot path allocates every frame: `new ColorRGBA(ambient…)` in `PlayerMapViewState`, `ownerPosition.add(offset)` for thing lights, occasional `activeSources()` copies. |
| **Evidence** | Deep audit of `PlayerMapViewState.update`, thing-light position updates, `LightRules.activeSources` |
| **Solution** | Reuse mutable `ColorRGBA` / `Vector3f` (`set` in place). |
| **Benefits** | Lower GC pressure with many creatures / torches |
| **Complexity** | S |
| **Risk** | Low |
| **Affected code** | `PlayerMapViewState.java`, `LightRules.java` / thing-light helpers |

#### Q6 — Drop or isolate unused root `AmbientLight`

| | |
|--|--|
| **Problem** | White `AmbientLight` on `rootNode` is a BatchNode workaround; world shading uses `DkLighting`. |
| **Evidence** | `Main` onLoadComplete FIXME |
| **Solution** | Verify no in-game geometry still uses `Lighting.j3md`; remove or isolate the light. |
| **Benefits** | Cleaner lighting ownership |
| **Complexity** | S |
| **Risk** | Medium if something still depends on it |
| **Affected code** | `Main.java` |

---

### 2. Severity

#### S1 — Tile / room update rebuild storm *(critical performance)*

| | |
|--|--|
| **Problem** | `updateTiles` expands to whole rooms + surroundings, tears down rooms, `handleTile` per point, re-`batch()` pages, and **always** `refreshCeilingSurface()`; water/lava rebuild when batch counts change. Author FIXME: “really quite heavy… quick fix”. |
| **Evidence** | `MapViewController.updateTiles` (~491–571) |
| **Impact** | Digging, claiming, FoW reveal → frame spikes / hitching |
| **Solution** | (1) Dirty ceiling only when openness/height set changes. (2) Rebuild water/lava only for affected components. (3) Prefer partial room rebuilds. (4) Long-term: chunk mesh pools instead of mutating BatchNodes. |
| **Benefits** | Removes gameplay hitching under construction / combat digs |
| **Complexity** | M→L |
| **Risk** | High (visual seams, FoW bugs) |
| **Affected code** | `MapViewController.java`, room constructors, `Ceiling.java`, `Water.java` |

#### S2 — Per-vertex × many lights on batched terrain *(GPU)*

| | |
|--|--|
| **Problem** | Every terrain vertex may evaluate all uploaded near-camera lights. Dense torch corridors + large pages ⇒ high VS cost. |
| **Evidence** | `SceneLights.update` + `Vertex.vert` loop; pages are 8×8 batched |
| **Solution** | After Q4: per-page light index lists (UBO/SSBO) or clustered forward **keeping `dkFalloff`**. Do not split to per-material lists without a continuity plan (shared LightData exists specifically to avoid border seams). |
| **Benefits** | Scales lighting with content density |
| **Complexity** | L |
| **Risk** | Medium |
| **Affected code** | `SceneLights`, shaders, page bind path |

#### S3 — AmbientMap whole-texture dirty on every tile write

| | |
|--|--|
| **Problem** | `setTileAmbient` marks the entire AmbientMap `setUpdateNeeded()` per tile. FoW floods re-upload full RGBA8 repeatedly. |
| **Evidence** | `SceneLights.setTileAmbient` |
| **Solution** | Frame-coalesced single upload; or dirty rectangles / subimage updates if jME allows |
| **Benefits** | Fewer FoW-related CPU→GPU spikes |
| **Complexity** | M |
| **Risk** | Low–Med |
| **Affected code** | `SceneLights.java` |

#### S4 — Forced max settings vs hardware utilization

| | |
|--|--|
| **Problem** | 16× MSAA + SSAO + high FPS target fights custom lighting cost. Fine on RTX 2060 SUPER; harsh on mid / iGPU. |
| **Evidence** | Settings force path; `MAX_FPS = 200` |
| **Solution** | Quality presets (Low/Med/High/Ultra) for MSAA / SSAO / AF / flicker; do not force Ultra in constructor |
| **Benefits** | Predictable performance across hardware |
| **Complexity** | S–M |
| **Risk** | Low |
| **Affected code** | `Settings.java`, pause / main-menu graphics UI |

#### S5 — LightRules load race (guarded but fragile)

| | |
|--|--|
| **Problem** | Map load vs entity lighting CME risk is gated by `isMapLoaded()`; new callers that skip the gate reintroduce races. |
| **Evidence** | `PlayerMapViewState` / `PlayerEntityViewState` comments |
| **Solution** | Single public API with thread assertions; freeze material registration after load |
| **Benefits** | Stability |
| **Complexity** | M |
| **Risk** | Low |
| **Affected code** | `LightRules.java`, map / entity view states |

#### S6 — Visual fidelity debt (know it; don’t blindly “fix”)

- Decay textures: FIXME on material model (`setTileMaterialToGeometries`)
- Soft-clamp / torch edge / soft tail are deliberate — preserve unless A/B proves better
- No in-game shadow maps: classic DK2 is not a modern shadow-mapped look; optional “enhanced” only

---

### 3. Long-term

#### L1 — Mesh architecture beyond BatchNode

Replace mutable `BatchNode` pages with immutable chunk meshes + pooled rebuilds (or GPU tile instances). Solves S1 at the root.  
**Complexity:** L · **Risk:** High

#### L2 — Lighting evolution (keep DK math)

1. Per-page light index lists (best fidelity-preserving step)  
2. Clustered forward with shared `dkFalloff`  
3. Avoid full deferred / PBR unless art direction changes  

#### L3 — Optional enhanced mode (not default)

TAA vs MSAA, HBAO, optional shadows — shipped as a preset fork; keep **classic** default for remake fidelity.

#### L4 — Instrumentation (required before big bets)

GPU timer queries (terrain VS), CPU timers for `updateTiles` / `applyVertexLight` / `SceneLights.update`, light-count HUD, draw-call counters. Use existing F6 `DetailedProfilerState` as a start.

#### L5 — Material pipeline cleanup

Stop assigning Normal/Spec/Parallax into defs that don’t use them; unify tint/highlight paths; finish room constructor static-style FIXMEs.

---

## Actionable roadmap

| Priority | Item | Effort | Impact | Depends on |
|----------|------|--------|--------|------------|
| 1 | Q1 Remove forced Ultra / default MSAA 4 | S | High GPU | — |
| 2 | Q2 Sync soft-falloff to 0.1 | S | Correctness | — |
| 3 | Q3 Always TranslucentBucketFilter | S | Visual bugs | — |
| 4 | Q4 Dynamic light loop bound | S | GPU VS | — |
| 5 | Q5 One-shot entity Vertex bind | M | High CPU | Measure |
| 6 | Q7 Reuse ColorRGBA / Vector3f in light hot path | S | GC | — |
| 7 | S4 Quality presets | M | UX + perf | Q1 |
| 8 | S3 Coalesce AmbientMap uploads | M | FoW spikes | — |
| 9 | S1 Incremental ceiling/water + milder tile updates | M–L | Hitching | Design |
| 10 | L4 Profiling HUD / timers | M | Enables L2 | — |
| 11 | S2 / L2 Clustered or per-page lights | L | Scale | L4 |
| 12 | L1 Chunk mesh architecture | L | Architecture | S1 lessons |
| 13 | L3 Optional “enhanced” post / shadows | L | Quality fork | Presets |

---

## Addendum — deep audit notes

From [Deep rendering audit](21d6408f-feea-4675-8c34-7668fa3bb04b):

- **Water/lava** rebuild only when batch-*list size* changes (not every tile touch); **ceiling** still rebuilds on every `updateTiles`.
- **`applyFlatLight` / `computeFlatLight`**: no gameplay call sites under `src/main/java` — entity path is Vertex; don’t optimize Flat first.
- **Anisotropy** is applied at `assetRequested` only — changing AF at runtime won’t retouch already-loaded textures without reload.
- **Force-max graphics** in `Settings` is a **local uncommitted** change from this session — remove before any upstream contribution.
- LightData footprint is small (~9253×1 RGBA32F ≈ 148 KB); the cost is the VS loop, not VRAM.

---

## What to preserve

- Shared `LightData` (tile-border light continuity)
- Exact `dkFalloff` table behaviour
- FoW explored gating (shader + CPU fog light cull)
- Pre-batch Vertex material swap discipline
- Classic look as default unless a change **demonstrably** improves fidelity or performance without breaking authenticity

---

## Suggested first profiling session

Tools: RenderDoc / Nsight / jME `DetailedProfilerState` (F6)

1. Idle camera, torch-dense corridor  
2. Same + dig ~10 tiles (catch `updateTiles` hitch)  
3. CPU time: `applyVertexLight` vs `SceneLights.update`  
4. VS time before / after Q4  

---

## Bottom line

Fix **settings force**, **soft-falloff sync**, **translucent coupling**, **shader loop bound**, and **entity bind cadence** first. Then attack **tile rebuilds** with measurement. Defer deferred / PBR / shadow overhauls—they fight the remake’s lighting identity unless shipped as an explicit enhanced mode.
