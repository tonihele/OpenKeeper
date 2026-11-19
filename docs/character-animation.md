# Character animation maintenance guide

This guide describes the current implementation. The [architecture assessment](character-creature-animation-architecture-assessment-2026-11-18.md) preserves the investigation and original checklist; the [code review](character-animation-code-review-2026-11-19.md#publication-preflight-2026-11-19) contains finding resolutions, commit references, measurements and remaining acceptance work. Historical review statements refer to their recorded revisions.

The [next-generation proposal and implementation evidence](character-animation-next-generation-proposal-2026-11-19.md#13-implementation-evidence) record the subsequent optional features, corpus audits, close-camera measurements and remaining broad acceptance gates. The earlier review does not approve these later changes.

## Asset and runtime ownership

1. `KmfModelLoader` converts DK2 vertex poses into J3O `PoseTrack` clips. Tracks validate layouts/coordinates, repair only unambiguous legacy terminal self-pairs, interpolate sparse frames and install conservative clip envelopes. They own mutable sample/output buffers and invalidate precise picking data after deformation. No skeleton or invented skinning weights are introduced.
2. `AssetUtils` clones raw model templates before applying resource-specific texture policies. Utility caches and in-flight model preparation belong to each AssetManager. Duplicate requests share preparation and get independent clones; failure, interruption and dependency cycles have explicit handling. `CreatureAnimationCatalog` enumerates one policy-aware preload pass, including static resources and authored Start/End segments. Release requires a completed loading boundary and rejects active model requests.
3. Gameplay publishes `CreatureViewState` identity (creature, state, start time, sequence), owned `Position` snapshots and `SimulationTime`. Optional `CreatureMotion` publications bind roots and requests to authoritative time/epochs. Render-owned bounded history delays non-controlled presentation by one publisher interval, holds on stalls and snaps across discontinuities; possession and attachments retain immediate policies. Hauled creatures follow their carrier's presented X/Z. Simulation transforms and task timing are unchanged; no stride calibration is inferred.
4. `CreatureViewControl` separates requested logical activity from the last successful visual. Failed art holds the previous pose and releases owned activity audio; logical death effects are not repeated by Position updates. Scene/material/composer mutations belong to the render update path.
5. `AnimationLoader` preflights every segment's endpoints and initial phase off the live tree. It publishes children, owner presentation and cache/revision changes transactionally. Active restarts use isolated replacements; inactive cached clips can be reused.
6. `AnimationPlayback` owns unwrapped elapsed time across Start/Main/End, looping and ping-pong traversals. Disabled `AnimComposer`s only sample that phase. World poses and markers consume the bounded, pause-aware presentation clock; initial seeks do not replay historical callbacks. Explicit terminal-frame requests are held states, not enormous elapsed-time sentinels.
7. `TextureAnimationControl` freezes when an explicitly supplied world clock is unavailable. Its local overload and cloned/imported controls retain local playback. Authored alpha and legacy RGB black-key coverage remain distinct and consistent across color/depth/shadow passes.
8. Optional `AnimationDataCache` prepares immutable Float32 vertex data on private templates within an asset-context primitive-payload budget. Clones share source data and keep one local scratch array plus their private output buffers. Retarget/retime invalidates derived data. Sparse sampling remains the compatible default/fallback; derived data is not serialized into existing J3Os.
9. Loading screens use a single-use `LoadingScope`: detachment cancels queued completion/error publications and interrupts without joining the loader. Worker completion is signaled only after the task returns. Prewarming additionally has an injected cancellation/render boundary, per-manager interruptible preparation lock and completion after render preloading; release rejects active or queued prewarming. The separate client/map loaders still need their lifetime token/publication integration.

## Compatibility and settings

- Authored FPS is interpreted by `AnimationTiming`; historical converted 30 Hz key times remain readable. The model conversion version is 16. Use normal conversion setup for a separate installation when required; the audit converts in memory and does not replace live assets.
- Current N/FPS duration and CLAMP-to-cycle interpretation are retained conventions. Synthetic/corpus tests establish implementation consistency, not independent original-DK2 timing fidelity.
- `Settings.Setting.ANIMATION_CACHE_CLIPS` defaults to **0**. `ANIMATION_CACHE_KIB` defaults to **256**; runtime clamps clips to 0–32 and the position budget to 0–4096 KiB. The budget counts owned output and two decoded position arrays, not all Java/native/GPU memory. Keep retention opt-in until level-residency acceptance is complete.
- Client/server builds must match: request sequence, simulation-clock and possession-gait fields extend the serialized component schema. Mixed-version networking is not supported or claimed.
- Explicitly hidden subtrees defer pose writes and flush pending terminal poses on re-entry. Frustum-only animation scheduling is not enabled because shadow/secondary cameras may still need them.
- `ANIMATION_INTERPOLATION` / `AnimationInterpolation` and `ANIMATION_PHASE_VARIATION` / `AnimationPhaseVariation` default to **false**. Host publication and client interpolation settings must be enabled before session creation. Phase variation is limited to eligible wrapping locomotion without Start/End or animated textures, uses a presentation seed and shares its phase with markers.
- `ANIMATION_PACKED_KIB` / `AnimationPackedKib` defaults to **0**, with a maximum of 262,144 KiB per asset context. Configure before model loads; changing policy needs a new/released context at a completed loading boundary. The budget covers packed primitive data, not total Java/native/GPU memory or transient bake allocations. Prepared scene retention remains separately budgeted and conservatively counts its previous worst-case position scratch estimate. Keep packed sampling opt-in pending cold-load and complete level-residency acceptance.

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
| `captureCharacterAnimation` | Fixed-camera eight-creature playback, transition, hover/reuse, pause and re-entry checks. Options: `animationShadows`, `animationMotionTrace`, `animationPhaseVariation`, `animationPackedKiB`. Inspect generated frames as well as assertions. |
| `benchmarkCharacterAnimation` | Raw-clip and production-owner CPU modes, plus transition retention bursts. Options: `animationRuns`, comma-separated `animationModels`, `animationJfr`. Excludes GPU rendering and full creature/world behavior. |
| `benchmarkCreaturePreloads` | Paired former/current warmed preload clone/setup allocation; excludes GPU scene preload and full startup. |
| `auditCreatureAnimationCatalog` | Availability inventory; optional `animationMetadataRoot` validates source/converter-bound semantic sidecars. Presence does not prove original behavior policy. |
| `auditAnimationTopology` | Ordered geometry/UV/index, transform and material screen across representative action pairs. Does not certify anatomical correspondence. |
| `bakeVertexAnimationClips` / `auditVertexAnimationCorpus` | Isolated source-bound Float32/UNORM16 data, exact/fractional/velocity comparison and source LOD index audit for eight examples / every catalog creature resource and segment. Existing converted data is never replaced. |
| `auditAnimationUpdateError` | Held-pose error across display/update rates, staggers and wrap seams. Fixed throttling is not enabled. |
| `validateVertexTexturePlayback` | Test-only real-GL indexed/nonindexed numeric shader parity. Not a production VAT backend or FPS benchmark. |
| `benchmarkSharedVertexClips` | Alternating-order sparse, packed array prototype and production packed PoseTrack CPU/output comparison; `animationRuns`. Primitive-payload accounting excludes full residency. |
| `benchmarkRenderedCharacterAnimation` | Close perspective eight-archetype CPU crowd, frame/spatial/render submission timings, asynchronous GPU queries and actual draws. Options: `animationRuns`, `animationPackedKiB`. Owners can be offscreen; snapshots are taken outside measured frames. Excludes AI/world lighting/shadows. |
| `smokeCharacterMotion` | Real local Alcatraz session, coherent root/pose clocks, pause/resume and bounded packed payload. Options: `animationPackedKiB`, `animationPhaseVariation`. Uses an isolated user home. |

Add `-PanimationAuditPacked=true` to `auditAnimationAssets` to require packed sampling for every inspected track. That diagnostic prepares each model separately to cover the corpus; it does not claim all resources fit one runtime budget. Quote properties containing commas/spaces in PowerShell. Avoid overlapping benchmarks; record and exclude unrelated GPU load and presentation stalls.

These maintained tools live under `src/test`; generated captures, profiles and corpus output stay under ignored `build/`. Do not commit proprietary converted assets or one-off review probes.

## Acceptance limits

All confirmed R1–R8 review defects are fixed at that report's revision, with additional lifecycle corrections documented in the publication preflight. Original endpoint/segment/CLAMP and retreat/drag/recoil/idle/alternate-form policies still need controlled, versioned DK2 reference timelines. New root interpolation and packed sampling have verified opt-in scopes, not completed broad acceptance. Delayed remote traffic, possession/hand/combat/audio, cold preparation and ten-level total residency remain outstanding.

Retain CPU vertex animation. The close-camera packed sampler improves measured spatial/frame time with byte-identical captures in its controlled workload; it does not establish whole-game FPS or justify shipping a GPU backend, instancing, worker migration or enabling retention by default. Semantic sidecars are validated by offline tools; no unverified contact, stride, rig, idle-weight, cubic or normal policy is silently installed. The proposal distinguishes numerical, rendered, audible, gameplay and original-reference evidence and leaves unmet acceptance gates open.
