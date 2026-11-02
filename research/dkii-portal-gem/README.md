# DKII portal-gem carrier: computation and rendering

Inspected 2026-10-29 and checked live on 2026-10-30. Source hashes are in `sources.json`. Both recovered IDA
function tables were checked before following the native instruction paths;
selected entries are in `ida-functions.tsv`. Those entries have placeholder
names, so semantic descriptions below are research findings, not recovered
original function names. `native-excerpts.txt` preserves the relevant original
instructions. No original executable, IDB, map, or asset was modified; the live probe adds a separately named map.

## Main finding

**The carried-creature sparkles use effect 257, not the loose gem's idle effect
297.** An early local preview selected the loose-gem source and used visual estimates.
The implementation in this change replaces that preview with the native carrier path.

The native carrier path is:

1. `0x0049AC80`, trigger action case `0x14` (`ATTACH_PORTAL_GEM`), creates
   object `0x1c` (28) through `0x004B7A90`.
2. It attaches that object using `0x0048B350` and sets bit `0x04` at
   creature `+0x92`. The carried object ID lives at creature `+0x3c3`.
   `0x0048B5C0` checks whether that slot refers to object 28.
3. Model submission `0x0059B880` calls the control-marker evaluator
   `0x005A3AA0` for both static and animated meshes when a world thing is
   supplied. The animated branch calls it after preparing the current pose
   (`0x0059BA83`); the static branch calls it at `0x0059B9FF`.
4. At `0x005A4162`, the evaluator checks thing `+0x92 & 4`. It selects
   marker category 9, corresponding to marker ID **10001 / 0x2711**, collects
   those marker positions from the current prepared model, and emits from
   each position.
5. The transformed marker coordinates are converted to fixed-point: X/Y
   multiply by 4096; Z multiplies by -4096. At `0x005A426B` it explicitly
   pushes **0x101 / 257**, then calls the bridge's virtual method `+0x94`.
6. The CBridge vtable at `0x0066CC94` resolves `+0x94` to `0x0043A8E0`.
   This forwards to `+0x90`, resolved to `0x00440420`. That routine applies
   bounds/visibility/allocation gating, then calls `+0x98`, which resolves
   to `0x00440350`, the effect creation path into `0x004347E0`.

This is a renderer/model-marker emission path. It is not an `olhiEffectId`
attachment and not a continuous idle effect attached to the creature origin.
The Lord's `olhiEffectId` is zero in Level 1.

## Original model controls

`DumpLordControls.java` reads the original `Meshes.WAD` using the existing
KmfFile parser. Its output is in `lord-model-controls.txt`:

- `LOL-PoseFrame.kmf`: mesh control `(vertex=0, marker=10001)`.
- `LOL-Walk.kmf`: animation control `(0, 2, 10001)`.
- `LOL-Melee.kmf`: animation control `(0, 2, 10001)`.
- `LOL-Die.kmf`: animation control `(0, 2, 10001)`.

The animation parser still calls these control fields `unknown1/2/3`; this
inspection identifies the last field as a marker identifier. For the Lord,
the second field indexes the ITAB/OFFS geometry stream. `KmfModelMarkers` exports
static markers and animation tracks, and reads marker data from Meshes.WAD
for models converted before marker export existed.

The presence of the marker in the death model does not prove that dead
creatures retain the carrier flag: that flag's full clearing lifecycle still
needs following separately.

## Effect computation

`DumpGemDefinitions.java` resolves the original Level 1 catalogs. Full values
are in `level1-definitions.txt`:

| Property | Native definition |
| --- | --- |
| Effect | 257, `Gem dust sparkles` |
| Element | 444, `Gem dust sparkle element` |
| Generation | CUBE_GEN, 2 elements per turn, effect life 2 steps |
| Flags | GENERATE_EFFECT_ELEMENTS, RANDOM_DISTRIBUTION, USE_GENERATION_SCALE, ALWAYS_TRIGGER |
| Spawn range | inner radius 0, outer 20; height 30–90 |
| Element lifetime | integer sample 36–50 effect steps |
| Element scale | randomized in approximately 0.100–0.150 |
| Element colour | authored RGB 180,180,250; initial alpha 255 |
| Element motion | lateral/vertical speed range about -0.2 to +0.2; mass 0.009765625; friction 0.004989624 |
| Texture | `gsparkle`, ADDITIVE_ALPHA, 16 frames, ANIMATING_TEXTURE + RANDOM_START_FRAME |
| Fade | FADE, fade percentage 100 |

At a nominal 20 effect steps per second, the authored particle lifetime is
1.8–2.5 seconds. This is a unit conversion, not a new observed runtime duration.

Effect update `0x00434E80` invokes the generation callback before decrementing
the effect's remaining life. CUBE_GEN `0x004386F0` tests random generation
eligibility and remaining life, then emits from the generation IDs. The native
loop distinguishes per-call generation from per-catch-up-step generation and
applies the generation scale. Therefore "2 elements per turn" is not evidence
for a fixed two-particles-per-frame rate or the preview's 60 particles/second.

Particle initialization `0x00435F20` samples lifetime inclusively from the
catalog's integer min/max, chooses motion and scale, initializes the sprite
animation, and copies the authored RGB bytes. Native art animation setup
`0x004B37D0` and update `0x004B3AD0` maintain the random initial frame and
advance the texture frame. This provides actual changing sparkle silhouettes,
not a generated soft circle.

Particle update `0x00436430` subtracts elapsed steps from remaining life, updates
orientation/motion, and under FADE changes **RGB** using integer arithmetic:

```text
L = remaining life after subtraction
if L != 0 and L <= max(R, G, B):
    each channel = trunc(channel * (L - 1) / L)
```

The alpha-fade threshold initialized at `0x004361FA` is
`life - fadePercentage * life / 100`. With element 444's 100%, the threshold
is zero, so the conditional alpha ramp is skipped. Alpha remains 255 while
RGB fades. The recurrence executes once per update call, not once per skipped
step; applying it repeatedly for a large catch-up delta would differ.

## Rendering

A carrier-specific live capture now confirms element 444's submitted state:

- source ONE, destination ONE, ADD;
- depth test enabled, depth writes disabled;
- texture RGB modulated by vertex RGB;
- initial vertex colour `0xffb4b4fa` and a 16-frame `gsparkle` atlas;
- no extra multiplication of source RGB by source alpha during blending.

`native-carrier-capture.jsonl` contains eight particle initializations and
matching draws. Their scales range from 412 to 600 in fixed-point /4096,
and lifetimes from 39 to 49 steps. `trace_native.py` records the native state.
The debugger stalls the renderer, so the archived timing samples must not be
used to estimate ordinary emission cadence.

Native `0x57573B` computes integer FPS from the frame duration in milliseconds.
`0x43B28B` derives generation scale as `clamp(50 + FPS * 205 / 14, 50, 255)`.
At 14 FPS and above, scale is 255. The debugger capture's value 50 is therefore
a low-frame-rate observation, not a constant authored density. The carrier
emitter reproduces this adjustment rather than hardcoding the captured value.

Low RGB texels accumulate additively across overlapping particles and saturate
toward white. The original texture itself contains pointed star silhouettes.

## Implementation and validation

`PortalGemEffect` uses effect 257 / element 444, model marker 10001, short-lived
parent generators, original random-start texture frames, native RGB fade and
additive blending. Emission requests come from visible creature model submission
and are deduplicated across viewports. Existing particles remain in world space
while the carrier moves. Losing the gem, entering the hand, death or removal
clears the effect. A missing marker does not invent an attachment position.

The fixed particle pool is an allocation bound, not an emission-rate setting.
At 60 Hz, a real-asset headless run observed 359 visible frames out of 360 and
1,308 particles at its peak, below the 2,048-particle capacity. It exercised
static, walking and combat markers, the real texture, and death cleanup.

Validation on the upstream-based PR branch: `JAVA_HOME=/home/wietse/.jdks/ms-25.0.4
./gradlew test --offline` passes the full suite without exclusions. The new
focused tests cover frame-rate scaling, RGB fade, texture animation, world-space
particles, generator expiry, marker conversion/serialization, and carrier lifecycle.

`CheckCarrierEffect.java` runs against the real Level 1 data and converted assets.
`PreviewGemCarrier.java --compare` opens ModelViewer with a wider camera, captures
idle and moving views, and starts walking after eight seconds. F5 toggles the gem;
F6/F7/F8 select walking/combat/idle. These tools require a local DKII installation;
no proprietary assets are included.

## Live comparison and remaining limits

See `live-spread-observation.md` for the normal-speed native recording. The
original effect forms a saturated white cluster at rest and a broad, irregular
trail during movement. Individual pointed stars remain visible around the edges.

The camera positions differ between DKII and ModelViewer, so the recording does
not establish an exact numerical sprite-size ratio. No arbitrary size reduction
was applied. Frame pacing, effect catch-up behavior, and the fixed pool can still
produce differences at unusually high or low rendering rates. Native lifecycle
behavior beyond the observed carrier path has not been exhaustively traced.
