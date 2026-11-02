# DK II room construction and sale ordering

Verified from the reference DKII.exe and an isolated execution probe of its x86 traversal loops. No OpenKeeper game code was changed.

Reference executable: `/home/wietse/Downloads/Data/Bullfrog/Dungeon Keeper 2/DKII.exe`
SHA-256: `2a00df642ff7f9296f1e8d053329ba152f5dac07e357f2b39b6f1f1f1d77e163`.

## Findings

Building preserves mouse-down and mouse-release coordinates. It starts at the release corner and sweeps toward the mouse-down corner along diagonals of constant Manhattan distance. Each diagonal is processed in a single game tick. Within a diagonal, x advances from the release x toward the starting x. A valid 3 by 3 rectangle therefore processes 1, 2, 3, 2, 1 tiles at elapsed ticks 0 through 4. This is not a complete row per tick or one tile per tick.

Selling uses the same diagonal progression, but the input handler normalizes the bounds first. The sweep therefore starts at (maxX,maxY), ending at (minX,minY), independently of drag direction. These are map axes, not screen directions under camera rotation.

For ordinary valid tiles the schedule is `elapsedTick = abs(x-endX) + abs(y-endY)`. The tile validation, affordability, effects, and completion branches were bypassed in the probe; this is a verification of traversal, not a complete gameplay test. Special room and failure behavior needs separate investigation. The origin tick is stored on queue creation; the first actual update's elapsed value depends on game-loop scheduling.

## Native call paths

- IDA databases inspected: `DKII.idb`/`newDKII.idb` merged name export and direct `newDKII.idb` string/xref queries. Most relevant functions lack recovered names; disassembly of the executable supplies the logic below.
- `0x4075E0`: input handler. Build dispatch at `0x408184`–`0x40826F` preserves endpoints, sending action `0x2F`. Sell dispatch at `0x4083DA`–`0x40847E` sorts endpoints and sends action `0x31`.
- `0x511A80`: runtime action-table initialization. Registers build `0x2F` at `0x6A556C` with callback `0x512850`, and sale `0x31` at `0x6A5594` with callback `0x5128F0`.
- `0x512850` -> `0x4C6EF0`: decode build action and store endpoints plus creation tick in one of three 25-byte pending records beginning at player offset `0x1C2`.
- `0x5128F0` -> `0x4C70A0`: decode sale action and store endpoints plus creation tick in one of three pending records beginning at player offset `0xE97`.
- `0x4C7380`: build updater. `0x4C7426`–`0x4C7471` calculates elapsed tick and `y = endY + sign(startY-endY) * (elapsed - abs(x-endX))`. The x loop starts at endX and advances toward startX at `0x4C7782`–`0x4C77A0`.
- `0x4C7160`: sale updater. Corresponding diagonal calculation at `0x4C71F9`–`0x4C723C`; sale call at `0x4C7283`; x advance at `0x4C7292`–`0x4C72A0`.
- `0x517490`: returns the world game tick via `[[this+4]+4]`.
- `GameTickStartedConstructionSequence` at `0x6A28A8` belongs to Dungeon Heart animation state; it is not the selection queue timestamp.

## Probe

`probe.c` loads the PE sections into a separate 32-bit process. It substitutes a controlled tick getter at `0x517490`, logs valid candidate coordinates at `0x4C74C4` and `0x4C727B`, then jumps past side effects. It leaves the executable on disk untouched. Build cases cover all four drag directions. Sale tests its normalized bounds, as supplied by the native input path.

Run on a system with 32-bit GCC support:

```sh
gcc -m32 -fno-pie -no-pie -O0 probe.c -o /tmp/dkii-room-order-probe
/tmp/dkii-room-order-probe '/home/wietse/Downloads/Data/Bullfrog/Dungeon Keeper 2/DKII.exe'
```

The full reference game was also launched directly with `-level level1 -nomovies`, but exited before a usable gameplay capture. Findings above come from disassembly and the isolated native-code probe, not a visual gameplay run.

## In-game showcase

From the repository root, run:

```sh
research/dkii-room-order/run_showcase.sh
```

Requires JDK 25, converted game assets, cached Gradle dependencies, and the local `TrainMixed______` reference map (not shipped in the repository). Set `JAVA_HOME` if needed.

The launcher uses the real `Main`, game server, room-action queue, entity replication and map renderer. It creates a temporary claimed patch in memory, then cycles through all four 5 by 5 build directions and normalized selling. The installed map files are not edited.

- **Space** pauses/resumes the showcase logic.
- **N** toggles normal tick rate / 4 ticks per second.
- **F12** captures the game view under `/tmp/openkeeper-room-showcase/frames/`.
- `--capture` also records 54 seconds of PNG frames at approximately 10 FPS for making a clip. Game audio is enabled; audio recording is separate.

Gold storage is filled before each build and before each sale. Sale proceeds then remain on the floor: 25 tiles costing 500 each, sold at 50%, leave 6,250 loose gold. The demo checks all 25 tiles were built and reports the loose gold total after each sale. Creatures collecting gold or spending it on training can reduce the visible floor total during later cycles. Between cycles it removes only the demo area's loose gold to reset the scene.

Slow mode changes only this process's logic-loop interval. The demo hides the ceiling and raises ambient light for clarity. These are showcase controls, not changes to normal build/sell timing.

The live run exposed a stale room tooltip reference during selling. `RoomTextParser` now resolves the room once and omits the tooltip if its room entity has already disappeared.

## Native sound follow-up

Static inspection of the same reference executable confirms:

- `0x4C5D7F`: build tile sound event `0xF1` (room sound category). This path checks the creation tick before playing it.
- `0x4C5EEE`: sold tile sound event `0xF5` (room sound category).
- `0x442D4D`: room placement sound event `0x2F8`.

OpenKeeper already maps `ROOM_TILE_BUILT`, `ROOM_SOLD`, and `ROOM_TILES_LAID` to those events in `GameEventSounds`. The new queue supplies their tile progression. These sound IDs do not establish the mapping of visual build/sell animations; that mapping is not claimed by this probe.
