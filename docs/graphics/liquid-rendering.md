# Water and lava rendering

OpenKeeper uses the shared `DkLighting/Vertex.j3md` material for water and lava. Enhanced liquids add texture stages to the existing terrain shader, preserving its lighting, fog-of-war visibility, and alpha handling without maintaining a second lighting implementation.

## Contributions and architecture

This consolidation combines work from [PR #852](https://github.com/tonihele/OpenKeeper/pull/852) by `belgianguy` and [PR #1278](https://github.com/tonihele/OpenKeeper/pull/1278) by `adrian-andrzjewski`.

- #852 contributed the recovered three-by-three liquid mesh, lava wave and normal profile, Classic Temple pools, and Model Viewer comparison control.
- #1278 contributed loose-texture conversion, persisted configuration and pause-menu integration, GPU-side water-frame selection, and the initial runtime/performance evidence.
- The consolidated path combines these with the shared terrain shader, GPU wave evaluation, per-liquid resource fallback, and Classic compatibility default.

## Modes and compatibility

`Settings.Setting.ENHANCED_LIQUIDS` is saved with the normal graphics settings and defaults to `false` (Classic). The main menu selects the mode for the next game start. The pause menu and Model Viewer can change it while a map is active; `LiquidSurfaceControl` applies the change on the next scene update without rebuilding lighting registrations.

`Dk2EmbmResources` loads and caches water and lava resources independently. Missing or unreadable resources leave that liquid on its Classic material and produce a diagnostic. Enhanced water uses a 32-layer bump `TextureArray`, so water remains Classic if the renderer lacks texture-array support. Lava uses a regular 2D bump texture and does not need that capability. `ConvertTextures` extracts the normal texture archives and copies the named liquid maps from installations that ship them loose in `Data/EngineTextures`. No DKII commercial textures are stored in the repository.

Temple pools pass `allowEnhanced=false`. `RoomConstructor` keeps their animated surface outside its static floor batch so its opaque Classic appearance, texture animation, and lighting binding remain intact. The ordinary map path passes the existing `TerrainDeformer` to `Water.construct`.

## Visual and animation behavior

Classic water retains its existing animated diffuse texture and four-vertex tiles. Enhanced water uses `BumpWater0.png` for base colour, preserves the Classic diffuse alpha, adds GPU waves, and advances the 32-frame bump array at 8 frames per second (a four-second cycle). It switches to a prebuilt three-by-three grid only in Enhanced mode.

Lava uses the recovered three-by-three sample grid in both modes. `TexCoord2` stores stable map-grid phase coordinates before shore deformation, so duplicated tile edges receive matching wave heights and normals. The GPU evaluates two 0.04-amplitude waves and their matching normal field using the shared jME shader clock; no per-frame CPU vertex-buffer upload is needed. The mesh bounds include the maximum displacement. Enhanced water uses the water-specific spatial frequency carried over from #852; native water samples have not independently confirmed that value.

The Enhanced fragment path follows the original three-stage structure: base colour multiplied by existing diffuse lighting, bump-dependent environment lookup, and additive environment colour. `Dk2EmbmResources` converts the source grayscale bump fields to wrapped central differences once at load time, stores the signed offsets in linear RG, and retains lava alpha for the luminance variant. The exact native height-to-offset conversion, filtering, environment coordinates, and strengths remain unverified. The environment lookup uses sphere mapping as an approximation. Exact G400 pixel parity is not claimed.

EMBM adds a bump-distorted environment reflection; it does not add emission or post-process bloom. Issue [#22](https://github.com/tonihele/OpenKeeper/issues/22) remains a separate bloom request.

## Performance

Classic water uses four vertices per tile; Enhanced water uses nine. Lava uses nine in both modes and replaces CPU-side per-frame displacement with GPU evaluation. Enhanced mode adds base, bump, and environment texture reads. A 128-by-128 RGBA water bump array uses about 2 MiB before driver alignment; lava and base/environment maps add roughly 0.2 MiB.

The available A/B measurement is from the pre-consolidation #1278 prototype on an RTX 2060 SUPER at 1920x1080 with VSync off. Each mode had 35 valid samples.

| Mode | Process GPU 3D average | Java working set |
| --- | ---: | ---: |
| Classic | 26.14% | 936.3 MiB |
| EMBM | 27.72% | 957.0 MiB |

That run showed a +1.58 percentage-point GPU-utilization difference (about 6% relative) and +20.7 MiB working set. It is not a GPU-time measurement, was not repeated statistically, and does not measure the consolidated mesh and animation path. A controlled timer-query comparison on small and large liquid maps remains follow-up work.

## Verification and limitations

The consolidated branch passed `gradlew --no-daemon clean test build`: 1,396 tests, zero failures or errors, and 4 skipped. `glslangValidator` compiled the GLSL 1.50 vertex and fragment stages for Classic animated water, Enhanced water with its texture-array sampler, and Enhanced lava with its 2D bump sampler, using jME's shader libraries and the project's falloff library.

A fresh gameplay render could not be run from this worktree because the local application setup had no valid DKII installation path; startup stopped at the first-run folder selector. The existing #1278 captures cover the prototype's Level 7 lava and Level 5 water, but their cameras are not aligned and they do not validate the consolidated mesh path. Native comparison still needs matching camera, resolution, lighting, fog state, and animation timestamp for water, lava, Temple pools, and Model Viewer output. No exact DKII pixel parity is claimed.
