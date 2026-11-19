# Animation improvement finalization and maintainer handoff

Date: 2026-11-19. Branch: `feat/animation-next-generation`. Validated source: `c21e5af75770a467c489a72d4212d6b5fc0a6e2c` (the final correction changes a test fixture only).

**Handoff decision: draft for maintainer review.** The delivered compatibility fixes, optional presentation improvements and measurement infrastructure are reviewable. The entire next-generation roadmap is not complete or release-qualified. In particular, the strict level-residency diagnostic still fails on the pinned dependency's database retention. Neither a passing ordinary build nor draft publication closes that acceptance gate.

## Scope and review baseline

The [original assessment](character-creature-animation-architecture-assessment-2026-11-18.md), [independent review and R1–R8 remediation](character-animation-code-review-2026-11-19.md), [proposal](character-animation-next-generation-proposal-2026-11-19.md) and [maintenance guide](character-animation.md) remain the source of requirements and implementation details. The proposal's section 14 verifies **every N01–N18 item** against its original exit criteria; section 20 maps the continuation's focused commits. Historical test counts and line references in the original reports remain revision-scoped.

The feature contains the unchanged `09bdf79b3` baseline submitted in [PR #1325](https://github.com/tonihele/OpenKeeper/pull/1325). This follow-up does not close or supersede that PR. A PR to upstream `master` includes that pending prerequisite's history; review the new work separately with `git diff 09bdf79b3..feat/animation-next-generation`. The material-lifecycle, world-filtering and Dungeon Heart branches are separate work and are not merged here.

Implemented and tested slices include:

- Optional coherent root/request presentation, epoch cuts, shortest-arc yaw, pause/reset and component-order recovery; hauled creatures follow presented carriers. Immediate possession/attachment policies and deterministic simulation behavior are retained.
- Asset-manager-owned caches, coalesced model preparation and bounded immutable Float32 source sharing with private owner output. Sparse CPU sampling and existing converted assets remain compatible fallbacks.
- Optional seeded locomotion phase variation without gameplay RNG changes, with one pose/marker timeline.
- Nonblocking loading cancellation, guarded render publication, prewarm ownership, early-detach cleanup, current-frame state disabling, server preparation/command retirement and cleanup after nested system failures.
- Retirement of pinned jME render-sort scratch through public APIs, preserving comparators and nonempty pending draws.
- Maintained source/corpus, topology, representation, held-pose error, real-GL, delivery, residency and crowd diagnostics. Numeric GPU experiments remain test-only; there is no production GPU backend or speculative procedural rig.

Root interpolation and phase variation remain disabled by default. Packed-source and prepared-clip retention budgets default to zero. Networking component extensions require matching client/server builds. Converted-model compatibility remains at version 16. Cache budgets count their documented primitive/output payloads, not complete Java/native/GPU residency.

## Final correction and cleanup

`c21e5af` fixes the remote-motion diagnostic's listener probe. A preflight failed when a loopback-only probe released an ephemeral range and jME subsequently could not bind a wildcard channel. Probing now matches jME's wildcard/exclusive TCP and UDP bindings and randomizes server ranges below the usual client ephemeral range. Both transport scenarios pass twice after the correction. A port handoff still exists between the probe and jME; bind failures are reported, not suppressed or converted to successful tests.

The branch diff was inspected for generated/proprietary binaries, captures, profiles, CSVs, heap dumps, credentials, unrelated paths and whitespace defects. None is included. Maintained `src/test` benchmarks are intentional, documented validation infrastructure; they are not disposable one-off experiments. Generated output stays in ignored `build/`, and local runners/logs stay outside tracked source. Unique local evidence and the canonical checkout's four pre-existing untracked documents are preserved. No blanket clean, forced push, shared-history rewrite or unrelated refactor is used.

The final review traced loading publication/cancellation, server command admission versus retirement, loop shutdown, render-queue ownership, motion epoch/order handling, and packed-source clone/output isolation. Tests exercise these paths. This self-review is not an independent maintainer approval or exhaustive proof of all engine interleavings.

## Executed validation

Windows, JDK 25, pinned jME 3.9.0-stable and Zay-ES 1.6.0; installed GOG DK2 data and existing converted assets. Real GL checks used the available NVIDIA RTX 2060 SUPER. Commands were run sequentially for performance-sensitive workloads.

| Check | Actual result | Evidence |
|---|---|---|
| Full `build`, separate installed minimap task, remote audit, `--rerun-tasks` | **Success**, all 13 tasks executed; **1,555 JUnit tests, 0 failures/errors, 4 existing minimap skips**; separate **4/4 minimap tests pass without skips** | `animation-final-maintainer-build-verified.txt`; both XML report directories |
| Deterministic wire/ECS delivery, included in ordinary `check` | **24/24 schedules pass** | Same build log; `build/animation-motion-delivery.csv` |
| Real localhost jME/Zay-ES transport | **2/2 scenarios pass twice** after probe fix; last run accepts regular/coalesced 81/32 of 81 publications; max component error 0.000005992/0.000006148 | `animation-final-maintainer-remote.txt`, verified build log |
| Shadowed gallery with 64 MiB packed-source budget | **39/39 pose hashes and 39/39 PNGs match the earlier baseline**; pause PNGs 450/465/480 identical; representative current shadow captures inspected | `animation-final-maintainer-visual.txt`; current and baseline `build/animation-capture/shadows/` |
| Synthetic color/shadow alpha and legacy black-key coverage | **4/4 cases pass** | Same visual log; `build/animation-coverage/` |
| Actual local Alcatraz session | **360 active frames, 48 sampled owners, all 48 paused owners**, 7,217,144 packed primitive bytes; successful exit | Same visual log, `MOTION_GAME_PASS` |
| Production-owner CPU and transition workloads | **3 repeats pass**; hidden/unchanged behavior and allocation observations below | `animation-final-maintainer-cpu.txt` |
| Paired sparse/prototype/production-packed CPU workloads | **3 alternating-order repeats pass** with exact output comparison | Same CPU log |
| Strict actual level residency, two levels, 64 MiB packed budget | **FAIL**: client/map/entity/server state weak counts are zero; retained database count grows **1 → 2** | `animation-final-maintainer-residency.txt`; `build/animation-level-residency/packed-65536/` |

The named preflight logs are local under `D:/Temp/`; generated reports are intentionally not Git artifacts. Earlier ten-cycle heap-root and corpus evidence remains in proposal sections 15/17. The final two-cycle rerun confirms the previously diagnosed failure; it is not a new ten-cycle plateau or GPU-byte measurement.

The local-game run also logs a 26 ms steering tick and missing converted particle textures (`scling15`, `SM16`, `mist8`, `gsparkle16`). The short smoke succeeds but does not certify sustained whole-game frame pacing or completeness of this local converted-asset installation. No original-DK2 motion/style, audible-contact, exhaustive combat/possession/hand, physics or full remote-gameplay approval is inferred from it.

### Reproduction

The ordinary suite requires no proprietary data:

```powershell
.\gradlew.bat build auditAnimationRemoteMotion --rerun-tasks --no-daemon --console=plain
```

Installed-data diagnostics use the properties described in the maintenance guide. For example, provide `dk2Root`, `convertedAssets` and `animationWorkingDir`, then run:

```powershell
.\gradlew.bat captureCharacterAnimation captureAnimationCoverage smokeCharacterMotion `
  -PanimationShadows=true -PanimationPackedKiB=65536 --max-workers=1 --no-daemon
.\gradlew.bat benchmarkCharacterAnimation benchmarkSharedVertexClips `
  -PanimationRuns=3 --max-workers=1 --no-daemon
.\gradlew.bat auditAnimationLevelResidency `
  -PanimationLevelCycles=2 -PanimationPackedKiB=65536 --no-daemon
```

The last command is expected to fail while the confirmed dependency defect is present. It is opt-in and is not silently excluded from this report. To repeat the four installed minimap tests without replacing the ordinary report, the executed local Gradle init file registers:

```groovy
allprojects { p ->
    p.afterEvaluate {
        if (p == p.rootProject) {
            p.tasks.register('testInstalledMinimapAssets', Test) {
                dependsOn p.tasks.named('testClasses')
                testClassesDirs = p.sourceSets.test.output.classesDirs
                classpath = p.sourceSets.test.runtimeClasspath
                useJUnitPlatform()
                workingDir '<checkout containing assets/Converted>'
                filter { includeTestsMatching 'toniarts.openkeeper.view.minimap.MinimapAssetsTest' }
            }
        }
    }
}
```

Run it with `-I <init-file> testInstalledMinimapAssets`; XML stays in `build/test-results/testInstalledMinimapAssets`. The local init file does not change repository build policy or bundle installed assets.

## Performance and fidelity observations

The fresh eight-archetype isolated sampler comparison uses 180 warmup updates and 600 samples per case. At 500 owners, paired sparse/production-packed medians are **5.8031/1.2779**, **6.2310/1.0311**, **5.0102/1.2676 ms**; at 1,000 they are **10.6281/3.4717**, **11.2136/3.8525**, **7.0994/3.0258 ms**. These are approximately **75–83%** and **57–67%** lower median sampling/output times in this fixture, respectively. P95 reductions are **66–76%** and **56–70%**. This repeats the optional sampler's benefit; it is not a whole-game FPS or GPU speedup.

All measured 500/1,000-owner sparse and production-packed cases report zero calling-thread steady-state allocation. At 500 owners, owner scratch primitive payload is 4,461,720 versus 2,230,860 bytes, plus **1,387,104 shared packed source bytes** for the cohort. This is not a claim that total residency halves; templates, sparse source, Java objects, direct/output buffers, bake peaks and GPU allocations are separate.

The separate production-control fixture reports 500 visible-owner medians **1.4129/1.5745/1.4888 ms**, P95 **2.8148/3.4844/3.1668 ms**, hidden medians **0.0721/0.0708/0.0711 ms**, unchanged medians **0.0288/0.0286/0.0287 ms**, and zero calling-thread update allocation. These remain near the preceding report's repeat (visible median/P95 1.5232/3.6849 ms); timing variance does not establish a new algorithmic gain.

For 3,000 transitions at 500 owners, retention off/three clips costs **142.220/53.602**, **114.833/39.404**, **74.955/31.183 ms**. Allocated bytes are approximately **72.2–72.5 MB / 5.74–5.93 MB**, while retained position accounting is **18,234,000 bytes**. Retention remains disabled by default; this burst/allocation benefit does not satisfy lifetime acceptance.

The prior controlled rendered crowd comparison remains in the proposal: frame P95 reductions of 37–47% at 500 owners and 49–60% at 1,000, with paired identical captures, within that isolated scene. It was not rerun as a new GPU-performance claim during this finalization. The new shadow-gallery parity check independently confirms unchanged rendered output for its fixed scenarios.

The residency rerun observes first/second readiness at **5,494/827 ms**, with maximum loading-frame times **1,729/283 ms**. First access is not controlled OS-cold loading, and this is not isolated animation-preparation latency. These visible loading hitches remain a performance limitation requiring attribution and render-work budgeting; no startup responsiveness improvement is claimed. Post-retirement tracked packed payload stays 7,217,144 bytes, but the database leak prevents overall lifetime acceptance.

## Remaining gates and actionable disposition

- **Confirmed dependency blocker, N11:** Zay-ES 1.6 registers an inner database reporter in a static registry and has no working public unregister/close path. Heap-root evidence and fresh strict failures are recorded. Fix ownership in a maintained dependency release/source change, then repeat strict ten-level and cancellation stress runs. No reflection, class shadowing or global database reuse is installed to conceal the leak.
- **GPU-residency blocker, N01/N11/N12:** the earlier WPR attempt failed with Windows policy error `0xc5585011`; no new privileged trace was obtained in this preflight. Establish trustworthy native/GPU allocation tracing, then prove disposal/plateau. Renderer object counts and known-unreliable GPU Process Memory counters are not byte-residency acceptance.
- **Reference/data prerequisites, N01/N03–N09/N15/N18:** acquire version-bound original action recordings and reviewed contact/stride/correspondence/head/limb/rig metadata before changing classic style, timing or deformation. The supplied DK2 installation does not provide approved semantic annotations.
- **Engineering still open, not externally blocked:** N03 runtime catalog consumers; N10 adaptive multi-view/shadow scheduling; N12 all-pass/material/picking/resource integration; N14 further runtime representation/key-reduction/animated-LOD trials; N01/N02 complete gameplay, camera, cross-process authentication and measured network costs. The proposal retains exact acceptance criteria. These are not completed by the shipped diagnostic or optional subsets.
- **Conditional follow-ups:** N13 instancing, N15 inferred rigs, N16 worker evaluation, N17 maintained compute support and N18 inertialization require their preceding measured contracts. No speculative production backend or measured rejection is claimed.

## Publication checks

- [x] Verify current upstream integration: fresh fetch retains `upstream/master` at `cd78e565462bd3ab50ef99c86cac7b54c3bee04e`, already an ancestor. No merge/rebase is needed.
- [x] Preserve the meaningful baseline and each focused implementation/test/documentation commit; the final fixture correction is `c21e5af`.
- [x] Execute full applicable headless tests, installed-data checks, actual GL/game checks, CPU repeats and the strict diagnostic; explicitly record the diagnostic failure.
- [x] Review tracked path scope, sensitive/generated file signatures, whitespace, ownership and final source diff; retain unrelated work.
- [x] Keep unmet N01–N18 exit criteria unchecked and settings conservative.

The publication target is the fork's `origin/feat/animation-next-generation` (`adrian-andrzjewski/OpenKeeper`), with a draft PR to `tonihele/OpenKeeper:master` and a maintainer review request. Remote SHA equality and PR state must be verified after publication; this local report does not substitute for that check. Full initiative approval remains withheld until the stated gates are satisfied.

### Verified publication receipt

The branch was pushed normally and its remote SHA verified as `bbcaeb642c8d55f11bfce6b7164b91d4b16f140e`. [Draft PR #1335](https://github.com/tonihele/OpenKeeper/pull/1335) is open against `tonihele/OpenKeeper:master`, with that exact head and the maintainer review request to `@tonihele` in its description. Formal reviewer assignment is left unset while the PR remains a draft. The [hosted build at this checkpoint](https://github.com/tonihele/OpenKeeper/actions/runs/44531703767/job/135586407921) succeeds; DeepSource is **skipped**, not a passed analysis. The connector could read the public PR but lacked creation permission; authenticated GitHub CLI creation succeeded. There was no forced push, history rewrite or merge.

This receipt is a documentation-only follow-up to the verified checkpoint. Any later publication SHA must still be checked against the remote; no source/test result is inferred from editing this receipt. The validated source remains `c21e5af` and the strict residency limitation remains unresolved.
