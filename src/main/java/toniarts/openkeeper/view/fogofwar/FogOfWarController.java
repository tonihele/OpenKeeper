/*
 * Copyright (C) 2014-2026 OpenKeeper
 *
 * OpenKeeper is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OpenKeeper is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenKeeper.  If not, see <http://www.gnu.org/licenses/>.
 */
package toniarts.openkeeper.view.fogofwar;

import com.simsilica.es.Entity;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.EntitySet;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureImprisoned;
import toniarts.openkeeper.game.component.Death;
import toniarts.openkeeper.game.component.DoorComponent;
import toniarts.openkeeper.game.component.DoorViewState;
import toniarts.openkeeper.game.component.DungeonHeart;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.RoomComponent;
import toniarts.openkeeper.game.fogofwar.FogOfWarRules;
import toniarts.openkeeper.game.fogofwar.FogState;
import toniarts.openkeeper.game.fogofwar.LineOfSightTable;
import toniarts.openkeeper.game.map.IMapDataInformation;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.Door;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.tools.convert.map.Thing;
import toniarts.openkeeper.tools.convert.map.Variable;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * The live, per-viewer fog-of-war orchestrator. Runs entirely client-side:
 * it is driven by the same shared {@link EntityData} the renderer already
 * reads (see the fog-of-war design document §4), so it needs no networking
 * of its own beyond the handful of scripted/cheat triggers that originate on
 * the server (reveal action points, the reset/disable console commands).
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class FogOfWarController implements IFogOfWarInformation {

    private static final float VISION_UPDATE_INTERVAL = 0.15f;

    /**
     * Dungeon-heart reveal disc radii, one per unfolding step (§6.6).
     */
    private static final int[] HEART_REVEAL_STEPS = {18, 37, 54, 73, 91, 110, 128};

    /**
     * Number of {@link #VISION_UPDATE_INTERVAL} passes before the heart
     * reveal starts. The design document's timing (tick 56-62) is calibrated
     * to the original's fixed simulation tick rate, which has no equivalent
     * constant in this engine; this approximates it against our own update
     * cadence instead.
     */
    private static final int HEART_REVEAL_START_INTERVALS = 56;

    private final EntityData entityData;
    private final IKwdFile kwdFile;
    private final IMapDataInformation<? extends IMapTileInformation> mapData;
    private final short viewerId;
    private final Consumer<Point[]> onTilesDirty;
    private final Consumer<EntityId> onEnemySighted;

    private final FogState state;
    private final LineOfSightTable losTable;

    private EntitySet creatureEntities;
    private EntitySet doorEntities;
    private EntitySet heartRoomEntities;

    private final Map<Point, Entity> doorsByPosition = new HashMap<>();
    private final Set<Point> closedDoorTiles = new HashSet<>();
    private final Set<EntityId> seenEnemies = new HashSet<>();
    private final List<PendingHeartReveal> pendingHeartReveals = new ArrayList<>();

    private EntityId possessedCreature;
    private float visionUpdateAccumulator;

    public FogOfWarController(EntityData entityData, IKwdFile kwdFile,
            IMapDataInformation<? extends IMapTileInformation> mapData, short viewerId,
            Consumer<Point[]> onTilesDirty, Consumer<EntityId> onEnemySighted) {
        this.entityData = entityData;
        this.kwdFile = kwdFile;
        this.mapData = mapData;
        this.viewerId = viewerId;
        this.onTilesDirty = onTilesDirty;
        this.onEnemySighted = onEnemySighted;

        this.state = new FogState(viewerId, mapData.getWidth(), mapData.getHeight());
        this.losTable = LineOfSightTable.generate(7);
    }

    public void start() {
        creatureEntities = entityData.getEntities(Position.class, Owner.class, CreatureComponent.class);
        doorEntities = entityData.getEntities(Position.class, DoorComponent.class, DoorViewState.class);
        heartRoomEntities = entityData.getEntities(RoomComponent.class, Owner.class, DungeonHeart.class);
    }

    public void stop() {
        creatureEntities.release();
        doorEntities.release();
        heartRoomEntities.release();
    }

    public void update(float tpf) {
        heartRoomEntities.applyChanges();
        for (Entity e : heartRoomEntities.getAddedEntities()) {
            Owner owner = e.get(Owner.class);
            RoomComponent room = e.get(RoomComponent.class);
            if (owner.ownerId == viewerId && room.location != null) {
                pendingHeartReveals.add(new PendingHeartReveal(room.location));
            }
        }

        visionUpdateAccumulator += tpf;
        if (visionUpdateAccumulator >= VISION_UPDATE_INTERVAL) {
            visionUpdateAccumulator -= VISION_UPDATE_INTERVAL;

            updateDoors();
            updateCreatureVision();
            updatePossession();
            updateHeartReveals();
        }

        Set<Point> dirty = state.drainDirtyTiles();
        if (!dirty.isEmpty()) {
            onTilesDirty.accept(dirty.toArray(new Point[0]));
        }
    }

    private void updateDoors() {
        doorEntities.applyChanges();
        doorsByPosition.clear();
        closedDoorTiles.clear();
        for (Entity e : doorEntities) {
            Position position = e.get(Position.class);
            Point p = WorldUtils.vectorToPoint(position.position);
            doorsByPosition.put(p, e);
            if (!e.get(DoorViewState.class).open) {
                closedDoorTiles.add(p);
            }
        }
    }

    private boolean blocksSight(int x, int y) {
        IMapTileInformation tile = mapData.getTile(x, y);
        if (tile == null) {
            return true;
        }
        Terrain terrain = kwdFile.getTerrain(tile.getTerrainId());
        if (terrain.getFlags().contains(Terrain.TerrainFlag.SOLID)) {
            return true;
        }
        Entity door = doorsByPosition.get(new Point(x, y));
        if (door == null) {
            return false;
        }
        Door doorData = kwdFile.getDoorById(door.get(DoorComponent.class).doorId);
        return !doorData.getFlags().contains(Door.DoorFlag.IS_BARRICADE);
    }

    private void updateCreatureVision() {
        creatureEntities.applyChanges();

        for (Entity e : creatureEntities) {
            Owner owner = e.get(Owner.class);
            if (owner.ownerId != viewerId) {
                continue;
            }
            if (entityData.getComponent(e.getId(), CreatureImprisoned.class) != null
                    || entityData.getComponent(e.getId(), Death.class) != null) {
                continue;
            }

            Position position = e.get(Position.class);
            Point tile = WorldUtils.vectorToPoint(position.position);
            if (closedDoorTiles.contains(tile)) {
                continue;
            }

            Creature.Attributes attributes = kwdFile.getCreature(e.get(CreatureComponent.class).creatureId).getAttributes();
            int radius = FogOfWarRules.clampVisionRadius(attributes.getDistanceCanSee());
            FogOfWarRules.exploreVisionRing(state, this::blocksSight, losTable, tile.x, tile.y, radius);
            FogOfWarRules.perceiveDisc(state, tile.x, tile.y, attributes.getPerceptionRange());
        }

        // "Enemy sighted" (§8.5.3): the first time an enemy creature's tile
        // becomes explored, notify once. Checked after our own creatures'
        // vision above so a tile explored this very pass still counts.
        Set<Point> newlyDirty = state.peekDirtyTiles();
        if (newlyDirty.isEmpty()) {
            return;
        }
        for (Entity e : creatureEntities) {
            Owner owner = e.get(Owner.class);
            if (owner.ownerId == viewerId || owner.ownerId == Player.NEUTRAL_PLAYER_ID) {
                continue;
            }
            EntityId id = e.getId();
            if (seenEnemies.contains(id)) {
                continue;
            }
            Position position = e.get(Position.class);
            Point tile = WorldUtils.vectorToPoint(position.position);
            if (newlyDirty.contains(tile) && state.isExplored(tile.x, tile.y)) {
                seenEnemies.add(id);
                onEnemySighted.accept(id);
            }
        }
    }

    private void updatePossession() {
        if (possessedCreature == null) {
            return;
        }
        Entity e = entityData.getEntity(possessedCreature, Position.class);
        if (e == null) {
            return;
        }
        Point tile = WorldUtils.vectorToPoint(e.get(Position.class).position);
        FogOfWarRules.explore(state, tile.x, tile.y);
    }

    private void updateHeartReveals() {
        if (pendingHeartReveals.isEmpty()) {
            return;
        }
        float scanRadius = kwdFile.getVariables()
                .get(Variable.MiscVariable.MiscType.DUNGEON_HEART_CLAIM_SCAN_RADIUS_TILES).getValue();

        Iterator<PendingHeartReveal> it = pendingHeartReveals.iterator();
        while (it.hasNext()) {
            PendingHeartReveal reveal = it.next();
            reveal.elapsedIntervals++;
            if (reveal.elapsedIntervals < HEART_REVEAL_START_INTERVALS) {
                continue;
            }
            int step = reveal.elapsedIntervals - HEART_REVEAL_START_INTERVALS;
            if (step >= HEART_REVEAL_STEPS.length) {
                it.remove();
                continue;
            }
            float radius = scanRadius * HEART_REVEAL_STEPS[step] / 128f;
            FogOfWarRules.exploreDisc(state, reveal.center.x, reveal.center.y, radius);
        }
    }

    /**
     * Re-runs the level-start seeding (§6.2): clears everything, then
     * explores the viewer's own tiles and their non-solid neighbours, any
     * neutral {@code ALWAYS_EXPLORED} terrain, clears the map border, and
     * finally reveals action points flagged for it.
     * <p>
     * The original "Fog of War" skirmish/network on-off option is not parsed
     * anywhere in this codebase's level loader, so this always seeds as if
     * fog were enabled (matching campaign behaviour) rather than half-wiring
     * a checkbox with no backing data.
     */
    public void seedLevelStart() {
        state.clearAll();
        seedOwnedAndAlwaysExploredTiles();
        clearBorder();
        seedActionPoints();

        Set<Point> dirty = state.drainDirtyTiles();
        if (!dirty.isEmpty()) {
            onTilesDirty.accept(dirty.toArray(new Point[0]));
        }
    }

    private void seedOwnedAndAlwaysExploredTiles() {
        int width = mapData.getWidth();
        int height = mapData.getHeight();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                IMapTileInformation tile = mapData.getTile(x, y);
                if (tile == null) {
                    continue;
                }
                Terrain terrain = kwdFile.getTerrain(tile.getTerrainId());
                if (tile.getOwnerId() == viewerId) {
                    FogOfWarRules.explore(state, x, y);
                    if (!terrain.getFlags().contains(Terrain.TerrainFlag.SOLID)) {
                        FogOfWarRules.explore(state, x - 1, y);
                        FogOfWarRules.explore(state, x + 1, y);
                        FogOfWarRules.explore(state, x, y - 1);
                        FogOfWarRules.explore(state, x, y + 1);
                    }
                } else if (tile.getOwnerId() == Player.NEUTRAL_PLAYER_ID
                        && terrain.getFlags().contains(Terrain.TerrainFlag.ALWAYS_EXPLORED)) {
                    FogOfWarRules.explore(state, x, y);
                }
            }
        }
    }

    private void clearBorder() {
        int width = mapData.getWidth();
        int height = mapData.getHeight();
        for (int x = 0; x < width; x++) {
            FogOfWarRules.unexplore(state, x, 0);
            FogOfWarRules.unexplore(state, x, height - 1);
        }
        for (int y = 0; y < height; y++) {
            FogOfWarRules.unexplore(state, 0, y);
            FogOfWarRules.unexplore(state, width - 1, y);
        }
    }

    private void seedActionPoints() {
        for (Thing.ActionPoint ap : kwdFile.getThings(Thing.ActionPoint.class)) {
            if (ap.getFlags().contains(Thing.ActionPoint.ActionPointFlag.REVEAL_THROUGH_FOG_OF_WAR)) {
                FogOfWarRules.exploreRect(state, new Point(ap.getStartX(), ap.getStartY()),
                        new Point(ap.getEndX(), ap.getEndY()));
            }
        }
    }

    /**
     * The {@code RESET_FOW} console command: re-seed as if the level had
     * just started.
     */
    public void resetToLevelStart() {
        seedLevelStart();
    }

    /**
     * The {@code REMOVE_FOW} cheat: reveal everything and stop hiding
     * anything further.
     */
    public void disableFogOfWar() {
        state.revealAll();
        state.setFogEnabled(false);

        Set<Point> dirty = state.drainDirtyTiles();
        if (!dirty.isEmpty()) {
            onTilesDirty.accept(dirty.toArray(new Point[0]));
        }
    }

    /**
     * §6.4: a tile now owned by the viewer becomes explored.
     */
    public void onTileOwnerChanged(List<Point> tiles) {
        for (Point p : tiles) {
            IMapTileInformation tile = mapData.getTile(p);
            if (tile != null && tile.getOwnerId() == viewerId) {
                FogOfWarRules.explore(state, p.x, p.y);
            }
        }
    }

    /**
     * §6.1/§6.4: a room built for the viewer whose {@code TileConstruction}
     * is one of the "whole room at once" kinds is explored in full.
     */
    public void onRoomBuilt(short ownerId, List<Point> tiles) {
        if (tiles.isEmpty() || ownerId != viewerId) {
            return;
        }
        IMapTileInformation sample = mapData.getTile(tiles.get(0));
        if (sample == null) {
            return;
        }
        Room room = kwdFile.getRoomByTerrain(sample.getTerrainId());
        if (room == null) {
            return;
        }
        Room.TileConstruction construction = room.getTileConstruction();
        if (construction == Room.TileConstruction._3_BY_3
                || construction == Room.TileConstruction._3_BY_3_ROTATED
                || construction == Room.TileConstruction._5_BY_5_ROTATED) {
            FogOfWarRules.exploreTiles(state, tiles);
        }
    }

    /**
     * §6.5: scripted reveal/conceal of an action point's tiles.
     */
    public void revealActionPointTiles(List<Point> points, boolean explore) {
        if (explore) {
            FogOfWarRules.exploreTiles(state, points);
        } else {
            FogOfWarRules.unexploreTiles(state, points);
        }
    }

    /**
     * §6.9/§2: while possessing a creature, its own tile explores every
     * tick, and {@link #isVisible(Point)} bypasses fog entirely (the
     * first-person camera renders without it; this keeps the two in sync).
     * Pass {@code null} when possession ends.
     */
    public void setPossessedCreature(EntityId entityId) {
        this.possessedCreature = entityId;
    }

    @Override
    public boolean isVisible(Point p) {
        if (possessedCreature != null) {
            return true;
        }
        IMapTileInformation tile = mapData.getTile(p);
        boolean revealThrough = tile != null
                && kwdFile.getTerrain(tile.getTerrainId()).getFlags().contains(Terrain.TerrainFlag.REVEAL_THROUGH_FOG_OF_WAR);
        return FogOfWarRules.isVisible(state, p.x, p.y, revealThrough);
    }

    @Override
    public boolean isExplored(Point p) {
        return state.isExplored(p.x, p.y);
    }

    @Override
    public boolean isPerceived(Point p) {
        return state.isPerceived(p.x, p.y);
    }

    @Override
    public boolean isHighlightable(Point p) {
        IMapTileInformation tile = mapData.getTile(p);
        if (tile == null) {
            return false;
        }
        if (!isExplored(p)) {
            return true;
        }
        return kwdFile.getTerrain(tile.getTerrainId()).getFlags().contains(Terrain.TerrainFlag.TAGGABLE);
    }

    private static final class PendingHeartReveal {

        final Point center;
        int elapsedIntervals;

        PendingHeartReveal(Point center) {
            this.center = center;
        }
    }

}
