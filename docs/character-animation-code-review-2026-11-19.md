# Independent character animation code review

Date: 2026-11-19. **Recommendation: Request Changes.**

Reviewed branch: `feat/character-animation`, `D:\Project\OpenKeeper-character-animation`, HEAD `dffb07b7a4d7770f4a7af8da6c5082da55ce820e`. Scope: all 23 commits after upstream baseline `cd78e5654`, 59 changed files, including production Java, shaders, tests, build tasks and implementation documentation.

Baseline requirements: [original architecture assessment and checklist](character-creature-animation-architecture-assessment-2026-11-18.md), especially sections 4, 8, 9 and the complete implementation record in section 11. Its original analysis used `a63005ab3`; the implementation correctly rebased the work onto a different upstream baseline. This review evaluates the final feature tree against `cd78e5654`, rather than attributing unrelated baseline differences to the animation work.

No production/test source, build configuration or Git history was changed during this review. New executable diagnostics and logs are ignored files under `build/review*`; this report is the only review deliverable outside build output. No commit, push or PR was created. The original checkout and other feature worktrees were preserved.

## Overall assessment

The changes contain useful, maintainable repairs: narrowly scoped legacy endpoint recovery, continuous sparse-frame interpolation, conservative bounds, collision-cache invalidation, complete request identity, shared metadata interpretation, owned transform snapshots and measured hidden/unchanged deformation suppression. Retaining the vertex-animation representation and declining an unmeasured GPU rewrite are sensible decisions. Small injectable model sources make loader testing practical.

The implementation is **not ready for integration as a completed animation initiative**. Three High findings are reproduced: paused seeks activate composers and restore stale segment states; looping playback loses elapsed time at normal refresh rates; and missing death art causes repeated death effects because logical state is inferred from the last successful visual. Opt-in reuse also retains transient material state. The passing suite and GL gallery do not exercise these interactions.

There are no identified Critical findings. Findings below distinguish new regressions, retained defects, conditional fault-handling defects, and an existing visual gap uncovered by this review. Five original action items remain explicitly incomplete in the assessment; that disclosure is appropriate, but several checked items also need reopening.

## Scope and commit verification

The final changes and affected call sites were traced from KMF parsing/conversion through ECS publication, scene preparation, playback, vertex buffers and shader selection. Commit history was checked against its implementation ledger; the final commit `dffb07b7a` is described in section 11.25 although absent from the preceding ledger table.

| Commits | Reviewed responsibility | Disposition |
|---|---|---|
| `42acfb77a`, `dabbc6cd9`, `2e0e29031` | Corpus tooling, finite frames, interpolation, bounds, current-pose picking | Numerical repairs verified; audit gating improvement R7 |
| `dcd3f77c0`, `1d1904749`, `dffb07b7a` | Preparation, failure rollback policy, held seeks, material-load failures | Useful load-time protection; R3 and R5 remain |
| `d97ec0ce8`, `e91de768b` | Request sequence, simulation clock, initial seek, pause | Identity preserved; R1/R2 prevent phase/pause acceptance |
| `fd84c13ee`, `b560724ba`, `999697aa4` | FPS, segment durations, cycle direction, playback policy | Shared metadata works; R1/R2 and original-policy acceptance remain |
| `dcabe70da`, `87e1abdc3` | Position ownership, fall integration, displacement/gait selection | Foundation verified; interpolation and lifecycle acceptance incomplete |
| `f659d6964` | Cached markers, catch-up budgeting, audio exits | Marker arithmetic tested; actual pose clock diverges and failed exits bypass cleanup |
| `6dd355289`, `02de74670`, `c70b1a87f` | CPU benchmark, hidden/unchanged suppression, stopped hidden poses | Benefits reproduced at documented CPU scope |
| `f54ed1a99`, `dc5d5dacf` | Resource policy isolation, preload enumeration, owner-local LRU | Isolation works; R4/R8 and global cache-lifetime acceptance remain |
| `5300e16b8` | Creature/action availability inventory | Useful availability evidence, not behavior parity |
| `6809a9fce`, `8de291fba` | GL gallery, controlled atlas frames, pause/shadow checks | Execution/pause verified; R6 exposes a visual acceptance gap |
| `8a4c779d6` | Conditional GPU/parallelism decision | Reasonable; no claim of proven GPU irrelevance is justified |

## Execution paths and architectural conclusions

1. `KmfModelLoader` converts compressed DK2 vertex positions into sprite-partition meshes and `PoseTrack` frames, using historical 30 Hz key coordinates. `AssetsConverter.MODELS` is now version 16. `PoseTrack` validates frame/pair data, repairs only unambiguous legacy self-pairs, builds a conservative envelope and owns two mutable decode arrays per instance. Animation remains CPU vertex deformation; there is no imported skeleton or skinning palette.
2. `AnimationTiming` interprets resource FPS/duration for simulation and `TimedClip`. `CreatureViewSystem` publishes state/start/sequence and a `SimulationTime` entity. It selects locomotion from displacement between logic ticks and forwards possession gait where art exists. Fall/navigation/possession integration now shares the steering loop; published `Position` vectors are copied.
3. `PlayerEntityViewState` owns a replicated `PresentationClock` and forwards complete requests to `CreatureViewControl`. The latter rejects duplicates, resolves/decorates assets and initially seeks to request age. **Subsequent vertex playback runs on independent composer time**, while world marker counting and controlled texture atlases use presentation time. This is the central remaining synchronization weakness.
4. `AnimationLoader` prepares main/start/end nodes and actions, then replaces live children. Optional `PreparedAnimationCache` retains owner-local nodes. Completion callbacks switch segments, hold/stop or reverse source traversal. Pause saves enabled flags. These mechanisms work independently but do not compose safely under paused seeking and material reuse.
5. `PoseTrack` writes owned Position buffers, marks them dirty and invalidates collision acceleration data. Explicitly hidden and unchanged samples avoid writes. `EntityViewControl` flushes pending hidden terminal poses on visibility re-entry. GL transforms/lights/rasterizes those CPU-deformed positions. Texture frames are separate material uniforms shared by custom color/shadow vertex paths.

Sources: [conversion](../src/main/java/toniarts/openkeeper/tools/convert/KmfModelLoader.java#L252), [pose sampler](../src/main/java/toniarts/openkeeper/animation/PoseTrack.java#L231), [selection](../src/main/java/toniarts/openkeeper/game/logic/CreatureViewSystem.java#L96), [view bridge](../src/main/java/toniarts/openkeeper/view/PlayerEntityViewState.java#L661), [request control](../src/main/java/toniarts/openkeeper/view/control/CreatureViewControl.java#L98), [loader](../src/main/java/toniarts/openkeeper/view/animation/AnimationLoader.java#L155), [atlas control](../src/main/java/toniarts/openkeeper/view/animation/TextureAnimationControl.java#L37).

No new general framework is necessary. Keep these focused helpers, but make logical state, requested playback phase and mutable presentation state explicit at their existing ownership boundaries. Fix the defects before adding cross-clip blending, worker execution or a GPU backend. Arbitrary blending between different DK2 model topologies would need an explicit correspondence contract; a generic skeletal blend layer is inappropriate here.

## Prioritized findings

### R1 — High: seeking across segments while paused activates playback and corrupts resume state

**Confirmed new integration defect.** [AnimationLoader.java:240–265](../src/main/java/toniarts/openkeeper/view/animation/AnimationLoader.java#L240), [350–357](../src/main/java/toniarts/openkeeper/view/animation/AnimationLoader.java#L350), [370–383](../src/main/java/toniarts/openkeeper/view/animation/AnimationLoader.java#L370), [430–434](../src/main/java/toniarts/openkeeper/view/animation/AnimationLoader.java#L430); [CreatureViewControl.java:115–128](../src/main/java/toniarts/openkeeper/view/control/CreatureViewControl.java#L115), [232–240](../src/main/java/toniarts/openkeeper/view/control/CreatureViewControl.java#L232).

`playAnimation()` applies pause after publication, but `setViewState()` subsequently calls `seekAnimation()`. Seeking past Start executes its completion callback, which unconditionally enables main. Seeking past main can similarly enable End. `WAS_ENABLED` still contains the flags captured before seeking. Creature control also calls initial synchronization before its pause guard, so a clock becoming available during pause takes this path.

**Reproduction:** use `AnimationLoaderTest.model` as the injected source and a three-frame/30 FPS resource with HAS_START_ANIMATION; pause the owner, play it, seek to age 0.08, then update logical state by 0.01. The synthetic Start lasts 1/30 s because its dummy clip is empty. The same problem applies to nonempty real segments whenever age crosses their actual duration.

```text
PAUSE before-seek start=false main=false
PAUSE after-seek paused=true main=true time=0.046666667->0.056666666
PAUSE after-resume start=true main=false visibleStart=Always visibleMain=Dynamic
```

The model advances during pause. Resume then restores the hidden Start as enabled and disables visible main; Start completion can restart main and destroy the seek phase. Existing `nestedPause...` and late-seek tests test these operations separately.

**Fix:** centralize segment activation through a pause-aware helper that updates desired enabled state and keeps actual composers disabled while paused. Seeking must update the saved resume state as well as visibility, without reactivating stale segments. Merely calling the current `pauseAnimations()` again is insufficient: it ignores nodes that already have `WAS_ENABLED`. Add start/main/end and HOLD/ONCE/LOOP matrices for paused creation, paused replacement and delayed clock arrival; require frozen vertices, textures, callbacks and composer time, followed by correct resume phase.

### R2 — High: looping playback loses elapsed time and drifts from the authoritative marker clock

**Confirmed retained timing defect, incompatible with the new synchronization claim.** [PlaybackAction.java:15](../src/main/java/toniarts/openkeeper/view/animation/PlaybackAction.java#L15), [TimedClip.java:36–38](../src/main/java/toniarts/openkeeper/view/animation/TimedClip.java#L36), [AnimationLoader.java:274–279](../src/main/java/toniarts/openkeeper/view/animation/AnimationLoader.java#L274), [398–444](../src/main/java/toniarts/openkeeper/view/animation/AnimationLoader.java#L398); initial-only synchronization at [CreatureViewControl.java:122–128](../src/main/java/toniarts/openkeeper/view/control/CreatureViewControl.java#L122), unwrapped marker time at [300–311](../src/main/java/toniarts/openkeeper/view/control/CreatureViewControl.java#L300).

The pinned jME 3.9.0-stable `AnimLayer.update(float,float)` adds delta, interpolates the action, then assigns time **zero** when it finishes. It discards overshoot rather than carrying a remainder. `PlaybackAction` delegates repetition to that layer; direction changes repair the old negative-time lock but not elapsed-time loss. Initial seeking never corrects subsequent divergence. Presentation-clock prediction also bounds stalled delivery, whereas composers continue to advance.

**Independent headless reproduction:** play the same 0.1 s WRAP resource with LOOP policy for ten seconds, using identical total logical time at each refresh rate:

| Update rate | Expected complete cycles | Observed cycles | Final local phase |
|---|---:|---:|---:|
| 25 Hz | 100 | 83 | 0.039999999 s |
| 60 Hz | 100 | 100 | 0 s |
| 144 Hz | 100 | 96 | 0 s |

This is ordinary frame quantization, not only severe hitch behavior. Error can accumulate indefinitely; short clips and noncommensurate rates are most sensitive. Footstep counts follow request age while vertices follow the slower cycle, so correct marker arithmetic does not ensure contact synchronization. The defect is present in the underlying repetition strategy; the new shared clock does not solve it.

A second synthetic test used the installed Imp WALK metadata (12 frames / 20 FPS, a 0.6 s main traversal). After 60 seconds it completed 100 traversals at 60 Hz, versus 99 traversals plus 0.187500001 s at 144 Hz. The latter represents 59.5875 s of authored traversal, approximately 0.4125 s behind the request clock. This tests the real resource duration through the production action driver; it is not a rendered Imp contact measurement.

**Fix:** own unwrapped playback age and derive segment/traversal phase from it, or use a focused playback driver that carries overshoot through loop and segment boundaries. Use the same phase source for pose sampling and markers. Preserve logical completion counts/direction without replaying historical gameplay events on initial seek. Do not invoke the current eventful `seekAnimation()` every frame as a shortcut. Validate equal poses, completion counts, segment timing and marker crossings at 25/30/60/120/144 Hz and irregular deltas spanning multiple traversals; test long sessions and stalled clock delivery.

### R3 — High: failed visual requests leave gameplay presentation lifecycle tied to the previous animation

**Confirmed regression in failure integration; audio consequence follows directly from source.** [CreatureViewControl.java:98–118](../src/main/java/toniarts/openkeeper/view/control/CreatureViewControl.java#L98), [343–375](../src/main/java/toniarts/openkeeper/view/control/CreatureViewControl.java#L343); [PlayerEntityViewState.java:667–683](../src/main/java/toniarts/openkeeper/view/PlayerEntityViewState.java#L667), [979–985](../src/main/java/toniarts/openkeeper/view/PlayerEntityViewState.java#L979); [EffectManagerState.java:145–155](../src/main/java/toniarts/openkeeper/view/effect/EffectManagerState.java#L145).

On unavailable/failed art, `currentState` and target intentionally retain the last valid visual. The world view still uses `currentState == DEATH_POSE` to decide whether death has already been presented. A repeated position notification therefore replays the death effect whenever DEATH_POSE art is absent. These calls create additional infinite effects; they are not coalesced by the effect manager. The installed catalog has no DEATH_POSE for Imp/Skeleton, and the GL gallery explicitly exercises that fallback.

**Independent reproduction:** create a valid IDLE visual with no DEATH_POSE resource, invoke the real private world-view update with the same death request three times and different Position snapshots, and count an injected effect manager's loads. The charred-texture call is bypassed with its existing user-data marker to isolate effect scheduling:

```text
DEATH same-request effectLoads=3 current=IDLE_1
```

`CreatureModelContainer` invokes this update for Position changes. A transported/relocated body or repeated component publication is sufficient; a stationary body with no further notification does not multiply effects spontaneously.

The same failure return bypasses `playStateSounds()` entirely. A prior research handle is stopped only by successful replacement, hiding or cleanup, so a visible missing-art transition can leave that sound active. Dig retries also consult retained `currentState`. Audible whole-game reproduction was not performed; the skipped cleanup path is confirmed.

**Fix:** track accepted logical request/state separately from the last renderable visual. Deduplicate death presentation by logical transition/entity lifetime, independently of load success. Run logical state-exit cleanup even when retaining geometry, and gate continuing dig/footstep/research scheduling by the actual requested activity. Define whether the retained visual should hold or continue; do not invent a DK2 corpse substitution without reference evidence. Add a world-view integration test for failed death art plus position updates, and backend-level tests proving old research/dig activity stops on failed work/death transitions.

### R4 — Medium: prepared clip reuse preserves stale transient material state

**Confirmed new defect in the opt-in path; default retention is disabled.** [AnimationLoader.java:160–172](../src/main/java/toniarts/openkeeper/view/animation/AnimationLoader.java#L160), [PreparedAnimationCache.java:40–43](../src/main/java/toniarts/openkeeper/view/animation/PreparedAnimationCache.java#L40); hover ownership at [EntityViewControl.java:192–205](../src/main/java/toniarts/openkeeper/view/control/EntityViewControl.java#L192), material changes at [AssetUtils.java:832–862](../src/main/java/toniarts/openkeeper/utils/AssetUtils.java#L832).

Cache hits reset composer/action flags but return the same mutable meshes and materials. Hover highlighting modifies only the attached subtree. After A is highlighted, a transition to B detaches A; ending hover restores B only. Returning to cached A retains its highlight while the owner now considers hover inactive. Future ordinary updates do not clear it.

**Reproduction:** owner-local cache capacity 3, load A, apply highlight, load B, disable highlight, load A again. With a standard lighting material the diagnostic reports `CACHE reused-highlight=true` (`UseMaterialColors` remains enabled). The same retained-state concern applies to owner-applied material conversions/overlays; those additional combinations were not exhaustively reproduced.

**Fix:** reapply current owner presentation state on each attach/reuse, including highlight and the relevant lighting/overlay policy. Alternatively retain a neutral prepared representation with explicit transient-state restoration. Avoid cloning all materials on every hit merely to conceal missing ownership. Cover transitions while hovered, hover exit while a clip is detached, material replacement, charred state and clone/cleanup. Keep default reuse disabled until those tests and full-game residency acceptance pass.

### R5 — Medium: replacement is transactional only until the first live sample

**Confirmed fault-injection defect; no installed corpus failure found.** [AnimationLoader.java:175–214](../src/main/java/toniarts/openkeeper/view/animation/AnimationLoader.java#L175), [274–279](../src/main/java/toniarts/openkeeper/view/animation/AnimationLoader.java#L274).

Old children are detached and revision incremented before `sampleAt(0)`, optional end-frame seeking and final pause handling. The broad catch returns FAILED without restoring the old subtree. An action/track that is prepared successfully but throws when first evaluated leaves the new visual published while `CreatureViewControl` rolls back its logical fields. Cache insertion also precedes successful initialization.

**Reproduction:** inject a wrapper with a composer/clip containing a malformed morph target that fails on initial sampling. It passes preparation and produces:

```text
TRANSACTION result=FAILED liveRetained=false revision=1
```

This diagnostic deliberately supplies an invalid target; the expanded installed corpus found no such failure. Nevertheless, FAILED does not satisfy the advertised preservation contract under evaluation failure. Existing failure tests throw during model loading, before the mutation boundary.

**Fix:** validate target buffers and initialize samples/actions off the live tree, then publish; provide rollback for publication-stage failures. Hidden-pose suppression must not accidentally skip preflight evaluation. Defer cache admission and successful revision/state publication until initialization succeeds, and avoid firing owner callbacks during preparation. Add first-sample failure, invalid target-buffer layout and end-frame failure tests asserting old children, cull/enabled state, revision and cache remain valid.

### R6 — Medium: shadow validation missed opaque atlas-plane shadows

**Confirmed current visual gap; the fragment logic predates this branch, so this is not attributed as a newly introduced shader regression.** [LightingSprite.j3md:247–275](../assets/MatDefs/LightingSprite.j3md#L247), [LightingSprite.frag:12–18](../assets/Shaders/LightingSprite.frag#L12); new controlled frame selector at [LightingSprite.vert:19](../assets/Shaders/LightingSprite.vert#L19). The implementation's shadow acceptance statements are in assessment sections 11.20–11.24.

The new shadow-enabled gallery was independently run and inspected. At `build/animation-capture/shadows/frame-150.png`, Firefly and Unique Firefly cast conspicuous rectangular wing-plane shadows. Other frames, including 060, have different silhouettes. The PreShadow fragment tests dark **RGB**, not texture alpha, then forces output alpha to 1. A transparent texel with nonblack RGB therefore survives depth writing; if discard is not enabled, the entire plane survives. Controlled UV selection alone does not establish alpha coverage parity.

**Impact:** visible color geometry can be coherent while shadow silhouettes expose the atlas quads. This invalidates treating the gallery run as complete alpha-cutout shadow acceptance. Original DK2 shadow appearance has not been independently compared.

**Fix:** establish and preserve the material's authored coverage policy, including actual alpha discard where the atlas carries alpha, consistently in color/depth/shadow paths. Keep any intentional black-key policy distinct rather than globally changing all sprite semantics. Add a transparent-but-colored synthetic atlas case and fixed-camera Firefly sequences verifying coverage across frames. If this pre-existing defect is split out of the integration scope, explicitly keep shadow fidelity open and remove the broader acceptance claim; it is not a reason to rewrite the animation backend.

### R7 — Low: installed-asset audit succeeds when sampled vertices exceed bounds

**Confirmed test-tool gap.** [AnimationAssetAudit.java:40–47](../src/test/java/toniarts/openkeeper/animation/AnimationAssetAudit.java#L40).

The audit accumulates and prints `outside`, but throws only when `invalid != 0`. A bounds regression can return a successful Gradle task even with nonzero out-of-bound samples. Focused synthetic bounds assertions reduce the risk but do not enforce installed-corpus acceptance.

**Fix/validation:** make nonzero bounds violations fail validation mode, retaining an explicit report-only mode if historical baseline collection needs it. Test the tool with a finite track and deliberately undersized bound; require a failing validation result. Current independently inspected totals are zero, so this finding does not invalidate those numerical results.

### R8 — Low: comprehensive preload enumeration duplicates the old creature preload pass

**Confirmed redundant work; performance magnitude not measured.** [AssetUtils.java:676–686](../src/main/java/toniarts/openkeeper/utils/AssetUtils.java#L676), [703–742](../src/main/java/toniarts/openkeeper/utils/AssetUtils.java#L703); [Creature.java:614–619](../src/main/java/toniarts/openkeeper/tools/convert/map/Creature.java#L614); [CreatureAnimationCatalog.java:14–23](../src/main/java/toniarts/openkeeper/animation/CreatureAnimationCatalog.java#L14).

The reflective pass still finds legacy creature getters such as WALK, then the new actual-slot pass loads those same PRELOAD resources again. Template caching avoids repeated file decode, but each `loadModel()` still clones an instance and applies resource-specific material setup; both passes enqueue preload scenes. Typed enumeration deduplicates internally, not across the two passes.

**Fix:** use one creature resource enumeration covering actual animation slots and required static/procedural mesh resources, including authored segments. Deduplicate equivalent preload requests while preserving genuinely different material/texture policies. Do not simply delete the old pass without preserving its non-animated resources. Verify intended model/policy counts and measure startup/prewarm allocation; no runtime FPS gain is claimed for this cleanup.

## Verification of every original action item

The assessment checks 11 items and leaves five unchecked. This table is an independent disposition, not a second completion assertion.

| # | Original checklist item | Independent status and evidence |
|---:|---|---|
| 1 | Deterministic sample corpus, hashes and maintained probes | Implemented at documented scope. Synthetic tests and opt-in tools exist; installed assets are not committed. Review expands numerical coverage to 944 main/segment resources. R7 concerns automated gating. |
| 2 | Finite terminal poses, including old converted files | Verified numerically. Fresh/cached corpus passes; old self-pair recovery is narrowly constrained. Original executable terminal dwell remains unproved, as disclosed. |
| 3 | Same-animation request identity and authoritative phase | Identity verified. **Reopen phase acceptance:** initial-only seek plus R1/R2 does not preserve phase through pause or sustained playback. |
| 4 | Shared timing and original endpoint/segment conventions | Partial; B1a remains. Seven-rate tests validate implementation, not N versus N-1 or `startAf/endAf` semantics. R2 also prevents nominal duration from implying actual repeated cadence. |
| 5 | Conservative clip bounds and picking semantics | Verified at numerical/synthetic scope: expanded corpus contained; precise current-pose collision invalidation tested. Camera/frustum-edge full-map acceptance still separate. |
| 6 | Correct CLAMP/cycle behavior | Partial; negative-speed lock repaired, direction tested. B1b original interpretation and R2 elapsed-time preservation remain. |
| 7 | Transactional replacement and explicit fallbacks | Partial: load/decorate failures preserve visuals, but R5 breaks first-evaluation rollback and R3 breaks logical lifecycle under fallback. |
| 8 | Once/loop/hold/exit/interrupt policies | Contract/helper implemented with useful tests. **Reopen composition acceptance:** paused segment activation/resume fails R1. Existing semantic rules are largely preserved; DK2 behavior expansion remains blocked separately. |
| 9 | Pause and markers aligned to shared phase | **Reopen:** basic composer/atlas pause passes, but R1 and R2 violate the combined requirement; failed state exits bypass sound cleanup in R3. No audible full-game acceptance. |
| 10 | Remove shared mutable transform publication | Implemented foundation: defensive Position copies, retained-snapshot tests, fall serialized with planar motion. Public ECS fields are a read-only convention, not type-enforced immutability. Concurrent command/removal acceptance remains outstanding before interpolation/workers. |
| 11 | Actual-motion/gait presentation and authored cadence | Partial B2: displacement/available possession gaits implemented. Transform timestamps, discontinuities, presentation interpolation and refresh-rate/contact acceptance are absent. R2 is an additional cadence defect. |
| 12 | Representative crowd/transition measurements | CPU workload implemented and reproduced; not full renderer/game telemetry. Raw dummy-clip steady-state benchmark omits several production controls and gameplay. Full-frame CPU/GPU/draw attribution remains unmeasured. |
| 13 | Skip hidden/unchanged deformation and correct re-entry | Verified for tested entity controls and explicit Always culling; stopped terminal flushing and GL re-entry pass. Not a frustum-only scheduling system. Non-entity callers need equivalent flush ownership if stopped hidden clips can become visible. |
| 14 | Measured bounded reuse/cache ownership | Partial B3 plus R4. Opt-in LRU count/position-byte bounds verified; process-global manager/root identity and level-lifetime ownership remain unchanged. Default is correctly zero clips. |
| 15 | Verified retreat/drag/recoil/idle/alternate forms | Not implemented; B4 properly remains unchecked. Resource availability does not specify behavior. Original scenarios/reference traces are still needed. |
| 16 | GPU interpolation/instancing only if measurements justify cost | Conditional evaluation performed. Retain CPU path for now; no measured whole-frame comparison supports a GPU implementation or a universal claim it would not help. |

B1a/B1b/B4 contain genuine missing original-behavior evidence. Parts of B2/B3 are **unfinished engineering or unexecuted acceptance work**, not inherently external blockers: designing a timestamp/discontinuity protocol, building a lifecycle stress fixture and measuring level unload are actionable tasks. Missing cadence references limit behavior changes, but do not by themselves prevent those prerequisite investigations. Completion should distinguish these categories.

## Independent tests, probes and measurements

Environment: Windows workstation, Java 25.0.4.1, Gradle 9.8.1, pinned jME 3.9.0-stable. GL device: NVIDIA GeForce RTX 2060 SUPER, OpenGL 3.3.0 / driver 617.42. DK2 data read from `C:\GOG Games\Dungeon Keeper 2`; existing converted models read from `D:\Project\OpenKeeper\assets\Converted`. No full on-disk model conversion was invoked.

### Build and tests

```powershell
rtk proxy .\gradlew.bat build --rerun-tasks --no-daemon --console=plain
```

Passed: **1449 tests, zero failures/errors, four skipped**, 268 XML suites. The four skips belong to existing `MinimapAssetsTest`, which needs generated assets inside this isolated checkout. All nine applicable build tasks executed; compilation, packaging and distributions passed. Log: `build/review-build.txt`.

Useful coverage includes pose interpolation/clone/import/sparse vertices/finite endpoints/bounds/picking; metadata rates; selected segment policies/interruption; request identity; normalized marker arithmetic; position ownership; locomotion selection; texture-frame pause/material replacement; and serialization round trips.

Important blind spots: request tests use static models; most loader fixtures use empty track arrays; pause, seek and replacement are not combined; there is no cadence-invariance assertion or world-view failed-death test; cached-material mutations are untested. The four defects reproduced by synthetic diagnostics demonstrate that passing totals cannot substitute for those cases. New protocol serialization tests do not exercise delayed/disconnected real network delivery or mixed-version compatibility.

### Expanded installed-data audit

The review-only `CorpusProbe` enumerates every ANIMATING_MESH in all 46 Alcatraz creature definitions, adds flagged Start/End resources and resolves the actual filename/WAD-entry case before loading. It reuses the maintained `AnimationAssetAudit.inspect()` and explicitly checks the returned invalid/outside totals. Each resource is loaded both from the existing J3O and from a fresh **in-memory** KMF conversion.

```text
CORPUS models=944 passed=1888 failed=0 samples=1745788 nonfinite=0 outside=0
```

Log: `build/review-corpus-canonical.txt`. A preliminary probe used catalog spelling directly and produced 68 case-resolution failures; that diagnostic was corrected, and those failures are **not** implementation findings. The final audit covers catalog-referenced main/segment resources, not every model in Meshes.WAD or every DK2 release/mod. Both paths still share the OpenKeeper decoder; agreement cannot prove independent DK2 coordinate/timing parity. Corpus hashes in the assessment remain provenance references, not golden animation timelines.

### Targeted independent diagnostics

Sources: ignored `build/review/ReviewProbe.java`, `CorpusProbe.java`, `review.init.gradle`. They compile against existing main/test output without editing maintained source. Commands:

```powershell
rtk proxy .\gradlew.bat reviewProbe -I build/review/review.init.gradle --no-configuration-cache --no-daemon --console=plain
rtk proxy .\gradlew.bat reviewCorpus -I build/review/review.init.gradle --no-configuration-cache --no-daemon --console=plain
```

`reviewProbe` deliberately reports observed failures rather than treating the product as passing. See `build/review-probe-final.txt`. Its fault-injection warning, missing synthetic asset path warning and optional OGG-loader warning are expected; synthetic model loading is supplied by an injected source/asset manager. The printed observations and reproduction descriptions in R1–R5 are the durable evidence; ignored source/logs can be removed by clean builds. The pinned dependency's `AnimLayer.update` bytecode was inspected with Java 25 `javap -c -p`, confirming unconditional zeroing on completed actions.

### CPU benchmark

```powershell
rtk proxy .\gradlew.bat benchmarkCharacterAnimation -PconvertedAssets=D:/Project/OpenKeeper/assets/Converted -PanimationWorkingDir=D:/Project/OpenKeeper --no-daemon --console=plain
```

Final standalone run: `build/review-benchmark-final.txt`. Warmup 180 updates, 600 timed updates, 60 Hz, three-model mix. A second completed review run (`review-benchmark-isolated.txt`) is included as variability context; the earlier run overlapping the corpus audit is excluded from comparisons.

| Population/mode | Final median / P95 / P99 ms | Dirty position bytes/update | Calling-thread allocated bytes/update |
|---|---|---:|---:|
| 200 visible | 0.6115 / 1.1053 / 1.4114 | 811416 | 32 |
| 500 visible | 1.3195 / 2.2295 / 2.7116 | 2027016 | 0 |
| 500 explicitly hidden | 0.0459 / 0.0565 / 0.0616 | 0 | 0 |
| 500 unchanged, zero delta | 0.0891 / 0.1256 / 0.3247 | 0 | 0 |
| 1000 visible | 4.7584 / 7.0220 / 8.2177 | 4052868 | 0 |
| 1000 explicitly hidden | 0.1059 / 0.1397 / 0.5135 | 0 | 0 |

The other completed run measured 500 visible median/P95 1.6361/2.8138 ms and 1000 visible 4.1924/6.3121 ms. Variation is material: no visible-population speedup is established. Hidden/unchanged dirty-write suppression is reproduced. Allocations include the diagnostic scan and only the calling thread; small nonzero values include warmup/library/JIT effects.

Paired warmed transitions in the final run, 500 owners / 3000 replacements:

| Retention | CPU burst | Allocated bytes | Counted retained position bytes |
|---|---:|---:|---:|
| Disabled | 117.869 ms | 68,928,096 | 0 |
| Three clips per owner | 20.468 ms | 1,404,360 | 18,234,000 |

This supports a substantial isolated transition-allocation benefit, subject to R4. The retained count includes output positions and two decoded arrays, not total Java/native/GPU residency. It does not include full world control, lighting, sounds, picking, network, navigation or rendering costs. The steady-state benchmark plays the raw dummy clip, rather than the complete production request/timing stack. No new before-branch run, asynchronous GPU timing, whole-game FPS result or retained native-memory plateau was obtained.

### OpenGL and visual inspection

```powershell
rtk proxy .\gradlew.bat captureCharacterAnimation '-Pdk2Root=C:/GOG Games/Dungeon Keeper 2' -PconvertedAssets=D:/Project/OpenKeeper/assets/Converted -PanimationWorkingDir=D:/Project/OpenKeeper -PanimationShadows=true --no-daemon --console=plain
```

Passed the maintained gallery's finite-pose, pause and hide/re-entry assertions with eight creature types. Thirty-nine frames were captured; selected frames 060, 150 and 330 were independently inspected. Log: `build/review-gl.txt`; generated images: `build/animation-capture/shadows/`. Frame 450 and 465 PNG SHA-256 both equal `d78d7de404a3cd3b59232fb5ec67a975bf304b37c1e555a6b316b5e6a8e8d3b6`, confirming actual identical paused output for this scenario. No GL/shader execution failure occurred. R6 documents the visible shadow defect despite successful assertions.

The gallery normalizes scale, uses full light, disables audio, performs synchronous screenshot I/O and advances synthetic game time by frame count while composers consume application `tpf` ([CharacterAnimationCapture.java:112–144](../src/test/java/toniarts/openkeeper/animation/CharacterAnimationCapture.java#L112)). It therefore cannot establish locomotion contact, frame pacing, shared-clock agreement or original lighting/size parity. Its calls go directly to creature controls, bypassing the world bridge responsible for R3. It enables the prepared cache explicitly, unlike the production default.

## Outstanding architectural and technical risks

These are scoped risks/acceptance gaps, not additional confirmed regressions:

- **Original behavior:** no independent executable traces establish endpoint dwell, start/end field meanings, CLAMP interpretation, gait cadence or the deferred action policies. Preserve assessment B1/B4; do not reinterpret names/flags or add random idle rules as substitutes for evidence.
- **Movement continuity:** owned snapshots are useful, but transforms have no authoritative sample timestamp or discontinuity flag. Latest-position snapping remains. Pickup/drop/teleport/hauling/removal and simultaneous command stress are unvalidated. Sources: [Position.java:25–44](../src/main/java/toniarts/openkeeper/game/component/Position.java#L25), [CreatureViewSystem.java:114–133](../src/main/java/toniarts/openkeeper/game/logic/CreatureViewSystem.java#L114), [MovementSystem.java:87–105](../src/main/java/toniarts/openkeeper/game/logic/MovementSystem.java#L87). Do not add interpolation that guesses across those boundaries.
- **Concurrency:** scene, composer and material mutations remain render-thread work; no broad worker migration occurred. Serializing fall with planar movement removes one producer conflict, not races with logic-thread pickup/removal/teleport. Sequence updates are ordinary component replacement, not a cross-thread transactional protocol. Require controlled lifecycle stress before claiming race freedom; this review did not reproduce a new race.
- **Cache lifetime:** [AssetUtils.java:79–89](../src/main/java/toniarts/openkeeper/utils/AssetUtils.java#L79) still has process-global caches and a process-global prewarm flag; model/material keys omit asset-manager/root identity. Per-owner LRU bounds only its own entries. Full level cycling, different roots/managers, GC/native residency and background-load interaction remain B3 acceptance work. Retention defaults to zero, which limits exposure but does not validate opt-in behavior.
- **Visibility ownership:** [PoseTrack.java:239–246](../src/main/java/toniarts/openkeeper/animation/PoseTrack.java#L239) defers hidden samples; [EntityViewControl.java:120–124](../src/main/java/toniarts/openkeeper/view/control/EntityViewControl.java#L120) flushes them. Generic non-entity consumers such as decorative/model-viewer/effect paths need their own re-entry guarantee if a hidden one-shot stops. No concrete failing non-entity lifecycle was reproduced here; do not broaden the completion claim without a caller fixture.
- **Picking cost:** clearing collision data fixes stale precise picking, but repeated mouse picks rebuild BIH data on changing meshes. Profile update plus cursor picking in a real crowd before selecting simpler proxy picking. The CPU benchmark excludes this cost.
- **Rendering/performance:** static authored normals and LOD0-only conversion are retained compatibility choices. Draw submission, shadow-pass amplification, scene traversal and GPU upload cost may dominate at scale; none is ranked by the logical-update benchmark. GPU interpolation/instancing requires all-pass material/picking/bounds parity and measured total-frame benefit, not just fewer Java loops.
- **Network and audio:** serialization schema changes require matching builds. Real packet delay/coalescing, disconnect/resume, voice pause/cleanup and sound-contact video have not been accepted. The bounded presentation predictor and freely advancing composers need a coherent stalled-delivery policy as part of R2.

## Actionable remediation and acceptance checklist

Complete the independent regressions first; keep fixes focused and rerun the relevant build/tests after each. This review does not authorize changes or commits.

- [x] **R1, High, before integration:** make seek/segment activation pause-aware, repair saved resume state, and cover delayed clock plus paused Start/Main/End/hold/replacement. See remediation record R1 below.
- [x] **R2, High, before integration:** preserve unwrapped elapsed time/overshoot and use one phase contract for poses and markers; add refresh-rate, irregular-delta and long-run invariance tests. See remediation R2.
- [ ] **R3, High, before integration:** separate logical lifecycle from successful visual state; prove exactly one death presentation across failed art and Position notifications, and release previous activity audio on failure.
- [ ] **R4, Medium, before shipping opt-in reuse:** restore current owner material/highlight/overlay policy on attach; cover detached hover exit and changed material policies. Keep default retention disabled meanwhile.
- [ ] **R5, Medium, before claiming transactional completion:** preflight first evaluation and rollback publication-stage failure; test children/revision/enabled/cull/cache preservation.
- [ ] **R6, Medium, visual acceptance:** correct or explicitly defer authored atlas shadow coverage; add transparent-colored texel and rendered wing-frame tests. Update shadow acceptance wording.
- [ ] **R7, Low:** fail installed-asset validation on out-of-bound samples; keep historical report-only operation explicit.
- [ ] **R8, Low:** replace duplicate creature preload passes with complete policy-aware enumeration; verify static resources/segments and measure startup allocation.
- [ ] Reopen assessment items 3/7/8/9 and update implementation evidence for R1–R5; retain all five incomplete original items with explicit prerequisite versus external-reference status.
- [ ] Obtain versioned original DK2 reference timelines for B1a/B1b and individual B4 behaviors; resolve conventions only from that evidence.
- [ ] Finish B2 prerequisites and B3 level-lifecycle/residency validation, or explicitly split them out of a narrower integration scope with no completion claim.
- [ ] Run full-game movement/combat/work/pause/pickup/drop/removal/network/audio scenarios at multiple refresh rates, including hundreds of mixed visible/hidden/shadow-visible creatures.
- [ ] Collect representative full-frame CPU/JFR, allocation/GC/native memory, draw counts and GPU pass timings before choosing further acceleration. Maintain separate startup/steady-state/transition results.
- [ ] Repeat full applicable build/test suite, expanded numerical corpus, fixed-camera GL sequences and final diff/working-tree checks; record actual limits and results.

Approval should be reconsidered after the High findings and transactional contract are fixed, with the cache path corrected or unavailable for use and the remaining fidelity work explicitly scoped. The current numerical and CPU improvements are valuable, but do not establish full animation, gameplay or DK2 fidelity readiness.

## Remediation record (2026-11-19)

The review and measurements above describe `dffb07b7a`; the following entries supersede the corresponding finding's implementation status. Validation gaps remain explicit.

### R1 — resolved: paused segment activation

All live segment enable/disable operations now publish playback intent through one pause-aware helper. A seek changes the saved resume state while keeping actual composers disabled. Start/end completion and HOLD/stop paths use the same mechanism. The regression matrix covers all policies at start, main, end and completed ages, replacement under a paused ancestor, frozen composer times/callback counts and correct visible segment on resume.

Validation: production and test compilation plus loader, request and texture-atlas suites pass: **16 tests, zero failures/errors/skips** (`build/fix-r1-tests.txt`). Sustained clock correctness is handled separately by R2.

### R2 — resolved: one owner timeline and silent initial phase

`AnimationPlayback` replaces reflective segment callbacks and independent layer repetition with one unwrapped owner age. Converted composers remain sampling adapters and stay disabled, preventing a second clock from advancing them. Start/main/end boundaries carry their remainder, completion counts survive multi-wrap deltas, ping-pong direction derives from traversal parity and reentrant replacement terminates the old driver's work. World playback binds continuously to `PresentationClock`; markers read that same driver age. Stalled delivery therefore freezes poses and markers together. Local/hand/decorative playback retains a local elapsed clock. Initial or discontinuous seeks do not replay historical cycle/stop callbacks.

R1's interim enabled-intent helper is superseded by the single owner's pause gate; segment samplers cannot enable themselves. The owner also flushes pending completed hidden poses, covering generic consumers without `EntityViewControl`. A clone regression found pinned jME `BaseAction` retains its final Tween reference: `PlaybackAction` now samples and clones its own `TimedClip` directly through `Action`, preserving target/buffer isolation. Unused direction-toggle methods were removed.

Validation: equal-elapsed 25/30/60/120/144 Hz tests produce 100 traversals and 200 marker crossings for both 0.1 s and installed Imp 0.6 s durations; irregular multi-wrap and start/main/end remainder, delayed/stalled clock, pause, silent seek, reentrant replacement, actual pose and deep-clone isolation tests pass. Full build: **1456 tests, zero failures/errors, four existing minimap skips** (`build/fix-r2-build.txt`). Shadow-enabled eight-creature GL gallery passes finite/pause/re-entry checks (`build/fix-r2-gl.txt`); the R6 coverage defect remains separate. Original timing/CLAMP interpretation and full-game contact/audio acceptance remain unclaimed.
