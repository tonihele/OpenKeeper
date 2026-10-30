# Native DKII lava rendering (#22)

Investigation on 2026-10-30 using the original installation and executable
fingerprinted in `provenance.json`. Both IDA databases were consulted through
`../dkii-symbols/ida-symbols.tsv` before tracing. Most relevant routines have
placeholder names in both catalogs. The newer database does identify the
constructor at `0x5873A0` as `CEngineDynamicHeightField` and its destructor.
Addresses below identify measured functions; descriptive labels are ours.

## Result: shared CPU waves, optional hardware EMBM

DKII has an ordinary textured lava appearance and an optional environment-mapped
bump-mapping (EMBM) enhancement. These are not mutually exclusive CPU-renderer
versus programmable-shader implementations. The liquid geometry is prepared on
the CPU; supported Direct3D hardware can additionally perturb environment-map
sampling per pixel through fixed-function texture stages.

The installation's original `Update.txt` explicitly describes adding EMBM to
lava and water for the Matrox G400 and documents `-enablebumpmapping` and
`-enablebumpluminance`. It describes that feature as unofficially supported and
hardware-dependent. This is corroborated by executable resource names and
Direct3D stage setup, not inferred solely from the patch notes.

The ordinary mode was observed live. A follow-up launch with the documented
`-enablebumpmapping` setting also passed native capability checks and loaded
the enhanced lava resources (see below). Enhanced pixel output has not yet
been verified; the command replay establishes its state setup.

## Ordinary lava surface

The quiet map's lava calls the interface routine at `0x5A2BE0` from `0x43D2ED`.
That routine computes sine heights and normals on the CPU and constructs a
`CEngineDynamicHeightField` through `0x5873A0`. Preparation and submission occur
at `0x5875D0` / `0x587DA0`. This path is used even with runtime bump mode zero;
the routine should not be described as a bump-only path.

In the sampled scene, each tile uses dimension 2: a **3x3 grid, 9 vertices,
4 quads / 8 triangles**, with half-tile vertex spacing. The ordinary OpenKeeper
surface currently uses one quad / two triangles per tile.

For tile coordinate `(tx,ty)`, local grid coordinates `i,j = 0..2`, and the
engine's integer counter `T` at interface offset `+0xC9`, the phase is:

```
phaseX = (((32*i - 64*tx + T) << 10) & 0x1ffff) * float32(pi) / 65536
phaseY = (((32*j - 64*ty + T) << 10) & 0x1ffff) * float32(pi) / 65536
height = -float32(0.85) + 0.04 * (sin(phaseX) + sin(phaseY))
normal = normalize(cos(phaseX), cos(phaseY), -3)
```

This is a mathematical transcription; x87 intermediate rounding and stored
floats produce small differences. Native renderer coordinates use negative z
upward. In continuous world coordinates, the phase corresponds approximately
to `pi*x + T*pi/64` and `pi*y + T*pi/64`: a two-tile spatial period. Each sine
has amplitude 0.04, giving a maximum combined displacement of 0.08. Do not
convert the counter's period to seconds without checking its time source.
The native normal is the formula above, not a generic recomputation from the
triangles or the exact derivative normal of the height equation.

The final read-only trace captures the same tile at `(12,37)` over eight
different bridge updates, counters 321–328. `verify_lava.py` checks all **72
heights and 72 normal vectors**. Maximum height error is about **5.83e-8**,
maximum normal-component error **9.68e-8**. Both heights and normals change over
time. Results are in `native-lava.jsonl` and `results.json`.

Compared with the current OpenKeeper `LavaWaveControl`, the native height is a
sum of two sines rather than a sine/cosine product, has a different wavelength
and phase convention, and supplies changing normals rather than leaving them
vertical. No production rendering code was changed in this investigation.

## Texture and lighting inputs

`DumpLavaAssets.java` uses the production KWD parser. `assets.txt` records the
original level 1 terrain definitions:

- Lava's top resource is `Lava0`, ALPHA, with no ANIMATING_TEXTURE flag and no
  authored texture frames; the bed is the separate `LAVA BED` mesh resource.
- Water's top resource is `Water`, ALPHA, with ANIMATING_TEXTURE and 32 frames.
- Lava's raw terrain-light RGB is `(44,100,20)` with the high red bit set,
  representing `(300,100,20)` before renderer normalization. This is a local
  terrain-light input, not evidence of a bloom post-process.

The three enhanced lava textures in the extracted original EngineTextures are:
`BumpLava0.png`, `BumpLavaBumpmap0.png`, `BumpLavaEnvMap0.png`, one frame each.
Water instead has 32 extracted bump-map frames. Only metadata and hashes are
included here, not original commercial texture data.

## Enhanced Direct3D path

At `0x59856B`, nonzero configuration at `0x759B10` enables registration of
BumpWater / BumpWaterBumpMap / BumpWaterEnvMap and
BumpLava / BumpLavaBumpMap / BumpLavaEnvMap resource triples. Lava handles are
stored at interface offsets `+0x7E`, `+0x82`, `+0x86`.

The height-field constructor receives the base resource and optional maps.
Submission `0x587DA0` checks renderer capability bit `0x10` and surface flag
`0x8000` before scheduling the enhanced path, selecting map frames and binding
additional texture stages. Capability bit `0x20` selects the luminance variant.

The Direct3D state setup at `0x589250` uses device vtable slot `+0xA0`
(`IDirect3DDevice3::SetTextureStageState`). The bump branch sets texture-stage
COLOROP to **22 (`D3DTOP_BUMPENVMAP`)**, supplies bump-matrix coefficients,
and uses the following environment-map stage. The luminance branch uses
**23 (`D3DTOP_BUMPENVMAPLUMINANCE`)**. These are fixed-function operations,
not a Shadertoy-style procedural fragment shader. Constant meanings are
confirmed by Microsoft's [D3DTEXTUREOP documentation](https://learn.microsoft.com/en-us/windows/win32/direct3d9/d3dtextureop).

In the completed local Wine capture, runtime configuration is **0**, renderer
flags are **0x2**, and all three enhanced lava handles are zero. Ordinary lava
still animates with CPU-generated heights/normals. A previously read saved
registry value of 1 therefore does not establish which path was actually used.
That initial trace did not establish why the saved value and live mode
differed. The follow-up launch-setting test below removes that immediate
activation blocker without changing the Wine setup.

## Reproduction and limits

`create_probe.py` builds `LavaProbe_______` from the previously prepared local
`LightBase_______` map family, adds an 8x8 lava patch, and centres the camera on
it. It retains the Heart for ownership, no placed creatures, zero automatic
Imp threshold, and disabled object lights/terrain emitters from the baseline.
This isolates surface behaviour; it does **not** reproduce stock lava glow.
The generator refuses existing output paths. Install only its new seven map
files into `Data/editor/maps`; stock maps must not be overwritten.

Launch via the dedicated Wine prefix and GDB proxy from the game directory:

```sh
env -u LD_PRELOAD WINEPREFIX=/home/wietse/.local/share/wineprefixes/dk2 \
  winedbg --gdb --no-start --port 31359 \
  'Z:\home\wietse\Downloads\Data\Bullfrog\Dungeon Keeper 2\DKII.exe' \
  -level LavaProbe_______ -nomovies
```

From the repository root:

```sh
gdb -q --batch -x research/dkii-lava/trace_lava.gdb
python3 research/dkii-lava/verify_lava.py
```

The live script writes `/tmp/dkii-lava-trace.jsonl`; pass that path to the
verifier for a new capture. It fingerprints the executable, disables most
breakpoints after eight samples, and samples one fixed patch on distinct
updates. It waits for eight real surface samples instead of assuming startup
updates already render lava. A 5000-update ceiling stops an unsuccessful run;
the verifier requires a completed eight-sample capture. Count fields are
bounded capture counts, not scene-wide draw-call statistics.

This establishes the default geometry/normal calculation and the existence and
mechanism of the optional hardware path. It does not establish final pixel
parity, enhanced-mode appearance, complete UV/lighting composition, or how
all software renderers approximate the same appearance. It also does not
justify adding bloom merely to emulate EMBM; these are distinct effects.

## Isolated original-code texture-stage replay

`replay_texture_stages.c` executes the original selector at `0x589250` using
the earlier research PE loader. A recording COM stand-in replaces the graphics
device: slot 40 records `SetTextureStageState`, slot 22 records
`SetRenderState`. No real Direct3D device, driver, texture sampling, Windows
entry point or display is invoked. The runner checks the executable hash.

```sh
research/dkii-lava/run_stage_replay.sh
```

Six controlled cases pass (`texture-stages.jsonl`, `stage-results.json`):

| Mode transition | Commands | Resulting texture stages |
| --- | ---: | --- |
| ordinary to EMBM (`0x8000`) | 20 | 3 |
| ordinary to EMBM+luminance (`0x10000`) | 20 | 3 |
| EMBM to ordinary | 10 | 1 |
| EMBM+luminance to ordinary | 10 | 1 |
| unchanged EMBM | 0 | 3 |
| unchanged EMBM+luminance | 0 | 3 |

Both enhanced cases configure these colour operations:

1. Stage 0: `MODULATE` (4), base texture and diffuse lighting.
2. Stage 1: `BUMPENVMAP` (22) or `BUMPENVMAPLUMINANCE` (23), with an identity
   2x2 bump matrix.
3. Stage 2: `ADD` (7), environment texture plus the current result.

The luminance case also sets BUMPENVLSCALE (22) to float 1.0 and
BUMPENVLOFFSET (23) to float 0.0. Returning to ordinary mode explicitly disables
stages 1 and 2. Unchanged state is cached and makes no device calls.

This verifies the actual command generation for the optional mode even though
the Wine scene selected ordinary mode. It does not substitute for a GPU pixel
capture: filtering, coordinate generation, source-texture format conversion,
colour precision and hardware-specific behaviour still need checking for an
exact shader port.

## Follow-up: the launch setting enables enhanced mode

A new read-only startup capture with `-enablebumpmapping` resolves the immediate
activation question (`native-bump-startup.jsonl`):

- The command-line handler calls the bump-setting setter at `0x566860` with 1,
  from return site `0x5A6A71`; the preceding live value is zero.
- The backend request changes from ordinary flags `0x2` to **`0x12`**.
- The native capability test accepts it (recorded capability byte `0xFF`);
  the rejection branch at `0x573EAE` is not taken before resource registration.
- Runtime bump configuration remains **1**, renderer flags remain **`0x12`**.
- All three lava resource handles are nonzero: **0x4A, 0x4B, 0x4C**.

Thus, the ordinary capture's missing launch setting was an immediate blocker.
The previously saved registry value alone did not activate the feature in the
tested executable. No different graphics hardware, Wine prefix or driver was
needed to pass this startup check. This does not by itself establish correct
GPU pixels or fully working environment mapping. The launch switch's setter
also writes the saved configuration; the pre-existing saved value was already 1.

Launch the probe through the normal dedicated-prefix launcher with:

```sh
'/home/wietse/Downloads/Data/Bullfrog/Dungeon Keeper 2/start-dkii.sh' \
  -level LavaProbe_______ -nomovies -enablebumpmapping
```

For startup-only tracing, use the same Wine GDB launch pattern with port 31360
and `-enablebumpmapping`, then run
`gdb -q --batch -x research/dkii-lava/trace_bump_setting.gdb`. The script stops
at resource registration and writes `/tmp/dkii-bump-setting.jsonl`.
