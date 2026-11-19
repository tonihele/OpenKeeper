# Creature animation slots and actions

`Creature.AnimationSlot` identifies the native descriptor. Its explicit hexadecimal
value is independent of Java enum order. `AnimationType` identifies the playback
action. Historical names and ordinals remain for existing saved/replicated states;
new worker actions are appended. New actions require matching client/server
versions, as with other changes to replicated enums.

There is one resource registry, keyed by slot. `KwdFile` reads each original field
into that registry. `getAnimation(slot)` and `getAnimation(action)` inspect the raw
resource, including missing resources. Availability decisions (casino choices,
celebration, model inspection) must continue to use raw resources.
`getAnimationForAction(action)` resolves playback fallback without populating or
changing the registry. Rendering and animation duration use this same resolver.
Sound remains attached to the requested action: sharing an eating resource slot
must not make floor claiming play an eating sound.

## File layout

The first 36 consecutive animation resource fields are descriptor IDs `0x01` through
`0x24`. Later fields are interleaved with other creature data:

| Descriptor | Historical resource name | Position in the reader |
| --- | --- | --- |
| `0x25` | DRUNKED_IDLE | After the first-person filter and unknown scalar fields |
| `0x26` | BACK_OFF | After the animation offsets |
| `0x27` | STAND_STILL | After the footstep marks |
| `0x28` | STEALTH_WALK | Following STAND_STILL |
| `0x29` | DEATH_POSE | Following STEALTH_WALK |
| `0x2A`–`0x2F` | SWIPE, IDLE_3, IDLE_4, IDLE_3_1, IDLE_4_1, DIG | Six fields after torture HP/mood changes |

`TRAINING` was never a resource field. It is an action using `0x05`.
There are 47 file resource slots; native descriptor zero is an empty record and
has no corresponding file resource entry.

## Shared meanings

Actions express what the creature is doing; capability and task selection decide
whether it can do that work. Slot resolution needs no Imp/Dwarf or worker/fighter
split. In the reference catalog:

| Action | Slot | Imp | Dwarf | Ordinary fighter example (Warlock) |
| --- | --- | --- | --- | --- |
| DIGGING / MELEE_ATTACK | `0x05` | Primary dig | Melee used for digging | Melee |
| REINFORCE_WALL / SLEEPING | `0x0C` | Wall claiming | Wall claiming | Sleeping |
| CLAIM_FLOOR / EATING | `0x0D` | Floor claiming | Land claiming | Eating |
| HAUL / DRINKING | `0x13` | Dragging | Dragging | Drinking |

The extra Imp resource `0x2F` is retained as the legacy `DIG` entry for inspection
and compatibility. The Dwarf lacks it. Neither worker's digging task selects it.
The native action table selects `0x05` instead; the Dwarf therefore uses its own
primary resource rather than depending on the Imp's extra resource.

The special and roar names also describe resources rather than universal actions.
Slot `0x20` includes whipping, bat transformation, attention and jumping, depending
on the creature; the implemented torture job now requests `TORTURE_VICTIM`.
Slot `0x23` includes roaring, looting, corpse eating, happiness and jumping.
Horny's `0x0D`/`0x0E` resources are alternate attacks, `0x17` is gem pickup, and
`0x19` is chicken stamping. These assignments do not justify inventing new game
actions from filenames or selecting them by worker/fighter class.

## Native audit and verification

Reference: DKII 1.7, the local `DKII.exe` and IDA symbol exports, and the
[issue audit](https://github.com/tonihele/OpenKeeper/issues/1334#issuecomment-7009775346).
Addresses below identify executable functions/instructions, not descriptor IDs.

- Loader `0x492320` fills 27-byte runtime descriptors. Its main resource loop
  starts at ID 1 and ends before `0x25`; the separate later fields account for
  the non-enum ordering above.
- Getter `0x494860` calls `0x494880` once, then indexes the returned descriptor.
  It does **not** recursively follow fallback chains. The fallback implementation
  is readable control flow; no executable fragments or extracted lookup tables
  are included in the repository.
- Action initialization `0x4950C0` assigns descriptor `0x05` and dig sound `0x334`
  to action `0x01`, whose handler `0x496E00` works on the target terrain (including
  the terrain operation at `0x49713D`). The resource choice is shared across
  creature types. Actions `0x07`/`0x08` use `0x0C` for wall work; floor-work actions
  use `0x0D`. Ordinary sleep/eat actions share these slots.
- Action playback `0x496700` reads the action's descriptor ID and obtains it
  through `0x494860`. The hauling picker `0x48BE50` requests `0x13` at `0x48C234`.
  No action in the shared table selects the extra fields `0x2A` through `0x2F`.
- Missing `0x13` falls back once to `0x09`; missing `0x09` falls back once to
  `0x05`. A single hauling lookup does not continue all the way to `0x05`.
  Missing wall-work slot `0x0C` has no replacement. Missing floor-work slot
  `0x0D` selects `0x08`. These are descriptor fallbacks, not a change of task.

Validation performed for this change:

- Loaded the local level-one reference catalogs through `KwdFile`: 46 creatures,
  each with 47 resource slots. Checked Imp/Dwarf primary digging, floor/wall work,
  hauling and the Imp-only extra resource alongside the Warlock's shared slots.
- Compiled the pre-change reader/data classes separately outside the repository
  and compared their catalog output with the refactored reader: all 2,162 entries,
  including empty entries, retained their names, types, frame counts and FPS.
- Synthetic tests cover aliasing without duplicated storage, differing Imp/Dwarf
  resources, raw availability, one-step fallback, action sounds, hauling state
  transitions, and preserved legacy enum ordinals.

This is static executable/catalog validation, not a new running-game visual or
GDB trace. Research outputs and original assets stay outside committed source.
Transport position/height behavior belongs to issue #1330 and is unchanged here.
