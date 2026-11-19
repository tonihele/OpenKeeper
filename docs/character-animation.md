# Character animation maintenance guide

This guide describes the current implementation. The [architecture assessment](character-creature-animation-architecture-assessment-2026-11-18.md) preserves the investigation and original checklist; the [code review](character-animation-code-review-2026-11-19.md#publication-preflight-2026-11-19) contains finding resolutions, commit references, measurements and remaining acceptance work. Historical review statements refer to their recorded revisions.

## Asset and runtime ownership

1. `KmfModelLoader` converts DK2 vertex poses into J3O `PoseTrack` clips. Tracks validate layouts/coordinates, repair only unambiguous legacy terminal self-pairs, interpolate sparse frames and install conservative clip envelopes. They own mutable sample/output buffers and invalidate precise picking data after deformation. No skeleton or invented skinning weights are introduced.
2. `AssetUtils` clones raw model templates before applying resource-specific texture policies. `CreatureAnimationCatalog` enumerates one policy-aware preload pass, including static resources and authored Start/End segments. Existing global template-cache lifetime remains a separate follow-up.
3. Gameplay publishes `CreatureViewState` identity (creature, state, start time, sequence), owned `Position` snapshots and `SimulationTime`. Actual displacement selects locomotion; this branch does not interpolate transforms or infer stride scaling.
4. `CreatureViewControl` separates requested logical activity from the last successful visual. Failed art holds the previous pose and releases owned activity audio; logical death effects are not repeated by Position updates. Scene/material/composer mutations belong to the render update path.
5. `AnimationLoader` preflights every segment's endpoints and initial phase off the live tree. It publishes children, owner presentation and cache/revision changes transactionally. Active restarts use isolated replacements; inactive cached clips can be reused.
6. `AnimationPlayback` owns unwrapped elapsed time across Start/Main/End, looping and ping-pong traversals. Disabled `AnimComposer`s only sample that phase. World poses and markers consume the bounded, pause-aware presentation clock; initial seeks do not replay historical callbacks. Explicit terminal-frame requests are held states, not enormous elapsed-time sentinels.
7. `TextureAnimationControl` freezes when an explicitly supplied world clock is unavailable. Its local overload and cloned/imported controls retain local playback. Authored alpha and legacy RGB black-key coverage remain distinct and consistent across color/depth/shadow passes.

## Compatibility and settings

- Authored FPS is interpreted by `AnimationTiming`; historical converted 30 Hz key times remain readable. The model conversion version is 16. Use normal conversion setup for a separate installation when required; the audit converts in memory and does not replace live assets.
- Current N/FPS duration and CLAMP-to-cycle interpretation are retained conventions. Synthetic/corpus tests establish implementation consistency, not independent original-DK2 timing fidelity.
- `Settings.Setting.ANIMATION_CACHE_CLIPS` defaults to **0**. `ANIMATION_CACHE_KIB` defaults to **256**; runtime clamps clips to 0–32 and the position budget to 0–4096 KiB. The budget counts owned output and two decoded position arrays, not all Java/native/GPU memory. Keep retention opt-in until level-residency acceptance is complete.
- Client/server builds must match: request sequence, simulation-clock and possession-gait fields extend the serialized component schema. Mixed-version networking is not supported or claimed.
- Explicitly hidden subtrees defer pose writes and flush pending terminal poses on re-entry. Frustum-only animation scheduling is not enabled because shadow/secondary cameras may still need them.

## Verification tools

Run the ordinary build without proprietary data:

```powershell
.\gradlew.bat build --rerun-tasks --no-daemon --console=plain
.\gradlew.bat captureAnimationCoverage --no-daemon --console=plain
```

The second command opens a real OpenGL fixture and checks synthetic alpha/black-key color and shadow coverage. It needs a desktop renderer; it is not part of the headless unit suite.

Installed-data commands use three explicit properties: `-Pdk2Root=<DK2 installation>`, `-PconvertedAssets=<existing converted root>`, and, if needed for relative asset lookup, `-PanimationWorkingDir=<checkout containing those assets>`:

| Task | Purpose / additional properties |
|---|---|
| `auditAnimationAssets` | Strict finite/bounds validation, cached J3O and fresh in-memory KMF. Add `-PanimationAuditAllCreatures=true` for all catalog main/segment resources. Historical report-only mode is explicitly `-PanimationAuditReportOnly=true`. |
| `captureCharacterAnimation` | Fixed-camera eight-creature playback, transition, hover/reuse, pause and re-entry checks. Add `-PanimationShadows=true` for shadow coverage; inspect generated frames as well as assertions. |
| `benchmarkCharacterAnimation` | Raw-clip and production-owner CPU modes, plus paired transition retention bursts. Optional `-PanimationJfr=<output.jfr>`. Excludes GPU rendering and full creature/world behavior. |
| `benchmarkCreaturePreloads` | Paired former/current warmed preload clone/setup allocation; excludes GPU scene preload and full startup. |
| `auditCreatureAnimationCatalog` | Availability inventory; resource presence does not prove original behavior policy. |

These maintained tools live under `src/test`; generated captures, profiles and corpus output stay under ignored `build/`. Do not commit proprietary converted assets or one-off review probes.

## Acceptance limits

All confirmed R1–R8 review defects are fixed, with additional lifecycle corrections documented in the publication preflight. Original endpoint/segment/CLAMP and retreat/drag/recoil/idle/alternate-form policies still need controlled, versioned DK2 reference timelines. Transform timestamps/discontinuities/interpolation, global cache/level residency and representative delayed-network/hand/combat/audio/crowd acceptance remain explicitly separate work.

Retain CPU vertex animation for now. Isolated pose/preload/transition measurements do not establish a whole-game FPS improvement or justify GPU interpolation, instancing, worker migration or enabling retention by default. The review records actual measurements and distinguishes numerical, rendered, audible, gameplay and original-reference evidence.
