# O1 entity material lifecycle: critical code review

**Original verdict at `7ff4765`: Request Changes.** The consolidation has a sound direction and useful CPU savings, but three reproduced lifecycle defects should be addressed before merge. Retain the architecture and fix its transition handling; another parallel material cache would make integration harder.

**Follow-up:** R1–R3 have been corrected and validated in the follow-up changes. The original verdict and pinned findings below describe reviewed head `7ff4765`; they are retained as review history. See [follow-up fixes and validation](#follow-up-fixes-and-validation) for the current result. Maintainer consolidation agreement remains pending.

## Review target and evidence

Reviewed on **18 November 2026**:

| Item | Revision / scope |
| --- | --- |
| Consolidated [PR #1310](https://github.com/tonihele/OpenKeeper/pull/1310) | `7ff476556c2c3ef5c75f53bfe37f7bd3af2c9b21` |
| Latest fetched upstream `master` and PR base | `16f1df4617042db56e99ead51dcfc96e7325351f` |
| Actual comparison | `git diff upstream/master...HEAD`: 25 files, 1,509 additions, 154 deletions |
| Coordination | [Issue #1306, O1](https://github.com/tonihele/OpenKeeper/issues/1306); maintainer integration agreement remains pending |
| Implementation assessment | [Architecture, contracts and validation record](entity-material-lifecycle.md), including the locally archived architecture and GPU investigations |

The review examined the actual production/test diff, related PR diffs and discussions, entity and effect creation/destruction, animation and material replacement, hand/menu/viewer consumers, lighting publication, shaders, and recorded validation artifacts. Earlier implementation claims were treated as evidence to check, rather than acceptance criteria already satisfied.

**Executed during this review:** external Java observation probes against the already built PR classes; actual Level20 effect-definition loading and finite-effect completion; comparison with frozen master `AssetUtils.applyFullLight`; source/class provenance checks; re-counting archived test XML; and live PR/CI inspection. Production source hashes matched the validation manifest, and no compared main-class hash differed. The probes use real lifecycle/provider/emitter code and real material definitions without creating an OpenGL context.

**Previously executed, inspected here:** the full build/test run, asset framebuffer comparisons, gameplay inspection, and performance captures. The full suite, interactive Model Viewer, and GPU benchmarks were **not rerun during this review**. A parameter/shader defect below is not presented as a newly captured screenshot. Probe exit success means the observations completed, not that the defective behavior passed a regression assertion.

All source line references below are pinned to the reviewed PR head. This review changes documentation only. Experimental probes and licensed assets remain outside the submitted diff.

## Overall architectural assessment

| Area | Assessment |
| --- | --- |
| Authority and separation | `EntityMaterialSetup` owns registrations, scheduling, retained materials and binding identity; `JmeMaterialProvider` performs conversion/isolation. This is a useful, small boundary. |
| Injection and configuration | Provider/lifecycle injection supports focused tests. An immutable policy record and constructor-time diagnostic cache switch are sufficient for the current scope; no additional service/factory framework is justified. |
| Steady-state CPU work | Stable roots avoid recurring material discovery. Moving centers and light image contents remain independently updated. Work remains proportional to registered roots/materials, so accurate removal is essential: R1 violates that premise. |
| Readiness and invalidation | Resource publication is coherent, and ordinary retained-material rebinding works. Deferring *all* structural preparation until ambient readiness leaves newly restored materials outside that protection: R3. |
| Shared materials | Identity deduplication and instance isolation are appropriate. Nested registration boundaries protect independent policies. Their interaction with the compatibility full-bright facade breaks a real consumer: R2. |
| Thread ownership | First update/flush claims the owner thread; later access is checked. Map/effect lighting lookup waits for map publication. No additional confirmed race was found in the traced paths. This remains a single-owner design, not a concurrent queue API. |
| Rendering integration | Late flush covers control-driven animation/material replacement before submission. Current GLSL, terrain and postprocessing are unchanged. Combined #1012 page lighting/SMAA compatibility has not been rendered. |
| Maintainability | Explicit mutation notifications are pragmatic. Missing notifications remain a maintenance risk, but the defects below arise even when existing notifications execute correctly. |

Useful code boundaries are [EntityMaterialSetup.java:23–46][lifecycle-owner], [JmeMaterialProvider.java:29–69][provider-prepare], and [SceneLights.java:135–178][binding]. Keep these boundaries. Extend the regression coverage around their callers and state transitions.

## Findings by severity

| ID | Severity | Classification | Finding |
| --- | --- | --- | --- |
| R1 | **High** | Confirmed; reproduced with real parsed game data | Naturally completed artless lit effects leak material registrations and permanent frame-loop work. |
| R2 | **Medium** | Confirmed material-state regression; shader consequence traced | Model Viewer full-bright traversal skips registered mesh particles, which then wait indefinitely for a nonexistent world context. |
| R3 | **Medium** | Confirmed lifecycle-contract defect; ordinary gameplay frequency unestablished | Material restoration/clone reuse during deferred readiness can leave old light/ambient textures attached to the geometry. |

No Critical defect or additional independently actionable Low defect was established. GPU timing variability and broader visual coverage are validation gaps, not proven additional production bugs.

### R1 — High: artless lighting effects leak registrations on natural completion

**Primary locations:** [VisualEffect.java:185–189][effect-target], [769–774][effect-register], and [856–870][effect-completion]. Supporting lifetime paths: [VisualEffect.java:158–169][effect-placeholder], [268–279][effect-control], [883–897][effect-remove], [EffectManagerState.java:72–82][manager-update], and [118–124][manager-clear].

**Technical evidence and execution path**

1. `load()` initializes `model` to an empty `Node`. With `effect.getArtResource() == null`, that node is never attached and never receives the `EffectControl` that unregisters rendered models on death/hit.
2. The empty node nevertheless becomes `lightTarget`. If the effect has a thing light, `registerLightTarget()` registers it as `WORLD`.
3. Finite natural completion removes the light and effect scene node and returns `false`, without unregistering `lightTarget`.
4. The manager removes the completed `VisualEffect` from `activeEffects`. Later `clearActiveEffects()` cannot reach it. Explicit `removeEffect()` has cleanup, but the natural-completion branch does not call it.
5. The lifecycle's strong `entries`/`active` collections retain the node, control and entry. Every `update()` continues visiting the entry, even though its material list is empty ([EntityMaterialSetup.java:31–34, 124–126][lifecycle-storage]). Owner disposal eventually clears it; ordinary effect completion does not.

This is a valid asset case. The installed Level20 effect catalog contains **83 definitions with null art and an enabled light**; that count is not a claim that all 83 are finite or reachable in one session. The probe used actual effect **242, “Mana2heart Light”**, whose art is null and death-effect ID is zero. Ten ordinary `load(..., false)` / `update(0.1f)` cycles produced:

```text
REAL_EFFECT id=242 name=Mana2heart Light death=0 art=null
REAL_ARTLESS worldChildren=0 retainedRegistrations=10
REAL_ARTLESS_AFTER_CLEAR registrations=10
```

A parsed fixture with null art reproduced the same result and confirmed that its thing light had been removed. The orphan material registration is the new leak; a scene traversal or attached-geometry binding inspector cannot see it.

**Impact:** heap retention and per-frame CPU work grow with the number of completed effects, rather than live effects. This undermines long-session performance and cleanup guarantees. The observation does **not** establish a distinct GPU allocation leak or retention of every completed effect's entire object graph.

**Recommended fix:** do not register a material target for a resource-less placeholder. Also release any remaining target registration on terminal natural completion, reset the target/registration state, and keep explicit removal idempotent. A small shared target-cleanup helper can cover restart, explicit removal and natural completion; do not clear an entire shared material owner to remove one effect. Preserve child-effect and infinite-restart behavior.

**Required regression:** repeatedly complete finite artless lit effects normally and assert that scene children, thing lights and registration count return to their initial values *before* manager disposal. Cover generated children, nested effects, delayed context, and infinite/self-death restart remaining bounded. Existing effect tests at [EntityMaterialSetupTest.java:411–474][effect-tests] use a model-bearing fixture and explicit removal/restart, missing this path.

### R2 — Medium: registered mesh particles lose Model Viewer full brightness

**Primary locations:** [JmeMaterialProvider.java:72–75][provider-boundary], [AssetUtils.java:353–354][fullbright-facade], and [EffectEmitter.java:223–225][emitter-register]. Consumer: [ModelViewer.java:728–746][viewer-preview] and [797–816][viewer-fullbright].

**Technical evidence and execution path**

1. Model Viewer constructs its effect manager without a world-light supplier ([ModelViewer.java:280–282][viewer-manager]). Without `PlayerMapViewState`, `EffectManagerState.getLightRules()` returns null ([191–203][manager-rules]). This is intentional: `setupModel()` explicitly says previews have no map and applies full brightness.
2. `previewEffect()` loads the effect before calling `setupModel()`. Initial mesh elements are emitted immediately ([VisualEffect.java:364–380][effect-elements]); their instances register with `WORLD` policy.
3. `AssetUtils.applyFullLight(previewRoot)` now uses the provider. The provider prunes every nested live registration, including the uncached compatibility path. Those mesh instances are skipped.
4. Their owning lifecycle also cannot fix them: `WORLD` preparation waits for a non-null, ambient-ready binding ([EntityMaterialSetup.java:148–155][readiness-gate]). That context never appears in a standalone preview.

An actual `EffectEmitter` containing a Vertex-material mesh, under an unregistered preview root, produced:

```text
VIEWER prRockFloor=0.0 registered=1
VIEWER masterRockFloor=1.0
```

The second observation calls frozen master's full-bright implementation on the same hierarchy. Thus this changes already-created mesh elements that master did illuminate; it is not solely a preexisting problem with effects spawned after the initial full-bright call.

The rendering consequence follows directly from the current definitions: `Ambient` defaults to black and `RockFloor` to zero ([Vertex.j3md:11–16, 44–57][vertex-defaults]). With no lighting textures, the shader has no tile/global/light contribution, and the absolute brightness floor remains zero ([Vertex.vert:66–89, 137–140][vertex-lighting]). Affected Vertex meshes can therefore render black instead of fully lit. Flat/unshaded sprites are not all implicated. This review verified material parameters and shader logic, not an interactive viewer screenshot.

**Impact:** reduced visibility and misleading effect previews for mesh/debris assets. This also demonstrates that the compatibility facade is no longer behaviorally equivalent for a current caller, contrary to the preservation claim in the architecture document.

**Recommended fix:** give the Model Viewer effect lifecycle an explicit `FULL_BRIGHT` policy and propagate it to effect targets and mesh-element creation/respawn. Ensure preparation is flushed before submission even with null lighting rules. Keep world effects on `WORLD`. Preserve nested-owner boundaries; globally removing the pruning would allow parent preparation to overwrite independently owned child policies. Making the facade reach only the initial children would leave later children/respawns inconsistent.

**Required regression:** construct a no-map preview with registered mesh elements, test the initial frame and later generated/respawned elements, and assert full-bright parameters and absent world bindings. Include preview reset/clear and relevant viewer material/debug modes. Existing standalone full-bright tests ([EntityMaterialSetupTest.java:282–303][fullbright-test]) do not exercise this consumer/ownership interaction.

### R3 — Medium: deferred structural preparation can preserve an obsolete lighting context

**Primary locations:** [EntityMaterialSetup.java:148–180][readiness-gate] and [AssetUtils.java:868–897][blueprint-restore]. Related ownership preparation: [JmeMaterialProvider.java:53–63][provider-ownership].

**Technical evidence and reproduction**

The readiness gate defers the provider before it discovers or isolates the root's *current* materials. Binding invalidation only visits `entry.materials`, which can be empty or no longer represent the geometry.

The probe performed this supported sequence:

1. Register a Vertex root and prepare/bind it against ready context A.
2. Apply a blueprint and flush while ready. Its replacement is unshaded, so the eligible retained list becomes empty; the old material remains saved in geometry user data.
3. Flush a null context, then restore the saved material using `AssetUtils.removeBlueprint()`; the existing notification correctly queues the root.
4. Flush with no context, then with a `LightRules` whose ambient map is not initialized.

Both old texture identities remain on the actual restored geometry:

```text
RESTORE staleLight=true staleAmbient=true
RESTORE_LOADING staleLight=true staleAmbient=true
```

Registering a clone of a previously prepared root with a second lifecycle and calling `update(null)` also retained its prior `AmbientMap`. The cloned observer does not transfer a registration, but the cloned material still carries texture parameters; deferred preparation leaves them untouched.

This contradicts the documented absent/loading-context cleanup contract ([architecture document:51–57](entity-material-lifecycle.md#architecture-and-contracts)). Rebinding a retained, unchanged material works; the failing case combines structural replacement/reuse with unavailable resources. Existing tests cover those conditions separately.

**Impact and qualification:** stale light/ambient textures, dimensions and exploration state can be sampled during the waiting interval, and old resources remain referenced until a ready context triggers preparation. The effect persists if readiness never arrives. The API-level defect is reproduced; its frequency in normal gameplay is **not established**. The normal map publication gate reduces exposure, and this review did not demonstrate a routine map transition rendering this exact sequence.

**Recommended fix:** separate structural discovery/isolation from readiness of ambient resources. On registration or material replacement, establish ownership of the actual materials once and sanitize/bind them against the current context, even when ambient binding must wait. Stable waiting frames should still avoid traversal. Freshly prepared entries also need an explicit “binding has been applied” state: assigning `entry.bound = null` and comparing it with a null current binding cannot force removal of inherited parameters. Update the readiness contract and tests to reflect the distinction between one-time structural work and repeated waiting work.

**Required regression:** ready A → blueprint → absent/loading context → restore; and prepared clone → new owner with absent/loading context. Assert texture identity/dimensions on the *attached geometry*, then verify correct null → loading → ready binding and shared-source isolation. Current [context-loss tests:152–171][context-test], [blueprint test:194–214][blueprint-test], and [clone/export test:307–325][clone-test] do not combine these transitions.

## Test coverage and remaining validation

The archived final run contains **253 suites, 1,404 tests, zero failures/errors/skips**, including 22 lifecycle tests. XML counts were independently re-read here. Current GitHub [build CI passed](https://github.com/tonihele/OpenKeeper/actions/runs/44289360305/job/134750233912); DeepSource was skipped. These successes do not cover the reproduced paths above.

| Coverage | Existing evidence | Missing acceptance |
| --- | --- | --- |
| Stable caching, coalescing and movement | Provider-call counts, material/vector identity assertions | Preserve these after transition fixes; unit assertions alone do not measure total frame allocations. |
| Animation replacement and late flush | Notification coverage and animated asset replay | First-draw checks with simultaneous blueprint/variant/charred replacement and pending context. |
| Effect lifecycle | Model-bearing restart, explicit removal, mesh self-respawn | Artless natural completion and long-running effect churn; R1 regression. |
| Full brightness / shared ownership | Standalone policy reversal and nested ownership tests | Actual no-map preview behavior, later particle generation, clear/reset; R2 regression. |
| Context and ambient readiness | Delayed clean initialization and retained-material rebind | Reintroduced prebound materials during unavailable context; R3 regression. |
| Visual parity | Twelve recorded framebuffer checkpoints, zero differing pixels | Viewer mesh effects, cold/waiting transitions, and a broader set of authored alpha/animation/depth materials. |
| #1012 compatibility | Additive page-parameter ownership test | Real combined page-index shaders/materials, overflow behavior, terrain and SMAA output. |
| Thread/resource lifetime | Wrong-thread rejection and owner cleanup tests | Render-thread shutdown/restart stress and late map publication; no confirmed additional race found. |

The recorded gameplay inspector examined attached geometry and reported no missing bindings. It cannot detect R1's detached dummy registrations or prove that a present binding belongs to the correct context. Strengthen lifecycle assertions rather than relying on scene traversal alone.

## Performance assessment and rendering risks

### What the measurements support

The fixed-work replay used 160 real animated Level20 imps, 64 lights, scripted animation replacement every third frame, delayed ambient initialization/recreation, separate JVMs, 300 warmup and 1,800 measured frames per run, and three alternating baseline/candidate pairs. The raw summary and class/source provenance were inspected; these are prior measurements, not new runs in this review. Summary values are medians of per-run statistics.

| Metric | Master preparation reference | Candidate | Interpretation |
| --- | ---: | ---: | --- |
| CPU frame mean / P95 / P99, ms | 3.457 / 4.253 / 4.832 | 3.167 / 3.909 / 4.795 | Mean about 8.4% lower for this workload. |
| Material update + flush mean / P95 / P99, ms | 0.492 / 0.600 / 0.684 | 0.043 / 0.081 / 0.117 | Strong setup-cost reduction, about 0.449 ms/frame. |
| Render-thread allocation, B/frame | 55,406 | 19,672 | About 64.5% lower. |
| GPU interval mean / P95 / P99, ms | 0.935 / 1.706 / 2.108 | 0.937 / 1.695 / 2.242 | No GPU improvement established; median P99 about 6.4% higher. |

Work counts matched per paired frame: 480 jME object submissions and 29,920 triangles; uniform counts differed on 145 replacement frames. The six final images matched. These are useful O1 results. Object counters are not an API draw-call trace, final-image equality does not prove every intermediate frame, and three pairs do not establish universal performance or confidence bounds. The reference uses frozen master preparation with the current binder/loaders/shaders, so it cannot independently certify all binder/rendering changes.

Full gameplay results are mixed: one monitored uncapped group has CPU mean **8.628 → 9.199 ms**, P99 **15.908 → 21.774 ms**, and GPU mean **1.399 → 1.487 ms**. Other groups improve. AI, visible work, harness variants and GPU conditions differ; they cannot be pooled or used to attribute causation. The older #1081 setup microbenchmark and #1012 page/SMAA timings also do not establish O1 end-to-end gains. Detailed protocols and contrary results remain in [the validation record](entity-material-lifecycle.md#performance-evidence-and-limits).

### Risks that remain after the findings are fixed

- **Effect churn:** R1 adds work proportional to completed effects. A stable population of imps cannot establish bounded lifecycle cost. Run sustained spawn/natural-complete cycles and verify registrations plateau, heap settles, and update/flush P95/P99 do not drift.
- **Cold readiness bursts:** preparation allocates on real mutations. Measure first ready frame, mass creation/removal and animation bursts; existing steady-state numbers do not establish their latency. R3's correction may move one-time work earlier and should be measured explicitly.
- **Removal complexity:** `active.remove`, `pending.remove` and `waiting.remove` are linear ([EntityMaterialSetup.java:81–92][lifecycle-unregister]). Bulk removal can be quadratic in population. This is an identified complexity bound, not a measured regression or a separate change request. Profile representative mass destruction before replacing simple collections.
- **GPU tails:** candidate replay pair 32 has GPU mean about **0.996 ms**, versus **0.933 ms** for its baseline, despite matching geometry/output and stable sampled clocks. Submission gaps and scheduling may affect timer intervals; no additional shader computation has been demonstrated. Keep this uncertainty visible and repeat matched work after fixes before making stronger GPU claims.
- **Visual transition coverage:** the setup-parity checks share the current binder. Add independent resource-identity assertions and rendered transition checkpoints; correct-looking steady-state images cannot rule out stale map bindings or first-frame darkness.

Moving lifecycle bookkeeping to GLSL would not address these defects. Lighting evaluation already uses shared GPU textures; this PR does not add draw-call reduction or shader work. Fix ownership/transitions and retain the existing GPU lighting infrastructure.

## Relationship to the overlapping PRs

Actual related heads and diffs reviewed:

| PR / head | Relevant contribution | Review conclusion |
| --- | --- | --- |
| [#1081](https://github.com/tonihele/OpenKeeper/pull/1081), `180e874dd90032a59f1e3825694ae8affb2eec74` | Setup caching and ambient readiness | Retain readiness-aware binding. Challenge the consolidated assumption that no structural work may occur while waiting: R3 requires handling reused/prebound material state. |
| [#1131](https://github.com/tonihele/OpenKeeper/pull/1131), `03450d8310a336e5f39f23cf845ff3fada3d9c6e` | Injectable material provider/configuration | The current narrow provider seam is sufficient. Do not restore a competing synchronized weak cache or broad factory plumbing. |
| [#1133](https://github.com/tonihele/OpenKeeper/pull/1133), `d233fc812cdeda3c37a27fb83c6f8f8fc790875e` | Event-driven setup and fewer recurring traversals | Keep creation/replacement notifications and the late flush. Creation must be paired with every terminal path, including artless effect completion. |
| [#1012](https://github.com/tonihele/OpenKeeper/pull/1012), `296c25fc845448dae9219a1fd3c9cf080a22e71a` | Material retention, mutation coverage, light-position allocation reduction; independent page lighting/SMAA | Current O1 adapts useful CPU concepts without importing its user-data cache. Preserve page lighting/SMAA separately and reconcile against current MeshCenter semantics; the additive test is not combined-renderer validation. |

Use #1310 as the integration vehicle **after fixes and regression validation**. Recommend superseding #1081/#1131/#1133's competing O1 mechanisms only with maintainer/contributor agreement and preserved history. Keep #1012's independent work available. No PR closure, coordination-status change or GitHub review/comment is performed by this review.

## Prioritized actionable checklist

- [x] **P1 — R1:** eliminate resource-less target registration and clean target ownership on natural completion. Add artless/nested/delayed/infinite lifecycle regressions; verify completed effects leave no entries before manager cleanup.
- [x] **P1 — R2:** propagate an explicit preview full-bright policy through initial and subsequent effect instances. Preserve nested ownership isolation. Add a no-map preview regression and inspect real mesh/debris previews.
- [x] **P1 — R3:** establish current material ownership and sanitize inherited bindings on structural changes independently of ambient readiness. Cover null/loading contexts and explicit initial binding state; update the readiness documentation/tests.
- [x] **P2:** run the focused regressions and full JDK 25 build (`.\gradlew.bat --no-daemon build --rerun-tasks`) after fixes. Preserve source/class provenance and attach actual results.
- [x] **P2:** render cold initialization, animation/blueprint restoration, shared materials and moving/object-light cases; inspect Model Viewer mesh-preview integration and actual hand/menu behavior. Record first-draw and waiting-state output; full interactive viewer/sprite acceptance remains qualified below.
- [x] **P2:** repeat matched asset replay and sustained effect churn. Report CPU frame/update cost, P95/P99, render-thread allocation, registration/heap stability, GPU timer samples, work counts and contrary runs. Keep instrumentation external.
- [x] **P2:** amend architecture claims about cleanup, compatibility full brightness and waiting behavior. Preserve the existing performance qualifications and record remaining GPU-tail uncertainty.
- [ ] **P3:** obtain maintainer agreement on PR consolidation. Validate #1012's combined renderer separately when integrated; do not claim that O1 alone certifies it.

**Original merge recommendation at `7ff4765`:** Request Changes for R1–R3. The observed CPU benefit supports continuing with this implementation, but passing CI and finite parity captures do not justify merging the reproduced lifetime and preview regressions. The corrections and their evidence are recorded below for maintainer review.

## Follow-up fixes and validation

### Implementation

| Finding | Follow-up correction | Focused regression evidence |
| --- | --- | --- |
| R1 | `VisualEffect.load()` creates no artless dummy target. Natural finite completion uses terminal/idempotent `removeEffect()`. `releaseLighting()` / `releaseLightTarget()` share cleanup across completion, explicit removal and restart. | Repeated natural completion, generated child with delayed context, self-death restart, late context change after completion; 50,000 actual effect-242 completions leave zero registrations. |
| R2 | Immutable manager policy defaults to WORLD; Model Viewer explicitly selects FULL_BRIGHT. The manager passes the policy to targets and mesh emitters, including later generation/respawn. Normals swaps/restoration notify owners. Nested traversal boundaries remain intact. | Actual manager/emitter pipeline with initial and respawned meshes, late creation, debug-material restoration, clear/reload and source-material isolation; real effect-6/effect-350 mesh renders. |
| R3 | The provider prepares current materials once per structural event, independently of ambient readiness. Fresh preparation forces binding/sanitation even with null/null binding identities. Resource changes rebind retained lists. The waiting list and now-unused readiness query were removed. | Absent **and** loading-context blueprint restoration, clone/import sanitation, policy reversal without resources, stable waiting frames and later resource binding without another preparation pass. |

The public provider seam and per-view ownership model are retained. No shader, terrain, postprocessing, page-lighting or SMAA changes are introduced. The cold-work trade-off is explicit: structural preparation happens earlier, rather than allowing old-world resources to survive while waiting.

### Verified results

- Final JDK 25 build: **1,411 cases in 253 suites, zero failures/errors/skips**; 29 lifecycle cases. Seven strengthened regression cases fail as assertions against frozen pre-fix production classes and pass with the fixed code. The preview test uses the new policy constructor and is separately backed by the original failing material-state probe.
- The binary-roundtrip fixture now uses the same configured importer instance for loading; it asserts the geometry's actual material. The old test could pass after the importer swallowed a missing-AssetManager exception and returned null material.
- **19** paired transition checkpoints and **12** real-effect mesh-preview checkpoints: zero differing pixels. Transition checks also compare attached materials against the current snapshot's exact texture identities and dimensions each frame. No-context output intentionally has no stale illumination.
- Actual Main/Menu/Hand validation: **244 frames, 1,611 material checks**, held-item empty/reload, blueprint restoration, disposal/recreation, menu root replacement and clean shutdown. Temporary ECS/proof objects were used, without campaign-progression changes.
- Fresh Level20 gameplay: 600 warmup + 1,000 inspected frames, **2,917,500 geometry checks / 2,907,886 Vertex checks / zero missing bindings**, inspected captures and clean shutdown.
- Real effect-242 churn: **50,000 completions**, zero fixed registrations versus 50,000 retained by the pre-fix consolidation. Fixed post-GC heap settled around 7.04 MB; pre-fix reached 48.04 MB. This is isolated lifecycle evidence, not a whole-game memory/FPS estimate.
- Bulk removal was profiled before adding data-structure complexity: 160-root mean/P95 **0.110/0.262 ms**, 1,000-root **0.483/0.624 ms**, 5,000-root **2.606/3.441 ms**, in the synthetic fixture. The simple collections remain.
- Real-asset owner initialization (160 Imps / 480 geometries) shifts work earlier: initial no-context mean **0.069 → 2.237 ms**, subsequent readiness **2.390 → 0.487 ms**. The summed one-time means rise about 0.27 ms. Animation replacement preparation mean **2.443 → 2.407 ms**, with a higher fixed P95 preserved in the report. Asset I/O/shader/GPU initialization are excluded from this headless fixture.

Corrected fixed-work replay pairs 60–62 retain the same 160-Imp/64-light protocol. Median per-run CPU mean/P95/P99: **3.540/4.488/5.162 → 3.223/4.022/5.079 ms**. Setup: **0.511/0.627/0.713 → 0.039/0.075/0.104 ms**. Allocation: **56,791 → 19,620 B/frame**. All reported rendering counters match on every paired frame, and all final PNGs match. The six PNGs also match the earlier fixed replay's hash.

GPU intervals remain inconclusive: median mean/P95/P99 **0.946/1.730/2.197 → 0.967/1.767/2.240 ms**. One candidate run reached 54% sampled load and changed graphics clocks; one baseline reached 100%. The stable-clock pair still had a small candidate increase. No external renderer was identified as the cause. No GPU improvement or universal non-regression is claimed. Earlier post-fix pairs 50–52 had a frame-zero statistics-initialization defect in their texture-object count, were retained, and are not pooled with the corrected group.

### Remaining conditions and limits

The reproduced code defects are addressed in the follow-up changes. This follow-up is not a maintainer approval or a declaration that O1 coordination is resolved. The P3 integration decision remains open; #1012's combined page/SMAA renderer still requires separate validation when integrated. Neither a second GPU nor campaign-wide/all-assets acceptance was available here. Interactive Model Viewer UI/sprite layers were not exhaustively exercised; the real mesh lifecycle/rendering path and debug-material restoration were covered. GPU-tail uncertainty remains recorded rather than hidden by the CPU improvement.

See [the updated architecture and detailed measurements](entity-material-lifecycle.md#fixed-work-replay-after-the-critical-review-fixes). Current evidence is preserved under `D:\Project\OpenKeeper-o1-review-fixes-20261118`; the test commands are portable from the project checkout, while renderer experiments remain external. No production benchmark/instrumentation source is included in the PR changes.

A separate `OpenKeeper-O1-review-fixes-evidence-20261118.zip` collects the reports, patch, experimental sources, raw accepted/contrary captures, XML and hashes. Its configurable launcher and external classpath helper were smoke-checked locally; no original/converted game asset files are included. This makes the evidence transferable without adding instrumentation to the production PR. The bundle remains in local custody; maintainer agreement cannot be claimed from local validation.

## Local evidence custody and reproduction notes

Review-only sources/logs are preserved in `D:\Project\OpenKeeper-o1-review-20261118` (`O1ReviewProbe.java`, `EffectCatalogProbe.java`, `run-probe.py`, `probe.log`, `catalog.log`). They compile against the existing PR main/test classes with JDK 25 and use the public parsed fixture plus the real provider/emitter/lifecycle. The real-data variant reads the installed licensed Level20 map and effect 242; no licensed assets are included here. The observation sequences under R1–R3 describe the cases to turn into portable in-repository regression tests; the local runner is not a portable project command.

Prior finalization evidence is in `D:\Project\OpenKeeper-o1-finalization-20261118`: `final-provenance.json`, `final-test-results/`, `replay-summary.json`, pairs 30–32 under `replay/`, `visual-summary.json`, sources and captures. Other pilots/rejected starts are excluded from the cited final replay. Maintainers need an accessible evidence bundle or reproducible harness to repeat those measurements; local custody alone does not make them independently reproducible from the PR checkout.

[lifecycle-owner]: https://github.com/tonihele/OpenKeeper/blob/7ff476556c2c3ef5c75f53bfe37f7bd3af2c9b21/src/main/java/toniarts/openkeeper/view/material/EntityMaterialSetup.java#L23-L46
[provider-prepare]: https://github.com/tonihele/OpenKeeper/blob/7ff476556c2c3ef5c75f53bfe37f7bd3af2c9b21/src/main/java/toniarts/openkeeper/view/material/JmeMaterialProvider.java#L29-L69
[binding]: https://github.com/tonihele/OpenKeeper/blob/7ff476556c2c3ef5c75f53bfe37f7bd3af2c9b21/src/main/java/toniarts/openkeeper/view/lighting/SceneLights.java#L135-L178
[effect-placeholder]: https://github.com/tonihele/OpenKeeper/blob/7ff476556c2c3ef5c75f53bfe37f7bd3af2c9b21/src/main/java/toniarts/openkeeper/view/effect/VisualEffect.java#L158-L169
[effect-target]: https://github.com/tonihele/OpenKeeper/blob/7ff476556c2c3ef5c75f53bfe37f7bd3af2c9b21/src/main/java/toniarts/openkeeper/view/effect/VisualEffect.java#L185-L189
[effect-register]: https://github.com/tonihele/OpenKeeper/blob/7ff476556c2c3ef5c75f53bfe37f7bd3af2c9b21/src/main/java/toniarts/openkeeper/view/effect/VisualEffect.java#L769-L774
[effect-completion]: https://github.com/tonihele/OpenKeeper/blob/7ff476556c2c3ef5c75f53bfe37f7bd3af2c9b21/src/main/java/toniarts/openkeeper/view/effect/VisualEffect.java#L856-L870
[effect-control]: https://github.com/tonihele/OpenKeeper/blob/7ff476556c2c3ef5c75f53bfe37f7bd3af2c9b21/src/main/java/toniarts/openkeeper/view/effect/VisualEffect.java#L268-L279
[effect-remove]: https://github.com/tonihele/OpenKeeper/blob/7ff476556c2c3ef5c75f53bfe37f7bd3af2c9b21/src/main/java/toniarts/openkeeper/view/effect/VisualEffect.java#L883-L897
[manager-update]: https://github.com/tonihele/OpenKeeper/blob/7ff476556c2c3ef5c75f53bfe37f7bd3af2c9b21/src/main/java/toniarts/openkeeper/view/effect/EffectManagerState.java#L72-L82
[manager-clear]: https://github.com/tonihele/OpenKeeper/blob/7ff476556c2c3ef5c75f53bfe37f7bd3af2c9b21/src/main/java/toniarts/openkeeper/view/effect/EffectManagerState.java#L118-L124
[lifecycle-storage]: https://github.com/tonihele/OpenKeeper/blob/7ff476556c2c3ef5c75f53bfe37f7bd3af2c9b21/src/main/java/toniarts/openkeeper/view/material/EntityMaterialSetup.java#L31-L126
[effect-tests]: https://github.com/tonihele/OpenKeeper/blob/7ff476556c2c3ef5c75f53bfe37f7bd3af2c9b21/src/test/java/toniarts/openkeeper/view/material/EntityMaterialSetupTest.java#L411-L474
[provider-boundary]: https://github.com/tonihele/OpenKeeper/blob/7ff476556c2c3ef5c75f53bfe37f7bd3af2c9b21/src/main/java/toniarts/openkeeper/view/material/JmeMaterialProvider.java#L72-L75
[fullbright-facade]: https://github.com/tonihele/OpenKeeper/blob/7ff476556c2c3ef5c75f53bfe37f7bd3af2c9b21/src/main/java/toniarts/openkeeper/utils/AssetUtils.java#L353-L354
[emitter-register]: https://github.com/tonihele/OpenKeeper/blob/7ff476556c2c3ef5c75f53bfe37f7bd3af2c9b21/src/main/java/toniarts/openkeeper/view/effect/EffectEmitter.java#L223-L225
[viewer-manager]: https://github.com/tonihele/OpenKeeper/blob/7ff476556c2c3ef5c75f53bfe37f7bd3af2c9b21/src/main/java/toniarts/openkeeper/tools/modelviewer/ModelViewer.java#L280-L282
[viewer-preview]: https://github.com/tonihele/OpenKeeper/blob/7ff476556c2c3ef5c75f53bfe37f7bd3af2c9b21/src/main/java/toniarts/openkeeper/tools/modelviewer/ModelViewer.java#L728-L746
[viewer-fullbright]: https://github.com/tonihele/OpenKeeper/blob/7ff476556c2c3ef5c75f53bfe37f7bd3af2c9b21/src/main/java/toniarts/openkeeper/tools/modelviewer/ModelViewer.java#L797-L816
[manager-rules]: https://github.com/tonihele/OpenKeeper/blob/7ff476556c2c3ef5c75f53bfe37f7bd3af2c9b21/src/main/java/toniarts/openkeeper/view/effect/EffectManagerState.java#L191-L203
[effect-elements]: https://github.com/tonihele/OpenKeeper/blob/7ff476556c2c3ef5c75f53bfe37f7bd3af2c9b21/src/main/java/toniarts/openkeeper/view/effect/VisualEffect.java#L364-L380
[readiness-gate]: https://github.com/tonihele/OpenKeeper/blob/7ff476556c2c3ef5c75f53bfe37f7bd3af2c9b21/src/main/java/toniarts/openkeeper/view/material/EntityMaterialSetup.java#L148-L180
[vertex-defaults]: https://github.com/tonihele/OpenKeeper/blob/7ff476556c2c3ef5c75f53bfe37f7bd3af2c9b21/assets/MatDefs/DkLighting/Vertex.j3md#L11-L57
[vertex-lighting]: https://github.com/tonihele/OpenKeeper/blob/7ff476556c2c3ef5c75f53bfe37f7bd3af2c9b21/assets/Shaders/DkLighting/Vertex.vert#L66-L140
[fullbright-test]: https://github.com/tonihele/OpenKeeper/blob/7ff476556c2c3ef5c75f53bfe37f7bd3af2c9b21/src/test/java/toniarts/openkeeper/view/material/EntityMaterialSetupTest.java#L282-L303
[blueprint-restore]: https://github.com/tonihele/OpenKeeper/blob/7ff476556c2c3ef5c75f53bfe37f7bd3af2c9b21/src/main/java/toniarts/openkeeper/utils/AssetUtils.java#L868-L897
[provider-ownership]: https://github.com/tonihele/OpenKeeper/blob/7ff476556c2c3ef5c75f53bfe37f7bd3af2c9b21/src/main/java/toniarts/openkeeper/view/material/JmeMaterialProvider.java#L53-L63
[context-test]: https://github.com/tonihele/OpenKeeper/blob/7ff476556c2c3ef5c75f53bfe37f7bd3af2c9b21/src/test/java/toniarts/openkeeper/view/material/EntityMaterialSetupTest.java#L152-L171
[blueprint-test]: https://github.com/tonihele/OpenKeeper/blob/7ff476556c2c3ef5c75f53bfe37f7bd3af2c9b21/src/test/java/toniarts/openkeeper/view/material/EntityMaterialSetupTest.java#L194-L214
[clone-test]: https://github.com/tonihele/OpenKeeper/blob/7ff476556c2c3ef5c75f53bfe37f7bd3af2c9b21/src/test/java/toniarts/openkeeper/view/material/EntityMaterialSetupTest.java#L307-L325
[lifecycle-unregister]: https://github.com/tonihele/OpenKeeper/blob/7ff476556c2c3ef5c75f53bfe37f7bd3af2c9b21/src/main/java/toniarts/openkeeper/view/material/EntityMaterialSetup.java#L81-L92
