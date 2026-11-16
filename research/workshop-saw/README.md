# Workshop saw activation

The user's native DKII captures from 2026-11-14, 18:14:11–18:14:14, show the same stationary workbench with zero workshop workers while the simulation advances. The OpenKeeper captures show the unattended workbench animating. The `Spin` resource name alone does not establish native idle behavior.

Read-only inspection of the supplied IDA catalog identifies manufacture-state functions through their `ManufactureState.cpp` references. Static inspection of the local reference executable shows:

- Manufacture callbacks at `0x47062F`/`0x47092F` set the workbench activation flag and request object state 26 (`ACTIVATE_ANIM`) via `0x49EF60`.
- The manufacturing update at `0x470AED` calls `0x49D0B0` with object type 46 and the work tile coordinates. This helper refreshes an activation flag on matching objects at that tile.
- The activation update at `0x4A1A13`–`0x4A1A49` tests that flag. Without activation it resets the animation counters and state; it consumes the flag each update.

The idle case was observed in the user's native screenshots. Active-state behavior here is supported by static analysis; no new full native manufacturing session was completed. Exact native frame cadence and audiovisual effects are not claimed.

## Furniture-use refactor — 16 November 2026

PR #1212's agreed boundary is a shared start/end-use mechanism, task and creature-state entry points, and object-specific presentation. The refactor removes `WorkshopViewSystem` and its game-loop registration.

- `AbstractFurnitureTask` is an opt-in base used by manufacturing and research. Assignment reserves work; first execution binds the exact floor furniture entity in the task's room and starts use. Unassignment ends that creature's use. Ambiguous same-tile furniture is rejected rather than chosen by set iteration order. Manufacturing selects object type 46 explicitly.
- Room furniture is generated with owner 0. Binding therefore uses the room's own furniture membership and room ownership, rather than comparing the object's owner to the worker. Removed or replaced bound furniture cannot redirect an old task's cleanup to a new entity. Research now also honors the inherited room validity checks.
- `FurnitureUse` on each creature records its active object entity. `IObjectController.beginUse/endUse` are idempotent per creature; the final user leaving resets the saw. Recreated controller instances share the same entity state. The component is registered with the network serializer; presentation continues to use `ObjectViewState`.
- Sleeping and recuperating start bed use after the creature enters the corresponding AI state, after the go-to-sleep task has ended. Leaving those states, losing AI, and entity removal release use. Normal task cleanup still handles work interruption; delayed cleanup after direct creature deletion recomputes remaining users.
- The object controller projects use into the saw's view state. The shared animation policy pauses only workbench meshes while idle. Research bookcases and beds use the lifecycle without changing their presentation: their exact native idle/active resources and playback remain unverified. Dummy hit reactions, torture binding, other room tasks, and room generation remain outside this change.

## Refactor validation

`FurnitureUseTest` exercises real task and object-controller code with synthetic definitions: first execution before the first manufacturing work step, unassignment/restart, separate benches, shared users, fresh controllers, stale cleanup after furniture replacement, capture, research progression and lifecycle, sleeping/recuperating without a task, AI loss/removal, direct entity deletion, dead/carried users, and ambiguous binding.

The renderer test runs a synthetic jME animation through the real `ObjectViewControl` for both a saw and an unrelated object. Only the saw pauses/resets while idle; both animate while active. Existing manufacturing and research rate tests still pass.

Java 25 `build installDist --offline --no-daemon --no-configuration-cache --console=plain` passed: **902 passed, 4 skipped, 0 failures, 0 errors** (906 tests total). An interactive OpenKeeper smoke test was subsequently run with the user. Trap crates were manufactured, delivered and installed. A local saw-only variant made workstation selection deterministic, and the user reported that the visual result looked good. This is user feedback, not a recorded pass of every interruption scenario. The variant, launchers and original assets are outside the repository and are not included in the change; normal workshop furniture selection is unchanged. The test also exposed the existing trap-only manufacturing queue: a door blueprint remained waiting. No new native manufacturing session was performed; the native observations above remain the evidence for the saw policy.
