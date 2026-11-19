# Walking, possession and door physics (#1339)

This change builds on #1258's creature sizing and logic-loop movement. It
corrects the earlier draft's use of the native door slab for walking and
possession. The slab implementation and tests asserting that behavior have
been removed, rather than treating a verified geometric query as proof that
it belongs in every movement mode.

## Native call paths

Both supplied IDA catalogues were consulted. Static inspection identifies
ordinary walking in 0x004D4760, whose landing/access calls use the function
pointer at 0x006A11D0. The executable initializes that pointer to edge rule
0x004C8BD0. This walking resolver does not call the slab query 0x004529A0.
The separate physics resolver 0x004B2030 does use that query, including its
centre-point and already-inside handling. Its geometry observations remain
valid, but do not establish walking clearance.

The possession client routine 0x0040E910 reads the full unsigned definition
width at runtime definition +0x63A. It offsets each leading point by that
width, checks tile access through 0x0040F6B0, clamps blocked axes to a tile
edge, then subtracts the signed width to recover the creature centre. The
width is not halved and visual height is not used in this calculation.
A live Workshop check for Trolls and Imps confirms that this definition field
is approximately twice the walking pilot radius, with the expected fixed-point
truncation of an odd half-width.

An original 32-bit harness loads the user's executable into private memory
and executes the collision block 0x0040EDB8–0x0040F04B with synthetic controller,
creature and width records. Only world bounds and tile-access responses are
substituted. Twenty-four cases cover three synthetic widths, both axes,
both directions and accessible/blocked tiles. All match full-width clearance.
This is execution of the native collision block, not a full in-game possession
matrix or validation of ownership, visibility and alliance policy.

No proprietary executable bytes, extracted fixtures, or captures are committed.
The harness and captured outputs remain outside the source tree.

## OpenKeeper behavior and boundaries

Possession probes the leading point using `CreatureSizing.getBodyWidth()`.
The sweep visits intermediate tile boundaries, so a large update cannot skip
a blocked door, wall, room obstacle or map boundary. Each axis is resolved
separately to preserve manual sliding and retreat. An occupant of a newly
blocked tile can still leave; the separate native door-tick displacement is
not implemented here.

Direct movement uses the existing tile-access policy, but rejects a blocking
door even when path planning permits paying a door-breaking cost. Existing
AI door attack/replan behavior is retained by #1258's walking tile guard.
No per-door-type slab or half-width expansion is added to ordinary walking.

Authoritative movement runs on the logic loop after door-view updates and
before hauling, as established in the parent PR. There is no new contact or
cache work on the legacy 60 FPS steering thread. Lower-frequency authoritative
updates remain visible without client prediction; this change does not add a
prediction/render-interpolation system.

Door opening from neighbouring passage tiles, alliances, secret-door visibility
and the complete passage policy remain coordinated with #1288 and #1327. This
PR does not duplicate their opening changes or claim that the inherited tile
policy is a complete match for the native edge rule.

Physics impulses/shots against the slab and closed-door push-away need their
own movement-mode integration and native validation. #1339 remains open.
