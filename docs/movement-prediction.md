# Movement simulation and presentation

## Design

The server owns creature position. The level tick rate drives one fixed-step
`GameLoop`; each simulation tick receives the same `1 / ticksPerSecond` delta.
AI path following, possessed input, falling, dungeon-heart construction, and
creature animation run on that loop. Animation observes movement changes from
the same tick. Game state requests arriving from the network are queued for the
start of a tick, while possessed movement keeps only the latest input per
player. Both paths avoid waiting for the simulation thread.

AI movement advances by at most `Mobile.maxSpeed * timestep` and clamps travel
to each path point. Turning uses the shortest angular difference and a bounded
rate. AI and possessed movement share a grid sweep that checks each crossed
tile with the existing navigation passability rule, holds creatures back from
blocked tile edges, and slides along a wall when one axis is blocked.
The same movement system routes chickens through their own navigable
controller, so a wandering chicken cannot abort movement for later entities
in the simulation tick.

The client view keeps a bounded history of received creature positions. It
interpolates position and facing on the render thread, then extrapolates for a
short configured window when snapshots pause. This changes only creature
models; it never writes predicted positions back to entity data. Non-creature
entities continue to use their existing direct view updates. Repeated
stationary positions are retained in the bounded history so stopping cancels
the extrapolated velocity. Large corrections clear history and snap to the new
authoritative position.

Network input mutations are applied on the simulation thread. The ordinary
action queue is bounded at 1,024 items and rejects excess requests without
blocking; high-rate possessed movement uses a per-player latest-value mailbox
whose inputs are applied in stable player-ID order. This follows the
main-thread request model discussed in [issue #425](https://github.com/tonihele/OpenKeeper/issues/425)
and [PR #904](https://github.com/tonihele/OpenKeeper/pull/904). Movement input
is coalesced separately because it arrives at a higher rate. The grid collision
step also addresses the wall-running regression in
[issue #430](https://github.com/tonihele/OpenKeeper/issues/430).

## Defaults and configuration

`GameLoopManager` and `PlayerEntityViewState` default to
`MovementSettings.DEFAULT`; both expose constructors for overriding it for a
session or test. Values use world units and seconds:

| Setting | Default | Purpose |
| --- | ---: | --- |
| Waypoint tolerance | 0.1 | Distance at which a path point is complete |
| Facing tolerance | 0.1 rad | Angular error accepted for a face target |
| Turn rate | 20 rad/s | Maximum stationary turning speed |
| Wall approach gap | 1/6 tile | Distance left before a blocked tile edge |
| Collision step | 1/4 tile | Maximum sweep distance per collision check |
| Interpolation delay | 0.1 s | Render behind local snapshot arrival time |
| Maximum extrapolation | 0.05 s | Maximum render projection beyond the latest sample |
| Teleport distance | 2 tiles | Correction that discards the old view history |

Collision settings reject a wall gap of half a tile or more and sweep steps
larger than half a tile, preventing invalid tuning from moving a creature
backwards or skipping a blocked tile.

## Limitations

The render buffer uses local snapshot arrival times because the view layer does
not currently receive authoritative server timestamps. Its short extrapolation
does not query map collision, so a remote model can render slightly ahead before
the next authoritative correction. The simulation remains authoritative and
collision checked. This implementation smooths received positions; it does not
add client command replay, rollback, or reconciliation. Multiplayer latency
behavior still needs validation in a runnable game session.
