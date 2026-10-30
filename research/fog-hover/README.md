# Fog hover hint (#817)

The supplied DK II screenshot (`2026-10-28 20:47:49`) shows “Mist” both in the header and beside the cursor.

## Native findings

The local IDA symbol catalogue identifies the relevant map text and hint routines. Inspection of the reference executable establishes:

- `0x0042D4A0` selects localized string 348 when the pointed tile's exploration flag is absent, before considering entities or terrain details. This is the text routine used by the map cursor popup.
- `0x0042E590` handles cursor hints. Normal map hints wait 1500 ms; GUI hints have a separate 250 ms delay. An explicit immediate-hint path also exists.
- `0x00419FF9`–`0x0041A02E` identifies the map hover target. Fog uses the shared target identifier `0x1000`, so moving between unexplored tiles does not restart the delay. Leaving that target does.

These notes describe behavior; no executable bytes, assembly excerpts or decompiler output are included.

## Implementation

Retain the existing `isExplored()` eligibility rule and header string 348. `MapHoverHint` uses Nifty's existing hint control and OpenKeeper styling, with a map-target delay rather than an inactivity delay. The same fog target remains eligible while moving between unexplored tiles.

Refresh eligibility each frame so revealing a tile clears the hint even without mouse movement. Mouse movement also checks eligibility immediately so entering the HUD clears it. Disabling or cleaning up the interaction state clears its hint. Entity hover text must not take priority over unexplored terrain.

## Validation

Build the installed distribution, then run the standalone probe with Java 25 from the repository root:

```sh
./gradlew installDist --offline
java -cp 'build/install/OpenKeeper/lib/*' research/fog-hover/FogHoverHintRenderProbe.java
```

The probe uses the production Nifty controls, styling and jME batch renderer. It verifies the 1.5-second delay, actual hint rendering, coexistence with HUD hints, cancellation, re-entry and cleanup. It writes `/tmp/fog-hover-visible.png` for visual inspection.

`installDist` and the GPU probe passed. The full test suite is blocked before execution by an existing `PortalSpawnCooldownTest.java:107` constructor mismatch. Live inspection on level 1 confirmed “Mist” in both the header and cursor popup; the user reviewed the result.
