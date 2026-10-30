# OpenKeeper liquid modes — implementation notes (#851)

Local implementation of Classic and Enhanced rendering for water and lava.

- Graphics settings: **Enhanced water and lava**, off by default (Classic).
- Model viewer: **Enhanced liquids** in the map controls; changes apply live.
- Both modes retain the existing terrain light/ambient/fog inputs. Shared
  vertex-lighting code was moved, unchanged, into `TerrainLighting.glsllib`;
  ordinary terrain and liquid vertex shaders call that same calculation.
- Liquid tiles use a 3x3 grid. Lava uses the recovered two-sine heights and
  changing normals in both modes. Classic water stays flat with its existing
  animated texture; enhanced water uses waves and an animated bump sequence.
- A shared monotonic animation clock prevents new/rebuilt surfaces from
  restarting the waves. Native geometry uses a frame counter; OpenKeeper uses
  a 30-tick-per-second reference with continuous interpolation, independent of
  rendering FPS. Wall-clock timing is an adaptation, not measured native parity.
- Enhanced rendering uses the original base and environment textures, and a
  displacement atlas derived from the original bump heights. The fragment
  shader applies lit base + perturbed environment, gated by explored terrain.
- Source lookup checks converted textures, then the configured original
  installation's loose `Data/EngineTextures/` PNGs, with case-correct lookup.
  The original textures are not copied into the source repository. Missing
  assets fall back to Classic for the affected liquid and log one diagnostic.
- Colour maps use sRGB; displacement uses linear data. Water's 32-frame atlas
  uses no mipmaps and constrains filtering within each frame. Switching back
  restores the classic texture/animation while retaining shared light bindings.

## Verification

`LiquidSurfaceTest` covers 72 captured native lava height/normal samples,
3x3 mesh density and independent tile UVs, neutral/gradient displacement,
frame wrapping, phase periodicity, and fallback for missing maps of both liquids.
Those six tests and all 24 upstream lighting tests pass (30 total) on the
isolated PR branch.

The offscreen OpenGL probe `CheckLiquidRendering.java` loads real level data
and original textures and renders both liquids. It verifies Classic -> Enhanced
-> Classic, shader variants with and without ambient/light-data maps, and the
ordinary terrain shader after the shared-code extraction. Enhanced rendering
changes more than 56,000 pixels in the test view. With unexplored ambient-map
alpha zero, all RGB pixels remain black; the environment map does not reveal fog.
Both Nifty XML documents parse successfully. `git diff --check` is clean.

To repeat the GPU check from the repository root (requires a display/OpenGL
context and the configured local reference assets):

```sh
mkdir -p /tmp/ok-liquid-gpu
/home/wietse/.jdks/ms-25.0.4/bin/javac \
  -cp 'build/classes/java/main:build/install/OpenKeeper/lib/*' \
  -d /tmp/ok-liquid-gpu research/dkii-lava/CheckLiquidRendering.java
/home/wietse/.jdks/ms-25.0.4/bin/java \
  -cp '/tmp/ok-liquid-gpu:build/classes/java/main:build/install/OpenKeeper/lib/*' \
  CheckLiquidRendering
```

Captures are written to `/tmp/openkeeper-liquid-render/`.

## Fidelity limits

The mode switch, mesh density, lava heights/normals and three-stage combination
are implemented and tested. Exact enhanced pixels are **not** claimed:

- Displacement is currently derived from wrapped central height differences.
  Native signed-channel conversion, especially the lava PNG's grayscale/alpha
  representation, still needs an input/output comparison.
- Environment coordinates currently use view-space spherical reflection.
  Native coordinate generation and animation still need comparison.
- Water's enhanced wave phase and the chosen reference timing are not covered
  by the captured native lava fixture.

These remaining parity checks are tracked in #851. Bloom is not part of this
implementation.

## Interactive comparison

After `./gradlew classes installDist` and asset conversion, run:

```sh
LIQUID_PROBE_JAVA_HOME="$JAVA_HOME" research/dkii-lava/show_liquid_modes.sh '/path/to/Dungeon Keeper 2/'
```

The window stays open until closed. Water is on top, lava below; Classic is
on the left and Enhanced on the right. All four panes use the same neutral
lighting and camera. Left/right arrows orbit, up/down zoom, F12 saves a
screenshot in `/tmp/openkeeper-liquid-comparison/`, and Esc closes the window.
Add `--capture` to save a frame and exit automatically.
