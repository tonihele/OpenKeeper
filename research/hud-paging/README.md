# HUD paging controls (#811)

## Native comparison

The original game was launched directly into level 1 and level 20. Level 1 has only two large room icons and cannot fill two rows. Level 20's spell panel has two rows. Both use a narrow column at the left: forward/right above back/left. The user's level 20 screenshot with pinned rooms and spells also shows a separate left arrow column for each category.

Private inspection of the original executable and local IDA catalogue establishes the state behavior:

- The GUI definitions pair paging arrows with update callback `0x00410A60` and draw callback `0x004129E0`.
- `0x00410A60` compares the available item count with the panel capacity. Both arrows use state 3 when everything fits, resetting the scroll position. Both use state 1 when paging is needed.
- `0x004129E0` draws state 3 with RGB 100/100/100, preserving alpha, and state 1 at full RGB. Hover highlighting applies only to enabled controls.
- Click callback `0x00417730` clamps the page/column offset. The arrows do not dim independently at the two limits.

These are prose findings. No original executable bytes, assembly excerpts or decompiler output are included. The exploratory debugger attempts did not yield a usable runtime state capture; the callback rules above were verified statically and agree with the reference screenshots.

## OpenKeeper change

Stack both arrows to the left of `customScroll` content at half the panel height each. The creature portrait controller uses the same `PagingButtons` state helper, including empty and fitting portrait lists. Apply disabled RGB to each arrow's `NiftyImage`, preserving opacity; changing the general renderer color does not reliably tint images in the jME/Nifty batch pipeline.

Recalculate row layout after Nifty completes deferred moves into a second row. Previously, stale row widths caused gaps and inconsistent scroll limits. Scroll both rows together by a full icon column, including its margin, and derive the common limit from the widest row. Recalculate whether paging is needed after adding or moving icons.

## Validation

`installDist --offline` passed. The standalone GPU probe exercises the production controls and renderer with:

- Two large icons, representing level 1.
- Two rows that fit without paging.
- 41 icons requiring paging, including a shorter final row.
- A narrower panel representing reduced HUD space.
- Repeated forward/back movement, limits and rebuilding a populated panel into a sparse one.

It checks arrow placement, enablement, exact disabled RGB within rounding tolerance, aligned rows and contiguous icon columns. It also checks that both arrows remain enabled at the paging limit. Run with Java 25 from the repository root:

```sh
./gradlew installDist --offline
java -cp 'build/install/OpenKeeper/lib/*' research/hud-paging/PagingControlsRenderProbe.java
```

Screenshots are written to `/tmp/paging-controls-before-scroll.png` and `/tmp/paging-controls-after-scroll.png`. OpenKeeper level 1 was also inspected live to compare its two-icon room panel with the native reference. The full repository test suite has an existing compilation blocker in `PortalSpawnCooldownTest.java:107` (outdated `CreatureSpawnSystem` constructor call).

Related observations: category header text is tracked separately in #823; Shift-click pinning is already #221. The collapse/expand header text is wired and was observed working in OpenKeeper.
