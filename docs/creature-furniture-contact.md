# Creature sizing and regular furniture contact (#1258)

Regular AI steering uses `CreatureSizing.getBodyRadius`: half the current
creature definition's authored width. Level-scaled visual height does not
scale this horizontal radius. Non-creature steering retains its existing size.

`FurnitureCollision` indexes regular objects with OBSTACLE and positive authored
width. Each footprint uses width/2; SOLID_OBSTACLE selects contact constraints,
while soft obstacles contribute capped, weighted outward acceleration. The
special Dungeon Heart, loose items, traps and possession are outside this policy.
Door passage rules remain with the existing navigation/controller code.

The index refreshes when object or position components are added, replaced or
removed. Queries visit only nearby cells and deduplicate overlapping footprints.
Contact order follows centre-tile rows and columns with entity ID as the stable
same-tile tie-break. Native same-tile object-chain order remains unestablished.

## Native evidence and limits

The original executable and both IDA databases were inspected in the earlier
research. Relevant routines are obstacle enumeration 0x004D12B0, Repel_Obstacle
0x004D31D0, its callback 0x004D3020, and movement solver 0x004D4760.
Research captures and disassembly remain outside the repository.

Native observations show large creatures traversing gaps narrower than their
combined furniture/body circles. The implementation therefore applies contact
steering rather than rigid positional separation. Tests check progress and
arrival, preserving overlap instead of rejecting it categorically.

The contact stage buffers at most twelve normals, consumes them in reverse,
projects force and velocity component by component, and adds tangential
acceleration. It preserves the native signed-shift ranking and slow-outward
cutoff. Rechecking the restored implementation against eleven saved native
cases gives a maximum component difference of 0.00002813. These isolated
comparisons establish the contact stage, not full trajectory or timing parity.

Movement recovery tries the full displacement, each axis, then perpendicular
alternatives. It retains OpenKeeper's access policy and also checks actual
landings, preventing three unsafe landings accepted by the native branch under
synthetic access policies. Five safe saved native cases match within 0.0001.
Native blocked-history recovery belongs to the replacement pilot in #917;
it is not duplicated in this contact adapter.

Moving body yaw approaches its velocity direction using the authored turn limit
and the shortest circular arc. Eight saved native turn cases match within
floating-point tolerance. The adapter does not reproduce every native integer
rounding step or render interpolation.

Contact yields near the final waypoint only when `Navigation.endInObstacle`
is explicitly set, using the native proximity metric and only on the final
path segment. Ordinary walks keep contact on their final leg. OpenKeeper sets
this flag for assigned-task destinations, including beds and explicit work
positions, retaining furniture access; this
is an OpenKeeper task policy, not a verified mapping of every native job's
0x20/0x40 request flags. #845 uses the same flag for withdraw walks.
Stationary navigation completes at its destination or after orientation-only
movement, rather than mistaking an unfinished walking stall for arrival.
One-point facing requests do not require proximity to an unused path point.
Orientation deceleration begins at least one full-speed tick before its target,
avoiding the coarse-tick oscillation exposed by the new facing regression.

This base PR retains OpenKeeper's gdx path follower and acceleration limits.
Contact converts velocity and acceleration using the actual logic update length,
including the native outward cutoff's per-tick displacement units. #917 builds
on this PR and replaces gdx following with DKII's pilot. It sets the shared
force cap to 1.5 / (tick length squared), so the summed force and contact edge
force use the same native limit, and adds recovery after no headway in five of
fifteen ticks, ending recovery when the count falls to two. These behaviors
are owned by #917 rather than independently reimplemented here. Full native
pilot parity is not claimed for #1341 in isolation.
The regular native enumeration and callback operate in the horizontal plane;
this work does not introduce an inferred height-based exemption for flyers.

## Validation

`./gradlew test installDist --offline --no-daemon` with JDK 25 passes all
1,464 tests on upstream cd78e565 using the locally converted assets. No asset
files are committed.

Synthetic regressions cover small and large authored widths, different furniture
widths, dense layouts, narrow gaps, wall-adjacent mixed solid/soft furniture,
work-position arrival and facing, and object lifecycle updates during movement.
Shared radius tests use synthetic widths; gameplay definitions are read at runtime.

A final one-minute offscreen Workshop run on the authoritative logic loop
completed and shut down cleanly. It sampled four Trolls and four Imps across
6,928 records. Trolls travelled approximately 25–28 tiles, with no sampled
creature entering solid terrain. The longest stationary-navigation interval
away from the goal was under half a second. Creatures also entered idle states;
this is not evidence of uninterrupted manufacturing or a deterministic native
trajectory comparison.

Earlier native Workshop research completed after 1,421 finished movement updates
across eight creatures and recorded 57 solid contacts. Enumerated ordinary
furniture included workbenches and both solid and soft obstacle definitions.
The reference process was then terminated. Those captures establish contact
examples, not parity with the final logic-loop timing.

Remaining validation includes sustained room-task scenes, native same-tile
ordering, vertical gameplay and more complex multiple-contact corners. Full native
movement fidelity is broader than this focused contact implementation.

## Intentional difference from DKII

The fresh Workshop trace produced solid-contact records for some objects whose
definitions have only OBSTACLE. Inspection of 0x004D1437–0x004D14A3 shows why:
the reused descriptor sets its solid bit for a solid definition but XORs that
bit for a soft definition. The descriptor is initialized once per enumeration,
not per object. A soft object's response therefore depends on earlier objects.

A separate original C harness loads the user's executable into private memory
and executes enumeration against synthetic four-object chains. All sixteen
solid/soft sequences confirm this behavior. For example, four soft definitions
produce solid bits 1, 0, 1, 0. The executable is read at runtime; no executable
bytes, extracted fixtures or captures are committed.

OpenKeeper deliberately treats this as a native DKII bug and does not reproduce
its unexpected order-dependent classification. Soft objects should clear the
solid bit; each object's response follows its own authored flags independently.
The implementation uses that policy. DKII's behavior is retained here
as an intentional difference. Reproducing native enumeration order solely to
preserve this bug is not required for #1258. The remaining validation limits are listed above.

## Logic-loop scheduling and walking door access

Authoritative AI and possession movement now run in the existing logic loop,
after door-view updates and before hauling. The separate Steering loop is
removed; animation retains its existing loop. This follows the maintainer's
instruction in #1330 to keep new work out of the legacy movement thread.
Client prediction and rendering interpolation are not introduced here, so
lower-frequency movement updates can remain visible.

Arrival controllers use a response interval no shorter than the logic step.
When the final path leg reports linear arrival within tolerance, residual
linear velocity is stopped; facing still completes separately. Linear and
angular damping use elapsed time to preserve their former per-second decay
when the update frequency changes. This avoids
coarse-step destination oscillation without treating an unfinished route as
arrived. Regressions exercise dense furniture and work-position facing at
four and eight logic updates per second as well as the existing finer steps.

Walking continues to use tile access. A blocking door invalidates an old route
for replanning/attack, even when path planning permits breaking it. No slab
query is applied to ordinary walking. The native walking resolver's access
calls through 0x006A11D0 resolve to edge rule 0x004C8BD0; the independently
researched slab query belongs to the separate physics resolver 0x004B2030.

## Review follow-up (2026-11-19)

Based on Scotty's [review](https://github.com/tonihele/OpenKeeper/pull/1341#issuecomment-7017609651)
and the implementations in [#917](https://github.com/tonihele/OpenKeeper/pull/917)
and [#845](https://github.com/tonihele/OpenKeeper/pull/845):

- Contact scaling now consumes the actual movement step rather than 0.25 seconds.
  Synthetic cutoff tests distinguish velocity times tick from force times tick squared
  at two, four and eight updates per second.
- Final-leg yielding requires an explicit obstacle-destination flag. Synthetic
  movement checks cover both solid and soft objects, ordinary walks and opted-in
  task destinations; native job-flag mapping remains incomplete.
- Completion checks distinguish facing-only requests from a linear journey.
  One-point facing requests can complete away from their unused point; unfinished
  multi-point walking paths still require final arrival.
- Furniture trajectory tests now run at the authoritative logic cadence, including
  a one-tick incursion allowance because contact is sampled once per tick. They
  do not claim rigid disc separation or native trajectory equality.

No new original-executable trace was performed for this review follow-up.

Review validation: JDK 25 `./gradlew test installDist --offline --no-daemon`
passes 1,466 tests with zero failures and four asset-dependent skips in a clean
checkout without converted game assets. These are synthetic regressions and a
packaging check, not a new live native or rendered gameplay comparison. The new
`Navigation` field requires matching client/server builds.

Integration notes for the stacked pilot PR: pass the logic `tpf` to
`FurnitureCollision.constrain` and only supply a final destination when
`Navigation.endInObstacle` is set. Keep #917's `FollowWaypoints.isFinished()`
completion and blocked-history handling; #1341's gdx arrival check is temporary
until that follower lands. #845's existing `endInObstacle` flag is the same
policy field and should be merged as one field, preserving task and withdraw
producers.
