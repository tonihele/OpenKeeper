# Secret-door discovery and route knowledge (#1365)

Built secret doors now have shared, persistent discovery state. The owner knows
its own door; an ally does not acquire knowledge merely because the door can
open for it. Exploring a tile does not discover the door. A qualifying enemy
observing an open door reveals it for all players.

This change builds on the allied-opening policy in #1366. It keeps the existing
ordinary-door route costs, locking and door-breaking decisions. Hidden foreign
doors are rejected before considering a breaking route, and the existing door
attack selector does not target them. After discovery, normal access and attack
rules apply. Capture changes which player knows an undiscovered door, without
clearing discovery that already happened. Removal and replacement create a new
lifecycle.

## Native evidence

The existing catalogue from both IDA databases was inspected first. Its recovered
entry points were restored into the existing Ghidra research project; relevant
bodies were decompiled and checked against disassembly. The executable and IDA
files on disk were not modified. Private decompilations and captures are outside
the contribution.

The research runner supports the reference executable with SHA-256
`2a00df642ff7f9296f1e8d053329ba152f5dac07e357f2b39b6f1f1f1d77e163`.

- Door tick `0x004A64A3`–`0x004A6661`, within `0x004A55F0`, checks open state,
  built state and the secret definition flag. It visits creatures belonging to
  keepers that consider the door owner an enemy. This is the observer's diplomacy
  direction, distinct from the door owner's opening permission.
- Horizontal distance uses squared fixed-point coordinates and must be strictly
  less than sixteen square tiles. The exact four-tile boundary is excluded;
  vertical separation is not part of that test.
- Effective activity category 22 is excluded. The original state initializer
  `0x0047BBE0` identifies combat, fleeing, threat-response and celebration cases
  in that category; a movement state can inherit its underlying category.
- The common eligibility predicate `0x0045A6D0(0)` also excludes unavailable
  creatures. In particular, the native runtime placement byte's `WILL_FIGHT`
  bit must be set. Sleeping, prisoner, torture and incapacitation paths have
  further checks. OpenKeeper maps its available activity/component states and
  `Peaceful` to these restrictions; possession may observe without CreatureAi.
- Discovery LOS calls `0x004512F0` with both options set to one. The cell helper
  `0x004511A0` checks terrain in that mode and skips its door enumeration. The
  implementation therefore reuses PositionSystem's terrain LOS, independently
  of hearing, exploration and passage permission. This does not establish exact
  agreement of the two line traversal algorithms at every diagonal corner.
- Successful discovery clears the single tile marker using `0x00450FF0`, then
  refreshes door navigation using `0x004A66F0(1)`. There is no viewer argument or
  per-player discovery bitmap in these calls. The native creature also receives
  a door reference through `0x0048F390`; existing OpenKeeper door targeting
  resumes after revelation, rather than adding a second attack implementation.
- The navigation helper reads the current marker into bit `0x02`. Closing and
  ownership refresh do not set the marker again. The original route predicate
  `0x004C8BD0` rejects a hidden foreign door before its other routing checks,
  regardless of alliances. An owned hidden door is allowed by this check.
- The native drawing predicate `0x004A6E10`, specifically `0x004A6E95` onward,
  suppresses an undiscovered secret door for every non-owner, including allies.
  Revealed doors still pass through the normal visibility rules.
- Construction at `0x00467604` sets the shared marker for a secret door. Secret
  door removal paths at `0x004A544E` and `0x004C37E9` clear it. The initial-world
  setup also marks built secret doors. Ownership refresh at `0x004A55B0` updates
  navigation/appearance without resetting the tile marker.

## Reproducible native checks

```sh
python3 research/dkii-secret-doors/run_probe.py \
  '/home/wietse/Downloads/Data/Bullfrog/Dungeon Keeper 2/DKII.exe' \
  --output /tmp/dkii-secret-door-check
```

Requires GCC multilib and permission to execute 32-bit code. Use a fresh capture
directory outside the repository. The runner verifies the executable hash, maps
its sections into private memory, and initializes the native state categories
by executing original code. No executable bytes or recovered state tables are
embedded in the harness.

The probe executes the original discovery block and its original enemy,
activity, observer-eligibility, tile-marker and route-encoding helpers. It
controls the LOS result and captures/isolates world notifications, neighborhood
refreshes and navigation writes. The matrix has **4,800 cases**: same/allied/enemy
relation; open/closed; built/blueprint; secret/ordinary; five distances including
both sides of the four-tile boundary; ten native activity states; WILL_FIGHT
on/off; and LOS on/off.

Additional calls verify the native shared marker and navigation bits across
closing and owner-slot changes, foreign route rejection, and the original
owner/allied/foreign drawing predicate. These are executions against synthetic
records, not a complete live AI/possession gameplay comparison. General observer
eligibility includes native orders, special-creature and spell flags which are
not all represented by OpenKeeper's current component/state model; this change
does not add those broader gameplay systems.

## Implementation and coverage

`DoorComponent.hidden` is authoritative. `DoorViewState.hidden` replicates its
presentation. Loading and blueprint completion derive the initial value from
`IS_SECRET`; repeated build completion is a no-op. Opening/closing and locking
preserve the flag. DoorDiscoverySystem runs after DoorViewSystem, searches the
existing tile index, and clears both flags on qualifying observation. State is
stored in entities, rather than an unsaved discovery-system cache.

The client uses its existing subscriptions. Door model subscriptions include
Owner, so capture also updates a stationary model. Fog-only updates recheck the
existing watched owner, without synchronous remote entity lookups. Known-door
visibility is combined with ordinary fog culling.

AI route queries and manual possession queries are separate. The latter use
physical opening/access state and never gain or require route knowledge. This
retains the existing terrain/room checks; it does not introduce door geometry.
The native swept-contact implementation and authored creature clearance remain
in #1339. Its direct movement path should continue using terrain access plus
DoorContactSystem; secret knowledge belongs only in the AI route/target checks.

Synthetic tests cover loading, blueprint completion, owned/allied/enemy routes,
normal versus breaking costs, manual access before/after discovery, possessed
observers, owner/diplomacy changes, closed doors, range boundaries, terrain LOS,
Peaceful and unavailable observers, persistence after closing, removal and
replacement, attack eligibility, client visibility/fog/capture subscriptions,
and round trips through the same FieldSerializer used by NetworkServer.

Both component schemas gained a field: client and server must use matching
builds. There are no proprietary maps, assets, executable fragments or captured
native tables in the committed tests or research harness.

## Validation

JDK 25, offline `test installDist`: **1,601 passed, four skipped, zero failures
or errors** (1,605 total). The 34 added cases include synthetic world/client
checks and serialization. CRLF-aware `git diff --check` passes. The hash-checked
native runner passed its 4,800 cases and the persistence/visibility assertions.
No new rendered full-game comparison is claimed.
