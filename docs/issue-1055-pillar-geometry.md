# Issue 1055: room pillar geometry

## Baseline behavior

Room construction and map rendering each had their own pillar room lists and
corner calculations. Normal rooms required a complete 3 by 3 interior at each
corner, while Workshop used its own exposed-corner rule. Workshop was missing
from the renderer's pillar list even though its object is flagged as a pillar.
Temple places candlesticks and has no cave-pillar layout; Lair and Prison have
no active native pillar object.

Every loaded object began at the same `FLOOR_HEIGHT`, even though the pillar
models have different local base heights. The model bounds therefore started
above the room floor by different amounts. Terrain deformation also pinned all
nine lattice points in each registered pillar tile, which affected shared
edges, and the ceiling skipped deformation at any vertex touching those tiles.

The pre-change tests reproduced both geometry discontinuities: pillar
registration pinned the pillar footprint and its shared edges, and a ceiling
boundary vertex touching a pillar produced a different result when its
deformation was skipped. These tests were run against the clean upstream-base
worktree before implementation. They have since been replaced by tests for the
intended contract below.

## Expected behavior

- Room construction and rendering use the same object IDs and placement
  algorithm for Treasury, Hatchery, Workshop, Torture, Graveyard, Casino, Pit,
  and Crypt.
- Each pillar's loaded model base aligns with the entity's floor height,
  regardless of the model's local origin.
- Pillar vertices and adjacent ceiling vertices sample the same terrain
  deformation field. Adding, moving, or removing a pillar does not pin or reset
  neighboring terrain deformation.
- A terrain shape update refreshes a tracked pillar's persistent mesh from its
  source geometry without accumulating displacement or allocating a new output
  mesh.
- Pillar tiles continue to contribute to cave clearance and do not receive a
  separate ceiling patch.

## Automated coverage

`PillarPlacementTest` covers all configured room IDs, normal-room interior
corners and holes, Workshop exposed-corner layouts, Temple candles, and
inactive Lair/Prison pillars. `PillarGeometryTest` varies model base offsets
and floor heights. `TerrainDeformerTest` checks shared-mesh safety, deterministic
reapplication, mesh reuse, and terrain invalidation. `CeilingPillarIntegrationTest`
compares a pillar-adjacent ceiling vertex with the same point passed directly
through the terrain field, then checks an aligned pillar mesh top against that
deformed ceiling boundary vertex.
