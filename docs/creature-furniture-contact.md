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
Native blocked-history handling is not ported.

Moving body yaw approaches its velocity direction using the authored turn limit
and the shortest circular arc. Eight saved native turn cases match within
floating-point tolerance. The adapter does not reproduce every native integer
rounding step or render interpolation.

Soft repulsion yields near the final waypoint using the native proximity
metric; contact is exempted only on the final path segment, permitting furniture
work positions. Stationary navigation completes at its destination or after
orientation-only movement, rather than mistaking an unfinished stall for arrival.

OpenKeeper retains its existing path follower, acceleration limits and update
cadence. Contact uses a quarter-second reference interval to adapt those rates;
complete native behavior blending and integration timing are not claimed.
The regular native enumeration and callback operate in the horizontal plane;
this work does not introduce an inferred height-based exemption for flyers.

## Validation

`./gradlew test installDist --offline --no-daemon` with JDK 25 passes all
1,459 tests on upstream cd78e565 using the locally converted assets. No asset
files are committed.

Synthetic regressions cover small and large authored widths, different furniture
widths, dense layouts, narrow gaps, wall-adjacent mixed solid/soft furniture,
work-position arrival and facing, and object lifecycle updates during movement.
Shared radius tests use synthetic widths; gameplay definitions are read at runtime.

A fresh two-minute offscreen Workshop run completed and shut down cleanly.
Eight creatures were sampled, including four Trolls and four Imps. The Trolls
travelled roughly 43–49 tiles; none of the sampled creatures entered solid
terrain or had a sustained stationary-navigation interval. They also entered
idle states, so this is not evidence of two minutes of continuous manufacturing.
The native Workshop trace completed after 1,421 finished movement updates
across eight creatures and recorded 57 solid contacts. Enumerated ordinary
furniture included workbenches and both solid and soft obstacle definitions.
The reference process was then terminated. These differing AI routes are not
deterministic trajectory comparisons.

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
linear velocity is stopped; facing still completes separately. This avoids
coarse-step destination oscillation without treating an unfinished route as
arrived. Regressions exercise dense furniture and work-position facing at
four and eight logic updates per second as well as the existing finer steps.

Walking continues to use tile access. A blocking door invalidates an old route
for replanning/attack, even when path planning permits breaking it. No slab
query is applied to ordinary walking. The native walking resolver's access
calls through 0x006A11D0 resolve to edge rule 0x004C8BD0; the independently
researched slab query belongs to the separate physics resolver 0x004B2030.
