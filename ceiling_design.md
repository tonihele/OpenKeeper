# Ceiling — implementation design

*Target: a re-implementation of Dungeon Keeper II's cave ceiling on a new
engine, consuming the original game data (`Terrain`, `Rooms`, the level
`.kwd`/`.kld` tile grid, the `Ceiling` engine texture) with its own class
hierarchy. Behavioural reference:
[research/ceiling.md](../research/ceiling.md). This paper contains no
binary-level detail; it describes the observable rules and a structure to
host them. It depends on two sibling papers for services it does not own:
[fog_of_war_design.md](fog_of_war_design.md) (the visibility query) and
[lighting_design.md](lighting_design.md) (everything about how the patch is
shaded). The open questions of the first draft were settled with the project
owner; §15 records the decisions and every section already reflects them.
One item (D6, a dependency on the room system) is still being researched.*

---

## 1. Goals and non-goals

**Goals**

1. Reproduce the look: a continuous rock ceiling over every open tile that
   presses down to a fixed height at wall faces and domes up over open
   caverns, tiled with one texture, ending exactly at the fog boundary.
2. Derive it entirely from the tile grid and the shipped terrain/room data,
   so stock *and* modded levels behave as in the original.
3. Keep the ceiling **entirely in the presentation layer**. In the original
   it is not saved, not checksummed and not transmitted; it is rebuilt from
   the tile grid whenever the presentation attaches to the world. Nothing in
   the deterministic simulation may read it.
4. Separate the three concerns the original interleaves: (a) the *clearance
   field* — a scalar defined on the tile grid that says how far the ceiling
   may rise (rules, derived from terrain), (b) the *patch* — the geometry
   emitted for one tile (a pure function of the field), (c) *when patches
   exist and are rebuilt* (an invalidation problem shared with the terrain
   slab builder).

**Non-goals**

- The terrain slab meshes themselves (floors, walls, wall tops). The ceiling
  is emitted alongside them by the same per-slab builder in the original,
  but it is independent geometry and this paper only specifies the ceiling.
- The lighting model. The patch is submitted black and is lit entirely by
  the model in [lighting_design.md](lighting_design.md) §9.
- The fog rules. This paper only *queries* visibility.
- Hero-fortress roofs and other above-ground scenery. The `Roof_*` /
  `*Ceiling` mesh assets in `Meshes.WAD` are ordinary level meshes placed as
  things and have nothing to do with this system.
- Reproducing the original's one-static-mesh-per-tile batching. That is a
  renderer decision (§11).

---

## 2. Concepts

**Clearance.** A per-tile integer `0..15`, defined on the tile grid, equal to
four times the distance from the tile's grid corner to the nearest tile of
the *opposite* solidity, clamped. It is the only input the ceiling geometry
has. Solid and open tiles both carry one, and because each measures distance
to the other kind, the field is continuous across every wall face — it is a
single "distance to the nearest solidity boundary" field, not two.

**Solidity.** A tile is *solid* if its terrain has the `SOLID` flag **or** it
carries the per-tile render-as-solid flag that rooms set on some of their
slabs (the same predicate the terrain builder uses). Solid tiles get no
ceiling patch; they still contribute clearance to their neighbours' patches.

**Patch.** The geometry emitted for one tile: a 3×3 grid of vertices forming
2×2 quads, one texture repeat across the whole tile, normals facing the
floor. The patch's 9 vertices are the clearance field evaluated at the tile's
4 corners, 4 edge midpoints and centre.

**Ceiling height.** Distance from the floor plane, **in tiles** — the unit
the sibling papers use for light radii and vision ranges (the simulation's
fixed-point equivalent is 4096 per tile, but nothing about the ceiling is
simulation state, so it stays in floats). `2.0` at a wall face, rising
asymptotically toward `4.0` over open ground and reaching `3.992` at maximum
clearance. Always *above* the floor; the original stores it negated because
of its vertex layout, which is an engine-local convention and is not
reproduced (see [research/ceiling.md](../research/ceiling.md), addendum 2).

**Front-end scene.** The 3-D main menu runs the same world renderer on a real
level but suppresses the ceiling entirely. This is a mode flag, not a
property of the level.

---

## 3. Data inputs (from the original files)

| input | source | used for |
|---|---|---|
| tile terrain id, per tile | level `*Map.kld` | solidity → clearance field |
| `Terrain.flags & SOLID` | `Terrain.kwd` | solidity |
| per-tile render-as-solid flag | runtime, set by room placement | solidity |
| tile → room id | runtime | per-room texture override |
| `Rooms[].ceilingResource` | `Rooms.kwd` | per-room texture override |
| `Rooms[].ceilingHeight` | `Rooms.kwd` | **nothing — dead field** (D2) |
| `Ceiling` texture (4 mip levels) | `DK2TextureCache/EngineTextures` | the one ceiling texture |
| explored bit, per tile | fog state | whether a patch is emitted |
| camera mode | presentation | first-person override of the fog test |

Two facts about the shipped data that shape the design:

- **Every shipped room has an empty `ceilingResource`.** The override path
  exists in code and is worth implementing (modded rooms may use it), but on
  stock data the entire ceiling is one texture. Do not build a
  texture-atlas/material-per-room architecture for a feature no stock asset
  uses; make the override a per-patch material lookup that almost always
  returns the same material (§9).
- **`ceilingHeight` is dead.** The original truncates it to one byte on load
  and never reads it. It is `0.0` in all 26 shipped rooms. Ceiling height
  comes from the clearance field alone.

---

## 4. Architecture

Four pieces, with a deliberate one-way dependency chain:

```
   TileGrid (terrain, solidity)          FogState        CameraMode
          |                                  |               |
          v                                  |               |
   ClearanceField        <-- derived, cached |               |
          |                                  |               |
          v                                  v               v
   CeilingPatchBuilder  <-------  CeilingVisibility (§7) ----+
          |
          v
   CeilingRenderer  (batching, materials, submission)
```

- **`ClearanceField`** owns the `0..15` grid. It is *derived state*: built
  from the tile grid, never saved, never transmitted, recomputable at any
  time. It is the only piece with a non-trivial algorithm (§6).
- **`CeilingVisibility`** answers "does tile *t* get a patch right now?" from
  solidity + fog + camera mode + scene mode (§7). Stateless.
- **`CeilingPatchBuilder`** turns (tile, clearance neighbourhood, material)
  into 9 vertices, 9 normals, 9 UVs and 8 triangles (§8). Pure function.
- **`CeilingRenderer`** decides how patches are grouped into draw calls and
  keeps them in sync with invalidation events (§10, §11).

The ceiling deliberately does **not** live inside the terrain slab builder,
even though the original emits it there. Keeping it separate means the
clearance field can be rebuilt on terrain change without touching slab
meshes, and the ceiling can be toggled (front-end scene, a graphics option)
without a slab rebuild.

---

## 5. Data structures

```
class ClearanceField {
    u8   value[width * height];      // 0..15, one per tile
    void rebuildAll(const TileGrid&);
    void rebuildRegion(Rect dirty, const TileGrid&);   // §6.3
    u8   at(int x, int y) const;     // 0 outside the map
}

struct CeilingPatch {
    TileCoord  tile;
    Vertex     v[9];                 // pos, normal, uv, colour
    MaterialId material;             // §9
}

struct CeilingConfig {
    float wallClearance = 2.0f;      // tiles, at a wall face (V = 0)
    float apexClearance = 4.0f;      // tiles, the parabola's asymptote (§8.1)
    bool  enabled       = true;      // false in the front-end scene; D8
}
```

`ClearanceField.value` is `u8` rather than a packed nibble on purpose. The
original packs it into the top 4 bits of the word that also holds the tile's
room id, which forces every room-id write in the codebase to mask around it.
A clone has no reason to inherit that coupling; a separate byte array costs
one byte per tile (16 KB on a 128×128 map) and removes a whole class of
aliasing bugs.

---

## 6. Rules — the clearance field

### 6.1 The metric

For a tile `(x, y)`, let `S(x,y)` be its solidity (§2). Search the 9×9 window
centred on the tile for tiles of the opposite solidity, and take the minimum
of

```
dx' = (tx - x < 0) ? (tx - x + 1) : (tx - x)
dy' = (ty - y < 0) ? (ty - y + 1) : (ty - y)
d²  = dx'² + dy'²
```

then

```
clearance(x,y) = min(15, round(4 · sqrt(min(d², 16))))
```

The `+1` on negative deltas is **not** an off-by-one. It converts a
centre-relative delta into a delta relative to the tile's upper-left grid
corner, which is where the value is conceptually anchored: the four tiles
meeting at that corner are all at `d = 0`. Reproduce it — the field is
sampled at corners in §8, and a true centre-to-centre metric would shift the
whole ceiling half a tile diagonally.

Consequences worth internalising:

- An open tile with a wall on its **left or top** edge has clearance `0`;
  the same tile with a wall only on its right or bottom edge has clearance
  `4`. The dome over a corridor is therefore not centred on the corridor —
  it is pushed half a tile toward `+x/+y`. This is the original's look.
- Tiles outside the map count as nothing (the search simply finds no
  opposite-solidity tile there). Treat out-of-bounds as "not a match" and let
  the `d² ≤ 16` cap do the rest; the map border row is solid in every shipped
  level, so this rarely matters.
- `4·d` with `d` capped at 4 means clearance saturates at 15 four tiles from
  any boundary. Practical range of the field: `0, 4, 5, 8, 9, 11, 12, 15`
  and a few values between, not a smooth ramp.

### 6.2 When it is computed

The original computes the whole field **once, at level load**, and never
again — no terrain change, room placement or dig updates it. Dig a corridor
into deep rock and the newly opened tiles keep the clearance they had *as
rock* (distance to the nearest open tile at load time), which is generally
larger than a freshly computed corridor value, so dug corridors get a higher,
lumpier ceiling than a corridor that existed at load.

**D1: the clone recomputes instead.** The field is rebuilt whenever solidity
changes, so clearance always matches the map as it now stands. This is a
deliberate, eyes-open divergence: a dug corridor will look different from the
original's — lower and more even — and since most of a played map is dug, the
difference is pervasive rather than marginal. It is chosen for consistency,
and it makes the field a pure function of the tile grid, which in turn makes
it safe to throw away and rebuild at any time (§4).

### 6.3 Incremental rebuild

Per D1 the incremental path is mandatory, and per **D5** its trigger is *any*
change to the solidity predicate — a terrain change **or** a render-as-solid
flag flip — not just terrain. One consequence to be aware of: building or
selling a room now reshapes the ceiling around it, which the original never
does because it never recomputes at all.

Two different radii are involved, and it is easy to conflate them:

- A solidity change at `(x, y)` can change `clearance` anywhere in the 9×9
  window around it — the search radius of §6.1.
- A clearance change at `(u, v)` can change the *height* of the patches of
  tiles `(u-1..u, v-1..v)` — a **2×2**, because a tile's clearance is the
  value at its own upper-left corner and each patch interpolates only its own
  four corners (§8.1). It is *not* the 4×4 that the original's sampling
  window suggests: that outer ring feeds normals, not heights.
- Normals reach one ring further, `(u-2..u+1, v-2..v+1)` — a 4×4.

Composing them: one changed tile dirties clearance over 9×9, patch *heights*
over 10×10, and patch *normals* over 12×12. Use 12×12 and stop thinking about
it; the saving from tracking heights and normals separately is not worth the
bug surface. Batch dirty rects per frame — digging out a 5×5 room is a single
16×16 rebuild, not 25 separate ones.

### 6.4 Cost

Full rebuild is `width · height · 81` solidity probes worst case. On a
128×128 map that is ~1.3 M probes of a byte array — a few milliseconds, fine
at load. Two easy wins if it ever matters: break out of the window scan as
soon as `d² = 0` is found (the common case next to a wall), and iterate the
window in increasing `d²` order using a precomputed offset list (the original
does exactly this — a 15-entry quarter-disc crossed with 4 rotations).

---

## 7. Rules — which tiles get a ceiling

A patch is emitted for tile `t` when **all** of:

1. `CeilingConfig.enabled` — false in the 3-D front-end scene.
2. `!isSolid(t)` — terrain `SOLID` flag clear **and** the per-tile
   render-as-solid flag clear. Same predicate as §2, same one the terrain
   builder uses; share the function, do not reimplement it.
3. `isVisible(t)` — the fog query from
   [fog_of_war_design.md](fog_of_war_design.md) §8.1, **or** the camera is in
   a first-person/possession mode. Possession renders the world without fog,
   and the ceiling follows that rule like everything else.

Nothing else gates it. In particular there is no per-terrain "has ceiling"
flag, no room opt-out, and no height threshold.

The fog condition is what makes the ceiling end in a clean line at the
explored boundary rather than floating over unexplored rock, and it is the
reason ceiling rebuilds are driven by fog events (§10).

---

## 8. The patch

### 8.1 Vertices

Nine vertices on a 3×3 grid over the tile, at local coordinates
`(i/2, j/2)` for `i, j ∈ {0,1,2}` — the 4 corners, 4 edge midpoints and the
centre.

A tile's clearance *is* the value at its upper-left grid corner (§6.1), so
the four corners of tile `(x, y)` carry the clearances of tiles `(x, y)`,
`(x+1, y)`, `(x, y+1)` and `(x+1, y+1)`. Interpolate those four bilinearly:

```
V(u, v) = bilinear( C[x][y],   C[x+1][y],
                    C[x][y+1], C[x+1][y+1] )        u, v ∈ {0, ½, 1}
```

There is no averaging across tiles and no wider stencil — heights use these
four values and nothing else. (The original samples a 4×4 tile block, but the
outer ring is for normals, §8.2, and its four diagonal entries are read by
nothing at all.)

Then map clearance to height:

```
h(V) = 4 − (4 − V/4)² / 8            tiles above the floor plane
     = 2 + V/4 − V²/128              same thing, expanded
```

| `V` | 0 | 2 | 4 | 6 | 8 | 10 | 12 | 15 |
|---|---|---|---|---|---|---|---|---|
| `h` | 2.000 | 2.469 | 2.875 | 3.219 | 3.500 | 3.719 | 3.875 | 3.992 |

A parabola whose apex sits at `V = 16`, one step past the maximum clearance
of 15 — so across the whole input range it is monotonic and concave. The
ceiling lifts fastest in the first tile away from a wall and flattens as it
approaches 4 tiles, which it never quite reaches.

Two things not to do:

- **Do not substitute a linear ramp.** It is nearly a full tile too low in
  open ground and it is the concavity that makes caverns read as domed rather
  than tented.
- **Map `V` to `h` per vertex, after interpolating `V`** — not the other way
  round. Interpolating `h` gives a different, flatter surface, and because
  `h` is non-linear the two sides of a shared edge would no longer agree.

Done in that order, neighbouring tiles interpolate the same corner values
along their shared edge, so patches join **exactly** — no cracks, no seam
pass, provided every patch uses the same rounding. Compute in `float` from
the integer clearances.

*Provenance:* this section is verified against the original's arithmetic, not
inferred — see [research/ceiling.md](../research/ceiling.md) addendum 2 for
the symbolic trace. An earlier draft of this paper specified a linear
`2 + V/16` with a cross-tile averaging stencil; both were wrong.

### 8.2 Normals

Per-vertex normals from the height field, facing **toward the floor**. The
lighting model's `n̂ · d` term drops back-facing contributions, so a ceiling
whose normals point up is lit by nothing and renders black.

Compute them by central differences over the bilinear surface, then
normalise. The rim vertices need a slope from *outside* the tile, so extend
the stencil one tile past each edge: evaluate `h(V)` at the edge-adjacent
neighbours `(0,-1) (1,-1) (-1,0) (-1,1) (2,0) (2,1) (0,2) (1,2)` relative to
the tile, exactly as the original does. This is the whole reason it samples a
4×4 block for a 1-tile patch.

Falling back to one-sided differences at the rim instead would be the single
most visible shortcut available here: normals would disagree across every
tile boundary and the ceiling would show a shading grid over the entire map,
independent of the texture's own 1-tile grid. The geometry would still be
crack-free, which makes this a surprisingly easy bug to misdiagnose.

Given that, independently computed normals already agree at shared vertices
to within rounding, so there is no need to share them across patches unless
your batching makes it free.

### 8.3 UVs

One full texture repeat per tile: `uv = (i/2, j/2)`. The original's ceiling
texture entry carries a width and height of exactly one tile, so the texture
tiles once per tile with no scaling, no rotation and no per-tile offset —
which means the ceiling shows a visible 1-tile grid. That is correct; do not
"fix" it by scaling the UVs across a cavern.

### 8.4 Triangles

8 triangles (2 per quad). Winding must present the patch's downward face to
the camera; cull the other side. Do not render it double-sided — the camera
is never above the ceiling except in debug fly-cams, and a double-sided
ceiling makes the overhead debug view unreadable.

### 8.5 Vertex colour

Black, opaque (`0, 0, 0, 255`). In the original's additive lighting model
(final = mesh base + global ambient + per-terrain vertex ambient + Σ light
contributions, [lighting_design.md](lighting_design.md) §9), a black base
means the ceiling has no self-colour and is entirely lit by torches, room
lights and ambient. That is why unlit caverns have a black ceiling and why
the ceiling directly over a torch is bright.

---

## 9. Texture and material

One material, resolved per patch:

```
material(tile):
    roomId = tile.roomId
    if roomId != 0:
        room = rooms[roomId]
        if room.data.ceilingResource.isValid():
            return materialFor(room.data.ceilingResource)
    return defaultCeilingMaterial        // the "Ceiling" engine texture
```

`defaultCeilingMaterial` is the engine texture named `Ceiling` — a 128×128
base with 64/32/16 mip levels in the texture cache. Load all four levels and
let the sampler mip; the ceiling is viewed at a grazing angle across the
whole screen and is the single most mip-sensitive surface in the game. Per
**D3**, anisotropic filtering is enabled on it — it cannot change silhouette
or colour, and no surface in the game gains more from it.

The room override reads the room's `ceilingResource` `ArtResource`. As noted
in §3 it is empty in all 26 shipped rooms, so this branch never fires on
stock data. Implement it anyway (it is cheap and modders can use it), but
keep it a plain material lookup — if every tile in a room resolves to the
same material, the renderer's batching (§11) handles it with no special
casing.

---

## 10. Invalidation

A patch must be rebuilt when anything in its inputs changes. The events, and
the region each dirties:

| event | dirty patches |
|---|---|
| fog: tile explored / unexplored | that tile |
| camera enters/leaves first-person or possession | **all** tiles (the fog gate flips globally) |
| terrain change at `(x,y)` (dig, reinforce) | clearance 9×9, patches **12×12** around it (§6.3) |
| render-as-solid flag flip (room built / sold) | same as a terrain change (§6.3, D5) |
| room created/destroyed/resized, with an override material | the room's tiles |
| `CeilingConfig.enabled` toggled | all tiles |

Two notes:

- **Fog is the high-frequency source.** Every creature step explores a few
  tiles. Coalesce dirty tiles into a per-frame set and rebuild once, after
  the fog system has finished its tick — not inside the fog callback.
- The camera-mode flip is a full rebuild of every ceiling patch on the map.
  It happens on possession enter/exit, which already stalls for other
  reasons; still, if your batching is per chunk, mark chunks dirty rather
  than tiles.

In the original this is all folded into "rebuild the slab", which rebuilds
floor, walls, wall tops, things and ceiling together. A clone that separates
them (§4) rebuilds far less per fog event, at the cost of tracking one more
dirty set.

---

## 11. Rendering contract

What the renderer receives, and what it may decide for itself.

**Fixed:**

- Geometry is static between invalidations. Nothing animates: no scroll, no
  wave, no per-frame vertex work.
- Per-vertex (Gouraud) lighting, like all static terrain geometry —
  [lighting_design.md](lighting_design.md) §9, per-vertex branch. Not flat,
  not per-pixel, unless you are deliberately upgrading the look.
- Opaque, depth-written, back-face culled, mipped, no alpha.
- Draw order: with the terrain, before things.

**Free:**

- Batching. The original emits one static mesh per tile; that is an artefact
  of its per-slab builder, not a requirement. Merging all patches of a chunk
  (say 16×16 tiles) that share a material into one mesh is strictly better
  and changes nothing visible. Rebuild the chunk mesh when any of its tiles
  is dirty.
- Culling. Per chunk, against the camera frustum.
- Whether normals are per-vertex arrays or derived in a shader.
- Filtering quality, within **D3**: anisotropy and mip generation are the
  renderer's business; per-pixel lighting and UVs smoothed across tile
  boundaries are not, because both change the look.

**Interaction with other systems:**

- Picking / mouse-over must ignore the ceiling. The cursor ray is cast
  against the floor plane and things; a ceiling patch between the camera and
  the floor would otherwise swallow every click in a top-down view. Put
  patches on a non-pickable layer.
- Shadows: the original has none. If you add shadow casting, exclude the
  ceiling or every room goes dark.

---

## 12. API sketch

```text
class CeilingSystem {
    // lifecycle
    void onWorldAttached(TileGrid&, FogState&, RoomTable&);
    void onWorldDetached();

    // invalidation (§10) — all idempotent, all coalesced until flush()
    void onTileFogChanged(int x, int y);
    void onRegionFogChanged(Rect r);
    void onTerrainChanged(int x, int y);
    void onRoomChanged(Room&);
    void onCameraModeChanged(CameraMode);
    void setEnabled(bool);                  // false for the front-end scene

    // called once per frame, after fog and terrain have settled
    void flush();                           // rebuild dirty patches, push to renderer

    // queries (debug / tools)
    u8    clearanceAt(int x, int y) const;
    float heightAt(float wx, float wy) const;   // bilinear, for a debug fly-cam
    bool  hasCeiling(int x, int y) const;       // §7
}
```

`heightAt` is not needed by the game — nothing collides with the ceiling, the
camera never clips it and no creature flies that high — but it is worth
having for a debug overlay that visualises the clearance field, which is by
far the fastest way to diagnose a wrong metric (§14).

---

## 13. Fidelity checklist

Things that are easy to get subtly wrong and are visible on stock data:

- [ ] The clearance metric is anchored at the tile's **upper-left corner**,
      not its centre: the 2×2 block around that corner is distance 0. A
      centre-anchored metric shifts every dome half a tile.
- [ ] Solid tiles carry a clearance too, and it is measured to the nearest
      *open* tile. Skipping them leaves the field discontinuous at every wall
      and produces a visible ridge along walls.
- [ ] Clearance saturates at 15 (four tiles from a boundary), so large
      caverns have a **flat** ceiling in the middle, not an ever-rising dome.
- [ ] Height at a wall face is `2.0`, not `0` — the ceiling never touches the
      wall top.
- [ ] The height curve is **quadratic**, `h = 4 − (4 − V/4)²/8`, not a linear
      ramp. A linear ramp is nearly a tile too low over open ground and makes
      caverns look tented rather than domed.
- [ ] `V` is interpolated first, `h` applied per vertex — never the reverse.
- [ ] Heights come from the tile's **own four corner clearances** only. The
      4×4 sampling window in the original is for normals; do not fold the
      outer ring into the height blend.
- [ ] Normals *do* need that outer ring. One-sided differences at the patch
      rim leave a shading grid across the whole map while the geometry stays
      perfectly crack-free — easy to misdiagnose as a lighting bug.
- [ ] Normals face down. Up-facing normals give a uniformly black ceiling
      that looks like "the ceiling isn't lit yet".
- [ ] Vertex colour is black; all visible colour comes from the lighting
      model. A white base makes every cavern look fogged.
- [ ] One texture repeat per tile. The 1-tile grid in the texture is correct.
- [ ] No ceiling over solid tiles, including room slabs flagged
      render-as-solid.
- [ ] No ceiling anywhere in the 3-D front-end scene.
- [ ] Unexplored tiles have no ceiling, *except* in first-person/possession,
      where the whole map is rendered unfogged.
- [ ] The ceiling is not pickable.
- [ ] `Rooms[].ceilingHeight` is ignored. If your loader wires it to
      anything, rooms will have wrong ceilings on modded data that sets it.
- [ ] Patches share corner and edge samples, so adjacent patches meet exactly
      — if you see cracks, the two sides are rounding differently.

---

## 14. Validation plan

1. **Clearance unit tests** on hand-written 16×16 grids: a single open tile
   in rock (expect 0 everywhere), a 1-wide corridor (expect the asymmetric
   0/4 pattern of §6.1), an open 9×9 cavern (expect 15 in the centre), a
   straight wall (expect the field symmetric across the face), the map
   border.
2. **Patch continuity test**: build patches for every tile of a random map
   and assert that every shared corner/edge vertex position is bit-identical
   between the two patches that own it.
3. **Debug overlay**: render the clearance field as a colour ramp on the
   floor. Almost every ceiling bug — wrong metric, stale field, wrong
   solidity predicate — is obvious in one screenshot of this overlay and
   nearly invisible in the ceiling itself.
4. **Screenshot comparison against the original** on a stock level: pick a
   large cavern (an open hero area) and a 1-wide dug corridor, match camera
   position, and compare ceiling silhouette against a wall and the dome
   apex height. The corridor case measures how far D1 takes us from the
   original: the original's dug corridor should sit noticeably higher than an
   identical corridor that existed at load, while this design's will not.
   Quantifying that gap is the point — see §15, "still to confirm", item 3.
5. **Fog boundary test**: reveal a region tile by tile and confirm the
   ceiling edge tracks the fog edge with no one-frame lag and no orphaned
   patches after unexplore.

---

## 15. Decision record

The first draft left eight questions open. They were settled as follows; each
section above already reflects the decision.

| # | question | decision | consequence in this design |
|---|---|---|---|
| D1 | Clearance field stale after digging (the original computes it once at load) | **Recompute on solidity change** | §6.2, §6.3. Deliberate divergence: dug corridors are lower and more even than the original's. Makes the incremental rebuild mandatory and the field a pure function of the tile grid. |
| D2 | `Rooms[].ceilingHeight`, dead in the original | **Ignore it** | §3, §9. Loader emits a warning if modded data sets it non-zero, so a modder is told the field is inert rather than silently ignored. |
| D3 | Where the line sits on visual upgrades | **Invisible-quality only** | §9, §11. Anisotropic filtering, better mip generation and chunk batching are in; per-pixel lighting and smoothed cross-tile UVs are out, since both change the look. The ceiling is the most grazing-angle surface in the game, so it gains the most from the filtering half. |
| D4 | Vertex blend weights (inferred in the first draft) | **Verify against the original** | Done — and the first draft was wrong. §8.1 now specifies a bilinear over the tile's own four corners plus the quadratic height curve `h = 4 − (4 − V/4)²/8` (range 2.000–3.992), replacing an inferred linear `2 + V/16` with cross-tile averaging (range 2.0–2.9375). Trace in [research/ceiling.md](../research/ceiling.md) addendum 2. |
| D5 | Does a render-as-solid flag flip also recompute clearance? | **Any solidity change** | §6.3, §10. One predicate, one trigger. Building or selling a room now reshapes nearby ceiling — new behaviour, following directly from D1. |
| D6 | Semantics of the per-tile render-as-solid flag | **Research it, own file** | Open. Traced only as far as "set by room placement, cleared by dungeon-heart and two room routines"; which room types set it on which slabs is unknown. Gets its own `research/*.md` and will be referenced from §2 and §7. **The one outstanding item in this paper.** |
| D7 | Which vertex component the original treats as vertical | **Won't-fix** | Closed in [research/ceiling.md](../research/ceiling.md) addendum 2. The implementation-relevant answer (height in tiles above the floor) is settled; the raw sign is an artefact of the original's vertex layout. |
| D8 | A "ceiling off" setting | **Debug-only** | §5 `CeilingConfig.enabled`. Exposed in the developer console, not in player-facing graphics options — the original has no such setting, and camera/lighting work needs it. |

### Still to confirm at integration

Not decisions, just things that want a look at the real thing:

1. **D6's rule**, once researched, may change which slabs get a ceiling in
   rooms. Until then, expect ceilings over some room slabs that should be
   solid.
2. **The exact expression for the 3 remaining vertices.** The symbolic trace
   covers the shared inner-loop body that writes all nine, so the model is
   general — but the eight further height evaluations that feed the normals
   (§8.2) were identified by their constants rather than traced term by term.
   If shading looks wrong at patch rims and the geometry is provably
   crack-free, this is where to look.
3. **Screenshot comparison** of a dug corridor against the original, to see
   exactly how large D1's divergence is in practice. If it turns out to be
   glaring rather than subtle, D1 is worth revisiting — the incremental
   machinery works either way, it is one flag.
