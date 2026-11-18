# Dungeon Heart startup implementation

The Heart introduction now runs as a persistent room presentation driven by server time. It uses DK2's authored effect graph and the existing `Dungeon centre` WRAP animation throughout startup and idle. There is no separate Heart introduction clip to select, and completion does not reload the mesh or restart its animation.

## Verified DK2 behavior

The reference is the installed DKII 1.7 executable (SHA-256 `b5e4e2f6b76335e2a2e7f43bb6903bfc6f4170eabf04598955b00f512a47b449`), its KWD data, original Meshes.WAD and sound banks. The [Flame executable mapping](https://github.com/DiaLight/Flame/blob/main/mapping/DKII_EXE_v170.sgmap) identifies `CRoom::tickDungeonHeart` at `0x4D9A60`; its instructions were inspected directly. The mapping identifies addresses; it does not itself document the following sequence.

Let T be the level's game tick rate and D its `TIME_BEFORE_DUNGEON_HEART_CONSTRUCTION_BEGINS` value. The native start tick is `1 + D*T`. Levels 1/20 use D=0, level 2 uses D=15 and level 8 uses D=16; all four sampled maps use T=4. Effect simulation uses a separate 20 Hz clock.

| Time relative to formation | Native behavior reproduced |
| --- | --- |
| Before start | Emit effect 223 on game ticks. |
| 0 | Create effect 225 and room sound event 0xF1 for the current keeper. |
| 7 seconds | Create effect 314 for the current keeper; begin revealing objects 86 and 88. |
| 7 to 9 seconds | Reveal opacity follows `clamp(32*(ageTicks-7*T), 0, 255)/255`. OpenKeeper interpolates between game ticks for a smooth fade. |
| 9 seconds | Force full opacity and complete construction. Existing effect descendants may continue. |
| Mesh resource | Object metadata supplies one normal WRAP resource, with no separate introductory clip. |

Object 13 uses `Dungeon centre`, an animating mesh with 70 authored frames at 30 fps, without HAS_START_ANIMATION/HAS_END_ANIMATION or additional animation resources. The KMF contains 71 samples with matching wrap endpoints. Its converted `dummyAnimClipName` lasts 70/30 seconds and uses vertex poses, not a skeleton. Objects 86 (`DHeart Arches`) and 88 (`DHeart BigSteps`) are static.

Native room assembly creation at `0x4DB700` does not create object 96. The inspected placed-thing data for levels 1, 2, 8 and 20 also contains no object 96. The intact and cracked plugs belong to the effect graph.

The relevant authored graph is:

```text
225 [25 visual ticks; emits mesh element 406, DHeartPlug]
  death -> 224 [30 ticks; emits static/overlay elements 383 and 407]
             next -> 223 [immediate, independent sibling]
             death -> 208 [explosion, light fade duration 120 ticks]
                        generates -> 205 [beam 379 + cracked plug 403, 100-tick elements]
                                  -> 206 [rocks, dust and mist]
                                  -> 207 [100 ticks of static]
                                       death -> 209
                                                  generates -> 206
                                                            -> 210 [sparkles]
                                                                 next -> 215 [immediate]
                                                                          Group_plugbroke
                                                                          -> nine mesh parts, element 396
314 -> sparkle element 525
```

The native creator at `0x440350` creates `nextEffectId` immediately. `deathEffectId` waits for the parent's emission lifetime and its own element drain, independently of next-effect descendants. This is why serializing the entire graph behind child completion is incorrect.

Sound event 0xF1 in `ROOM_DUNGEON_HEART` resolves to `Global/SetPiecesHD/Heart form Alph.mp2`; heartbeat event 0x150 resolves to `Global/HeartHD/k1Heart.mp2`. The object category `OBJECT_HEART` does not provide the formation sound.

## Lifecycle and ownership

1. [FiveByFiveRotatedController.constructNonDestroyed](../src/main/java/toniarts/openkeeper/game/controller/room/FiveByFiveRotatedController.java) creates the Heart, arches and three rotated steps. It publishes [DungeonHeartIntroduction](../src/main/java/toniarts/openkeeper/game/component/DungeonHeartIntroduction.java) on that room, including all part entity IDs and the authoritative waiting/formation epochs. Reconstruction copies the previous epochs and completion state. The duplicate persistent plug is removed.
2. [GameLoopManager](../src/main/java/toniarts/openkeeper/game/state/loop/GameLoopManager.java) runs [DungeonHeartConstruction.start/processTick](../src/main/java/toniarts/openkeeper/game/logic/DungeonHeartConstruction.java) on the game-logic loop with room construction/destruction. It schedules the monotonic epochs once, publishes completion, and shifts both epochs over pauses. GameLoop.pause acknowledges the end of an in-flight tick before the scheduler publishes pause state, so room reconstruction/destruction cannot be overwritten by a stale component copy. It does not write object visibility or emit transient presentation events.
3. [NetworkServer.initialize](../src/main/java/toniarts/openkeeper/game/network/NetworkServer.java) registers the component with the existing FieldSerializer. [GameClientService.getServerTime](../src/main/java/toniarts/openkeeper/game/network/game/GameClientService.java) uses Ethereal's remote clock; local sessions use System.nanoTime. The installed sim-ethereal 1.8.0 host uses NanoTimeSource/System.nanoTime. Its RemoteTimeSource returns zero before receiving a time sample; presentation explicitly waits for synchronization.
4. [GameClientState](../src/main/java/toniarts/openkeeper/game/state/GameClientState.java) attaches [DungeonHeartIntroductionState](../src/main/java/toniarts/openkeeper/view/DungeonHeartIntroductionState.java), prewarms effects/audio before loadComplete, and calls start only at onGameStarted. The state observes EntitySets and a nonblocking model lookup; it never makes synchronous client EntityData queries.
5. The presentation consumes crossed milestones in timestamp order, including after a render stall. It reconstructs waiting and formation effects from their authoritative epochs to the current age, seeks available formation audio, and suppresses an already-expired audio cue using its loaded duration. Creation/reveal cues and formation audio belong to the current keeper; furniture reveal and normal animation are tracked per assembly.
6. The existing object loader selects the normal animation. The presentation owns its absolute composer phase with automatic speed zero. It retries composers arriving under an existing entity root and rebinds when animated children are replaced. From formation onward the same composer, mesh and phase continue across the nine-second completion boundary. Before formation, its pose is held at zero beneath the plug presentation; native pre-formation mesh phase still needs matched capture validation. [ObjectViewControl.setTargetState](../src/main/java/toniarts/openkeeper/view/control/ObjectViewControl.java) compares animation fields, so republished visibility does not reload/reset the animation.
7. Room destruction removes the component; room removal, view cleanup and level teardown cancel the effect graph/audio. Opacity overrides/materials are restored, obsolete furniture references are released, and the composer is returned to ordinary playback. Introduction cleanup precedes effect-manager cleanup so registered lights can be removed. A temporarily missing furniture entity does not erase the persistent epoch.

Normal health, defeat, gold storage, creature spawning and trigger behavior retain their existing owners. The introduction is presentation state, not a prerequisite for gameplay.

## Effect and rendering changes

[EffectPlayback](../src/main/java/toniarts/openkeeper/view/effect/EffectPlayback.java) provides a finite, seeded, 50 ms simulation context for this sequence. It reuses [VisualEffect](../src/main/java/toniarts/openkeeper/view/effect/VisualEffect.java) with an optional timed path; ordinary effect callers retain their existing update path. It processes every required step, without a frame-delta cap that would discard phases. Empty graphs skip simulation work, retain a numerically normalized fractional step, and release their managed-control references.

jME flattens controlled descendants during logical updates. A context-root-only override would therefore double-update particles and physics. Timed controls use a manual-update wrapper while forwarding their normal render callbacks. The application remains responsible for geometric updates; the effect context resolves world transforms lazily instead of running an extra scene traversal.

The timed path separates emission HP from element HP, emits the authored packet each visual tick, sizes sprite pools for live overlapping packets, and drains elements before the death effect. Next-effects remain immediate. It recursively reads the converted group hierarchy and preserves all nine `Group_plugbroke` offsets. A solid collision kills one debris element without cancelling its siblings. Mesh element fade and authored explosion light fade are applied from lifetime metadata. A transform parent preserves static resource scale, including `plugbeam`'s 3x scale, while the element control animates its own scale.

Emitter iteration is ordered for timed playback. Spawn position, velocity, lifetime, orientation, spin and random sprite-frame choices use the cue-local RNG. Each root cue derives its stream from room identity, effect ID and occurrence; its descendants share that stream. Omitting a waiting cue cannot perturb formation or reveal randomness. jME's private global birth randomness is replaced for the visible particle fields that it initializes. This is deterministic OpenKeeper playback, not a claim to reproduce DK2's original random-number stream.

[SpatialOpacity](../src/main/java/toniarts/openkeeper/view/control/SpatialOpacity.java) uses a per-instance material override and temporary material clones. Opaque surfaces use alpha blending without depth writes during reveal; authored additive blending stays additive. Full opacity restores the original material and queue bucket. Cleanup removes the override. AnimationLoader increments a model revision when replacing children; the presentation rebuilds opacity bindings only when that revision or root changes, restoring obsolete materials without a per-frame geometry traversal. [Vertex.j3md](../assets/MatDefs/DkLighting/Vertex.j3md) and [Vertex.frag](../assets/Shaders/DkLighting/Vertex.frag) expose/apply the opacity uniform.

[PoseTrack.getDataAtTime](../src/main/java/toniarts/openkeeper/animation/PoseTrack.java) now interpolates decoded positions between adjacent sparse KMF frames. It normalizes old converted caches whose first sample is at 1/30 rather than zero, preserving the full wrap period without asset reconversion. Two reusable position arrays and cached frame indices avoid per-frame allocation; cloned tracks have independent caches.

[RoomSoundState.tickHeart](../src/main/java/toniarts/openkeeper/game/state/RoomSoundState.java) defers ordinary heartbeat calls during formation and follows the mesh period from frames/fps rather than accumulating a separate two-second timer. The exact audible beat offset within the original animation cycle remains a capture-validation item.

## Review corrections

- Delayed effect/element asset creation now fails at the affected branch, cleans partial resources and reports the failure once per failed definition in that parent. Other effects and the furniture/idle timeline continue. Root asset prewarming still logs unavailable assets instead of making them a level-load requirement.
- Pause waits for active game-loop ticks before publishing timeline components. Resume rebases waiting and formation epochs before restarting the loops. On clients, presentation/audio wait for the rebased component even when the resume notification arrives first.
- Waiting history is reconstructed from a persisted epoch. Root cues have independent random streams, and empty-graph clock normalization prevents an almost-full floating remainder from adding an extra tick.
- Model revision changes invalidate retained fade surfaces. Original and replacement materials are restored independently.
- Audio resumes only after phase synchronization, seeks before resuming a paused backend, and never restarts a stopped/expired voice. These are backend-contract tests; they do not establish the original audible beat offset.

## Automated and runtime evidence

Validation on 2026-11-18 used Java 25, jME 3.9.0-stable and Gradle 9.8.1.

- `rtk proxy .\gradlew.bat test build --offline --rerun-tasks --console=plain`: 1,426 tests passed, zero failures/errors; the four proprietary-data integration tests are skipped without their opt-in environment variable. Application JAR and distributions build successfully.
- The four [DungeonHeartAssetsTest](../src/test/java/toniarts/openkeeper/view/effect/DungeonHeartAssetsTest.java) integration tests passed against the installed DK2 data and converted assets. They validate graph/clip metadata and compare complete particle/transform snapshots at 1, 3, 6, 8.5 and 20 seconds between 100 Hz updates and a single large update. All nine plug parts are present during breakup; the graph is empty by 20 seconds. Whole-presentation snapshots also match early and late first updates for delays 0, 15 and 16 seconds, including waiting, formation, reveal and drained idle. Representative Imp walk/death tracks remain seekable through endpoints and rewinds.
- Focused tests cover epoch scheduling, large skips, pause/resume, room isolation, reconstruction, missing furniture, destruction, network serialization, late composer arrival, unsynchronized remote time, cue uniqueness, material isolation/restoration, interpolation/wrap, control update ownership, RNG isolation, art scale and collision cleanup. Review regressions additionally cover delayed missing assets, in-flight reconstruction/destruction at pause, same-root geometry replacement, imported/scene-cloned tracks, and recording-backend audio seek/pause/resume/expiry/cleanup.
- The real-asset probe observed effect 224 at 1.25 seconds, explosion 208 at 2.75 seconds, and group breakup at 7.75 seconds. These values emerge from metadata and element drain, not hardcoded timers.
- [DungeonHeartStartupCapture](../src/test/java/toniarts/openkeeper/view/DungeonHeartStartupCapture.java) successfully rendered the production ObjectViewControl/AnimationLoader/presentation path using OpenGL 3.2 / GLSL 1.50 with actual converted DK2 assets. Eleven captures cover waiting through 15 seconds. Inspected frames show the beam, plug/static effects, reveal, breakup and settled architecture without render errors. This fixture uses neutral full lighting and disables audio; it does not launch a complete gameplay session.
- Local captures are generated under `build/validation/dungeon-heart/heart-1.png` through `heart-11.png`. The corresponding ages are -0.2, 0, 1.3, 2.8, 6.9, 7.5, 7.8, 8.5, 9, 10 and 15 seconds. Proprietary meshes/textures and captures are not added to source control.

To rerun asset integration checks in PowerShell:

```powershell
$env:OPENKEEPER_DK2 = 'C:/GOG Games/Dungeon Keeper 2'
$env:OPENKEEPER_CONVERTED_ASSETS = (Resolve-Path 'assets/Converted').Path
rtk proxy .\gradlew.bat test --tests '*DungeonHeartAssetsTest' --rerun-tasks --offline
```

The normal suite does not require these variables or a DK2 installation. Run it in a separate shell without OPENKEEPER_DK2 to exercise the default skip behavior.

To generate the OpenGL captures:

```powershell
rtk proxy .\gradlew.bat captureDungeonHeartStartup '-Pdk2Root=C:/GOG Games/Dungeon Keeper 2' --offline
```

Use `-PconvertedAssets=...` to override the converted asset directory. This task is opt-in and requires a graphics context; ordinary tests/builds do not launch it. Frame-strip end probes can log missing terminal texture names; these are AssetUtils's existing frame-count detection, not a skipped introduction phase.

## Acceptance and remaining parity work

Completed checks:

- [x] Select the verified normal WRAP resource; no guessed start/end clip.
- [x] Play formation/reveal cues once per assembly timeline and retain completion across furniture reconstruction.
- [x] Keep waiting, formation, reveal and idle independent of render frame partitioning.
- [x] Preserve phase through completion and pause/resume; wait for the network clock.
- [x] Respect graph metadata, immediate next-effects, independent child drain and nine-piece breakup.
- [x] Cancel owned effects/audio and release obsolete presentation/material references.
- [x] Pass focused tests, complete the default suite/build, and render actual converted assets.

Remaining validation, in priority order:

1. Capture DK2 and OpenKeeper from the same camera in levels 1, 2 and 8. Align at sound event 0xF1, verify the seven/nine-second milestones, and compare the intact plug, cracked plug, beam, debris motion and architecture reveal. Native update quantization/element drain has not been recorded frame by frame; the 2.75/7.75-second graph milestones are metadata-derived compatibility targets.
2. Record/listen to formation and heartbeat in a full level. Confirm the original heartbeat's intra-cycle offset and whether any heartbeat overlaps formation. The current phase-zero heartbeat choice and formation suppression are implementation choices pending that recording, not verified native audio timing.
3. Exercise remote multiplayer with different process uptimes, loading stalls and pause/resume at each phase. Component serialization, unsynchronized clocks and resume-notification/ECS ordering are tested; a complete multi-process session has not been run.
4. Profile a full level during the longest startup delay on a slower renderer. The implementation bounds simulation to 20 Hz and reuses pose buffers, but no GPU/CPU timing or allocation benchmark is claimed.
5. Compare existing effect physics and light falloff against the original renderer. Authored assets/flags/periods are consumed, but trajectory integration, light fade interpolation and DK2's random stream are not established as pixel-exact equivalents.

Fresh level entities start a fresh timeline. Epoch persistence is scoped to the running session; restoring monotonic timestamps across a different JVM would require save-game clock rebasing if/when that lifecycle is implemented. This change does not claim to implement save-game loading or Dungeon Heart destruction visuals.
