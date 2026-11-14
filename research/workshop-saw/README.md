# Workshop saw activation

The user's native DKII captures from 2026-11-14, 18:14:11–18:14:14, show the same stationary workbench with zero workshop workers while the simulation advances. The OpenKeeper captures show the unattended workbench animating. The `Spin` resource name alone does not establish native idle behavior.

Read-only inspection of the supplied IDA catalog identifies manufacture-state functions through their `ManufactureState.cpp` references. Static inspection of the local reference executable shows:

- Manufacture callbacks at `0x47062F`/`0x47092F` set the workbench activation flag and request object state 26 (`ACTIVATE_ANIM`) via `0x49EF60`.
- The manufacturing update at `0x470AED` calls `0x49D0B0` with object type 46 and the work tile coordinates. This helper refreshes an activation flag on matching objects at that tile.
- The activation update at `0x4A1A13`–`0x4A1A49` tests that flag. Without activation it resets the animation counters and state; it consumes the flag each update.

The idle case was observed in the user's native screenshots. Active-state behavior here is supported by static analysis; no new full native manufacturing session was completed. Exact native frame cadence and audiovisual effects are not claimed.

## Implementation

`WorkshopViewSystem`, after creature AI, derives the used workbench from a started manufacturing task in WORK state, without navigation, hand or death state, for the bench's owner. It updates the existing replicated `ObjectViewState`; no additional network component is needed. The object renderer holds the first frame for inactive workbenches and plays the normal authored resource for active workbenches. Other objects retain their animation behavior.

Synthetic tests cover empty/approaching workshops, separate benches and owners, interrupted work, multiple workers, restart and reconstructed systems. A real `ObjectViewControl` with a synthetic jME transform animation verifies idle, active, return-to-idle and restart playback without proprietary fixtures.

Validation: Java 25 `./gradlew build installDist --offline --no-configuration-cache --console=plain` passed: **895 passed, 4 skipped, zero failures**. A separate local probe loaded the user's converted original `Spin` model through the real object control and confirmed idle frame zero, active time advancement, and reset/stationary playback after stopping. That probe used local assets only and is not a committed test fixture.
