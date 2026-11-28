# Door passage investigation (#1288)

The allied-door fix applies independently to current upstream. It was also
validated with the native contact implementation in #1339 (`ca57778b`). Contact
geometry, possession sweeps and the distinction between breaking a door and
physically passing it remain in that separate implementation.

## Verified ownership gap and change

An unlocked DK II door opens for creatures of its owner or an ally. In
OpenKeeper, both the opening system and the position lookup required an exact
owner match. An allied creature could not obtain a route through the door,
and reaching it by another means would not make it open.

`DoorAccessRules` now reads the door owner's live `Keeper.isAlly()` relationship.
`PositionSystem` uses that relationship for normal owner access, preserving its
existing blueprint, locking and enemy-door handling. `DoorViewSystem` uses the
same ownership predicate to request opening. The game controller and logic loop
supply the existing level/player information; there is no additional registry
or cached diplomacy state. Existing constructor signatures remain available for
callers without level/player information and retain same-owner behavior.

The ownership predicate is deliberately separate from route knowledge. Being
allowed to open a door does not establish that an unrevealed secret door is
known to the moving player.

## Native evidence

The recovered IDA catalogue was consulted for the CDoor methods and alliance
helper. Function bodies and their branches were then inspected with Ghidra and
checked against native disassembly. The source installation and databases were
not modified. Research exports and captures remain outside the repository.

The probe supports the researched executable with SHA-256
`2a00df642ff7f9296f1e8d053329ba152f5dac07e357f2b39b6f1f1f1d77e163`.

- Door tick `0x004A55F0`, at `0x004A5EA5`–`0x004A5EAD`, supplies the creature's
  owner to the door owner's alliance helper at `0x004BF3D0`. That helper accepts
  either the same owner or an alliance-table entry.
- The same-tile opening-candidate block at `0x004A5E85`–`0x004A5F37` then checks
  the lock mode and creature worker flag. Mode 0 admits either worker category;
  mode 1 admits non-workers; mode 2 admits workers; mode 3 admits neither.
  The game also excludes a neutral-owner case and runtime flag `0x200000` before
  accepting the candidate. This patch does not claim to implement all those
  additional native conditions: OpenKeeper currently represents locks as a
  boolean, and this change preserves that model.
- The original candidate block was executed against synthetic records for
  48 combinations of same/allied/foreign ownership, lock mode, worker flag and
  the excluded runtime flag. All matched the recorded conditions. The harness
  enters that block directly and redirects its exit to a capture callback in
  private memory. It does not execute the entire door tick or its neighboring
  approach-tile scans.
- `0x004A66F0` encodes a secret tile marker in navigation bit `0x02` and door
  ownership in the upper nibble. The native route predicate at `0x004C8BD0`
  rejects a hidden door belonging to another owner before its other route-mode
  checks. This rejection does not consult alliances. An additional 32 original
  predicate calls verified hidden/known, owner, lock and route-mode combinations.
  Route-mode bits are recorded by bit position rather than given guessed names.
- The physical query at `0x004529A0` instead checks blueprint/open state and
  footprint; it does not consult ownership, alliances or the secret flag.
  The #1339 movement resolver clears its barricade result. Those geometry and
  response rules are unchanged by this passage patch.

## Reproduce the isolated native predicates

Requires GCC multilib and 32-bit execution. The executable is read and mapped
into private memory; no binary bytes or game fixtures are included in source.
The runner verifies the executable hash and refuses capture paths inside the
repository. Choose fresh output directories.

```sh
python3 research/dkii-door-passage/run_probe.py \
  '/home/wietse/Downloads/Data/Bullfrog/Dungeon Keeper 2/DKII.exe' \
  --kind opening --output /tmp/dkii-door-opening-check
python3 research/dkii-door-passage/run_probe.py \
  '/home/wietse/Downloads/Data/Bullfrog/Dungeon Keeper 2/DKII.exe' \
  --kind routing --output /tmp/dkii-door-routing-check
```

## Synthetic coverage

`DoorAccessTest` verifies opening for allied AI and possessed creatures,
normal versus breaking path costs, locking/unlocking, ownership changes,
alliance creation/breaking, blueprint build and removal. It uses synthetic
entities and the existing opening, position-index and path-cost code.

The change was additionally tested on the #1339 contact branch with allied AI
and possession movement, ordinary/known-secret/hero variants, stale AI routes,
and passage through an already-open foreign door. That combined suite passed
1,445 tests with four skipped (1,449 total). Those movement/geometry changes are
not included in this independent allied-door PR. The secret variant was a
synthetic definition-flag case, not evidence of a discovery transition.

The independent PR branch's complete suite also passed: 1,567 passed and
4 skipped (1,571 total), with no failures or errors.

Both saved native probes were run against the hash-checked reference executable:
48 opening candidates and 32 routing cases passed.

## Secret-door follow-up: #1365

The implementation and native discovery investigation are documented in
[secret-door-discovery.md](secret-door-discovery.md). The new work preserves
this allied-opening policy while keeping route knowledge and physical movement
separate. The original allied-only validation above did not test discovery.
