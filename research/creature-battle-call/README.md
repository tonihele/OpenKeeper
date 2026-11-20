# Creature battle calls (#815)

## Native findings

Starting with the Goblin's sound map, event `0xD6` resolves to the four
`gb_chrg10`, `gb_chrg12`, `gb_chrg17` and `gb_chrg18` clips. Troll and Warlock
categories map the same event to their own charge clips. These events are
one-shots with priority 230 in the local reference data.

The normal AI combat handler at `0x0045F030` requests event `0xD6` at
`0x0045F9B0`, after the target-navigation helper `0x00471D10` succeeds.
That helper establishes a route, enters movement state 8, saves the combat
state to resume, and records the pursued target. It initializes a ten-update
route-refresh counter. That counter controls route refresh, not sound cooldown.

The dispatch table uses 25-byte records at `0x006DA8B0`; state 8's update
callback is `0x00470E30`. This callback can refresh the route to the moving
target without requesting event `0xD6`. It returns to the saved combat state
when appropriate. The native call site stores the returned sound handle at
creature offset `+0x2E`, also used by other creature vocalizations; it is not a
separate "already played the battle call" flag.

Thus, for this normal AI path, the state/route lifecycle controls repeated
requests. This does not establish a once-per-entire-battle rule or exclude
other uses of the charge sound (for example, the original executable also
requests it on a separate randomized branch). These findings were established
by static inspection of the local executable, after checking the two IDA
catalogues. No live native audio trace was captured for this change.

## OpenKeeper implementation

OpenKeeper stays in `FIGHT` while pursuing an enemy. Its `Navigation` component
now records whether the movement is attack pursuit. A successful new pursuit
emits `CREATURE_CHARGE` through the existing server/client game event service;
the client resolves event `0xD6` on the creature's sound category at the
creature's location. Ordinary navigation defaults to non-pursuit.

Repeated calls and route refreshes while pursuit is active stay silent, even
if the target moves or changes. Removing navigation (including stopping to
attack) ends pursuit, so a later pursuit can make another call. Replacing it
with ordinary movement also ends pursuit. Failed paths, missing targets and
targets on the same tile produce no event. There is no extra timer or lasting
"already shouted" flag on the creature controller.

## Validation

`CreaturePursuitTest` exercises the real creature controller, entity data and
server event service with controlled pathfinding. It covers starting pursuit,
repeated ticks, moving/changing targets, controller recreation, stop/restart,
ordinary navigation to the same destination, replacing pursuit, failed-path
retry, missing targets and same-tile targets. The emitted event is also checked
through `GameEventSounds` for Goblin, Troll and Warlock categories.

The focused pursuit, creature-state and game-event tests passed. On this base
revision, compiling the entire test source set is blocked by the unrelated
outdated `CreatureSpawnSystem` constructor in `PortalSpawnCooldownTest.java`.
The focused run excluded only that source through a temporary Gradle init
script; repository build configuration and that test were unchanged.

No original-game assets, executable bytes, assembly or decompiler excerpts are
included in this directory or the implementation.

## Follow-up: pursuit animation and flower visibility

Static native tracing confirms that the combat-start selector `0x0045EB20`
selects locomotion mode 1 via `0x004887D0(1, 0)`. That mode uses the authored
run speed, and animation selection in `0x0048BE50` selects animation slot 2
(RUN) while moving. The Goblin's original level data maps RUN to `Goblin-Run`
with `HAS_START_ANIMATION`; the converted resources include `Goblin-RunStart`.
The exact sword-raising pose was not visually compared during this check.
OpenKeeper's `CreatureViewSystem` currently selects WALK for all Navigation,
including pursuit, so it never selects that RUN/start sequence on this path.

Native flower visibility predicate `0x0048E830` returns visible for effective
state category `0x16` (22). The combat states have category 22; the category
lookup at `0x00478050` unwraps movement states to their saved combat state,
so combat pursuit also retains this visibility reason. A separate branch keeps
the flower visible while current health is below the integer threshold
`levelAdjustedMaxHealth * 100 / 600` (roughly one-sixth health). These conditions
remain subject to the predicate's initial rendering/visibility eligibility
checks; they do not override fog or every special creature state.

OpenKeeper's `CreatureFlowerControl.isPersistentVisibilityRequired()` currently
checks only unhappy/angry mood. Combat and critically low health therefore lack
the corresponding persistent visibility reasons. These follow-ups are tracked in #833 and #834 and are now implemented alongside
the battle-call fix: pursuit selects RUN, the view control loops it and falls
back to WALK when RUN is absent, and flowers retain visibility for combat and
strictly sub-threshold positive health. Existing start-animation loading plays
`Goblin-RunStart` before the main run clip. Footstep lookup follows the selected
resource, including the WALK fallback.

Validation: 33 focused regressions passed across pursuit events, view-state
transitions, RUN looping/fallback, flower persistence, strict health boundaries,
recovery, existing mood behavior and preservation of inherited fog culling.
The standalone `GoblinRunSequenceProbe.java` also used the actual converted
Goblin models with the production animation loader: the start clip handed off
to RUN, which remained looping after ten seconds of simulated updates.
`installDist --offline` passed. The existing Portal test source compilation
blocker still requires the temporary exclusion documented above.
